package de.sudokuonline.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.SudokuBoard
import de.sudokuonline.app.data.model.SudokuCell
import de.sudokuonline.app.ui.theme.*

@Composable
fun SudokuBoard(
    board: SudokuBoard,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    myPlayerId: String = "",
    highlightPlayerCells: Boolean = false
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkTheme) BoardBackgroundDark else BoardBackground)
            .border(
                width = 3.dp,
                color = if (isDarkTheme) GridLineBoldDark else GridLineBold,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            for (row in 0 until 9) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (col in 0 until 9) {
                        val cell = board.cells[row][col]
                        val isSelected = selectedCell == Pair(row, col)
                        val isHighlighted = selectedCell?.let { (selRow, selCol) ->
                            row == selRow || col == selCol ||
                            (row / 3 == selRow / 3 && col / 3 == selCol / 3)
                        } ?: false
                        val hasSameNumber = selectedCell?.let { (selRow, selCol) ->
                            val selectedValue = board.cells[selRow][selCol].value
                            selectedValue != 0 && cell.value == selectedValue
                        } ?: false
                        
                        SudokuCell(
                            cell = cell,
                            isSelected = isSelected,
                            isHighlighted = isHighlighted && !isSelected,
                            hasSameNumber = hasSameNumber && !isSelected,
                            onClick = { onCellClick(row, col) },
                            modifier = Modifier.weight(1f),
                            showRightBorder = col % 3 == 2 && col != 8,
                            showBottomBorder = row % 3 == 2 && row != 8,
                            isMyCell = highlightPlayerCells && cell.enteredBy == myPlayerId,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
        
        // Draw grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / 9
            val cellHeight = size.height / 9
            
            val thinLineColor = if (isDarkTheme) GridLineLightDark else GridLineLight
            val boldLineColor = if (isDarkTheme) GridLineBoldDark else GridLineBold
            
            // Thin lines
            for (i in 1 until 9) {
                if (i % 3 != 0) {
                    // Vertical
                    drawLine(
                        color = thinLineColor,
                        start = Offset(cellWidth * i, 0f),
                        end = Offset(cellWidth * i, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Horizontal
                    drawLine(
                        color = thinLineColor,
                        start = Offset(0f, cellHeight * i),
                        end = Offset(size.width, cellHeight * i),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            
            // Bold lines (3x3 boxes)
            for (i in 1 until 3) {
                // Vertical
                drawLine(
                    color = boldLineColor,
                    start = Offset(cellWidth * i * 3, 0f),
                    end = Offset(cellWidth * i * 3, size.height),
                    strokeWidth = 2.dp.toPx()
                )
                // Horizontal
                drawLine(
                    color = boldLineColor,
                    start = Offset(0f, cellHeight * i * 3),
                    end = Offset(size.width, cellHeight * i * 3),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun SudokuCell(
    cell: SudokuCell,
    isSelected: Boolean,
    isHighlighted: Boolean,
    hasSameNumber: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRightBorder: Boolean = false,
    showBottomBorder: Boolean = false,
    isMyCell: Boolean = false,
    isDarkTheme: Boolean = false
) {
    val backgroundColor = when {
        isSelected -> if (isDarkTheme) CellSelectedDark else CellSelected
        hasSameNumber -> if (isDarkTheme) CellSameNumberDark else CellSameNumber
        cell.isError -> if (isDarkTheme) CellErrorDark else CellError
        isHighlighted -> if (isDarkTheme) CellHighlightedDark else CellHighlighted
        cell.isFixed -> if (isDarkTheme) CellFixedDark else CellFixed
        else -> if (isDarkTheme) CellDefaultDark else CellDefault
    }
    
    val textColor = when {
        cell.isError -> TextError
        cell.isFixed -> if (isDarkTheme) TextFixedDark else TextFixed
        isMyCell -> Player1Color
        cell.enteredBy != null -> Player2Color
        else -> if (isDarkTheme) TextUserDark else TextUser
    }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (cell.value != 0) {
            Text(
                text = cell.value.toString(),
                fontSize = 24.sp,
                fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        } else if (cell.notes.isNotEmpty()) {
            // Show notes in a 3x3 grid
            NotesGrid(notes = cell.notes, isDarkTheme = isDarkTheme)
        }
    }
}

@Composable
private fun NotesGrid(
    notes: List<Int>,
    isDarkTheme: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(1.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0 until 3) {
                        val num = row * 3 + col + 1
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (num in notes) {
                                Text(
                                    text = num.toString(),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkTheme) TextSecondaryDark else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 7.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
