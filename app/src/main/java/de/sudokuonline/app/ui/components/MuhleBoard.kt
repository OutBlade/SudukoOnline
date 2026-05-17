package de.sudokuonline.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.model.MuhleBoard
import de.sudokuonline.app.data.model.MuhleStoneOwner
import de.sudokuonline.app.ui.theme.ErrorColor
import de.sudokuonline.app.ui.theme.SuccessColor
import de.sudokuonline.app.ui.theme.WarningColor

// Stone colors
val WhiteStoneColor = Color(0xFFF5F5F5)
val BlackStoneColor = Color(0xFF424242)
val WhiteStoneBorder = Color(0xFFBDBDBD)
val BlackStoneBorder = Color(0xFF212121)

/**
 * Position coordinates for the 24 board positions
 * Normalized to 0-1 range for scaling
 *
 * Board layout:
 * 0---------1---------2
 * |         |         |
 * |   3-----4-----5   |
 * |   |     |     |   |
 * |   |  6--7--8  |   |
 * |   |  |     |  |   |
 * 9--10-11    12-13--14
 * |   |  |     |  |   |
 * |   | 15-16-17  |   |
 * |   |     |     |   |
 * |  18----19----20   |
 * |         |         |
 * 21-------22--------23
 */
val POSITION_COORDS: List<Pair<Float, Float>> = listOf(
    // Outer square top
    Pair(0f, 0f),      // 0 - top left
    Pair(0.5f, 0f),    // 1 - top middle
    Pair(1f, 0f),      // 2 - top right
    // Middle square top
    Pair(0.167f, 0.167f),  // 3
    Pair(0.5f, 0.167f),    // 4
    Pair(0.833f, 0.167f),  // 5
    // Inner square top
    Pair(0.333f, 0.333f),  // 6
    Pair(0.5f, 0.333f),    // 7
    Pair(0.667f, 0.333f),  // 8
    // Left side (middle row)
    Pair(0f, 0.5f),        // 9
    Pair(0.167f, 0.5f),    // 10
    Pair(0.333f, 0.5f),    // 11
    // Right side (middle row)
    Pair(0.667f, 0.5f),    // 12
    Pair(0.833f, 0.5f),    // 13
    Pair(1f, 0.5f),        // 14
    // Inner square bottom
    Pair(0.333f, 0.667f),  // 15
    Pair(0.5f, 0.667f),    // 16
    Pair(0.667f, 0.667f),  // 17
    // Middle square bottom
    Pair(0.167f, 0.833f),  // 18
    Pair(0.5f, 0.833f),    // 19
    Pair(0.833f, 0.833f),  // 20
    // Outer square bottom
    Pair(0f, 1f),          // 21
    Pair(0.5f, 1f),        // 22
    Pair(1f, 1f)           // 23
)

/**
 * Lines connecting positions on the board
 */
val BOARD_LINES: List<Pair<Int, Int>> = listOf(
    // Outer square
    Pair(0, 1), Pair(1, 2),
    Pair(0, 9), Pair(9, 21),
    Pair(2, 14), Pair(14, 23),
    Pair(21, 22), Pair(22, 23),
    // Middle square
    Pair(3, 4), Pair(4, 5),
    Pair(3, 10), Pair(10, 18),
    Pair(5, 13), Pair(13, 20),
    Pair(18, 19), Pair(19, 20),
    // Inner square
    Pair(6, 7), Pair(7, 8),
    Pair(6, 11), Pair(11, 15),
    Pair(8, 12), Pair(12, 17),
    Pair(15, 16), Pair(16, 17),
    // Cross lines
    Pair(1, 4), Pair(4, 7),
    Pair(9, 10), Pair(10, 11),
    Pair(12, 13), Pair(13, 14),
    Pair(16, 19), Pair(19, 22)
)

@Composable
fun MuhleBoardView(
    board: MuhleBoard,
    selectedPosition: Int,
    validMoves: List<Int>,
    removableStones: List<Int>,
    highlightedMill: List<Int>,
    isMyTurn: Boolean,
    onPositionClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    boardSize: Dp = 320.dp
) {
    val lineColor = MaterialTheme.colorScheme.outline
    val boardBackground = MaterialTheme.colorScheme.surfaceVariant

    val density = LocalDensity.current
    val boardSizePx = with(density) { boardSize.toPx() }
    val padding = with(density) { 24.dp.toPx() }
    val stoneSizePx = with(density) { 32.dp.toPx() }

    Surface(
        modifier = modifier.size(boardSize),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = boardBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Draw board lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val drawWidth = size.width - padding * 2
                val drawHeight = size.height - padding * 2

                // Draw lines
                BOARD_LINES.forEach { (from, to) ->
                    val fromCoord = POSITION_COORDS[from]
                    val toCoord = POSITION_COORDS[to]

                    drawLine(
                        color = lineColor,
                        start = Offset(
                            padding + fromCoord.first * drawWidth,
                            padding + fromCoord.second * drawHeight
                        ),
                        end = Offset(
                            padding + toCoord.first * drawWidth,
                            padding + toCoord.second * drawHeight
                        ),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Draw position circles (empty spots)
                POSITION_COORDS.forEachIndexed { index, (x, y) ->
                    val centerX = padding + x * drawWidth
                    val centerY = padding + y * drawHeight
                    val position = board.positions.getOrNull(index)
                    val isSelected = index == selectedPosition
                    val isValidMove = index in validMoves
                    val isRemovable = index in removableStones
                    val isInMill = index in highlightedMill
                    val isEmpty = position?.isEmpty() == true

                    // Draw background highlight
                    if (isSelected || isValidMove || isRemovable || isInMill) {
                        val highlightColor = when {
                            isRemovable -> ErrorColor.copy(alpha = 0.3f)
                            isSelected -> SuccessColor.copy(alpha = 0.3f)
                            isValidMove -> SuccessColor.copy(alpha = 0.5f)
                            isInMill -> WarningColor.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        }
                        drawCircle(
                            color = highlightColor,
                            radius = stoneSizePx / 2 + 4.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )
                    }

                    // Draw stone or empty spot
                    when (position?.owner) {
                        MuhleStoneOwner.PLAYER_1.name -> {
                            // White stone
                            drawCircle(
                                color = WhiteStoneColor,
                                radius = stoneSizePx / 2,
                                center = Offset(centerX, centerY),
                                style = Fill
                            )
                            drawCircle(
                                color = WhiteStoneBorder,
                                radius = stoneSizePx / 2,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        MuhleStoneOwner.PLAYER_2.name -> {
                            // Black stone
                            drawCircle(
                                color = BlackStoneColor,
                                radius = stoneSizePx / 2,
                                center = Offset(centerX, centerY),
                                style = Fill
                            )
                            drawCircle(
                                color = BlackStoneBorder,
                                radius = stoneSizePx / 2,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        else -> {
                            // Empty position - small circle
                            val emptyColor = if (isValidMove) {
                                SuccessColor.copy(alpha = 0.7f)
                            } else {
                                lineColor.copy(alpha = 0.5f)
                            }
                            drawCircle(
                                color = emptyColor,
                                radius = if (isValidMove) stoneSizePx / 3 else stoneSizePx / 4,
                                center = Offset(centerX, centerY),
                                style = Fill
                            )
                        }
                    }
                }
            }

            // Clickable overlay for each position
            POSITION_COORDS.forEachIndexed { index, (x, y) ->
                val position = board.positions.getOrNull(index)
                val isValidMove = index in validMoves
                val isRemovable = index in removableStones
                val hasStone = position?.owner != MuhleStoneOwner.EMPTY.name

                val clickable = isMyTurn && (isValidMove || isRemovable || hasStone)

                if (clickable) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((boardSize - 48.dp) * x) + 24.dp - 20.dp,
                                y = ((boardSize - 48.dp) * y) + 24.dp - 20.dp
                            )
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onPositionClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun MuhleStoneIndicator(
    isPlayer1: Boolean,
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val stoneColor = if (isPlayer1) WhiteStoneColor else BlackStoneColor
    val borderColor = if (isPlayer1) WhiteStoneBorder else BlackStoneBorder

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Stone preview
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(stoneColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = borderColor,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        androidx.compose.material3.Text(
            text = "$label: $count",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
