package com.example.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.PrivacyPreferences
import com.example.data.TrackerBlocker
import java.io.ByteArrayInputStream

interface WebViewCallbacks {
    fun currentPreferences(): PrivacyPreferences
    fun onPageStarted(tabId: String, url: String)
    fun onPageFinished(tabId: String, url: String, title: String?, canGoBack: Boolean, canGoForward: Boolean)
    fun onProgressChanged(tabId: String, progress: Int)
    fun onTitleReceived(tabId: String, title: String)
    fun onTrackerBlocked(tabId: String, domain: String)
    fun onFindResult(tabId: String, activeMatchOrdinal: Int, numberOfMatches: Int)
    fun onDownloadRequested(url: String, fileName: String)
}

class WebViewRegistry(
    private val appContext: Context,
    private val callbacks: WebViewCallbacks,
) {
    private val webViews = mutableMapOf<String, WebView>()
    private val defaultUserAgents = mutableMapOf<String, String>()

    fun getOrCreateWebView(tab: BrowserTab): WebView {
        val existing = webViews[tab.id]
        if (existing != null) {
            syncSettings(existing, tab.isDesktopMode, callbacks.currentPreferences())
            return existing
        }

        val webView = createConfiguredWebView(tab)
        webViews[tab.id] = webView
        if (tab.url != TrackerBlocker.AERO_HOME_INTERNAL_URL) {
            loadUrl(tab.id, tab.url)
        }
        return webView
    }

    fun getExisting(tabId: String): WebView? = webViews[tabId]

    fun loadUrl(tabId: String, url: String) {
        val webView = webViews[tabId] ?: return
        if (url == TrackerBlocker.AERO_HOME_INTERNAL_URL) return
        val prefs = callbacks.currentPreferences()
        val finalUrl = TrackerBlocker.upgradeUrlIfHttpsOnly(url, prefs.httpsOnlyEnabled)
        if (prefs.doNotTrackEnabled) {
            webView.loadUrl(finalUrl, mapOf("DNT" to "1", "Sec-GPC" to "1"))
        } else {
            webView.loadUrl(finalUrl)
        }
    }

    fun syncAllSettings(prefs: PrivacyPreferences, desktopTabIds: Set<String>) {
        webViews.forEach { (tabId, webView) ->
            syncSettings(webView, desktopTabIds.contains(tabId), prefs)
        }
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val webView = webViews[tabId] ?: return
        syncSettings(webView, enabled, callbacks.currentPreferences())
        webView.reload()
    }

    fun destroyTab(tabId: String) {
        webViews.remove(tabId)?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.clearHistory()
            webView.destroy()
        }
        defaultUserAgents.remove(tabId)
    }

    fun shredAllData() {
        val allIds = webViews.keys.toList()
        allIds.forEach { id ->
            webViews[id]?.apply {
                stopLoading()
                clearCache(true)
                clearHistory()
                clearFormData()
            }
            destroyTab(id)
        }
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            removeSessionCookies(null)
            flush()
        }
    }

    fun destroyAll() {
        val allIds = webViews.keys.toList()
        allIds.forEach { destroyTab(it) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(tab: BrowserTab): WebView {
        val prefs = callbacks.currentPreferences()
        val webView = WebView(appContext)
        defaultUserAgents[tab.id] = webView.settings.userAgentString

        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        syncSettings(webView, tab.isDesktopMode, prefs)

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting) {
                callbacks.onFindResult(tab.id, activeMatchOrdinal, numberOfMatches)
            }
        }

        webView.setDownloadListener(
            DownloadListener { url, _, contentDisposition, mimeType, _ ->
                val guessedName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
                callbacks.onDownloadRequested(url, guessedName)
            }
        )

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val activePrefs = callbacks.currentPreferences()
                if (!activePrefs.trackerBlockingEnabled) return null
                val requestUrl = request?.url?.toString() ?: return null
                // Never block main frame navigation itself
                if (request.isForMainFrame) return null

                val currentPageUrl = view?.let {
                    // Use request header Referer or host check cleanly without main-thread view access
                    request.requestHeaders?.get("Referer")
                }
                if (TrackerBlocker.shouldBlockRequest(currentPageUrl, requestUrl)) {
                    val blockedHost = TrackerBlocker.extractHost(requestUrl)
                    if (blockedHost != null) {
                        callbacks.onTrackerBlocked(tab.id, blockedHost)
                    }
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        ByteArrayInputStream(ByteArray(0)),
                    )
                }
                return null
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false
                if (scheme == "http" || scheme == "https") {
                    val activePrefs = callbacks.currentPreferences()
                    val raw = uri.toString()
                    val upgraded = TrackerBlocker.upgradeUrlIfHttpsOnly(raw, activePrefs.httpsOnlyEnabled)
                    if (upgraded != raw) {
                        if (activePrefs.doNotTrackEnabled) {
                            view?.loadUrl(upgraded, mapOf("DNT" to "1", "Sec-GPC" to "1"))
                        } else {
                            view?.loadUrl(upgraded)
                        }
                        return true
                    }
                    return false
                }

                // Handle external schemes like mailto:, tel: via safe system Intent
                if (scheme == "mailto" || scheme == "tel" || scheme == "sms") {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        appContext.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url != null) {
                    callbacks.onPageStarted(tab.id, url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && view != null) {
                    callbacks.onPageFinished(
                        tabId = tab.id,
                        url = url,
                        title = view.title,
                        canGoBack = view.canGoBack(),
                        canGoForward = view.canGoForward(),
                    )
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                callbacks.onProgressChanged(tab.id, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    callbacks.onTitleReceived(tab.id, title)
                }
            }
        }

        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun syncSettings(
        webView: WebView,
        isDesktopMode: Boolean,
        prefs: PrivacyPreferences,
    ) {
        val settings = webView.settings
        settings.javaScriptEnabled = prefs.javascriptEnabled
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setGeolocationEnabled(false)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mixedContentMode = if (prefs.httpsOnlyEnabled) {
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        } else {
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }

        val tabDefaultUa = webViews.entries.firstOrNull { it.value === webView }?.key?.let { defaultUserAgents[it] }
        if (isDesktopMode) {
            settings.userAgentString =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        } else if (tabDefaultUa != null) {
            settings.userAgentString = tabDefaultUa
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, !prefs.blockThirdPartyCookies)
    }
}
