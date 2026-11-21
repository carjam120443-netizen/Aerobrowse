package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PrivacyPreferences
import com.example.data.SearchEngine
import com.example.data.StartPageMode
import com.example.data.TrackerBlocker
import com.example.data.local.BookmarkEntity
import com.example.ui.browser.BrowserTab
import com.example.ui.components.AeroBackdrop
import com.example.ui.components.AeroGlassSurface
import com.example.ui.components.AeroIconButton
import com.example.ui.theme.AeroThemePreset
import com.example.ui.theme.LocalAeroPalette

@Composable
fun TabSwitcherOverlay(
    tabs: List<BrowserTab>,
    activeTabId: String,
    searchEngineName: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onShredAll: () -> Unit,
    onCloseOverlay: () -> Unit,
) {
    BackHandler { onCloseOverlay() }
    val palette = LocalAeroPalette.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }

        AeroBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            AeroGlassSurface(
                shape = RoundedCornerShape(22.dp),
                elevated = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(
                            text = "Private Tabs (${tabs.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Starts on $searchEngineName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AeroIconButton(
                            icon = Icons.Rounded.LocalFireDepartment,
                            contentDescription = stringResource(R.string.cd_shred_session),
                            tint = palette.burnAccent,
                            onClick = onShredAll,
                            modifier = Modifier.testTag("tabs_shred_all_button"),
                        )
                        AeroIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close_sheet),
                            onClick = onCloseOverlay,
                            modifier = Modifier.testTag("tabs_close_overlay_button"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId
                    AeroGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        elevated = isActive,
                        shadowElevation = if (isActive) 10.dp else 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isActive) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(22.dp),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("tab_card_${tab.id}")
                            .clickable { onSelectTab(tab.id) },
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = if (tab.isSecure) Icons.Rounded.Lock else Icons.Rounded.Language,
                                        contentDescription = null,
                                        tint = if (tab.isSecure) palette.shieldActive else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = tab.title.ifBlank { TrackerBlocker.formatDisplayHost(tab.url) },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                AeroIconButton(
                                    icon = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_close_tab),
                                    onClick = { onCloseTab(tab.id) },
                                    modifier = Modifier.testTag("close_tab_${tab.id}"),
                                )
                            }
                            Text(
                                text = tab.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.shieldActive.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "${tab.blockedCount} trackers blocked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.shieldActive,
                                    )
                                }
                                if (isActive) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onNewTab,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("new_private_tab_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "New Private Tab ($searchEngineName)",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrivacyShieldSheet(
    activeTab: BrowserTab,
    preferences: PrivacyPreferences,
    onToggleTrackerBlocking: (Boolean) -> Unit,
    onToggleHttpsOnly: (Boolean) -> Unit,
    onToggleThirdPartyCookies: (Boolean) -> Unit,
    onToggleDoNotTrack: (Boolean) -> Unit,
    onShredData: () -> Unit,
    onClose: () -> Unit,
) {
    AeroSheetScaffold(
        title = "Aero Privacy Shield",
        subtitle = TrackerBlocker.formatDisplayHost(activeTab.url),
        onClose = onClose,
    ) {
        val palette = LocalAeroPalette.current
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            // Connection & Shield Hero Summary
            AeroGlassSurface(
                shape = RoundedCornerShape(20.dp),
                elevated = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(palette.shieldActive.copy(alpha = 0.18f)),
                    ) {
                        Icon(
                            imageVector = if (activeTab.isSecure) Icons.Rounded.Shield else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = palette.shieldActive,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeTab.isSecure) "Encrypted HTTPS Connection" else "Unencrypted HTTP Connection",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${activeTab.blockedCount} trackers intercepted on this page • ${preferences.totalTrackersBlocked} lifetime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (activeTab.blockedDomains.isNotEmpty()) {
                Text(
                    text = "Blocked Domains",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    activeTab.blockedDomains.forEach { domain ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.shieldActive.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.shieldActive,
                            )
                        }
                    }
                }
            }

            AeroToggleRow(
                icon = Icons.Rounded.Shield,
                title = "Tracker & Ad Blocker",
                subtitle = "Blocks third-party analytics, pixels & ad networks",
                checked = preferences.trackerBlockingEnabled,
                onCheckedChange = onToggleTrackerBlocking,
                testTag = "toggle_tracker_blocker",
            )
            AeroToggleRow(
                icon = Icons.Rounded.Lock,
                title = "HTTPS-Only Mode",
                subtitle = "Automatically upgrades HTTP requests to HTTPS",
                checked = preferences.httpsOnlyEnabled,
                onCheckedChange = onToggleHttpsOnly,
                testTag = "toggle_https_only",
            )
            AeroToggleRow(
                icon = Icons.Rounded.Cookie,
                title = "Block Third-Party Cookies",
                subtitle = "Prevents cross-site tracking cookies",
                checked = preferences.blockThirdPartyCookies,
                onCheckedChange = onToggleThirdPartyCookies,
                testTag = "toggle_3p_cookies",
            )
            AeroToggleRow(
                icon = Icons.Rounded.PrivacyTip,
                title = "Send DNT & Global Privacy Control",
                subtitle = "Includes DNT: 1 and Sec-GPC: 1 headers",
                checked = preferences.doNotTrackEnabled,
                onCheckedChange = onToggleDoNotTrack,
                testTag = "toggle_dnt_gpc",
            )

            Button(
                onClick = onShredData,
                colors = ButtonDefaults.buttonColors(containerColor = palette.burnAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("shield_shred_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Shred Cookies, Cache & Tabs")
            }
        }
    }
}

@Composable
fun AeroThemeStudioSheet(
    currentTheme: AeroThemePreset,
    glassIntensity: Float,
    onSelectTheme: (AeroThemePreset) -> Unit,
    onGlassIntensityChange: (Float) -> Unit,
    onClose: () -> Unit,
) {
    AeroSheetScaffold(
        title = "Aero Theme Studio",
        subtitle = "Customize Frutiger Aero glass & light refraction",
        onClose = onClose,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            AeroGlassSurface(
                shape = RoundedCornerShape(20.dp),
                elevated = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Glass Opacity",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${(glassIntensity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = glassIntensity,
                        onValueChange = onGlassIntensityChange,
                        valueRange = 0.50f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.testTag("glass_intensity_slider"),
                    )
                }
            }

            AeroThemePreset.entries.forEach { preset ->
                val selected = preset == currentTheme
                AeroGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    elevated = selected,
                    shadowElevation = if (selected) 8.dp else 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (selected) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(20.dp),
                                )
                            } else {
                                Modifier
                            }
                        )
                        .testTag("theme_option_${preset.id}")
                        .clickable { onSelectTheme(preset) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(14.dp),
                    ) {
                        // Glossy Aero Orb Swatch
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(preset.previewSwatchPrimary, preset.previewSwatchSecondary)
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp)
                                    .size(width = 28.dp, height = 12.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.45f)),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected theme",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    onOpenUrl: (String) -> Unit,
    onOpenNewTab: (String) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onClose: () -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(bookmarks, filter) {
        if (filter.isBlank()) bookmarks
        else bookmarks.filter {
            it.title.contains(filter, ignoreCase = true) ||
                it.url.contains(filter, ignoreCase = true)
        }
    }

    AeroSheetScaffold(
        title = "Local Bookmarks",
        subtitle = "Stored strictly on-device, never synced",
        onClose = onClose,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                singleLine = true,
                placeholder = { Text("Filter bookmarks...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bookmarks_filter_input"),
            )

            if (filtered.isEmpty()) {
                AeroGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = "No matching bookmarks",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Tap the star in the address bar to save any page locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 380.dp),
                ) {
                    items(filtered, key = { it.id }) { bookmark ->
                        AeroGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bookmark_row_${bookmark.id}")
                                .clickable { onOpenUrl(bookmark.url) },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = bookmark.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                AeroIconButton(
                                    icon = Icons.Rounded.OpenInNew,
                                    contentDescription = "Open in new tab",
                                    onClick = { onOpenNewTab(bookmark.url) },
                                )
                                AeroIconButton(
                                    icon = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete bookmark",
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { onDeleteBookmark(bookmark.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsSheet(
    preferences: PrivacyPreferences,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onSelectStartPageMode: (StartPageMode) -> Unit,
    onToggleJavascript: (Boolean) -> Unit,
    onToggleClearOnExit: (Boolean) -> Unit,
    onOpenThemeStudio: () -> Unit,
    onClose: () -> Unit,
) {
    AeroSheetScaffold(
        title = "Privacy & Search",
        subtitle = "DuckDuckGo defaults & zero-telemetry browsing",
        onClose = onClose,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = "Default Search Engine",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            SearchEngine.entries.forEach { engine ->
                val selected = engine == preferences.searchEngine
                AeroGlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    elevated = selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_engine_${engine.id}")
                        .clickable { onSelectSearchEngine(engine) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectSearchEngine(engine) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = engine.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                if (engine.isDefaultRecommended) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = engine.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Default Start Page",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            StartPageMode.entries.forEach { mode ->
                val selected = mode == preferences.startPageMode
                AeroGlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    elevated = selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_page_${mode.id}")
                        .clickable { onSelectStartPageMode(mode) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectStartPageMode(mode) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            AeroToggleRow(
                icon = Icons.Rounded.Language,
                title = "Enable JavaScript",
                subtitle = "Disable for maximum hardening on static sites",
                checked = preferences.javascriptEnabled,
                onCheckedChange = onToggleJavascript,
                testTag = "toggle_javascript",
            )
            AeroToggleRow(
                icon = Icons.Rounded.LocalFireDepartment,
                title = "Shred Session on Exit",
                subtitle = "Automatically clear cookies & cache when closing",
                checked = preferences.clearSessionOnExit,
                onCheckedChange = onToggleClearOnExit,
                testTag = "toggle_clear_on_exit",
            )

            OutlinedButton(
                onClick = onOpenThemeStudio,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_open_themes"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Customize Aero Theme (${preferences.aeroTheme.displayName})")
            }
        }
    }
}

@Composable
fun BrowserMenuSheet(
    activeTab: BrowserTab,
    currentThemeName: String,
    onOpenThemes: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onSharePage: () -> Unit,
    onClose: () -> Unit,
) {
    AeroSheetScaffold(
        title = "AeroBrowser Menu",
        subtitle = activeTab.title,
        onClose = onClose,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MenuActionTile(
                    icon = Icons.Rounded.Palette,
                    label = "Aero Themes",
                    badge = currentThemeName,
                    onClick = onOpenThemes,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_aero_themes"),
                )
                MenuActionTile(
                    icon = Icons.Rounded.Bookmark,
                    label = stringResource(R.string.label_bookmarks),
                    onClick = onOpenBookmarks,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_bookmarks"),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MenuActionTile(
                    icon = Icons.Rounded.Home,
                    label = stringResource(R.string.label_home_start),
                    onClick = onGoHome,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_duckduckgo_home"),
                )
                MenuActionTile(
                    icon = Icons.Rounded.Settings,
                    label = stringResource(R.string.label_privacy_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_privacy_settings"),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MenuActionTile(
                    icon = Icons.Rounded.DesktopWindows,
                    label = stringResource(R.string.label_desktop_site),
                    badge = if (activeTab.isDesktopMode) "On" else "Off",
                    selected = activeTab.isDesktopMode,
                    onClick = onToggleDesktopMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_desktop_mode"),
                )
                MenuActionTile(
                    icon = Icons.Rounded.FindInPage,
                    label = stringResource(R.string.label_find_in_page),
                    onClick = onOpenFindInPage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_find_in_page"),
                )
                MenuActionTile(
                    icon = Icons.Rounded.Share,
                    label = stringResource(R.string.label_share_page),
                    onClick = onSharePage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_share_page"),
                )
            }
        }
    }
}

@Composable
private fun MenuActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    selected: Boolean = false,
) {
    AeroGlassSurface(
        shape = RoundedCornerShape(18.dp),
        elevated = selected,
        modifier = modifier
            .heightIn(min = 82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AeroToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    AeroGlassSurface(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag),
            )
        }
    }
}

@Composable
private fun AeroSheetScaffold(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler { onClose() }
    val palette = LocalAeroPalette.current

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(onClick = onClose)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        AeroGlassSurface(
            shape = RoundedCornerShape(28.dp),
            elevated = true,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .clickable(enabled = false, onClick = {}),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AeroIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close_sheet),
                        onClick = onClose,
                        containerColor = palette.glassBorderShadow.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("sheet_close_button"),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}
