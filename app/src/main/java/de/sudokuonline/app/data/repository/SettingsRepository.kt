package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for app settings with persistence
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _darkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION, true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _showTimer = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TIMER, true))
    val showTimer: StateFlow<Boolean> = _showTimer.asStateFlow()

    private val _highlightErrors = MutableStateFlow(prefs.getBoolean(KEY_HIGHLIGHT_ERRORS, true))
    val highlightErrors: StateFlow<Boolean> = _highlightErrors.asStateFlow()

    private val _highlightSameNumbers = MutableStateFlow(prefs.getBoolean(KEY_HIGHLIGHT_SAME, true))
    val highlightSameNumbers: StateFlow<Boolean> = _highlightSameNumbers.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _darkMode.value = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _soundEnabled.value = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _vibrationEnabled.value = enabled
    }

    fun setShowTimer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TIMER, enabled).apply()
        _showTimer.value = enabled
    }

    fun setHighlightErrors(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGHLIGHT_ERRORS, enabled).apply()
        _highlightErrors.value = enabled
    }

    fun setHighlightSameNumbers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGHLIGHT_SAME, enabled).apply()
        _highlightSameNumbers.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "sudoku_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_SHOW_TIMER = "show_timer"
        private const val KEY_HIGHLIGHT_ERRORS = "highlight_errors"
        private const val KEY_HIGHLIGHT_SAME = "highlight_same_numbers"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
