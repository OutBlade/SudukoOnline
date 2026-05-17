package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.game.KillerSudokuGenerator
import de.sudokuonline.app.ui.theme.*
import de.sudokuonline.app.util.rememberHapticFeedback
import de.sudokuonline.app.util.rememberSoundManager
import kotlinx.coroutines.delay

// Cage colors - soft pastels
private val CageColors = listOf(
    Color(0xFFFFCDD2), // Red
    Color(0xFFC8E6C9), // Green
    Color(0xFFBBDEFB), // Blue
    Color(0xFFFFF9C4), // Yellow
    Color(0xFFE1BEE7), // Purple
    Color(0xFFFFE0B2), // Orange
    Color(0xFFB2EBF2), // Cyan
    Color(0xFFF8BBD0)  // Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KillerSudokuScreen(
    difficulty: Difficulty,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val soundManager = rememberSoundManager()
    
    val generator = remember { KillerSudokuGenerator.getInstance() }
    
    // Game state
    var puzzle by remember { mutableStateOf<KillerSudokuGenerator.KillerSudokuPuzzle?>(null) }
    var board by remember { mutableStateOf(Array(9) { IntArray(9) { 0 } }) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isNotesMode by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(Array(9) { Array(9) { mutableSetOf<Int>() } }) }
    var errors by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var showGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var moveHistory by remember { mutableStateOf(listOf<Triple<Int, Int, Int>>()) }
    
    // Generate puzzle
    LaunchedEffect(Unit) {
        puzzle = generator.generate(difficulty)
        puzzle?.let { p ->
            board = Array(9) { row ->
                IntArray(9) { col ->
                    if (Pair(row, col) in p.givenCells) p.solution[row][col] else 0
                }
            }
        }
    }
    
    // Timer
    LaunchedEffect(isPaused, isComplete) {
        while (!isPaused && !isComplete) {
            delay(1000)
            elapsedSeconds++
        }
    }
    
    // Check completion
    LaunchedEffect(board) {
        puzzle?.let { p ->
            if (board.all { row -> row.all { it != 0 } }) {
                // All cells filled - check if correct
                val allCorrect = board.indices.all { row ->
                    board[row].indices.all { col ->
                        board[row][col] == p.solution[row][col]
                    }
                }
                if (allCorrect) {
                    isComplete = true
                    showGameOver = true
                    soundManager.playWin()
                    haptic.win()
                }
            }
        }
    }
    
    fun onCellClick(row: Int, col: Int) {
        puzzle?.let { p ->
            if (Pair(row, col) !in p.givenCells && board[row][col] != p.solution[row][col]) {
                selectedCell = Pair(row, col)
                haptic.tap()
            }
        }
    }
    
    fun onNumberClick(num: Int) {
        selectedCell?.let { (row, col) ->
            puzzle?.let { p ->
                if (Pair(row, col) in p.givenCells) return
                
                if (isNotesMode) {
                    val newNotes = notes.map { it.map { it.toMutableSet() }.toTypedArray() }.toTypedArray()
                    if (num in newNotes[row][col]) {
                        newNotes[row][col].remove(num)
                    } else {
                        newNotes[row][col].add(num)
                    }
                    notes = newNotes
                } else {
                    // Save for undo
                    moveHistory = moveHistory + Triple(row, col, board[row][col])
                    
                    // Check if valid
                    if (num == p.solution[row][col]) {
                        val newBoard = board.map { it.copyOf() }.toTypedArray()
                        newBoard[row][col] = num
                        board = newBoard
                        soundManager.playPlace()
                        haptic.success()
                        
                        // Clear notes for this cell and related cells
                        val newNotes = notes.map { it.map { it.toMutableSet() }.toTypedArray() }.toTypedArray()
                        newNotes[row][col].clear()
                        // Remove from row, column, and box
                        for (i in 0 until 9) {
                            newNotes[row][i].remove(num)
                            newNotes[i][col].remove(num)
                        }
                        val boxRow = (row / 3) * 3
                        val boxCol = (col / 3) * 3
                        for (r in boxRow until boxRow + 3) {
                            for (c in boxCol until boxCol + 3) {
                                newNotes[r][c].remove(num)
                            }
                        }
                        notes = newNotes
                    } else {
                        errors++
                        soundManager.playError()
                        haptic.error()
                        
                        if (errors >= 3) {
                            showGameOver = true
                        }
                    }
                }
            }
        }
    }
    
    fun onUndo() {
        if (moveHistory.isNotEmpty()) {
            val (row, col, prevValue) = moveHistory.last()
            moveHistory = moveHistory.dropLast(1)
            val newBoard = board.map { it.copyOf() }.toTypedArray()
            newBoard[row][col] = prevValue
            board = newBoard
            haptic.tap()
        }
    }
    
    fun onClear() {
        selectedCell?.let { (row, col) ->
            puzzle?.let { p ->
                if (Pair(row, col) !in p.givenCells) {
                    val newBoard = board.map { it.copyOf() }.toTypedArray()
                    newBoard[row][col] = 0
                    board = newBoard
                    
                    val newNotes = notes.map { it.map { it.toMutableSet() }.toTypedArray() }.toTypedArray()
                    newNotes[row][col].clear()
                    notes = newNotes
                    
                    haptic.tap()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Killer Sudoku")
                        Text(
                            text = difficulty.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Timer
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Errors
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (errors > 0) ErrorColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { i ->
                                Icon(
                                    if (i < errors) Icons.Default.Close else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (i < errors) ErrorColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (puzzle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Killer Sudoku Board
                KillerSudokuBoard(
                    board = board,
                    puzzle = puzzle!!,
                    selectedCell = selectedCell,
                    notes = notes,
                    onCellClick = ::onCellClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Undo
                    IconButton(
                        onClick = { onUndo() },
                        enabled = moveHistory.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Rückgängig")
                    }
                    
                    // Notes mode
                    FilledTonalIconButton(
                        onClick = { isNotesMode = !isNotesMode },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isNotesMode) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Notizen",
                            tint = if (isNotesMode) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Clear
                    IconButton(onClick = { onClear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Number pad
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (num in 1..5) {
                            NumberButton(
                                number = num,
                                onClick = { onNumberClick(num) },
                                isNotesMode = isNotesMode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (num in 6..9) {
                            NumberButton(
                                number = num,
                                onClick = { onNumberClick(num) },
                                isNotesMode = isNotesMode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    
    // Game Over Dialog
    if (showGameOver) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    if (isComplete) Icons.Default.EmojiEvents else Icons.Default.SentimentDissatisfied,
                    contentDescription = null,
                    tint = if (isComplete) Color(0xFFFFD700) else ErrorColor,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (isComplete) "Gewonnen!" else "Spiel vorbei",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isComplete) {
                        Text("Du hast das Killer Sudoku gelöst!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Zeit: ${elapsedSeconds / 60}:${(elapsedSeconds % 60).toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text("Du hast 3 Fehler gemacht.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = onBackClick) {
                    Text("Zurück")
                }
            }
        )
    }
}

@Composable
private fun KillerSudokuBoard(
    board: Array<IntArray>,
    puzzle: KillerSudokuGenerator.KillerSudokuPuzzle,
    selectedCell: Pair<Int, Int>?,
    notes: Array<Array<MutableSet<Int>>>,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize = 36.dp
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        // Draw cage backgrounds and borders
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / 9
            val cellHeight = size.height / 9
            
            // Draw cage backgrounds
            puzzle.cages.forEach { cage ->
                val color = CageColors[cage.color % CageColors.size]
                cage.cells.forEach { (row, col) ->
                    drawRect(
                        color = color,
                        topLeft = Offset(col * cellWidth, row * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }
            
            // Draw cage borders (dashed)
            puzzle.cages.forEach { cage ->
                val cageSet = cage.cells.toSet()
                cage.cells.forEach { (row, col) ->
                    val x = col * cellWidth
                    val y = row * cellHeight
                    
                    // Draw border on sides where adjacent cell is not in same cage
                    // Top
                    if (Pair(row - 1, col) !in cageSet) {
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(x, y),
                            end = Offset(x + cellWidth, y),
                            strokeWidth = 2f
                        )
                    }
                    // Bottom
                    if (Pair(row + 1, col) !in cageSet) {
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(x, y + cellHeight),
                            end = Offset(x + cellWidth, y + cellHeight),
                            strokeWidth = 2f
                        )
                    }
                    // Left
                    if (Pair(row, col - 1) !in cageSet) {
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(x, y),
                            end = Offset(x, y + cellHeight),
                            strokeWidth = 2f
                        )
                    }
                    // Right
                    if (Pair(row, col + 1) !in cageSet) {
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(x + cellWidth, y),
                            end = Offset(x + cellWidth, y + cellHeight),
                            strokeWidth = 2f
                        )
                    }
                }
            }
            
            // Draw 3x3 box borders
            for (i in 0..3) {
                // Vertical lines
                drawLine(
                    color = Color.Black,
                    start = Offset(i * 3 * cellWidth, 0f),
                    end = Offset(i * 3 * cellWidth, size.height),
                    strokeWidth = 3f
                )
                // Horizontal lines
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, i * 3 * cellHeight),
                    end = Offset(size.width, i * 3 * cellHeight),
                    strokeWidth = 3f
                )
            }
            
            // Draw thin grid lines
            for (i in 0..9) {
                if (i % 3 != 0) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(i * cellWidth, 0f),
                        end = Offset(i * cellWidth, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(0f, i * cellHeight),
                        end = Offset(size.width, i * cellHeight),
                        strokeWidth = 1f
                    )
                }
            }
        }
        
        // Draw cells
        Column {
            for (row in 0 until 9) {
                Row {
                    for (col in 0 until 9) {
                        val isSelected = selectedCell == Pair(row, col)
                        val isGiven = Pair(row, col) in puzzle.givenCells
                        val value = board[row][col]
                        val cellNotes = notes[row][col]
                        
                        // Find if this is the top-left cell of a cage (to show sum)
                        val cage = puzzle.cages.find { it.cells.contains(Pair(row, col)) }
                        val isTopLeftOfCage = cage?.cells?.minWithOrNull(
                            compareBy({ it.first }, { it.second })
                        ) == Pair(row, col)
                        
                        KillerSudokuCell(
                            value = value,
                            notes = cellNotes,
                            isSelected = isSelected,
                            isGiven = isGiven,
                            cageSum = if (isTopLeftOfCage) cage?.targetSum else null,
                            onClick = { onCellClick(row, col) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KillerSudokuCell(
    value: Int,
    notes: Set<Int>,
    isSelected: Boolean,
    isGiven: Boolean,
    cageSum: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Cage sum in top-left corner
        if (cageSum != null) {
            Text(
                text = "$cageSum",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
            )
        }
        
        if (value != 0) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isGiven) FontWeight.Bold else FontWeight.Normal,
                color = if (isGiven) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.primary
            )
        } else if (notes.isNotEmpty()) {
            // Show notes
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                notes.forEach { note ->
                    val noteRow = (note - 1) / 3
                    val noteCol = (note - 1) % 3
                    Text(
                        text = "$note",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(
                                when (noteRow * 3 + noteCol) {
                                    0 -> Alignment.TopStart
                                    1 -> Alignment.TopCenter
                                    2 -> Alignment.TopEnd
                                    3 -> Alignment.CenterStart
                                    4 -> Alignment.Center
                                    5 -> Alignment.CenterEnd
                                    6 -> Alignment.BottomStart
                                    7 -> Alignment.BottomCenter
                                    8 -> Alignment.BottomEnd
                                    else -> Alignment.Center
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberButton(
    number: Int,
    onClick: () -> Unit,
    isNotesMode: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isNotesMode)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isNotesMode)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
