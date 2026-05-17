package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.ui.theme.SuccessColor
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.ui.components.BombColor
import de.sudokuonline.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeLobbyScreen(
    availableRooms: List<TicTacToeRoom>,
    selectedGameMode: TicTacToeGameMode,
    selectedBoardSize: TicTacToeBoardSize,
    isLoading: Boolean,
    error: String?,
    onGameModeChange: (TicTacToeGameMode) -> Unit,
    onBoardSizeChange: (TicTacToeBoardSize) -> Unit,
    onCreateRoom: (Boolean) -> Unit,
    onJoinRoom: (String) -> Unit,
    onJoinByCode: (String) -> Unit,
    onBackClick: () -> Unit,
    onClearError: () -> Unit
) {
    var showCodeDialog by remember { mutableStateOf(false) }
    var roomCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                    Text(
                        text = "TicTacToe Online",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Game mode selection
            item {
                Text(
                    text = "Spielmodus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(TicTacToeGameMode.entries) { mode ->
                GameModeOption(
                    mode = mode,
                    isSelected = mode == selectedGameMode,
                    onClick = { onGameModeChange(mode) }
                )
            }

            // Board size selection - hide for Ultimate mode
            if (selectedGameMode != TicTacToeGameMode.ULTIMATE) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Spielfeldgröße",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TicTacToeBoardSize.entries.forEach { size ->
                            BoardSizeOption(
                                size = size,
                                isSelected = size == selectedBoardSize,
                                onClick = { onBoardSizeChange(size) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Show info for Ultimate mode
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "🎯 Ultimate TicTacToe: 9 kleine Bretter (3×3). Gewinne 3 Bretter in einer Reihe!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Create room buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onCreateRoom(false) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Öffentlich")
                    }
                    OutlinedButton(
                        onClick = { onCreateRoom(true) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privat")
                    }
                }
            }

            // Join by code button
            item {
                OutlinedButton(
                    onClick = { showCodeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mit Code beitreten")
                }
            }

            // Available rooms header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verfügbare Räume",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (availableRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Keine offenen Räume gefunden",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Erstelle einen neuen Raum!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(availableRooms) { room ->
                    RoomCard(
                        room = room,
                        onClick = { onJoinRoom(room.id) },
                        enabled = !isLoading
                    )
                }
            }

            // Loading indicator
            if (isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Error message
            error?.let {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = ErrorColor.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = ErrorColor,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearError) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Schließen",
                                    tint = ErrorColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Join by code dialog
    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text("Mit Code beitreten") },
            text = {
                Column {
                    Text(
                        text = "Gib den 6-stelligen Raumcode ein:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = {
                            if (it.length <= 6) roomCode = it.uppercase()
                        },
                        label = { Text("Raumcode") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomCode.length == 6) {
                            onJoinByCode(roomCode)
                            showCodeDialog = false
                            roomCode = ""
                        }
                    },
                    enabled = roomCode.length == 6
                ) {
                    Text("Beitreten")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun GameModeOption(
    mode: TicTacToeGameMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (mode) {
        TicTacToeGameMode.CLASSIC -> InfoColor
        TicTacToeGameMode.BOMB -> WarningColor
        TicTacToeGameMode.L_BOMB -> BombColor
        TicTacToeGameMode.ULTIMATE -> SuccessColor
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) {
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(color)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val icon = when (mode) {
                TicTacToeGameMode.CLASSIC -> "⭕"
                TicTacToeGameMode.BOMB -> "💣"
                TicTacToeGameMode.L_BOMB -> "💥"
                TicTacToeGameMode.ULTIMATE -> "🎯"
            }
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color
                )
            }
        }
    }
}

@Composable
private fun BoardSizeOption(
    size: TicTacToeBoardSize,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${size.size}×${size.size}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "${size.winCondition} in Reihe",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoomCard(
    room: TicTacToeRoom,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val gameMode = room.getGameModeEnum()
    val boardSize = room.getBoardSizeEnum()
    val hostName = room.players[room.hostId]?.displayName ?: "Unbekannt"
    val playerCount = room.players.size

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game mode icon
            val icon = when (gameMode) {
                TicTacToeGameMode.CLASSIC -> "⭕"
                TicTacToeGameMode.BOMB -> "💣"
                TicTacToeGameMode.L_BOMB -> "💥"
                TicTacToeGameMode.ULTIMATE -> "🎯"
            }
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hostName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${gameMode.displayName} • ${boardSize.size}×${boardSize.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Player count
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$playerCount/${room.maxPlayers}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
