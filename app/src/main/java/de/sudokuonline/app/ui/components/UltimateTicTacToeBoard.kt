package de.sudokuonline.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.UltimateTicTacToeBoard
import de.sudokuonline.app.data.model.UltimateMiniBoard
import de.sudokuonline.app.ui.theme.SuccessColor

/**
 * Ultimate TicTacToe Board Composable
 * Displays a 3x3 grid of mini TicTacToe boards
 */
@Composable
fun UltimateTicTacToeBoard(
    board: UltimateTicTacToeBoard,
    playableBoards: List<Pair<Int, Int>>,
    isMyTurn: Boolean,
    mySymbol: Int,
    onCellClick: (boardRow: Int, boardCol: Int, cellRow: Int, cellCol: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (boardRow in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (boardCol in 0..2) {
                        val miniBoard = board.miniBoards[boardRow][boardCol]
                        val isPlayable = Pair(boardRow, boardCol) in playableBoards

                        UltimateMiniBoard(
                            miniBoard = miniBoard,
                            isPlayable = isPlayable && isMyTurn,
                            mySymbol = mySymbol,
                            onCellClick = { cellRow, cellCol ->
                                onCellClick(boardRow, boardCol, cellRow, cellCol)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UltimateMiniBoard(
    miniBoard: UltimateMiniBoard,
    isPlayable: Boolean,
    mySymbol: Int,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            miniBoard.winner == 1 -> XColor
            miniBoard.winner == 2 -> OColor
            miniBoard.winner == 3 -> MaterialTheme.colorScheme.outline
            isPlayable -> SuccessColor
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            miniBoard.winner == 1 -> XColor.copy(alpha = 0.1f)
            miniBoard.winner == 2 -> OColor.copy(alpha = 0.1f)
            miniBoard.winner == 3 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            isPlayable -> SuccessColor.copy(alpha = 0.05f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (miniBoard.isFinished() && miniBoard.winner in 1..2) {
            // Show big X or O for won boards
            Text(
                text = if (miniBoard.winner == 1) "X" else "O",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (miniBoard.winner == 1) XColor else OColor
            )
        } else {
            // Show the mini grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (cellRow in 0..2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        for (cellCol in 0..2) {
                            val cell = miniBoard.cells[cellRow][cellCol]
                            val isClickable = isPlayable && cell.isEmpty() && !miniBoard.isFinished()

                            UltimateMiniCell(
                                value = cell.value,
                                isClickable = isClickable,
                                onClick = { onCellClick(cellRow, cellCol) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UltimateMiniCell(
    value: Int,
    isClickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            value == 1 -> XColor.copy(alpha = 0.2f)
            value == 2 -> OColor.copy(alpha = 0.2f)
            isClickable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        animationSpec = tween(200),
        label = "cellBackground"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(enabled = isClickable) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            1 -> Text(
                text = "X",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = XColor
            )
            2 -> Text(
                text = "O",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OColor
            )
        }
    }
}
