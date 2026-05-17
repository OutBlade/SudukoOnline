package de.sudokuonline.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.TicTacToeBoard
import de.sudokuonline.app.data.model.TicTacToeCell
import de.sudokuonline.app.ui.theme.*

/**
 * TicTacToe Board Composable
 * Supports both 3x3 and 5x5 boards with animations
 */
@Composable
fun TicTacToeBoard(
    board: TicTacToeBoard,
    winningLine: List<Pair<Int, Int>>?,
    isMyTurn: Boolean,
    mySymbol: Int,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    cellSize: Dp = if (board.size == 3) 100.dp else 70.dp
) {
    val totalSize = cellSize * board.size + (4.dp * (board.size - 1))

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width(totalSize)
        ) {
            for (row in 0 until board.size) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until board.size) {
                        val cell = board.cells[row][col]
                        val isWinningCell = winningLine?.contains(Pair(row, col)) == true

                        TicTacToeCell(
                            cell = cell,
                            isWinningCell = isWinningCell,
                            isClickable = cell.isEmpty() && isMyTurn,
                            mySymbol = mySymbol,
                            onClick = { onCellClick(row, col) },
                            size = cellSize
                        )
                    }
                }

                if (row < board.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun TicTacToeCell(
    cell: TicTacToeCell,
    isWinningCell: Boolean,
    isClickable: Boolean,
    mySymbol: Int,
    onClick: () -> Unit,
    size: Dp
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isWinningCell -> SuccessColor.copy(alpha = 0.3f)
            cell.isBomb -> WarningColor.copy(alpha = 0.2f)
            cell.isX() -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            cell.isO() -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            isClickable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "cellBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isWinningCell -> SuccessColor
            cell.isBomb -> WarningColor
            isClickable -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "cellBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (cell.isEmpty() && !cell.isBomb) 1f else 1f,
        animationSpec = tween(200),
        label = "cellScale"
    )

    val textColor = when {
        cell.isX() -> XColor
        cell.isO() -> OColor
        cell.isBomb -> BombColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isClickable) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val symbol = cell.getSymbol()
        if (symbol.isNotEmpty()) {
            Text(
                text = symbol,
                fontSize = when {
                    cell.isBomb -> (size.value * 0.5f).sp
                    size >= 100.dp -> 48.sp
                    size >= 70.dp -> 36.sp
                    else -> 28.sp
                },
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

// Colors for TicTacToe
val XColor = Color(0xFF2196F3)  // Blue
val OColor = Color(0xFFE91E63)  // Pink
val BombColor = Color(0xFFFF9800)  // Orange
