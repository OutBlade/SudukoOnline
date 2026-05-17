package de.sudokuonline.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import de.sudokuonline.app.data.repository.SettingsRepository
import de.sudokuonline.app.data.repository.ThemeRepository
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.navigation.SudokuNavGraph
import de.sudokuonline.app.ui.components.CoinNotificationData
import de.sudokuonline.app.ui.components.CoinNotificationHost
import de.sudokuonline.app.ui.theme.SudokuOnlineTheme

class MainActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var themeRepository: ThemeRepository
    private lateinit var currencyRepository: CurrencyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge with transparent status bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )

        settingsRepository = SettingsRepository.getInstance(this)
        themeRepository = ThemeRepository.getInstance(this)
        currencyRepository = CurrencyRepository.getInstance(this)

        setContent {
            val darkMode by settingsRepository.darkMode.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val currentTheme by themeRepository.currentTheme.collectAsState()
            val coinEarnedEvent by currencyRepository.coinEarnedEvent.collectAsState()

            // Use user preference if set, otherwise follow system
            val useDarkTheme = darkMode
            
            // Convert to notification data
            val coinNotification = coinEarnedEvent?.let {
                CoinNotificationData(it.amount, it.reason, it.id)
            }

            SudokuOnlineTheme(
                darkTheme = useDarkTheme,
                appTheme = currentTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CoinNotificationHost(
                        notification = coinNotification,
                        onDismiss = { currencyRepository.clearCoinEarnedEvent() }
                    ) {
                        val navController = rememberNavController()
                        SudokuNavGraph(
                            navController = navController,
                            settingsRepository = settingsRepository
                        )
                    }
                }
            }
        }
    }
}
