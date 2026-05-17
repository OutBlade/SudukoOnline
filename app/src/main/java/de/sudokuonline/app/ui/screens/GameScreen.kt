package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.model.GameMode
import de.sudokuonline.app.data.model.SudokuBoard
import de.sudokuonline.app.ads.RewardedAdManager
import de.sudokuonline.app.ui.components.GameHeader
import de.sudokuonline.app.ui.components.NumberPad
import de.sudokuonline.app.ui.components.SudokuBoard
import de.sudokuonline.app.ui.components.WatchAdForHintDialog

@Composable
fun GameScreen(
    board: SudokuBoard,
    selectedCell: Pair<Int, Int>?,
    isNotesMode: Boolean,
    errors: Int,
    maxErrors: Int = 3,
    hintsUsed: Int = 0,
    maxHints: Int = 3,
    elapsedSeconds: Int,
    difficulty: Difficulty,
    gameMode: GameMode,
    isPaused: Boolean,
    isComplete: Boolean,
    showGameOver: Boolean,
    isWinner: Boolean?,
    myProgress: Int,
    opponentProgress: Int,
    opponentName: String,
    playerId: String,
    onCellClick: (Int, Int) -> Unit,
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    onNotesToggle: () -> Unit,
    onUndoClick: () -> Unit,
    onHintClick: () -> Unit,
    onPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    onGameOverDismiss: () -> Unit,
    onPlayAgain: () -> Unit,
    onNavigateToTutorial: (() -> Unit)? = null,
    showAdForHintDialog: Boolean = false,
    bonusHints: Int = 0,
    onAdRewardEarned: () -> Unit = {},
    onDismissAdDialog: () -> Unit = {}
) {
    var showExitDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with timer, errors, etc.
            GameHeader(
                elapsedSeconds = elapsedSeconds,
                errors = errors,
                maxErrors = maxErrors,
                hintsUsed = hintsUsed,
                maxHints = maxHints,
                difficulty = difficulty,
                gameMode = gameMode,
                isPaused = isPaused,
                onPauseClick = onPauseClick,
                onBackClick = { showExitDialog = true },
                myProgress = myProgress,
                opponentProgress = opponentProgress,
                opponentName = opponentName
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sudoku board
            SudokuBoard(
                board = board,
                selectedCell = selectedCell,
                onCellClick = onCellClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                myPlayerId = playerId,
                highlightPlayerCells = gameMode == GameMode.COOP || gameMode == GameMode.COMPETITIVE
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Number pad - hint directly places the correct value
            NumberPad(
                onNumberClick = onNumberClick,
                onClearClick = onClearClick,
                onNotesToggle = onNotesToggle,
                onUndoClick = onUndoClick,
                onHintClick = onHintClick,
                isNotesMode = isNotesMode,
                isHintLoading = false
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Pause overlay
        if (isPaused && !showGameOver) {
            PauseOverlay(
                onResume = onPauseClick,
                onExit = { showExitDialog = true }
            )
        }
        
        // Game over dialog
        if (showGameOver) {
            GameOverDialog(
                isComplete = isComplete,
                isWinner = isWinner,
                elapsedSeconds = elapsedSeconds,
                errors = errors,
                gameMode = gameMode,
                onDismiss = onGameOverDismiss,
                onPlayAgain = onPlayAgain
            )
        }
        
        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Spiel verlassen?") },
                text = {
                    Text(
                        if (gameMode == GameMode.SINGLE_PLAYER)
                            "Dein aktueller Fortschritt geht verloren."
                        else
                            "Du wirst das Spiel aufgeben."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onBackClick()
                        }
                    ) {
                        Text("Verlassen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Weiterspielen")
                    }
                }
            )
        }

        // Watch ad for hint dialog
        if (showAdForHintDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val adManager = remember { RewardedAdManager.getInstance(context) }
            val adState by adManager.adState.collectAsState()

            LaunchedEffect(Unit) {
                adManager.initialize(context)
                if (!adManager.isAdReady()) {
                    adManager.loadAd(context)
                }
            }

            WatchAdForHintDialog(
                hintsRemaining = (maxHints - hintsUsed) + bonusHints,
                maxFreeHints = maxHints,
                onWatchAd = {
                    val activity = context as? android.app.Activity
                    if (activity != null && adManager.isAdReady()) {
                        adManager.showAd(
                            activity = activity,
                            onRewardEarned = { _ -> onAdRewardEarned() },
                            onAdClosed = { adManager.loadAd(context) }
                        )
                    }
                },
                onDismiss = onDismissAdDialog,
                isAdLoading = adState == RewardedAdManager.AdState.LOADING,
                isAdReady = adManager.isAdReady()
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Pause",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fortsetzen")
                }
                
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Beenden")
                }
            }
        }
    }
}

@Composable
private fun GameOverDialog(
    isComplete: Boolean,
    isWinner: Boolean?,
    elapsedSeconds: Int,
    errors: Int,
    gameMode: GameMode,
    onDismiss: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    
    val title = when {
        gameMode == GameMode.SINGLE_PLAYER && isComplete -> "Geschafft!"
        isWinner == true -> "Du hast gewonnen!"
        isWinner == false -> "Leider verloren"
        isComplete -> "Spiel beendet"
        else -> "Spiel beendet"
    }
    
    val icon = when {
        isWinner == true || (gameMode == GameMode.SINGLE_PLAYER && isComplete) -> Icons.Default.EmojiEvents
        isWinner == false -> Icons.Default.SentimentDissatisfied
        else -> Icons.Default.CheckCircle
    }
    
    val iconColor = when {
        isWinner == true || (gameMode == GameMode.SINGLE_PLAYER && isComplete) -> MaterialTheme.colorScheme.primary
        isWinner == false -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = iconColor
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Stats
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.Timer,
                            value = String.format("%02d:%02d", minutes, seconds),
                            label = "Zeit"
                        )
                        StatItem(
                            icon = Icons.Default.Close,
                            value = errors.toString(),
                            label = "Fehler"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nochmal spielen")
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zum Menue")
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
