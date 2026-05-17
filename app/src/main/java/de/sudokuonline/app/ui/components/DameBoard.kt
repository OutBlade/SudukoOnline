package de.sudokuonline.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.model.DameBoard
import de.sudokuonline.app.data.model.DameCell
import de.sudokuonline.app.data.model.DameMove
import de.sudokuonline.app.data.model.DamePieceType
import de.sudokuonline.app.data.model.DamePlayerColor

private val LightSquareColor = Color(0xFFF0D9B5)
private val DarkSquareColor = Color(0xFFB58863)
private val WhitePieceColor = Color(0xFFFFF8E1)
private val WhitePieceBorder = Color(0xFF8D6E63)
private val BlackPieceColor = Color(0xFF3E2723)
private val BlackPieceBorder = Color(0xFF1B0000)
private val SelectedHighlight = Color(0xFF4CAF50)
private val ValidMoveColor = Color(0xFF4CAF50).copy(alpha = 0.4f)
private val ValidCaptureColor = Color(0xFFF44336).copy(alpha = 0.4f)

@Composable
fun DameBoardView(
    board: DameBoard,
    selectedPiece: Pair<Int, Int>?,
    validMoves: List<DameMove>,
    isMyTurn: Boolean,
    playerColor: DamePlayerColor,
    onCellClick: (Int, Int) -> Unit,
    boardSize: Dp = 320.dp
) {
    val flipBoard = playerColor == DamePlayerColor.BLACK
    val cellSize = boardSize / 8

    BoxWithConstraints(
        modifier = Modifier.size(boardSize),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.size(boardSize)) {
            for (displayRow in 0 until 8) {
                val row = if (flipBoard) 7 - displayRow else displayRow
                Row(modifier = Modifier.height(cellSize)) {
                    for (displayCol in 0 until 8) {
                        val col = if (flipBoard) 7 - displayCol else displayCol
                        val isDarkSquare = (row + col) % 2 == 1
                        val squareColor = if (isDarkSquare) DarkSquareColor else LightSquareColor
                        val cell = board.cells[row][col]
                        val isSelected = selectedPiece?.first == row && selectedPiece?.second == col

                        val moveToHere = validMoves.find { it.toRow == row && it.toCol == col }
                        val isValidMove = moveToHere != null
                        val isCapture = moveToHere != null && moveToHere.captures.isNotEmpty()

                        DameBoardCell(
                            cell = cell,
                            squareColor = squareColor,
                            isSelected = isSelected,
                            isValidMove = isValidMove,
                            isCapture = isCapture,
                            cellSize = cellSize,
                            onClick = { onCellClick(row, col) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DameBoardCell(
    cell: DameCell,
    squareColor: Color,
    isSelected: Boolean,
    isValidMove: Boolean,
    isCapture: Boolean,
    cellSize: Dp,
    onClick: () -> Unit
) {
    val hasPiece = cell.pieceType != DamePieceType.NONE.name
    val isKing = cell.pieceType == DamePieceType.KING.name
    val isWhiteOwner = cell.owner == DamePlayerColor.WHITE.name

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(squareColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(modifier = Modifier.size(cellSize).border(width = 2.dp, color = SelectedHighlight))
        }

        if (isValidMove && !hasPiece) {
            val indicatorColor = if (isCapture) ValidCaptureColor else ValidMoveColor
            Canvas(modifier = Modifier.size(cellSize * 0.4f)) {
                drawCircle(color = indicatorColor)
            }
        }

        if (hasPiece) {
            val pieceColor = if (isWhiteOwner) WhitePieceColor else BlackPieceColor
            val borderColor = if (isWhiteOwner) WhitePieceBorder else BlackPieceBorder
            val pieceSize = cellSize * 0.8f

            val effectiveBorderColor = when {
                isSelected -> SelectedHighlight
                isValidMove && isCapture -> ValidCaptureColor
                else -> borderColor
            }
            val borderWidth = if (isSelected) 3.dp else 2.dp

            Box(
                modifier = Modifier
                    .size(pieceSize)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(pieceColor)
                    .border(width = borderWidth, color = effectiveBorderColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isKing) {
                    val crownColor = if (isWhiteOwner) Color(0xFF8D6E63) else Color(0xFFFFD54F)
                    Canvas(modifier = Modifier.size(pieceSize * 0.5f)) {
                        val w = size.width
                        val h = size.height
                        val crownPath = Path().apply {
                            moveTo(0.1f * w, 0.75f * h)
                            lineTo(0.1f * w, 0.35f * h)
                            lineTo(0.3f * w, 0.55f * h)
                            lineTo(0.5f * w, 0.2f * h)
                            lineTo(0.7f * w, 0.55f * h)
                            lineTo(0.9f * w, 0.35f * h)
                            lineTo(0.9f * w, 0.75f * h)
                            close()
                        }
                        drawPath(crownPath, color = crownColor, style = Fill)
                        drawPath(crownPath, color = crownColor.copy(alpha = 0.8f), style = Stroke(width = 1.5f))
                    }
                }
            }
        }
    }
}
