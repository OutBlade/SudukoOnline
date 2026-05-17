package de.sudokuonline.app.data.model

import androidx.compose.ui.graphics.Color

/**
 * App Theme that can be purchased and applied
 */
data class AppTheme(
    val id: String,
    val name: String,
    val description: String,
    val price: Int, // in coins, 0 = free
    val isPremium: Boolean = false,
    val previewColors: ThemeColors,
    val icon: String // Emoji
)

/**
 * Color scheme for a theme
 */
data class ThemeColors(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    val error: Long = 0xFFBA1A1A,
    val boardBackground: Long,
    val cellBackground: Long,
    val cellHighlight: Long,
    val cellSelected: Long,
    val fixedNumberColor: Long,
    val enteredNumberColor: Long,
    val errorNumberColor: Long = 0xFFBA1A1A,
    val gridLineColor: Long,
    val gridBoldLineColor: Long
)

/**
 * Available themes in the shop
 */
object ThemeShop {
    
    val allThemes = listOf(
        // FREE THEMES
        AppTheme(
            id = "default",
            name = "Standard",
            description = "Das klassische Sudoku-Design",
            price = 0,
            icon = "🎨",
            previewColors = ThemeColors(
                primary = 0xFF6750A4,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFEADDFF,
                onPrimaryContainer = 0xFF21005D,
                secondary = 0xFF625B71,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFE8DEF8,
                onSecondaryContainer = 0xFF1D192B,
                tertiary = 0xFF7D5260,
                background = 0xFFFFFBFE,
                surface = 0xFFFFFBFE,
                surfaceVariant = 0xFFE7E0EC,
                onSurface = 0xFF1C1B1F,
                onSurfaceVariant = 0xFF49454F,
                boardBackground = 0xFFFFFBFE,
                cellBackground = 0xFFFFFFFF,
                cellHighlight = 0xFFEADDFF,
                cellSelected = 0xFFD0BCFF,
                fixedNumberColor = 0xFF1C1B1F,
                enteredNumberColor = 0xFF6750A4,
                gridLineColor = 0xFFCAC4D0,
                gridBoldLineColor = 0xFF49454F
            )
        ),
        
        AppTheme(
            id = "dark",
            name = "Dunkel",
            description = "Schont die Augen bei Nacht",
            price = 0,
            icon = "🌙",
            previewColors = ThemeColors(
                primary = 0xFFD0BCFF,
                onPrimary = 0xFF381E72,
                primaryContainer = 0xFF4F378B,
                onPrimaryContainer = 0xFFEADDFF,
                secondary = 0xFFCCC2DC,
                onSecondary = 0xFF332D41,
                secondaryContainer = 0xFF4A4458,
                onSecondaryContainer = 0xFFE8DEF8,
                tertiary = 0xFFEFB8C8,
                background = 0xFF1C1B1F,
                surface = 0xFF1C1B1F,
                surfaceVariant = 0xFF49454F,
                onSurface = 0xFFE6E1E5,
                onSurfaceVariant = 0xFFCAC4D0,
                boardBackground = 0xFF2B2930,
                cellBackground = 0xFF1C1B1F,
                cellHighlight = 0xFF4F378B,
                cellSelected = 0xFF6750A4,
                fixedNumberColor = 0xFFE6E1E5,
                enteredNumberColor = 0xFFD0BCFF,
                gridLineColor = 0xFF49454F,
                gridBoldLineColor = 0xFFCAC4D0
            )
        ),
        
        // PREMIUM THEMES
        AppTheme(
            id = "ocean",
            name = "Ozean",
            description = "Beruhigende blaue Töne wie das Meer",
            price = 500,
            isPremium = true,
            icon = "🌊",
            previewColors = ThemeColors(
                primary = 0xFF0077B6,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFCAF0F8,
                onPrimaryContainer = 0xFF03045E,
                secondary = 0xFF00B4D8,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFADE8F4,
                onSecondaryContainer = 0xFF023E8A,
                tertiary = 0xFF48CAE4,
                background = 0xFFF0F9FF,
                surface = 0xFFFFFFFF,
                surfaceVariant = 0xFFCAF0F8,
                onSurface = 0xFF03045E,
                onSurfaceVariant = 0xFF023E8A,
                boardBackground = 0xFFF0F9FF,
                cellBackground = 0xFFFFFFFF,
                cellHighlight = 0xFFCAF0F8,
                cellSelected = 0xFF90E0EF,
                fixedNumberColor = 0xFF03045E,
                enteredNumberColor = 0xFF0077B6,
                gridLineColor = 0xFFADE8F4,
                gridBoldLineColor = 0xFF0077B6
            )
        ),
        
        AppTheme(
            id = "forest",
            name = "Wald",
            description = "Natürliche grüne Farben",
            price = 500,
            isPremium = true,
            icon = "🌲",
            previewColors = ThemeColors(
                primary = 0xFF2D6A4F,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFD8F3DC,
                onPrimaryContainer = 0xFF1B4332,
                secondary = 0xFF40916C,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFB7E4C7,
                onSecondaryContainer = 0xFF1B4332,
                tertiary = 0xFF52B788,
                background = 0xFFF1F8F4,
                surface = 0xFFFFFFFF,
                surfaceVariant = 0xFFD8F3DC,
                onSurface = 0xFF1B4332,
                onSurfaceVariant = 0xFF2D6A4F,
                boardBackground = 0xFFF1F8F4,
                cellBackground = 0xFFFFFFFF,
                cellHighlight = 0xFFD8F3DC,
                cellSelected = 0xFFB7E4C7,
                fixedNumberColor = 0xFF1B4332,
                enteredNumberColor = 0xFF2D6A4F,
                gridLineColor = 0xFFB7E4C7,
                gridBoldLineColor = 0xFF2D6A4F
            )
        ),
        
        AppTheme(
            id = "sunset",
            name = "Sonnenuntergang",
            description = "Warme Orange- und Rosatöne",
            price = 750,
            isPremium = true,
            icon = "🌅",
            previewColors = ThemeColors(
                primary = 0xFFE85D04,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFFFE5D9,
                onPrimaryContainer = 0xFF6A040F,
                secondary = 0xFFDC2F02,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFFFD7BA,
                onSecondaryContainer = 0xFF9D0208,
                tertiary = 0xFFF48C06,
                background = 0xFFFFF5F0,
                surface = 0xFFFFFFFF,
                surfaceVariant = 0xFFFFE5D9,
                onSurface = 0xFF370617,
                onSurfaceVariant = 0xFF6A040F,
                boardBackground = 0xFFFFF5F0,
                cellBackground = 0xFFFFFFFF,
                cellHighlight = 0xFFFFE5D9,
                cellSelected = 0xFFFFD7BA,
                fixedNumberColor = 0xFF370617,
                enteredNumberColor = 0xFFE85D04,
                gridLineColor = 0xFFFFD7BA,
                gridBoldLineColor = 0xFFE85D04
            )
        ),
        
        AppTheme(
            id = "lavender",
            name = "Lavendel",
            description = "Sanftes Lila für Entspannung",
            price = 500,
            isPremium = true,
            icon = "💜",
            previewColors = ThemeColors(
                primary = 0xFF7B2CBF,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFE0AAFF,
                onPrimaryContainer = 0xFF3C096C,
                secondary = 0xFF9D4EDD,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFC77DFF,
                onSecondaryContainer = 0xFF240046,
                tertiary = 0xFFE0AAFF,
                background = 0xFFFAF5FF,
                surface = 0xFFFFFFFF,
                surfaceVariant = 0xFFE0AAFF,
                onSurface = 0xFF240046,
                onSurfaceVariant = 0xFF5A189A,
                boardBackground = 0xFFFAF5FF,
                cellBackground = 0xFFFFFFFF,
                cellHighlight = 0xFFE0AAFF,
                cellSelected = 0xFFC77DFF,
                fixedNumberColor = 0xFF240046,
                enteredNumberColor = 0xFF7B2CBF,
                gridLineColor = 0xFFC77DFF,
                gridBoldLineColor = 0xFF7B2CBF
            )
        ),
        
        AppTheme(
            id = "cherry",
            name = "Kirschblüte",
            description = "Japanisches Sakura-Design",
            price = 1000,
            isPremium = true,
            icon = "🌸",
            previewColors = ThemeColors(
                primary = 0xFFFF69B4,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFFFE4EC,
                onPrimaryContainer = 0xFF8B0A50,
                secondary = 0xFFFF85A2,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFFFD1DC,
                onSecondaryContainer = 0xFFB03060,
                tertiary = 0xFFFFC0CB,
                background = 0xFFFFF5F7,
                surface = 0xFFFFFFFF,
                surfaceVariant = 0xFFFFE4EC,
                onSurface = 0xFF4A0E2A,
                onSurfaceVariant = 0xFF8B0A50,
                boardBackground = 0xFFFFF5F7,
                cellBackground = 0xFFFFFFFF,
                cellHighlight = 0xFFFFE4EC,
                cellSelected = 0xFFFFD1DC,
                fixedNumberColor = 0xFF4A0E2A,
                enteredNumberColor = 0xFFFF69B4,
                gridLineColor = 0xFFFFD1DC,
                gridBoldLineColor = 0xFFFF69B4
            )
        ),
        
        AppTheme(
            id = "midnight",
            name = "Mitternacht",
            description = "Elegantes Schwarz mit Gold-Akzenten",
            price = 1500,
            isPremium = true,
            icon = "✨",
            previewColors = ThemeColors(
                primary = 0xFFFFD700,
                onPrimary = 0xFF000000,
                primaryContainer = 0xFF2C2C2C,
                onPrimaryContainer = 0xFFFFD700,
                secondary = 0xFFFFC107,
                onSecondary = 0xFF000000,
                secondaryContainer = 0xFF3D3D3D,
                onSecondaryContainer = 0xFFFFC107,
                tertiary = 0xFFFFE082,
                background = 0xFF121212,
                surface = 0xFF1E1E1E,
                surfaceVariant = 0xFF2C2C2C,
                onSurface = 0xFFE0E0E0,
                onSurfaceVariant = 0xFFBDBDBD,
                boardBackground = 0xFF1A1A1A,
                cellBackground = 0xFF121212,
                cellHighlight = 0xFF2C2C2C,
                cellSelected = 0xFF3D3D3D,
                fixedNumberColor = 0xFFE0E0E0,
                enteredNumberColor = 0xFFFFD700,
                gridLineColor = 0xFF3D3D3D,
                gridBoldLineColor = 0xFFFFD700
            )
        ),
        
        AppTheme(
            id = "neon",
            name = "Neon",
            description = "Leuchtende Cyber-Farben",
            price = 2000,
            isPremium = true,
            icon = "🎮",
            previewColors = ThemeColors(
                primary = 0xFF00FF87,
                onPrimary = 0xFF000000,
                primaryContainer = 0xFF1A1A2E,
                onPrimaryContainer = 0xFF00FF87,
                secondary = 0xFF00D9FF,
                onSecondary = 0xFF000000,
                secondaryContainer = 0xFF16213E,
                onSecondaryContainer = 0xFF00D9FF,
                tertiary = 0xFFFF00FF,
                background = 0xFF0F0F1A,
                surface = 0xFF1A1A2E,
                surfaceVariant = 0xFF16213E,
                onSurface = 0xFFE0E0E0,
                onSurfaceVariant = 0xFFBDBDBD,
                boardBackground = 0xFF0F0F1A,
                cellBackground = 0xFF1A1A2E,
                cellHighlight = 0xFF16213E,
                cellSelected = 0xFF2C2C54,
                fixedNumberColor = 0xFFE0E0E0,
                enteredNumberColor = 0xFF00FF87,
                gridLineColor = 0xFF2C2C54,
                gridBoldLineColor = 0xFF00FF87
            )
        )
    )
    
    fun getThemeById(id: String): AppTheme? = allThemes.find { it.id == id }
    
    fun getFreeThemes(): List<AppTheme> = allThemes.filter { it.price == 0 }
    
    fun getPremiumThemes(): List<AppTheme> = allThemes.filter { it.price > 0 }
}
