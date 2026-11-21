package com.example.data

import java.net.URI
import java.util.Locale

object TrackerBlocker {

    const val AERO_HOME_INTERNAL_URL = "aero://home"

    private val blockedTrackerDomains: Set<String> = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "analytics.google.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "an.facebook.com",
        "ads-twitter.com",
        "analytics.twitter.com",
        "ads.linkedin.com",
        "snap.licdn.com",
        "px.ads.linkedin.com",
        "analytics.tiktok.com",
        "business-api.tiktok.com",
        "scorecardresearch.com",
        "sb.scorecardresearch.com",
        "quantserve.com",
        "pixel.quantserve.com",
        "taboola.com",
        "trc.taboola.com",
        "outbrain.com",
        "widgets.outbrain.com",
        "hotjar.com",
        "static.hotjar.com",
        "script.hotjar.com",
        "mixpanel.com",
        "api.mixpanel.com",
        "cdn.mxpnl.com",
        "segment.io",
        "segment.com",
        "api.segment.io",
        "cdn.segment.com",
        "amplitude.com",
        "api.amplitude.com",
        "cdn.amplitude.com",
        "criteo.com",
        "criteo.net",
        "static.criteo.net",
        "adnxs.com",
        "ib.adnxs.com",
        "pubmatic.com",
        "ads.pubmatic.com",
        "rubiconproject.com",
        "fastlane.rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "demdex.net",
        "omtrdc.net",
        "2o7.net",
        "krxd.net",
        "bluekai.com",
        "mathtag.com",
        "rlcdn.com",
        "tapad.com",
        "exelator.com",
        "addthis.com",
        "sharethis.com",
        "appsflyer.com",
        "adjust.com",
        "branch.io",
        "app.link",
        "chartbeat.com",
        "static.chartbeat.com",
        "newrelic.com",
        "js-agent.newrelic.com",
        "bam.nr-data.net",
        "fullstory.com",
        "rs.fullstory.com",
        "heap.io",
        "heapanalytics.com",
        "kissmetrics.com",
        "optimizely.com",
        "cdn.optimizely.com",
        "crazyegg.com",
        "mouseflow.com",
        "clarity.ms",
        "bat.bing.com",
        "mc.yandex.ru",
        "hm.baidu.com",
        "ads.yahoo.com",
        "analytics.yahoo.com",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "fls-na.amazon.com",
        "moatads.com",
        "doubleverify.com",
        "adsafeprotected.com",
        "zemanta.com",
        "revcontent.com",
        "mgid.com",
        "buysellads.com",
        "carbonads.com",
        "sentry-cdn.com",
        "browser.sentry-cdn.com",
        "bugsnag.com",
    )

    private val domainRegex = Regex(
        pattern = "^(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(?::\\d{1,5})?(?:/.*)?$"
    )
    private val ipOrLocalhostRegex = Regex(
        pattern = "^(?:localhost|(?:\\d{1,3}\\.){3}\\d{1,3})(?::\\d{1,5})?(?:/.*)?$",
        option = RegexOption.IGNORE_CASE
    )

    fun extractHost(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = URI(url.trim())
            uri.host?.lowercase(Locale.US)?.removePrefix("www.")
        } catch (_: Exception) {
            null
        }
    }

    fun shouldBlockRequest(pageUrl: String?, requestUrl: String?): Boolean {
        val reqHost = extractHost(requestUrl) ?: return false
        val pageHost = extractHost(pageUrl)

        // Never block same-site / first-party requests
        if (pageHost != null && (reqHost == pageHost || reqHost.endsWith(".$pageHost") || pageHost.endsWith(".$reqHost"))) {
            return false
        }

        // Never break DuckDuckGo search or static resources
        if (reqHost == "duckduckgo.com" || reqHost.endsWith(".duckduckgo.com")) {
            return false
        }

        return matchesBlocklist(reqHost)
    }

    fun matchesBlocklist(host: String): Boolean {
        val normalized = host.lowercase(Locale.US).removePrefix("www.")
        if (blockedTrackerDomains.contains(normalized)) return true
        var dotIndex = normalized.indexOf('.')
        while (dotIndex != -1) {
            val parent = normalized.substring(dotIndex + 1)
            if (blockedTrackerDomains.contains(parent)) return true
            dotIndex = normalized.indexOf('.', dotIndex + 1)
        }
        return false
    }

    fun upgradeUrlIfHttpsOnly(url: String, httpsOnly: Boolean): String {
        if (!httpsOnly) return url
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true)) return trimmed
        val host = extractHost(trimmed) ?: return trimmed
        if (host == "localhost" || host.matches(Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$"))) {
            return trimmed
        }
        return "https://" + trimmed.substring(7)
    }

    fun normalizeInputToUrl(
        input: String,
        searchEngine: SearchEngine,
        httpsOnly: Boolean,
    ): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return searchEngine.homeUrl
        if (trimmed.equals(AERO_HOME_INTERNAL_URL, ignoreCase = true)) {
            return AERO_HOME_INTERNAL_URL
        }
        if (trimmed.startsWith("about:", ignoreCase = true)) {
            return trimmed
        }
        if (trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            return upgradeUrlIfHttpsOnly(trimmed, httpsOnly)
        }

        val hasWhitespace = trimmed.any { it.isWhitespace() }
        if (!hasWhitespace && (domainRegex.matches(trimmed) || ipOrLocalhostRegex.matches(trimmed))) {
            val scheme = if (ipOrLocalhostRegex.matches(trimmed) || !httpsOnly) "http://" else "https://"
            return scheme + trimmed
        }

        return searchEngine.buildSearchUrl(trimmed)
    }

    fun formatDisplayHost(url: String): String {
        if (url == AERO_HOME_INTERNAL_URL) return "Aero Quick Dial"
        val host = extractHost(url)
        return host ?: url
    }
}
