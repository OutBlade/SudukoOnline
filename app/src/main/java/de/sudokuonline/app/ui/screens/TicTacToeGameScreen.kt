package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.ui.components.*
import de.sudokuonline.app.viewmodel.TicTacToeActionMode
import de.sudokuonline.app.ui.theme.*
import de.sudokuonline.app.ui.components.UltimateTicTacToeBoard as UltimateBoardComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeGameScreen(
    board: TicTacToeBoard,
    winningLine: List<Pair<Int, Int>>?,
    ultimateBoard: UltimateTicTacToeBoard = UltimateTicTacToeBoard(),
    playableBoards: List<Pair<Int, Int>> = emptyList(),
    gameMode: TicTacToeGameMode,
    boardSize: TicTacToeBoardSize,
    isMyTurn: Boolean,
    mySymbol: Int,
    myName: String,
    opponentName: String,
    bombsRemaining: Int,
    opponentBombsRemaining: Int,
    actionMode: TicTacToeActionMode,
    elapsedSeconds: Int,
    isComplete: Boolean,
    isDraw: Boolean,
    winnerId: String?,
    playerId: String,
    showGameOver: Boolean,
    // Rematch parameters
    rematchStatus: RematchStatus = RematchStatus.NONE,
    rematchRequestedByMe: Boolean = false,
    mySeriesWins: Int = 0,
    opponentSeriesWins: Int = 0,
    roundNumber: Int = 1,
    onRequestRematch: () -> Unit = {},
    onAcceptRematch: () -> Unit = {},
    onDeclineRematch: () -> Unit = {},
    // Other callbacks
    onCellClick: (Int, Int) -> Unit,
    onUltimateCellClick: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onActionModeChange: (TicTacToeActionMode) -> Unit,
    onBackClick: () -> Unit,
    onGameOverDismiss: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val isWinner = winnerId == playerId
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück"
                    )
                }

                // Timer
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Spacer for balance
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Game mode info
            GameModeInfoCard(
                gameMode = gameMode,
                boardSize = boardSize.displayName
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player info cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerInfoCard(
                    name = myName.ifEmpty { "Du" },
                    symbol = mySymbol,
                    bombsRemaining = bombsRemaining,
                    isCurrentPlayer = isMyTurn,
                    gameMode = gameMode,
                    modifier = Modifier.weight(1f)
                )
                PlayerInfoCard(
                    name = opponentName.ifEmpty { "Gegner" },
                    symbol = 3 - mySymbol,  // Other symbol
                    bombsRemaining = opponentBombsRemaining,
                    isCurrentPlayer = !isMyTurn,
                    gameMode = gameMode,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Turn indicator
            TurnIndicator(
                isMyTurn = isMyTurn,
                mySymbol = mySymbol,
                myName = myName.ifEmpty { "Du" },
                opponentName = opponentName.ifEmpty { "Gegner" }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Game board - different for Ultimate mode
            if (gameMode == TicTacToeGameMode.ULTIMATE) {
                UltimateBoardComposable(
                    board = ultimateBoard,
                    playableBoards = playableBoards,
                    isMyTurn = isMyTurn && !isComplete,
                    mySymbol = mySymbol,
                    onCellClick = onUltimateCellClick
                )
            } else {
                TicTacToeBoard(
                    board = board,
                    winningLine = winningLine,
                    isMyTurn = isMyTurn && !isComplete,
                    mySymbol = mySymbol,
                    onCellClick = onCellClick
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action selector (for bomb modes only, not Classic or Ultimate)
            if (gameMode == TicTacToeGameMode.BOMB || gameMode == TicTacToeGameMode.L_BOMB) {
                TicTacToeActionSelector(
                    currentMode = actionMode,
                    gameMode = gameMode,
                    mySymbol = mySymbol,
                    bombsRemaining = bombsRemaining,
                    isMyTurn = isMyTurn && !isComplete,
                    onModeChange = onActionModeChange
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bomb mode explanation (only for bomb modes)
            if ((gameMode == TicTacToeGameMode.BOMB || gameMode == TicTacToeGameMode.L_BOMB) && !isComplete) {
                BombModeHelp(gameMode = gameMode)
            }

            // Ultimate mode explanation
            if (gameMode == TicTacToeGameMode.ULTIMATE && !isComplete) {
                UltimateModeHelp()
            }
        }
    }

    // Game over dialog with rematch support
    if (showGameOver) {
        GameOverDialogWithRematch(
            isWinner = isWinner,
            isDraw = isDraw,
            opponentName = opponentName,
            rematchStatus = rematchStatus,
            rematchRequestedByMe = rematchRequestedByMe,
            mySeriesWins = mySeriesWins,
            opponentSeriesWins = opponentSeriesWins,
            roundNumber = roundNumber,
            onDismiss = onGameOverDismiss,
            onRequestRematch = onRequestRematch,
            onAcceptRematch = onAcceptRematch,
            onDeclineRematch = onDeclineRematch
        )
    }
}

@Composable
private fun UltimateModeHelp() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = "🎯 Gewinne 3 kleine Bretter in einer Reihe! Dein Zug bestimmt, wo dein Gegner spielen muss.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BombModeHelp(gameMode: TicTacToeGameMode) {
    val helpText = when (gameMode) {
        TicTacToeGameMode.BOMB -> "💣 Bomben löschen Felder oben, unten, links und rechts"
        TicTacToeGameMode.L_BOMB -> "💣 L-Bomben löschen die Spalte darüber und das Feld rechts"
        else -> ""
    }

    if (helpText.isNotEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = helpText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GameOverDialogWithRematch(
    isWinner: Boolean,
    isDraw: Boolean,
    opponentName: String,
    rematchStatus: RematchStatus,
    rematchRequestedByMe: Boolean,
    mySeriesWins: Int,
    opponentSeriesWins: Int,
    roundNumber: Int,
    onDismiss: () -> Unit,
    onRequestRematch: () -> Unit,
    onAcceptRematch: () -> Unit,
    onDeclineRematch: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round indicator (if series)
                if (roundNumber > 1 || mySeriesWins > 0 || opponentSeriesWins > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Runde $roundNumber",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Result icon
                val icon = when {
                    isDraw -> Icons.Default.Balance
                    isWinner -> Icons.Default.EmojiEvents
                    else -> Icons.Default.SentimentDissatisfied
                }
                val iconColor = when {
                    isDraw -> InfoColor
                    isWinner -> SuccessColor
                    else -> ErrorColor
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = iconColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Result text
                val resultText = when {
                    isDraw -> "Unentschieden!"
                    isWinner -> "Du hast gewonnen!"
                    else -> "Du hast verloren!"
                }

                Text(
                    text = resultText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )

                if (!isDraw && !isWinner) {
                    Text(
                        text = "$opponentName hat gewonnen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Series Score
                if (mySeriesWins > 0 || opponentSeriesWins > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Du",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = mySeriesWins.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mySeriesWins > opponentSeriesWins) SuccessColor 
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = opponentName.take(8),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = opponentSeriesWins.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (opponentSeriesWins > mySeriesWins) ErrorColor 
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Rematch status indicator
                when (rematchStatus) {
                    RematchStatus.PENDING -> {
                        if (rematchRequestedByMe) {
                            // Waiting for opponent
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Warte auf $opponentName...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = onDismiss) {
                                Text("Abbrechen & Beenden")
                            }
                        } else {
                            // Opponent requested rematch
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "$opponentName moechte nochmal spielen!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDeclineRematch,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ablehnen")
                                }
                                Button(
                                    onClick = onAcceptRematch,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Annehmen")
                                }
                            }
                        }
                    }
                    RematchStatus.DECLINED -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Rematch wurde abgelehnt",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text("Zurueck zur Lobby")
                        }
                    }
                    else -> {
                        // No rematch requested yet - show rematch button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Beenden")
                            }
                            Button(
                                onClick = onRequestRematch,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rematch")
                            }
                        }
                    }
                }
            }
        }
    }
}
