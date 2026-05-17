package de.sudokuonline.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.SettingsRepository
import de.sudokuonline.app.data.repository.ThemeRepository
import de.sudokuonline.app.navigation.SudokuNavGraph
import de.sudokuonline.app.ui.components.CoinNotificationData
import de.sudokuonline.app.ui.components.CoinNotificationHost
import de.sudokuonline.app.ui.theme.SudokuOnlineTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var themeRepository: ThemeRepository
    private lateinit var currencyRepository: CurrencyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        settingsRepository = SettingsRepository.getInstance(this)
        themeRepository = ThemeRepository.getInstance(this)
        currencyRepository = CurrencyRepository.getInstance(this)

        setContent {
            val darkMode by settingsRepository.darkMode.collectAsState()
            val currentTheme by themeRepository.currentTheme.collectAsState()
            val coinEarnedEvent by currencyRepository.coinEarnedEvent.collectAsState()

            val coinNotification = coinEarnedEvent?.let {
                CoinNotificationData(it.amount, it.reason, it.id)
            }

            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }

            // Check for update once on launch — skip if installed via Play Store
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (!UpdateManager.isPlayStoreInstall(this@MainActivity)) {
                    val info = UpdateManager.checkForUpdate(BuildConfig.VERSION_CODE)
                    if (info != null) {
                        updateInfo = info
                        showUpdateDialog = true
                    }
                }
            }

            SudokuOnlineTheme(darkTheme = darkMode, appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CoinNotificationHost(
                        notification = coinNotification,
                        onDismiss = { currencyRepository.clearCoinEarnedEvent() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val navController = rememberNavController()
                            SudokuNavGraph(
                                navController = navController,
                                settingsRepository = settingsRepository
                            )

                            if (showUpdateDialog && updateInfo != null) {
                                AlertDialog(
                                    onDismissRequest = {
                                        if (!isDownloading) showUpdateDialog = false
                                    },
                                    title = { Text("Update verfügbar") },
                                    text = {
                                        Text("Version ${updateInfo!!.versionName} ist verfügbar. Jetzt herunterladen?")
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showUpdateDialog = false
                                            UpdateManager.openDownloadPage(this@MainActivity)
                                        }) { Text("Herunterladen") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showUpdateDialog = false }) {
                                            Text("Später")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
