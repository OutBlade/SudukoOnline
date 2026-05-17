package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DameWaitingRoomScreen(
    room: DameRoom?,
    currentPlayerId: String,
    onStartGame: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    if (room == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isHost = room.hostId == currentPlayerId
    val hasEnoughPlayers = room.players.size >= 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Warteraum", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onLeaveRoom) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Verlassen")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Room code
            if (room.isPrivate && room.code.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Raum-Code", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = room.code,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("Teile diesen Code mit deinem Gegner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Players
            Text("Spieler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            // Player 1 (White)
            val whitePlayer = room.players.values.firstOrNull { it.playerColor == DamePlayerColor.WHITE.name }
            DameWaitingPlayerCard(
                player = whitePlayer,
                colorName = "Weiss",
                isWhite = true,
                isCurrentPlayer = whitePlayer?.playerId == currentPlayerId
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("vs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Player 2 (Black)
            val blackPlayer = room.players.values.firstOrNull { it.playerColor == DamePlayerColor.BLACK.name }
            DameWaitingPlayerCard(
                player = blackPlayer,
                colorName = "Schwarz",
                isWhite = false,
                isCurrentPlayer = blackPlayer?.playerId == currentPlayerId
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!hasEnoughPlayers) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Warten auf Gegner...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isHost) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasEnoughPlayers
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Spiel starten")
                }
            } else {
                Text(
                    text = if (hasEnoughPlayers) "Warte auf Host zum Starten..." else "Warten auf Gegner...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLeaveRoom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Raum verlassen")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DameWaitingPlayerCard(
    player: DameRoomPlayer?,
    colorName: String,
    isWhite: Boolean,
    isCurrentPlayer: Boolean
) {
    val pieceColor = if (isWhite) Color(0xFFFFF8E1) else Color(0xFF3E2723)
    val borderColor = if (isWhite) Color(0xFF8D6E63) else Color(0xFF1B0000)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(pieceColor)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = borderColor, style = Stroke(width = 2.dp.toPx()))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player?.displayName ?: "Warten...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (player != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = colorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (player != null) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}
