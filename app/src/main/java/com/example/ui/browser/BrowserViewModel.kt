package com.example.ui.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.data.PrivacyPreferences
import com.example.data.SearchEngine
import com.example.data.StartPageMode
import com.example.data.TrackerBlocker
import com.example.data.local.AeroDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkRepository
import com.example.ui.theme.AeroThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)
    private val bookmarkRepository = BookmarkRepository(
        AeroDatabase.getInstance(application).bookmarkDao()
    )

    private val _preferences = MutableStateFlow(PrivacyPreferences())
    val preferences: StateFlow<PrivacyPreferences> = _preferences.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val initialTab = BrowserTab(
        url = SearchEngine.DUCKDUCKGO.homeUrl,
        title = SearchEngine.DUCKDUCKGO.displayName,
        isSecure = true,
    )

    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<BrowserTab> = combine(_tabs, _activeTabId) { list, activeId ->
        list.firstOrNull { it.id == activeId } ?: list.first()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialTab)

    private val _overlay = MutableStateFlow(BrowserOverlay.NONE)
    val overlay: StateFlow<BrowserOverlay> = _overlay.asStateFlow()

    private val _findState = MutableStateFlow(FindInPageState())
    val findState: StateFlow<FindInPageState> = _findState.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _pendingDownload = MutableStateFlow<Pair<String, String>?>(null)
    val pendingDownload: StateFlow<Pair<String, String>?> = _pendingDownload.asStateFlow()

    private var hasUserNavigated = false

    val webViewRegistry: WebViewRegistry = WebViewRegistry(
        appContext = application.applicationContext,
        callbacks = object : WebViewCallbacks {
            override fun currentPreferences(): PrivacyPreferences = _preferences.value

            override fun onPageStarted(tabId: String, url: String) {
                viewModelScope.launch {
                    updateTab(tabId) { tab ->
                        val isNewSite = TrackerBlocker.extractHost(tab.url) != TrackerBlocker.extractHost(url)
                        tab.copy(
                            url = url,
                            isLoading = true,
                            progress = 15,
                            isSecure = url.startsWith("https://", ignoreCase = true) ||
                                url == TrackerBlocker.AERO_HOME_INTERNAL_URL,
                            blockedCount = if (isNewSite) 0 else tab.blockedCount,
                            blockedDomains = if (isNewSite) emptyList() else tab.blockedDomains,
                        )
                    }
                }
            }

            override fun onPageFinished(
                tabId: String,
                url: String,
                title: String?,
                canGoBack: Boolean,
                canGoForward: Boolean,
            ) {
                viewModelScope.launch {
                    updateTab(tabId) { tab ->
                        tab.copy(
                            url = url,
                            title = title?.takeIf { it.isNotBlank() } ?: TrackerBlocker.formatDisplayHost(url),
                            isLoading = false,
                            progress = 100,
                            canGoBack = canGoBack,
                            canGoForward = canGoForward,
                            isSecure = url.startsWith("https://", ignoreCase = true) ||
                                url == TrackerBlocker.AERO_HOME_INTERNAL_URL,
                        )
                    }
                }
            }

            override fun onProgressChanged(tabId: String, progress: Int) {
                viewModelScope.launch {
                    updateTab(tabId) { tab ->
                        tab.copy(
                            progress = progress,
                            isLoading = progress in 1..99,
                        )
                    }
                }
            }

            override fun onTitleReceived(tabId: String, title: String) {
                viewModelScope.launch {
                    updateTab(tabId) { it.copy(title = title) }
                }
            }

            override fun onTrackerBlocked(tabId: String, domain: String) {
                viewModelScope.launch {
                    updateTab(tabId) { tab ->
                        val updatedDomains = if (tab.blockedDomains.contains(domain)) {
                            tab.blockedDomains
                        } else {
                            (tab.blockedDomains + domain).takeLast(30)
                        }
                        tab.copy(
                            blockedCount = tab.blockedCount + 1,
                            blockedDomains = updatedDomains,
                        )
                    }
                    preferencesRepository.incrementTrackersBlocked(1)
                }
            }

            override fun onFindResult(tabId: String, activeMatchOrdinal: Int, numberOfMatches: Int) {
                if (tabId == _activeTabId.value) {
                    _findState.update {
                        it.copy(
                            activeMatchIndex = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0,
                            totalMatches = numberOfMatches,
                        )
                    }
                }
            }

            override fun onDownloadRequested(url: String, fileName: String) {
                _pendingDownload.value = url to fileName
            }
        }
    )

    init {
        viewModelScope.launch {
            bookmarkRepository.ensureSeeded()
        }
        viewModelScope.launch {
            val firstPrefs = preferencesRepository.preferencesFlow.first()
            _preferences.value = firstPrefs
            if (!hasUserNavigated && firstPrefs.startPageMode == StartPageMode.AERO_QUICK_DIAL) {
                updateTab(initialTab.id) {
                    it.copy(
                        url = TrackerBlocker.AERO_HOME_INTERNAL_URL,
                        title = "Aero Quick Dial",
                        isSecure = true,
                    )
                }
            } else if (!hasUserNavigated && firstPrefs.searchEngine != SearchEngine.DUCKDUCKGO) {
                val home = firstPrefs.resolveStartUrl()
                updateTab(initialTab.id) {
                    it.copy(url = home, title = firstPrefs.searchEngine.displayName)
                }
                webViewRegistry.loadUrl(initialTab.id, home)
            }

            preferencesRepository.preferencesFlow.collect { prefs ->
                _preferences.value = prefs
                val desktopIds = _tabs.value.filter { it.isDesktopMode }.map { it.id }.toSet()
                webViewRegistry.syncAllSettings(prefs, desktopIds)
            }
        }
    }

    fun navigateToInput(input: String) {
        hasUserNavigated = true
        val prefs = _preferences.value
        val targetUrl = TrackerBlocker.normalizeInputToUrl(
            input = input,
            searchEngine = prefs.searchEngine,
            httpsOnly = prefs.httpsOnlyEnabled,
        )
        loadUrlInActiveTab(targetUrl)
    }

    fun loadUrlInActiveTab(url: String) {
        hasUserNavigated = true
        val currentId = _activeTabId.value
        val finalUrl = if (url == TrackerBlocker.AERO_HOME_INTERNAL_URL) {
            url
        } else {
            TrackerBlocker.upgradeUrlIfHttpsOnly(url, _preferences.value.httpsOnlyEnabled)
        }
        updateTab(currentId) {
            it.copy(
                url = finalUrl,
                title = TrackerBlocker.formatDisplayHost(finalUrl),
                isSecure = finalUrl.startsWith("https://", ignoreCase = true) ||
                    finalUrl == TrackerBlocker.AERO_HOME_INTERNAL_URL,
                blockedCount = 0,
                blockedDomains = emptyList(),
            )
        }
        if (finalUrl != TrackerBlocker.AERO_HOME_INTERNAL_URL) {
            webViewRegistry.loadUrl(currentId, finalUrl)
        }
        _overlay.value = BrowserOverlay.NONE
    }

    fun goHome() {
        loadUrlInActiveTab(_preferences.value.resolveStartUrl())
    }

    fun goBack() {
        val currentId = _activeTabId.value
        val webView = webViewRegistry.getExisting(currentId)
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        }
    }

    fun goForward() {
        val currentId = _activeTabId.value
        val webView = webViewRegistry.getExisting(currentId)
        if (webView != null && webView.canGoForward()) {
            webView.goForward()
        }
    }

    fun reloadOrStop() {
        val tab = activeTab.value
        val webView = webViewRegistry.getExisting(tab.id) ?: return
        if (tab.isLoading) {
            webView.stopLoading()
            updateTab(tab.id) { it.copy(isLoading = false, progress = 100) }
        } else if (tab.url != TrackerBlocker.AERO_HOME_INTERNAL_URL) {
            webView.reload()
        }
    }

    fun newTab(url: String? = null) {
        hasUserNavigated = true
        val startUrl = url ?: _preferences.value.resolveStartUrl()
        val tab = BrowserTab(
            url = startUrl,
            title = if (startUrl == TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                "Aero Quick Dial"
            } else {
                TrackerBlocker.formatDisplayHost(startUrl)
            },
            isSecure = startUrl.startsWith("https://", ignoreCase = true) ||
                startUrl == TrackerBlocker.AERO_HOME_INTERNAL_URL,
        )
        _tabs.update { it + tab }
        _activeTabId.value = tab.id
        _overlay.value = BrowserOverlay.NONE
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        _overlay.value = BrowserOverlay.NONE
        closeFindInPage()
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            // Reset single remaining tab back to start page
            webViewRegistry.destroyTab(tabId)
            val startUrl = _preferences.value.resolveStartUrl()
            val fresh = BrowserTab(
                url = startUrl,
                title = if (startUrl == TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                    "Aero Quick Dial"
                } else {
                    _preferences.value.searchEngine.displayName
                },
                isSecure = true,
            )
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            _overlay.value = BrowserOverlay.NONE
            return
        }

        val index = currentTabs.indexOfFirst { it.id == tabId }
        webViewRegistry.destroyTab(tabId)
        val remaining = currentTabs.filterNot { it.id == tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            val nextIndex = (index - 1).coerceAtLeast(0).coerceAtMost(remaining.lastIndex)
            _activeTabId.value = remaining[nextIndex].id
        }
    }

    fun toggleCurrentBookmark() {
        val tab = activeTab.value
        if (tab.url == TrackerBlocker.AERO_HOME_INTERNAL_URL) return
        viewModelScope.launch {
            val exists = bookmarks.value.any { it.url == tab.url }
            if (exists) {
                bookmarkRepository.removeByUrl(tab.url)
                showBanner("Removed from bookmarks")
            } else {
                bookmarkRepository.addBookmark(tab.title, tab.url)
                showBanner("Saved to local bookmarks")
            }
        }
    }

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.removeById(id)
        }
    }

    fun toggleDesktopMode() {
        val tab = activeTab.value
        val next = !tab.isDesktopMode
        updateTab(tab.id) { it.copy(isDesktopMode = next) }
        webViewRegistry.setDesktopMode(tab.id, next)
        _overlay.value = BrowserOverlay.NONE
    }

    fun openFindInPage() {
        _overlay.value = BrowserOverlay.NONE
        _findState.value = FindInPageState(isActive = true)
    }

    fun updateFindQuery(query: String) {
        _findState.update { it.copy(query = query) }
        val webView = webViewRegistry.getExisting(_activeTabId.value) ?: return
        if (query.isBlank()) {
            webView.clearMatches()
            _findState.update { it.copy(activeMatchIndex = 0, totalMatches = 0) }
        } else {
            webView.findAllAsync(query)
        }
    }

    fun findNext(forward: Boolean) {
        webViewRegistry.getExisting(_activeTabId.value)?.findNext(forward)
    }

    fun closeFindInPage() {
        webViewRegistry.getExisting(_activeTabId.value)?.clearMatches()
        _findState.value = FindInPageState(isActive = false)
    }

    fun shredSessionData() {
        webViewRegistry.shredAllData()
        val startUrl = _preferences.value.resolveStartUrl()
        val freshTab = BrowserTab(
            url = startUrl,
            title = if (startUrl == TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                "Aero Quick Dial"
            } else {
                _preferences.value.searchEngine.displayName
            },
            isSecure = true,
        )
        _tabs.value = listOf(freshTab)
        _activeTabId.value = freshTab.id
        _overlay.value = BrowserOverlay.NONE
        closeFindInPage()
        showBanner("Cookies, cache & history shredded")
    }

    fun setOverlay(target: BrowserOverlay) {
        _overlay.value = target
    }

    fun dismissPendingDownload() {
        _pendingDownload.value = null
    }

    fun openDownloadExternally(url: String) {
        _pendingDownload.value = null
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            showBanner("No system handler available for download")
        }
    }

    fun setAeroTheme(preset: AeroThemePreset) {
        viewModelScope.launch { preferencesRepository.setAeroTheme(preset) }
    }

    fun setGlassIntensity(intensity: Float) {
        viewModelScope.launch { preferencesRepository.setGlassIntensity(intensity) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { preferencesRepository.setSearchEngine(engine) }
    }

    fun setStartPageMode(mode: StartPageMode) {
        viewModelScope.launch { preferencesRepository.setStartPageMode(mode) }
    }

    fun setTrackerBlocking(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setTrackerBlocking(enabled) }
    }

    fun setHttpsOnly(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHttpsOnly(enabled) }
    }

    fun setBlockThirdPartyCookies(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBlockThirdPartyCookies(enabled) }
    }

    fun setDoNotTrack(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDoNotTrack(enabled) }
    }

    fun setJavascriptEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setJavascriptEnabled(enabled) }
    }

    fun setClearSessionOnExit(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setClearSessionOnExit(enabled) }
    }

    fun clearSessionIfConfiguredOnExit() {
        if (_preferences.value.clearSessionOnExit) {
            webViewRegistry.shredAllData()
        }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    private fun showBanner(message: String) {
        _bannerMessage.value = message
        viewModelScope.launch {
            delay(2600)
            if (_bannerMessage.value == message) {
                _bannerMessage.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_preferences.value.clearSessionOnExit) {
            webViewRegistry.shredAllData()
        } else {
            webViewRegistry.destroyAll()
        }
    }
}
