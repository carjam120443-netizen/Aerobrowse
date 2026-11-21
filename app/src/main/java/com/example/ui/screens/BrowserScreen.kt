package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPasteGo
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.TrackerBlocker
import com.example.ui.browser.BrowserOverlay
import com.example.ui.browser.BrowserViewModel
import com.example.ui.components.AeroBackdrop
import com.example.ui.components.AeroGlassSurface
import com.example.ui.components.AeroIconButton
import com.example.ui.components.BrowserWebViewContainer
import com.example.ui.theme.LocalAeroPalette

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    val findState by viewModel.findState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val pendingDownload by viewModel.pendingDownload.collectAsStateWithLifecycle()

    val palette = LocalAeroPalette.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    var isEditingOmnibox by remember { mutableStateOf(false) }
    var omniboxField by remember {
        mutableStateOf(TextFieldValue(activeTab.url))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(activeTab.id, activeTab.url, isEditingOmnibox) {
        if (!isEditingOmnibox) {
            val initial = if (activeTab.url == TrackerBlocker.AERO_HOME_INTERNAL_URL) "" else activeTab.url
            omniboxField = TextFieldValue(
                text = initial,
                selection = TextRange(0, initial.length),
            )
        }
    }

    LaunchedEffect(isEditingOmnibox) {
        if (isEditingOmnibox) {
            focusRequester.requestFocus()
        }
    }

    // System back handling when overlays are closed
    BackHandler(enabled = overlay == BrowserOverlay.NONE && (isEditingOmnibox || findState.isActive || activeTab.canGoBack)) {
        when {
            isEditingOmnibox -> {
                isEditingOmnibox = false
                focusManager.clearFocus()
            }
            findState.isActive -> viewModel.closeFindInPage()
            activeTab.canGoBack -> viewModel.goBack()
        }
    }

    val isBookmarked = remember(bookmarks, activeTab.url) {
        bookmarks.any { it.url == activeTab.url }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AeroBackdrop(animateOrbs = activeTab.url == TrackerBlocker.AERO_HOME_INTERNAL_URL || overlay != BrowserOverlay.NONE)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Top Aero Glass Omnibox Bar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                AeroGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    elevated = true,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            // Privacy Shield Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.shieldActive.copy(alpha = 0.14f))
                                    .clickable {
                                        isEditingOmnibox = false
                                        focusManager.clearFocus()
                                        viewModel.setOverlay(BrowserOverlay.PRIVACY_SHIELD)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("privacy_shield_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = stringResource(R.string.cd_privacy_shield),
                                    tint = if (preferences.trackerBlockingEnabled) {
                                        palette.shieldActive
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp),
                                )
                                if (activeTab.blockedCount > 0) {
                                    Text(
                                        text = "${activeTab.blockedCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.shieldActive,
                                    )
                                }
                            }

                            // Omnibox Display / Edit Area
                            if (isEditingOmnibox) {
                                BasicTextField(
                                    value = omniboxField,
                                    onValueChange = { omniboxField = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            isEditingOmnibox = false
                                            focusManager.clearFocus()
                                            viewModel.navigateToInput(omniboxField.text)
                                        }
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            contentAlignment = Alignment.CenterStart,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            if (omniboxField.text.isEmpty()) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.omnibox_search_hint,
                                                        preferences.searchEngine.displayName,
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            innerTextField()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                        .focusRequester(focusRequester)
                                        .testTag("omnibox_text_field"),
                                )

                                if (omniboxField.text.isNotEmpty()) {
                                    AeroIconButton(
                                        icon = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_clear_input),
                                        onClick = { omniboxField = TextFieldValue("") },
                                        modifier = Modifier.testTag("omnibox_clear_button"),
                                    )
                                } else {
                                    AeroIconButton(
                                        icon = Icons.Rounded.ContentPasteGo,
                                        contentDescription = stringResource(R.string.cd_paste_and_go),
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                isEditingOmnibox = false
                                                focusManager.clearFocus()
                                                viewModel.navigateToInput(clip)
                                            }
                                        },
                                        modifier = Modifier.testTag("omnibox_paste_go_button"),
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            val initial = if (activeTab.url == TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                                                ""
                                            } else {
                                                activeTab.url
                                            }
                                            omniboxField = TextFieldValue(
                                                text = initial,
                                                selection = TextRange(0, initial.length),
                                            )
                                            isEditingOmnibox = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                        .testTag("omnibox_display_button"),
                                ) {
                                    Icon(
                                        imageVector = if (activeTab.isSecure) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                        contentDescription = null,
                                        tint = if (activeTab.isSecure) {
                                            palette.shieldActive
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = if (activeTab.url == TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                                            stringResource(
                                                R.string.omnibox_search_hint,
                                                preferences.searchEngine.displayName,
                                            )
                                        } else {
                                            TrackerBlocker.formatDisplayHost(activeTab.url)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                if (activeTab.url != TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                                    AeroIconButton(
                                        icon = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = if (isBookmarked) {
                                            stringResource(R.string.cd_bookmark_remove)
                                        } else {
                                            stringResource(R.string.cd_bookmark_add)
                                        },
                                        tint = if (isBookmarked) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        onClick = { viewModel.toggleCurrentBookmark() },
                                        modifier = Modifier.testTag("bookmark_toggle_button"),
                                    )
                                    AeroIconButton(
                                        icon = if (activeTab.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                        contentDescription = if (activeTab.isLoading) {
                                            stringResource(R.string.cd_stop_loading)
                                        } else {
                                            stringResource(R.string.cd_reload)
                                        },
                                        onClick = { viewModel.reloadOrStop() },
                                        modifier = Modifier.testTag("reload_stop_button"),
                                    )
                                }
                            }
                        }

                        if (activeTab.isLoading) {
                            LinearProgressIndicator(
                                progress = { (activeTab.progress / 100f).coerceIn(0.05f, 1f) },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                            )
                        }
                    }
                }
            }

            // Optional Find-in-Page Aero Bar
            AnimatedVisibility(visible = findState.isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    AeroGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        elevated = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            BasicTextField(
                                value = findState.query,
                                onValueChange = { viewModel.updateFindQuery(it) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (findState.query.isEmpty()) {
                                            Text(
                                                text = "Find on page...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                                    .testTag("find_in_page_input"),
                            )
                            Text(
                                text = "${findState.activeMatchIndex}/${findState.totalMatches}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AeroIconButton(
                                icon = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.cd_find_prev),
                                onClick = { viewModel.findNext(forward = false) },
                            )
                            AeroIconButton(
                                icon = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.cd_find_next),
                                onClick = { viewModel.findNext(forward = true) },
                            )
                            AeroIconButton(
                                icon = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_find_close),
                                onClick = { viewModel.closeFindInPage() },
                            )
                        }
                    }
                }
            }

            // Main Content Viewport (DuckDuckGo WebView by default, or Aero Quick Dial)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(palette.glassBorderHighlight, palette.glassBorderShadow)
                        ),
                        shape = RoundedCornerShape(22.dp),
                    ),
            ) {
                if (activeTab.url == TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                    AeroQuickDialView(
                        preferences = preferences,
                        bookmarks = bookmarks,
                        onSearchOrUrl = { viewModel.navigateToInput(it) },
                        onOpenBookmark = { viewModel.loadUrlInActiveTab(it) },
                        onSelectTheme = { viewModel.setAeroTheme(it) },
                        onOpenPrivacyShield = { viewModel.setOverlay(BrowserOverlay.PRIVACY_SHIELD) },
                    )
                } else {
                    BrowserWebViewContainer(
                        tab = activeTab,
                        registry = viewModel.webViewRegistry,
                    )
                }
            }

            // Bottom Aero Glass Navigation Dock
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                AeroGlassSurface(
                    shape = RoundedCornerShape(28.dp),
                    elevated = true,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        AeroIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_nav_back),
                            enabled = activeTab.canGoBack,
                            onClick = { viewModel.goBack() },
                            modifier = Modifier.testTag("nav_back_button"),
                        )
                        AeroIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = stringResource(R.string.cd_nav_forward),
                            enabled = activeTab.canGoForward,
                            onClick = { viewModel.goForward() },
                            modifier = Modifier.testTag("nav_forward_button"),
                        )

                        // Glowing Aero Shred / Burn Data Orb
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            palette.burnAccent.copy(alpha = 0.90f),
                                            Color(0xFFF97316),
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                .clickable { viewModel.shredSessionData() }
                                .testTag("shred_data_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = stringResource(R.string.cd_shred_session),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        AeroIconButton(
                            icon = Icons.Rounded.Home,
                            contentDescription = stringResource(R.string.cd_home_duckduckgo),
                            onClick = { viewModel.goHome() },
                            modifier = Modifier.testTag("home_button"),
                        )

                        // Tabs Counter Pill
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.setOverlay(BrowserOverlay.TAB_SWITCHER) }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("tabs_button"),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .border(
                                        width = 1.8.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(7.dp),
                                    ),
                            ) {
                                Text(
                                    text = "${tabs.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        AeroIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.cd_browser_menu),
                            onClick = { viewModel.setOverlay(BrowserOverlay.BROWSER_MENU) },
                            modifier = Modifier.testTag("menu_button"),
                        )
                    }
                }
            }
        }

        // Floating Toast / Feedback Pill
        AnimatedVisibility(
            visible = bannerMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 74.dp),
        ) {
            AeroGlassSurface(
                shape = RoundedCornerShape(50),
                elevated = true,
                shadowElevation = 12.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = palette.shieldActive,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = bannerMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Overlays
        when (overlay) {
            BrowserOverlay.NONE -> Unit

            BrowserOverlay.TAB_SWITCHER -> TabSwitcherOverlay(
                tabs = tabs,
                activeTabId = activeTab.id,
                searchEngineName = preferences.searchEngine.displayName,
                onSelectTab = { viewModel.selectTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onNewTab = { viewModel.newTab() },
                onShredAll = { viewModel.shredSessionData() },
                onCloseOverlay = { viewModel.setOverlay(BrowserOverlay.NONE) },
            )

            BrowserOverlay.PRIVACY_SHIELD -> PrivacyShieldSheet(
                activeTab = activeTab,
                preferences = preferences,
                onToggleTrackerBlocking = { viewModel.setTrackerBlocking(it) },
                onToggleHttpsOnly = { viewModel.setHttpsOnly(it) },
                onToggleThirdPartyCookies = { viewModel.setBlockThirdPartyCookies(it) },
                onToggleDoNotTrack = { viewModel.setDoNotTrack(it) },
                onShredData = { viewModel.shredSessionData() },
                onClose = { viewModel.setOverlay(BrowserOverlay.NONE) },
            )

            BrowserOverlay.THEME_STUDIO -> AeroThemeStudioSheet(
                currentTheme = preferences.aeroTheme,
                glassIntensity = preferences.glassIntensity,
                onSelectTheme = { viewModel.setAeroTheme(it) },
                onGlassIntensityChange = { viewModel.setGlassIntensity(it) },
                onClose = { viewModel.setOverlay(BrowserOverlay.NONE) },
            )

            BrowserOverlay.BOOKMARKS -> BookmarksSheet(
                bookmarks = bookmarks,
                onOpenUrl = { viewModel.loadUrlInActiveTab(it) },
                onOpenNewTab = { viewModel.newTab(it) },
                onDeleteBookmark = { viewModel.removeBookmark(it) },
                onClose = { viewModel.setOverlay(BrowserOverlay.NONE) },
            )

            BrowserOverlay.PRIVACY_SETTINGS -> PrivacySettingsSheet(
                preferences = preferences,
                onSelectSearchEngine = { viewModel.setSearchEngine(it) },
                onSelectStartPageMode = { viewModel.setStartPageMode(it) },
                onToggleJavascript = { viewModel.setJavascriptEnabled(it) },
                onToggleClearOnExit = { viewModel.setClearSessionOnExit(it) },
                onOpenThemeStudio = { viewModel.setOverlay(BrowserOverlay.THEME_STUDIO) },
                onClose = { viewModel.setOverlay(BrowserOverlay.NONE) },
            )

            BrowserOverlay.BROWSER_MENU -> BrowserMenuSheet(
                activeTab = activeTab,
                currentThemeName = preferences.aeroTheme.displayName,
                onOpenThemes = { viewModel.setOverlay(BrowserOverlay.THEME_STUDIO) },
                onOpenBookmarks = { viewModel.setOverlay(BrowserOverlay.BOOKMARKS) },
                onOpenSettings = { viewModel.setOverlay(BrowserOverlay.PRIVACY_SETTINGS) },
                onGoHome = { viewModel.goHome() },
                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                onOpenFindInPage = { viewModel.openFindInPage() },
                onSharePage = {
                    viewModel.setOverlay(BrowserOverlay.NONE)
                    if (activeTab.url != TrackerBlocker.AERO_HOME_INTERNAL_URL) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                            putExtra(Intent.EXTRA_TEXT, activeTab.url)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share page"))
                    }
                },
                onClose = { viewModel.setOverlay(BrowserOverlay.NONE) },
            )
        }

        // Download Hand-off Dialog (zero-permission safe system handoff)
        pendingDownload?.let { (downloadUrl, fileName) ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissPendingDownload() },
                title = { Text("Open Download") },
                text = {
                    Text("Open \"$fileName\" with your device download handler?")
                },
                confirmButton = {
                    Button(onClick = { viewModel.openDownloadExternally(downloadUrl) }) {
                        Text("Open")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissPendingDownload() }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
