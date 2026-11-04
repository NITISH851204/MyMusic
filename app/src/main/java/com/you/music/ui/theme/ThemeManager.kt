package com.you.music.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeStore by preferencesDataStore(name = "theme")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED")
}

data class Preset(val name: String, val color: Int)

val Presets = listOf(
    Preset("Purple", 0xFF6750A4.toInt()),
    Preset("Ocean", 0xFF0B79D0.toInt()),
    Preset("Sakura", 0xFFE75480.toInt()),
    Preset("Forest", 0xFF2E7D32.toInt()),
    Preset("Sunset", 0xFFFF6D00.toInt()),
    Preset("Mint", 0xFF00BFA5.toInt()),
    Preset("Mono", 0xFF9E9E9E.toInt())
)

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val artColors: Boolean = true,
    val preset: Int = 0
)

class ThemeManager(context: Context) {

    private val store = context.applicationContext.themeStore

    private val modeKey = stringPreferencesKey("mode")
    private val artKey = booleanPreferencesKey("art")
    private val presetKey = intPreferencesKey("preset")

    val settings: Flow<ThemeSettings> = store.data.map { p ->
        ThemeSettings(
            mode = runCatching { ThemeMode.valueOf(p[modeKey] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            artColors = p[artKey] ?: true,
            preset = (p[presetKey] ?: 0).coerceIn(0, Presets.size - 1)
        )
    }

    suspend fun setMode(mode: ThemeMode) {
        store.edit { it[modeKey] = mode.name }
    }

    suspend fun setArtColors(enabled: Boolean) {
        store.edit { it[artKey] = enabled }
    }

    suspend fun setPreset(index: Int) {
        store.edit { it[presetKey] = index }
    }
}
