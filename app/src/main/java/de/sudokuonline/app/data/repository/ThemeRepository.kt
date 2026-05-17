package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing app themes and color schemes
 */
class ThemeRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _currentTheme = MutableStateFlow(loadCurrentTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _useSystemTheme = MutableStateFlow(prefs.getBoolean(KEY_USE_SYSTEM, true))
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()
    
    private val _ownedThemes = MutableStateFlow(loadOwnedThemes())
    val ownedThemes: StateFlow<Set<String>> = _ownedThemes.asStateFlow()

    private fun loadCurrentTheme(): AppTheme {
        val themeId = prefs.getString(KEY_THEME, AppTheme.DEFAULT.id) ?: AppTheme.DEFAULT.id
        return AppTheme.entries.find { it.id == themeId } ?: AppTheme.DEFAULT
    }
    
    private fun loadOwnedThemes(): Set<String> {
        // Default and Monochrome are free
        val defaultOwned = setOf("default", "monochrome")
        val stored = prefs.getStringSet(KEY_OWNED_THEMES, defaultOwned) ?: defaultOwned
        return stored.toSet()
    }
    
    private fun saveOwnedThemes(themes: Set<String>) {
        prefs.edit().putStringSet(KEY_OWNED_THEMES, themes).apply()
    }

    fun setTheme(theme: AppTheme): Boolean {
        if (!isThemeOwned(theme.id)) return false
        prefs.edit().putString(KEY_THEME, theme.id).apply()
        _currentTheme.value = theme
        return true
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    fun setUseSystemTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_SYSTEM, enabled).apply()
        _useSystemTheme.value = enabled
    }
    
    /**
     * Check if a theme is owned
     */
    fun isThemeOwned(themeId: String): Boolean {
        val theme = AppTheme.entries.find { it.id == themeId } ?: return false
        // Free themes are always owned
        if (theme.price == 0) return true
        return themeId in _ownedThemes.value
    }
    
    /**
     * Purchase a theme
     */
    fun purchaseTheme(themeId: String, currencyRepository: CurrencyRepository): PurchaseResult {
        val theme = AppTheme.entries.find { it.id == themeId }
            ?: return PurchaseResult.Error("Theme nicht gefunden")
        
        // Already owned
        if (isThemeOwned(themeId)) {
            return PurchaseResult.AlreadyOwned
        }
        
        // Free theme
        if (theme.price == 0) {
            addOwnedTheme(themeId)
            return PurchaseResult.Success(theme)
        }
        
        // Check if can afford
        if (!currencyRepository.canAfford(theme.price)) {
            return PurchaseResult.InsufficientFunds(
                required = theme.price,
                current = currencyRepository.getBalance()
            )
        }
        
        // Make purchase
        if (currencyRepository.spendCoins(theme.price)) {
            addOwnedTheme(themeId)
            return PurchaseResult.Success(theme)
        }
        
        return PurchaseResult.Error("Kauf fehlgeschlagen")
    }
    
    private fun addOwnedTheme(themeId: String) {
        val updated = _ownedThemes.value.toMutableSet()
        updated.add(themeId)
        _ownedThemes.value = updated
        saveOwnedThemes(updated)
    }

    fun forceSetOwnedThemes(themes: Set<String>) {
        _ownedThemes.value = themes
        saveOwnedThemes(themes)
    }
    
    /**
     * Get all themes with their status
     */
    fun getAllThemesWithStatus(currencyRepository: CurrencyRepository): List<ThemeWithStatus> {
        return AppTheme.entries.map { theme ->
            ThemeWithStatus(
                theme = theme,
                isOwned = isThemeOwned(theme.id),
                isActive = theme.id == _currentTheme.value.id,
                canAfford = currencyRepository.canAfford(theme.price)
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "app_theme"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_USE_SYSTEM = "use_system_theme"
        private const val KEY_OWNED_THEMES = "owned_themes"

        @Volatile
        private var instance: ThemeRepository? = null

        fun getInstance(context: Context): ThemeRepository {
            return instance ?: synchronized(this) {
                instance ?: ThemeRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Result of a theme purchase attempt
 */
sealed class PurchaseResult {
    data class Success(val theme: AppTheme) : PurchaseResult()
    data object AlreadyOwned : PurchaseResult()
    data class InsufficientFunds(val required: Int, val current: Int) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

/**
 * Theme with its ownership/purchase status
 */
data class ThemeWithStatus(
    val theme: AppTheme,
    val isOwned: Boolean,
    val isActive: Boolean,
    val canAfford: Boolean
)

/**
 * Available app themes with their color schemes
 */
enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val price: Int, // 0 = free
    val icon: String, // Emoji
    // Light mode colors
    val primaryLight: Color,
    val secondaryLight: Color,
    val tertiaryLight: Color,
    val backgroundLight: Color,
    val surfaceLight: Color,
    // Dark mode colors
    val primaryDark: Color,
    val secondaryDark: Color,
    val tertiaryDark: Color,
    val backgroundDark: Color,
    val surfaceDark: Color
) {
    DEFAULT(
        id = "default",
        displayName = "Standard",
        description = "Klassisches blaues Design",
        price = 0,
        icon = "🎨",
        primaryLight = Color(0xFF1976D2),
        secondaryLight = Color(0xFF424242),
        tertiaryLight = Color(0xFF7B1FA2),
        backgroundLight = Color(0xFFFAFAFA),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFF90CAF9),
        secondaryDark = Color(0xFFB0BEC5),
        tertiaryDark = Color(0xFFCE93D8),
        backgroundDark = Color(0xFF121212),
        surfaceDark = Color(0xFF1E1E1E)
    ),

    OCEAN(
        id = "ocean",
        displayName = "Ozean",
        description = "Beruhigende Blau- und Türkistöne",
        price = 500,
        icon = "🌊",
        primaryLight = Color(0xFF0097A7),
        secondaryLight = Color(0xFF00796B),
        tertiaryLight = Color(0xFF0288D1),
        backgroundLight = Color(0xFFE0F7FA),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFF4DD0E1),
        secondaryDark = Color(0xFF80CBC4),
        tertiaryDark = Color(0xFF81D4FA),
        backgroundDark = Color(0xFF0D1B1E),
        surfaceDark = Color(0xFF1A2C30)
    ),

    FOREST(
        id = "forest",
        displayName = "Wald",
        description = "Natürliche Grüntöne",
        price = 500,
        icon = "🌲",
        primaryLight = Color(0xFF388E3C),
        secondaryLight = Color(0xFF689F38),
        tertiaryLight = Color(0xFF8BC34A),
        backgroundLight = Color(0xFFF1F8E9),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFF81C784),
        secondaryDark = Color(0xFFAED581),
        tertiaryDark = Color(0xFFC5E1A5),
        backgroundDark = Color(0xFF0D1A0D),
        surfaceDark = Color(0xFF1A2E1A)
    ),

    SUNSET(
        id = "sunset",
        displayName = "Sonnenuntergang",
        description = "Warme Orange- und Rottöne",
        price = 750,
        icon = "🌅",
        primaryLight = Color(0xFFE64A19),
        secondaryLight = Color(0xFFF57C00),
        tertiaryLight = Color(0xFFFF9800),
        backgroundLight = Color(0xFFFFF3E0),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFFFF8A65),
        secondaryDark = Color(0xFFFFB74D),
        tertiaryDark = Color(0xFFFFCC80),
        backgroundDark = Color(0xFF1A0D0A),
        surfaceDark = Color(0xFF2E1A14)
    ),

    LAVENDER(
        id = "lavender",
        displayName = "Lavendel",
        description = "Sanfte Lilatöne",
        price = 500,
        icon = "💜",
        primaryLight = Color(0xFF7B1FA2),
        secondaryLight = Color(0xFF9C27B0),
        tertiaryLight = Color(0xFFE91E63),
        backgroundLight = Color(0xFFF3E5F5),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFFCE93D8),
        secondaryDark = Color(0xFFE1BEE7),
        tertiaryDark = Color(0xFFF48FB1),
        backgroundDark = Color(0xFF1A0D1A),
        surfaceDark = Color(0xFF2E1A2E)
    ),

    MIDNIGHT(
        id = "midnight",
        displayName = "Mitternacht",
        description = "Elegantes dunkles Design mit Gold-Akzenten",
        price = 1500,
        icon = "✨",
        primaryLight = Color(0xFF3F51B5),
        secondaryLight = Color(0xFF303F9F),
        tertiaryLight = Color(0xFF5C6BC0),
        backgroundLight = Color(0xFFE8EAF6),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFFFFD700),
        secondaryDark = Color(0xFFFFC107),
        tertiaryDark = Color(0xFFFFE082),
        backgroundDark = Color(0xFF0A0A14),
        surfaceDark = Color(0xFF14142A)
    ),

    CHERRY(
        id = "cherry",
        displayName = "Kirschblüte",
        description = "Japanisches Sakura-Design",
        price = 1000,
        icon = "🌸",
        primaryLight = Color(0xFFFF69B4),
        secondaryLight = Color(0xFFFF85A2),
        tertiaryLight = Color(0xFFFFC0CB),
        backgroundLight = Color(0xFFFFF5F7),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFFFF69B4),
        secondaryDark = Color(0xFFFF85A2),
        tertiaryDark = Color(0xFFFFC0CB),
        backgroundDark = Color(0xFF1A0A0A),
        surfaceDark = Color(0xFF2E1414)
    ),

    MONOCHROME(
        id = "monochrome",
        displayName = "Monochrom",
        description = "Klassisches Schwarz-Weiß",
        price = 0,
        icon = "🖤",
        primaryLight = Color(0xFF424242),
        secondaryLight = Color(0xFF616161),
        tertiaryLight = Color(0xFF757575),
        backgroundLight = Color(0xFFFAFAFA),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFFBDBDBD),
        secondaryDark = Color(0xFF9E9E9E),
        tertiaryDark = Color(0xFF757575),
        backgroundDark = Color(0xFF121212),
        surfaceDark = Color(0xFF1E1E1E)
    ),
    
    NEON(
        id = "neon",
        displayName = "Neon",
        description = "Leuchtende Cyber-Farben",
        price = 2000,
        icon = "🎮",
        primaryLight = Color(0xFF00E676),
        secondaryLight = Color(0xFF00B0FF),
        tertiaryLight = Color(0xFFFF4081),
        backgroundLight = Color(0xFFF5F5F5),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFF00FF87),
        secondaryDark = Color(0xFF00D9FF),
        tertiaryDark = Color(0xFFFF00FF),
        backgroundDark = Color(0xFF0F0F1A),
        surfaceDark = Color(0xFF1A1A2E)
    ),
    
    GOLD(
        id = "gold",
        displayName = "Gold Premium",
        description = "Luxuriöses Gold-Design",
        price = 3000,
        icon = "👑",
        primaryLight = Color(0xFFFFD700),
        secondaryLight = Color(0xFFFFC107),
        tertiaryLight = Color(0xFFFF8F00),
        backgroundLight = Color(0xFFFFFDF5),
        surfaceLight = Color(0xFFFFFFFF),
        primaryDark = Color(0xFFFFD700),
        secondaryDark = Color(0xFFFFC107),
        tertiaryDark = Color(0xFFFFE082),
        backgroundDark = Color(0xFF1A1A0A),
        surfaceDark = Color(0xFF2E2E14)
    )
}
