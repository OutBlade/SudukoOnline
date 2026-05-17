package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.repository.SettingsRepository
import de.sudokuonline.app.util.MusicManager

private const val PRIVACY_POLICY_URL = "https://outblade.github.io/sudoku-online-privacy/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository?,
    onBackClick: () -> Unit
) {
    // Collect settings from repository or use defaults
    val darkMode by settingsRepository?.darkMode?.collectAsState() ?: remember { mutableStateOf(false) }
    val soundEnabled by settingsRepository?.soundEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val vibrationEnabled by settingsRepository?.vibrationEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val showTimer by settingsRepository?.showTimer?.collectAsState() ?: remember { mutableStateOf(true) }
    val highlightErrors by settingsRepository?.highlightErrors?.collectAsState() ?: remember { mutableStateOf(true) }
    val highlightSameNumbers by settingsRepository?.highlightSameNumbers?.collectAsState() ?: remember { mutableStateOf(true) }

    // Music Manager
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val musicManager = remember { MusicManager.getInstance(context) }
    val musicEnabled by musicManager.isEnabled.collectAsState()
    val musicVolume by musicManager.volume.collectAsState()
    val currentTrack by musicManager.currentTrack.collectAsState()
    var showTrackPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance - Move to top for visibility
            SettingsSection(title = "Darstellung") {
                SwitchSetting(
                    icon = Icons.Default.DarkMode,
                    title = "Dunkelmodus",
                    subtitle = "Dunkles Farbschema verwenden",
                    checked = darkMode,
                    onCheckedChange = { settingsRepository?.setDarkMode(it) }
                )
            }

            // Game settings
            SettingsSection(title = "Spiel") {
                SwitchSetting(
                    icon = Icons.Default.Timer,
                    title = "Timer anzeigen",
                    subtitle = "Zeige die verstrichene Zeit",
                    checked = showTimer,
                    onCheckedChange = { settingsRepository?.setShowTimer(it) }
                )

                SwitchSetting(
                    icon = Icons.Default.Error,
                    title = "Fehler hervorheben",
                    subtitle = "Markiere falsche Eingaben",
                    checked = highlightErrors,
                    onCheckedChange = { settingsRepository?.setHighlightErrors(it) }
                )

                SwitchSetting(
                    icon = Icons.Default.GridView,
                    title = "Gleiche Zahlen hervorheben",
                    subtitle = "Hebe identische Zahlen hervor",
                    checked = highlightSameNumbers,
                    onCheckedChange = { settingsRepository?.setHighlightSameNumbers(it) }
                )
            }

            // Audio & Haptics
            SettingsSection(title = "Audio & Haptik") {
                SwitchSetting(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Sound",
                    subtitle = "Spielsounds aktivieren",
                    checked = soundEnabled,
                    onCheckedChange = { settingsRepository?.setSoundEnabled(it) }
                )

                SwitchSetting(
                    icon = Icons.Default.Vibration,
                    title = "Vibration",
                    subtitle = "Haptisches Feedback aktivieren",
                    checked = vibrationEnabled,
                    onCheckedChange = { settingsRepository?.setVibrationEnabled(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Background Music
                SwitchSetting(
                    icon = Icons.Default.MusicNote,
                    title = "Hintergrundmusik",
                    subtitle = if (musicEnabled) "Spielt: ${musicManager.tracks[currentTrack].name}" else "Entspannende Musik abspielen",
                    checked = musicEnabled,
                    onCheckedChange = { musicManager.setEnabled(it) }
                )

                if (musicEnabled) {
                    // Track Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrackPicker = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Musik auswählen",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = musicManager.tracks[currentTrack].description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Volume Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Slider(
                            value = musicVolume,
                            onValueChange = { musicManager.setVolume(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // About
            SettingsSection(title = "Über") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.3.0"
                )

                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Entwickelt mit",
                    subtitle = "Jetpack Compose & Firebase"
                )
            }
            
            // Legal
            SettingsSection(title = "Rechtliches") {
                ClickableSettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Datenschutzerklärung",
                    subtitle = "Informationen zum Datenschutz",
                    onClick = {
                        uriHandler.openUri(PRIVACY_POLICY_URL)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Track Picker Dialog
    if (showTrackPicker) {
        AlertDialog(
            onDismissRequest = { showTrackPicker = false },
            title = { Text("Musik auswählen") },
            text = {
                Column {
                    musicManager.tracks.forEachIndexed { index, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    musicManager.setTrack(index)
                                    showTrackPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = currentTrack == index,
                                onClick = {
                                    musicManager.setTrack(index)
                                    showTrackPicker = false
                                }
                            )
                            Column {
                                Text(
                                    text = track.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentTrack == index) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = track.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackPicker = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SwitchSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClickableSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
