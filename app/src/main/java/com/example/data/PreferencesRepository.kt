package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AeroThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aeroPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "aero_privacy_prefs")

data class PrivacyPreferences(
    val aeroTheme: AeroThemePreset = AeroThemePreset.SKY_AERO,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val startPageMode: StartPageMode = StartPageMode.DUCKDUCKGO_HOME,
    val trackerBlockingEnabled: Boolean = true,
    val httpsOnlyEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val doNotTrackEnabled: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val clearSessionOnExit: Boolean = false,
    val glassIntensity: Float = 0.85f,
    val totalTrackersBlocked: Int = 0,
) {
    fun resolveStartUrl(): String = when (startPageMode) {
        StartPageMode.DUCKDUCKGO_HOME -> searchEngine.homeUrl
        StartPageMode.AERO_QUICK_DIAL -> TrackerBlocker.AERO_HOME_INTERNAL_URL
    }
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val AERO_THEME = stringPreferencesKey("aero_theme")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val START_PAGE_MODE = stringPreferencesKey("start_page_mode")
        val TRACKER_BLOCKING = booleanPreferencesKey("tracker_blocking")
        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val CLEAR_ON_EXIT = booleanPreferencesKey("clear_on_exit")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val TOTAL_TRACKERS_BLOCKED = intPreferencesKey("total_trackers_blocked")
    }

    val preferencesFlow: Flow<PrivacyPreferences> = context.aeroPrefsDataStore.data.map { prefs ->
        PrivacyPreferences(
            aeroTheme = AeroThemePreset.fromId(prefs[Keys.AERO_THEME]),
            searchEngine = SearchEngine.fromId(prefs[Keys.SEARCH_ENGINE]),
            startPageMode = StartPageMode.fromId(prefs[Keys.START_PAGE_MODE]),
            trackerBlockingEnabled = prefs[Keys.TRACKER_BLOCKING] ?: true,
            httpsOnlyEnabled = prefs[Keys.HTTPS_ONLY] ?: true,
            blockThirdPartyCookies = prefs[Keys.BLOCK_THIRD_PARTY_COOKIES] ?: true,
            doNotTrackEnabled = prefs[Keys.DO_NOT_TRACK] ?: true,
            javascriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
            clearSessionOnExit = prefs[Keys.CLEAR_ON_EXIT] ?: false,
            glassIntensity = prefs[Keys.GLASS_INTENSITY] ?: 0.85f,
            totalTrackersBlocked = prefs[Keys.TOTAL_TRACKERS_BLOCKED] ?: 0,
        )
    }

    suspend fun setAeroTheme(preset: AeroThemePreset) {
        context.aeroPrefsDataStore.edit { it[Keys.AERO_THEME] = preset.id }
    }

    suspend fun setGlassIntensity(intensity: Float) {
        context.aeroPrefsDataStore.edit { it[Keys.GLASS_INTENSITY] = intensity.coerceIn(0.45f, 1.0f) }
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.aeroPrefsDataStore.edit { it[Keys.SEARCH_ENGINE] = engine.id }
    }

    suspend fun setStartPageMode(mode: StartPageMode) {
        context.aeroPrefsDataStore.edit { it[Keys.START_PAGE_MODE] = mode.id }
    }

    suspend fun setTrackerBlocking(enabled: Boolean) {
        context.aeroPrefsDataStore.edit { it[Keys.TRACKER_BLOCKING] = enabled }
    }

    suspend fun setHttpsOnly(enabled: Boolean) {
        context.aeroPrefsDataStore.edit { it[Keys.HTTPS_ONLY] = enabled }
    }

    suspend fun setBlockThirdPartyCookies(enabled: Boolean) {
        context.aeroPrefsDataStore.edit { it[Keys.BLOCK_THIRD_PARTY_COOKIES] = enabled }
    }

    suspend fun setDoNotTrack(enabled: Boolean) {
        context.aeroPrefsDataStore.edit { it[Keys.DO_NOT_TRACK] = enabled }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) {
        context.aeroPrefsDataStore.edit { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun setClearSessionOnExit(enabled: Boolean) {
        context.aeroPrefsDataStore.edit { it[Keys.CLEAR_ON_EXIT] = enabled }
    }

    suspend fun incrementTrackersBlocked(delta: Int = 1) {
        if (delta <= 0) return
        context.aeroPrefsDataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_TRACKERS_BLOCKED] ?: 0
            prefs[Keys.TOTAL_TRACKERS_BLOCKED] = current + delta
        }
    }
}
