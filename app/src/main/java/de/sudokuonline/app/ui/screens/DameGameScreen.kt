package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.ui.components.DameBoardView
import de.sudokuonline.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DameGameScreen(
    room: DameRoom?,
    playerId: String,
    selectedPiece: Pair<Int, Int>?,
    validMoves: List<DameMove>,
    isMyTurn: Boolean,
    myColor: DamePlayerColor,
    elapsedSeconds: Int,
    isComplete: Boolean,
    winnerId: String?,
    isDraw: Boolean,
    showGameOver: Boolean,
    rematchStatus: String,
    rematchRequestedByMe: Boolean,
    onCellClick: (Int, Int) -> Unit,
    onRequestRematch: () -> Unit,
    onAcceptRematch: () -> Unit,
    onDeclineRematch: () -> Unit,
    onBackClick: () -> Unit,
    onGameOverDismiss: () -> Unit,
    onPlayAgain: () -> Unit
) {
    if (room == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val myPlayer = room.players[playerId]
    val opponent = room.players.values.firstOrNull { it.playerId != playerId }
    val myPieces = myPlayer?.piecesRemaining ?: 0
    val opponentPieces = opponent?.piecesRemaining ?: 0
    val iWon = winnerId == playerId

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zuruck")
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text = timeString, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Player info
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (isMyTurn && !isComplete) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(myPlayer?.displayName ?: "Du", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Steine: $myPieces", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (!isMyTurn && !isComplete) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(opponent?.displayName ?: "Gegner", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Steine: $opponentPieces", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Turn indicator
        if (!isComplete) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    room.mustContinueJump && isMyTurn -> WarningColor.copy(alpha = 0.15f)
                    isMyTurn -> SuccessColor.copy(alpha = 0.15f)
                    else -> ErrorColor.copy(alpha = 0.15f)
                }
            ) {
                Text(
                    text = when {
                        room.mustContinueJump && isMyTurn -> "Weiter springen!"
                        isMyTurn -> "Dein Zug"
                        else -> "Gegner ist dran..."
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        room.mustContinueJump && isMyTurn -> WarningColor
                        isMyTurn -> SuccessColor
                        else -> ErrorColor
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        DameBoardView(
            board = room.board,
            selectedPiece = selectedPiece,
            validMoves = validMoves,
            isMyTurn = isMyTurn && !isComplete,
            playerColor = myColor,
            onCellClick = onCellClick,
            boardSize = 320.dp
        )

        Spacer(modifier = Modifier.weight(1f))
    }

    // Game over dialog
    if (showGameOver) {
        Dialog(onDismissRequest = onGameOverDismiss) {
            Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = when {
                        isDraw -> Icons.Default.Balance
                        iWon -> Icons.Default.EmojiEvents
                        else -> Icons.Default.SentimentDissatisfied
                    }
                    val iconColor = when {
                        isDraw -> WarningColor
                        iWon -> SuccessColor
                        else -> ErrorColor
                    }

                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = iconColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            isDraw -> "Unentschieden!"
                            iWon -> "Du hast gewonnen!"
                            else -> "Du hast verloren!"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Rematch section
                    when {
                        rematchStatus == RematchStatus.PENDING.name && !rematchRequestedByMe -> {
                            Text("Gegner mochte nochmal spielen!", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onDeclineRematch, modifier = Modifier.weight(1f)) {
                                    Text("Ablehnen")
                                }
                                Button(onClick = onAcceptRematch, modifier = Modifier.weight(1f)) {
                                    Text("Annehmen")
                                }
                            }
                        }
                        rematchStatus == RematchStatus.PENDING.name && rematchRequestedByMe -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Warte auf Antwort...", style = MaterialTheme.typography.bodyMedium)
                        }
                        rematchStatus == RematchStatus.DECLINED.name -> {
                            Text("Rematch abgelehnt", style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                                Text("Zuruck zur Lobby")
                            }
                        }
                        else -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onBackClick, modifier = Modifier.weight(1f)) {
                                    Text("Verlassen")
                                }
                                Button(onClick = onRequestRematch, modifier = Modifier.weight(1f)) {
                                    Text("Nochmal")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
