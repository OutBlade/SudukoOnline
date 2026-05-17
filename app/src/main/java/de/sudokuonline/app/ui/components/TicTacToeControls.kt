package de.sudokuonline.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.TicTacToeGameMode
import de.sudokuonline.app.viewmodel.TicTacToeActionMode

/**
 * Action mode selector for TicTacToe bomb modes
 */
@Composable
fun TicTacToeActionSelector(
    currentMode: TicTacToeActionMode,
    gameMode: TicTacToeGameMode,
    mySymbol: Int,
    bombsRemaining: Int,
    isMyTurn: Boolean,
    onModeChange: (TicTacToeActionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't show for classic mode
    if (gameMode == TicTacToeGameMode.CLASSIC) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol mode button
            ActionButton(
                label = if (mySymbol == 1) "X" else "O",
                isSelected = currentMode == TicTacToeActionMode.SYMBOL,
                isEnabled = isMyTurn,
                color = if (mySymbol == 1) XColor else OColor,
                onClick = { onModeChange(TicTacToeActionMode.SYMBOL) }
            )

            // Bomb mode button
            ActionButton(
                label = "💣",
                subtitle = "$bombsRemaining übrig",
                isSelected = currentMode == TicTacToeActionMode.BOMB,
                isEnabled = isMyTurn && bombsRemaining > 0,
                color = BombColor,
                onClick = { onModeChange(TicTacToeActionMode.BOMB) }
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    color: Color,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> color.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "actionBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> color
            isEnabled -> color.copy(alpha = 0.5f)
            else -> Color.Gray.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "actionBorder"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) color else Color.Gray
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
            )
        }
    }
}

/**
 * Turn indicator showing whose turn it is
 */
@Composable
fun TurnIndicator(
    isMyTurn: Boolean,
    mySymbol: Int,
    myName: String,
    opponentName: String,
    modifier: Modifier = Modifier
) {
    val currentPlayerName = if (isMyTurn) myName else opponentName
    val currentSymbol = if (isMyTurn) mySymbol else (3 - mySymbol)  // Toggle between 1 and 2
    val symbolText = if (currentSymbol == 1) "X" else "O"
    val symbolColor = if (currentSymbol == 1) XColor else OColor

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isMyTurn) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = symbolText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = symbolColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isMyTurn) "Dein Zug" else "$opponentName ist dran",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isMyTurn) {
                    Text(
                        text = "Tippe auf ein Feld",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Player info card showing bombs and stats
 */
@Composable
fun PlayerInfoCard(
    name: String,
    symbol: Int,
    bombsRemaining: Int,
    isCurrentPlayer: Boolean,
    gameMode: TicTacToeGameMode,
    modifier: Modifier = Modifier
) {
    val symbolText = if (symbol == 1) "X" else "O"
    val symbolColor = if (symbol == 1) XColor else OColor

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentPlayer) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isCurrentPlayer) {
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(symbolColor)
            )
        } else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Symbol badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(symbolColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbolText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = symbolColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (gameMode != TicTacToeGameMode.CLASSIC) {
                    Text(
                        text = "💣 $bombsRemaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Game mode info card
 */
@Composable
fun GameModeInfoCard(
    gameMode: TicTacToeGameMode,
    boardSize: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = gameMode.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = boardSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bomb mode info
            if (gameMode != TicTacToeGameMode.CLASSIC) {
                val bombInfo = when (gameMode) {
                    TicTacToeGameMode.BOMB -> "✚ Kreuz"
                    TicTacToeGameMode.L_BOMB -> "⌐ L-Form"
                    else -> ""
                }
                Text(
                    text = bombInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = BombColor
                )
            }
        }
    }
}
