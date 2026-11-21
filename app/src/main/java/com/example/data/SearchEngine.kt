package com.example.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class SearchEngine(
    val id: String,
    val displayName: String,
    val homeUrl: String,
    private val searchUrlFormat: String,
    val tagline: String,
    val isDefaultRecommended: Boolean = false,
) {
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        homeUrl = "https://duckduckgo.com/",
        searchUrlFormat = "https://duckduckgo.com/?q=%s",
        tagline = "Default private search, zero profiling",
        isDefaultRecommended = true,
    ),
    DUCKDUCKGO_LITE(
        id = "duckduckgo_lite",
        displayName = "DuckDuckGo Lite",
        homeUrl = "https://lite.duckduckgo.com/lite/",
        searchUrlFormat = "https://lite.duckduckgo.com/lite/?q=%s",
        tagline = "Ultra-lightweight HTML DuckDuckGo",
    ),
    STARTPAGE(
        id = "startpage",
        displayName = "Startpage",
        homeUrl = "https://www.startpage.com/",
        searchUrlFormat = "https://www.startpage.com/do/search?q=%s",
        tagline = "Private search proxy",
    ),
    BRAVE_SEARCH(
        id = "brave_search",
        displayName = "Brave Search",
        homeUrl = "https://search.brave.com/",
        searchUrlFormat = "https://search.brave.com/search?q=%s",
        tagline = "Independent private web index",
    ),
    QWANT(
        id = "qwant",
        displayName = "Qwant",
        homeUrl = "https://www.qwant.com/",
        searchUrlFormat = "https://www.qwant.com/?q=%s",
        tagline = "European privacy-respecting search",
    );

    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        return searchUrlFormat.replace("%s", encoded)
    }

    companion object {
        fun fromId(id: String?): SearchEngine =
            entries.firstOrNull { it.id == id } ?: DUCKDUCKGO
    }
}

enum class StartPageMode(
    val id: String,
    val displayName: String,
    val description: String,
) {
    DUCKDUCKGO_HOME(
        id = "duckduckgo_home",
        displayName = "DuckDuckGo (Default)",
        description = "Opens https://duckduckgo.com automatically on launch & new tabs",
    ),
    AERO_QUICK_DIAL(
        id = "aero_quick_dial",
        displayName = "Aero Quick Dial",
        description = "Translucent Aero glass bookmarks & DuckDuckGo search launchpad",
    );

    companion object {
        fun fromId(id: String?): StartPageMode =
            entries.firstOrNull { it.id == id } ?: DUCKDUCKGO_HOME
    }
}
