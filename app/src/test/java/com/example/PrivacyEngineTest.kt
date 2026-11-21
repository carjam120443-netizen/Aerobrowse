package com.example

import com.example.data.PrivacyPreferences
import com.example.data.SearchEngine
import com.example.data.StartPageMode
import com.example.data.TrackerBlocker
import com.example.ui.theme.AeroThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyEngineTest {

    @Test
    fun `default preferences use DuckDuckGo search and start page`() {
        val prefs = PrivacyPreferences()
        assertEquals(SearchEngine.DUCKDUCKGO, prefs.searchEngine)
        assertEquals(StartPageMode.DUCKDUCKGO_HOME, prefs.startPageMode)
        assertEquals("https://duckduckgo.com/", prefs.resolveStartUrl())
        assertEquals(AeroThemePreset.SKY_AERO, prefs.aeroTheme)
    }

    @Test
    fun `omnibox query builds DuckDuckGo search URL by default`() {
        val url = TrackerBlocker.normalizeInputToUrl(
            input = "aero glass privacy",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsOnly = true,
        )
        assertEquals("https://duckduckgo.com/?q=aero+glass+privacy", url)
    }

    @Test
    fun `omnibox domain upgrades http to https when httpsOnly enabled`() {
        val url = TrackerBlocker.normalizeInputToUrl(
            input = "http://example.org/test",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsOnly = true,
        )
        assertEquals("https://example.org/test", url)

        val bareDomain = TrackerBlocker.normalizeInputToUrl(
            input = "wikipedia.org",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsOnly = true,
        )
        assertEquals("https://wikipedia.org", bareDomain)
    }

    @Test
    fun `tracker blocker blocks known third party trackers but preserves first party and duckduckgo`() {
        assertTrue(
            TrackerBlocker.shouldBlockRequest(
                pageUrl = "https://news.example.com/article",
                requestUrl = "https://www.google-analytics.com/analytics.js",
            )
        )
        assertTrue(
            TrackerBlocker.shouldBlockRequest(
                pageUrl = "https://shop.example.com",
                requestUrl = "https://ad.doubleclick.net/pixel",
            )
        )
        assertFalse(
            TrackerBlocker.shouldBlockRequest(
                pageUrl = "https://duckduckgo.com/?q=kotlin",
                requestUrl = "https://duckduckgo.com/assets/logo.svg",
            )
        )
        assertFalse(
            TrackerBlocker.shouldBlockRequest(
                pageUrl = "https://wikipedia.org/wiki/Privacy",
                requestUrl = "https://upload.wikipedia.org/image.png",
            )
        )
    }
}
