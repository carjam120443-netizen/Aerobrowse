package com.example.ui.browser

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://duckduckgo.com/",
    val title: String = "DuckDuckGo",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = true,
    val blockedCount: Int = 0,
    val blockedDomains: List<String> = emptyList(),
    val isDesktopMode: Boolean = false,
)

enum class BrowserOverlay {
    NONE,
    TAB_SWITCHER,
    PRIVACY_SHIELD,
    BOOKMARKS,
    THEME_STUDIO,
    PRIVACY_SETTINGS,
    BROWSER_MENU,
}

data class FindInPageState(
    val isActive: Boolean = false,
    val query: String = "",
    val activeMatchIndex: Int = 0,
    val totalMatches: Int = 0,
)
