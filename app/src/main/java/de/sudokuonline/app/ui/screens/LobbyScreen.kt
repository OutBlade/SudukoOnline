package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    availableRooms: List<GameRoom>,
    selectedGameMode: GameMode,
    selectedDifficulty: Difficulty,
    isSearching: Boolean,
    isLoading: Boolean,
    error: String?,
    onGameModeChange: (GameMode) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onCreateRoom: (isPrivate: Boolean) -> Unit,
    onJoinRoom: (roomId: String) -> Unit,
    onJoinByCode: (code: String) -> Unit,
    onStartMatchmaking: () -> Unit,
    onCancelMatchmaking: () -> Unit,
    onBackClick: () -> Unit,
    onClearError: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinCodeDialog by remember { mutableStateOf(false) }
    var roomCode by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearError) {
                            Icon(Icons.Default.Close, contentDescription = "Schliessen")
                        }
                    }
                }
            }
            
            // Game mode selection
            GameModeSelector(
                selectedMode = selectedGameMode,
                onModeSelected = onGameModeChange
            )
            
            // Difficulty selection
            DifficultySelector(
                selectedDifficulty = selectedDifficulty,
                onDifficultySelected = onDifficultyChange
            )
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Create room button
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && !isSearching
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Raum erstellen")
                }
                
                // Join by code button
                OutlinedButton(
                    onClick = { showJoinCodeDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && !isSearching
                ) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mit Code")
                }
            }
            
            // Quick match button
            if (selectedGameMode == GameMode.COMPETITIVE) {
                Button(
                    onClick = {
                        if (isSearching) onCancelMatchmaking() else onStartMatchmaking()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSearching) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Suche abbrechen...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Schnelles Spiel finden")
                    }
                }
            }
            
            HorizontalDivider()
            
            // Available rooms
            Text(
                text = "Verfuegbare Raeume",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (availableRooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Keine Raeume gefunden",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Erstelle einen neuen Raum oder\nnutze 'Schnelles Spiel'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableRooms) { room ->
                        RoomItem(
                            room = room,
                            onJoin = { onJoinRoom(room.id) },
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
    
    // Create room dialog
    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { isPrivate ->
                showCreateDialog = false
                onCreateRoom(isPrivate)
            }
        )
    }
    
    // Join by code dialog
    if (showJoinCodeDialog) {
        JoinByCodeDialog(
            code = roomCode,
            onCodeChange = { roomCode = it.uppercase().take(6) },
            onDismiss = { 
                showJoinCodeDialog = false
                roomCode = ""
            },
            onJoin = {
                showJoinCodeDialog = false
                onJoinByCode(roomCode)
                roomCode = ""
            },
            isLoading = isLoading
        )
    }
}

@Composable
private fun GameModeSelector(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit
) {
    val modes = listOf(GameMode.COMPETITIVE, GameMode.COOP)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = when (mode) {
                            GameMode.COMPETITIVE -> Icons.Default.EmojiEvents
                            GameMode.COOP -> Icons.Default.Handshake
                            else -> Icons.Default.Games
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DifficultySelector(
    selectedDifficulty: Difficulty,
    onDifficultySelected: (Difficulty) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Difficulty.entries.forEach { difficulty ->
            val color = when (difficulty) {
                Difficulty.EASY -> SuccessColor
                Difficulty.MEDIUM -> InfoColor
                Difficulty.HARD -> WarningColor
                Difficulty.EXPERT -> ErrorColor
            }
            
            FilterChip(
                selected = selectedDifficulty == difficulty,
                onClick = { onDifficultySelected(difficulty) },
                label = { 
                    Text(
                        difficulty.displayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoomItem(
    room: GameRoom,
    onJoin: () -> Unit,
    isLoading: Boolean
) {
    val difficultyColor = when (Difficulty.valueOf(room.difficulty)) {
        Difficulty.EASY -> SuccessColor
        Difficulty.MEDIUM -> InfoColor
        Difficulty.HARD -> WarningColor
        Difficulty.EXPERT -> ErrorColor
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.players.values.firstOrNull()?.displayName ?: "Unbekannt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = difficultyColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = Difficulty.valueOf(room.difficulty).displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = difficultyColor
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = GameMode.valueOf(room.gameMode).displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${room.players.size}/${room.maxPlayers} Spieler",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(
                onClick = onJoin,
                enabled = !isLoading && room.players.size < room.maxPlayers
            ) {
                Text("Beitreten")
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (isPrivate: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raum erstellen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Wähle den Raumtyp:")
                
                // Public room option
                Card(
                    onClick = { onCreate(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Oeffentlich",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Jeder kann beitreten",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Private room option
                Card(
                    onClick = { onCreate(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Privat",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Nur mit Code beitreten",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun JoinByCodeDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onJoin: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mit Code beitreten") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Gib den 6-stelligen Raumcode ein:")
                
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text("Raumcode") },
                    placeholder = { Text("ABCDEF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onJoin,
                enabled = code.length == 6 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Beitreten")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
