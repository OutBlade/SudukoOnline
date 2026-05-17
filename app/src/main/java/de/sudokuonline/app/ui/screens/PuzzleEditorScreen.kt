package de.sudokuonline.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import de.sudokuonline.app.game.SudokuSolver
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Puzzle Editor Screen - Erstelle eigene Sudoku-Rätsel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleEditorScreen(
    onBackClick: () -> Unit,
    onPlayPuzzle: (Array<IntArray>) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val puzzleStorage = remember { PuzzleStorage(context) }

    var board by remember { mutableStateOf(Array(9) { IntArray(9) { 0 } }) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var history by remember { mutableStateOf(listOf<Array<IntArray>>()) }

    var showValidationResult by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
    var showSavedPuzzles by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var puzzleName by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val savedPuzzles by puzzleStorage.savedPuzzles.collectAsState()

    // Helper to save state for undo
    fun saveState() {
        history = history + listOf(board.map { it.copyOf() }.toTypedArray())
        if (history.size > 50) {
            history = history.drop(1)
        }
    }

    fun undo() {
        if (history.isNotEmpty()) {
            board = history.last()
            history = history.dropLast(1)
        }
    }

    fun setCell(row: Int, col: Int, value: Int) {
        saveState()
        board = board.mapIndexed { r, rowArray ->
            if (r == row) {
                rowArray.mapIndexed { c, cell ->
                    if (c == col) value else cell
                }.toIntArray()
            } else rowArray
        }.toTypedArray()
    }

    fun clearBoard() {
        saveState()
        board = Array(9) { IntArray(9) { 0 } }
        selectedCell = null
    }

    fun validatePuzzle(): ValidationResult {
        val solver = SudokuSolver()
        val boardCopy = board.map { it.copyOf() }.toTypedArray()

        // Check for invalid placements
        val conflicts = mutableListOf<String>()

        for (row in 0..8) {
            for (col in 0..8) {
                val value = board[row][col]
                if (value != 0) {
                    // Temporarily remove to check conflict
                    board[row][col] = 0
                    if (!isValidPlacement(board, row, col, value)) {
                        conflicts.add("Konflikt bei Zeile ${row + 1}, Spalte ${col + 1}")
                    }
                    board[row][col] = value
                }
            }
        }

        if (conflicts.isNotEmpty()) {
            return ValidationResult(
                isValid = false,
                isSolvable = false,
                hasUniqueSolution = false,
                message = "Das Rätsel enthält Konflikte:\n${conflicts.take(3).joinToString("\n")}",
                clueCount = countClues(board)
            )
        }

        // Check if solvable
        solver.initialize(boardCopy.map { it.toList() })
        val steps = solver.solveWithSteps()
        if (!solver.isSolved()) {
            return ValidationResult(
                isValid = true,
                isSolvable = false,
                hasUniqueSolution = false,
                message = "Das Rätsel ist nicht lösbar.",
                clueCount = countClues(board)
            )
        }

        // Check for unique solution
        val solutionCount = countSolutions(board.map { it.copyOf() }.toTypedArray(), 0)
        val hasUnique = solutionCount == 1

        val clueCount = countClues(board)
        val difficulty = when {
            clueCount >= 36 -> "Sehr Leicht"
            clueCount >= 32 -> "Leicht"
            clueCount >= 28 -> "Mittel"
            clueCount >= 24 -> "Schwer"
            else -> "Experte"
        }

        return ValidationResult(
            isValid = true,
            isSolvable = true,
            hasUniqueSolution = hasUnique,
            message = if (hasUnique)
                "Gültiges Rätsel mit eindeutiger Lösung!\nSchwierigkeit: $difficulty"
            else
                "Das Rätsel hat mehrere Lösungen. Für ein echtes Sudoku sollte es nur eine geben.",
            clueCount = clueCount,
            difficulty = difficulty
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Puzzle Editor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { undo() },
                        enabled = history.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Rueckgaengig")
                    }
                    IconButton(onClick = { showSavedPuzzles = true }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Gespeicherte")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                            text = "Eingegebene Zahlen",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${countClues(board)}/81",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { showClearConfirm = true },
                            label = { Text("Leeren") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Sudoku Board
            EditorSudokuBoard(
                board = board,
                selectedCell = selectedCell,
                onCellClick = { row, col -> selectedCell = row to col }
            )

            // Number Pad
            EditorNumberPad(
                onNumberClick = { number ->
                    selectedCell?.let { (row, col) ->
                        setCell(row, col, number)
                    }
                },
                onDeleteClick = {
                    selectedCell?.let { (row, col) ->
                        setCell(row, col, 0)
                    }
                }
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        validationResult = validatePuzzle()
                        showValidationResult = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prüfen")
                }

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = countClues(board) > 0
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speichern")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        sharePuzzle(context, board)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = countClues(board) > 0
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Teilen")
                }

                Button(
                    onClick = {
                        val result = validatePuzzle()
                        if (result.isSolvable) {
                            onPlayPuzzle(board)
                        } else {
                            validationResult = result
                            showValidationResult = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = countClues(board) >= 17
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Spielen")
                }
            }
        }
    }

    // Validation Result Dialog
    if (showValidationResult && validationResult != null) {
        AlertDialog(
            onDismissRequest = { showValidationResult = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (validationResult!!.isSolvable && validationResult!!.hasUniqueSolution)
                            Icons.Default.CheckCircle
                        else if (validationResult!!.isValid)
                            Icons.Default.Warning
                        else
                            Icons.Default.Error,
                        contentDescription = null,
                        tint = when {
                            validationResult!!.isSolvable && validationResult!!.hasUniqueSolution -> Color(0xFF4CAF50)
                            validationResult!!.isValid -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text("Validierung")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(validationResult!!.message)
                    Text(
                        text = "Hinweise: ${validationResult!!.clueCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showValidationResult = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Puzzle speichern") },
            text = {
                OutlinedTextField(
                    value = puzzleName,
                    onValueChange = { puzzleName = it },
                    label = { Text("Name") },
                    placeholder = { Text("Mein Puzzle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = puzzleName.ifBlank { "Puzzle ${savedPuzzles.size + 1}" }
                        val validation = validatePuzzle()
                        puzzleStorage.savePuzzle(
                            CustomPuzzle(
                                name = name,
                                board = board.map { it.toList() },
                                createdAt = System.currentTimeMillis(),
                                difficulty = validation.difficulty,
                                clueCount = validation.clueCount
                            )
                        )
                        puzzleName = ""
                        showSaveDialog = false
                        Toast.makeText(context, "Puzzle gespeichert!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Saved Puzzles Dialog
    if (showSavedPuzzles) {
        AlertDialog(
            onDismissRequest = { showSavedPuzzles = false },
            title = { Text("Gespeicherte Puzzles") },
            text = {
                if (savedPuzzles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Noch keine Puzzles gespeichert",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedPuzzles) { puzzle ->
                            SavedPuzzleItem(
                                puzzle = puzzle,
                                onLoad = {
                                    saveState()
                                    board = puzzle.board.map { it.toIntArray() }.toTypedArray()
                                    showSavedPuzzles = false
                                },
                                onDelete = {
                                    puzzleStorage.deletePuzzle(puzzle)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedPuzzles = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    // Clear Confirm Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Spielfeld leeren?") },
            text = { Text("Alle eingegebenen Zahlen werden gelöscht.") },
            confirmButton = {
                Button(
                    onClick = {
                        clearBoard()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leeren")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun EditorSudokuBoard(
    board: Array<IntArray>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit
) {
    val cellSize = 36.dp

    Card(
        modifier = Modifier.padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.outline)
                .padding(2.dp)
        ) {
            for (row in 0..8) {
                Row {
                    for (col in 0..8) {
                        val isSelected = selectedCell == (row to col)
                        val value = board[row][col]

                        // Check for conflicts
                        val hasConflict = value != 0 && hasConflict(board, row, col, value)

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .padding(
                                    start = if (col % 3 == 0 && col > 0) 2.dp else 0.5.dp,
                                    top = if (row % 3 == 0 && row > 0) 2.dp else 0.5.dp,
                                    end = 0.5.dp,
                                    bottom = 0.5.dp
                                )
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        hasConflict -> Color(0xFFF44336).copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .clickable { onCellClick(row, col) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (value != 0) {
                                Text(
                                    text = value.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasConflict)
                                        Color(0xFFF44336)
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorNumberPad(
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Numbers 1-5
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (num in 1..5) {
                NumberPadButton(
                    number = num,
                    onClick = { onNumberClick(num) }
                )
            }
        }

        // Numbers 6-9 and Delete
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (num in 6..9) {
                NumberPadButton(
                    number = num,
                    onClick = { onNumberClick(num) }
                )
            }
            // Delete button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onDeleteClick() },
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Backspace,
                        contentDescription = "Loeschen",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberPadButton(
    number: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SavedPuzzleItem(
    puzzle: CustomPuzzle,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLoad() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.GridOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = puzzle.name,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${puzzle.clueCount} Hinweise • ${puzzle.difficulty ?: "Unbekannt"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(puzzle.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Loeschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Puzzle löschen?") },
            text = { Text("\"${puzzle.name}\" wird unwiderruflich gelöscht.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

// Data classes
data class ValidationResult(
    val isValid: Boolean,
    val isSolvable: Boolean,
    val hasUniqueSolution: Boolean,
    val message: String,
    val clueCount: Int,
    val difficulty: String? = null
)

data class CustomPuzzle(
    val name: String,
    val board: List<List<Int>>,
    val createdAt: Long,
    val difficulty: String? = null,
    val clueCount: Int = 0
)

// Puzzle Storage
class PuzzleStorage(context: Context) {
    private val prefs = context.getSharedPreferences("puzzle_editor", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _savedPuzzles = kotlinx.coroutines.flow.MutableStateFlow(loadPuzzles())
    val savedPuzzles: kotlinx.coroutines.flow.StateFlow<List<CustomPuzzle>> = _savedPuzzles

    fun savePuzzle(puzzle: CustomPuzzle) {
        val puzzles = _savedPuzzles.value.toMutableList()
        puzzles.add(0, puzzle)
        _savedPuzzles.value = puzzles
        savePuzzles(puzzles)
    }

    fun deletePuzzle(puzzle: CustomPuzzle) {
        val puzzles = _savedPuzzles.value.toMutableList()
        puzzles.removeAll { it.createdAt == puzzle.createdAt }
        _savedPuzzles.value = puzzles
        savePuzzles(puzzles)
    }

    private fun loadPuzzles(): List<CustomPuzzle> {
        val json = prefs.getString("puzzles", null) ?: return emptyList()
        return try {
            gson.fromJson(json, Array<CustomPuzzle>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePuzzles(puzzles: List<CustomPuzzle>) {
        prefs.edit().putString("puzzles", gson.toJson(puzzles)).apply()
    }
}

// Helper functions
private fun countClues(board: Array<IntArray>): Int {
    return board.sumOf { row -> row.count { it != 0 } }
}

private fun hasConflict(board: Array<IntArray>, row: Int, col: Int, value: Int): Boolean {
    // Check row
    for (c in 0..8) {
        if (c != col && board[row][c] == value) return true
    }

    // Check column
    for (r in 0..8) {
        if (r != row && board[r][col] == value) return true
    }

    // Check box
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    for (r in boxRow until boxRow + 3) {
        for (c in boxCol until boxCol + 3) {
            if ((r != row || c != col) && board[r][c] == value) return true
        }
    }

    return false
}

private fun isValidPlacement(board: Array<IntArray>, row: Int, col: Int, value: Int): Boolean {
    // Check row
    for (c in 0..8) {
        if (board[row][c] == value) return false
    }

    // Check column
    for (r in 0..8) {
        if (board[r][col] == value) return false
    }

    // Check box
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    for (r in boxRow until boxRow + 3) {
        for (c in boxCol until boxCol + 3) {
            if (board[r][c] == value) return false
        }
    }

    return true
}

private fun countSolutions(board: Array<IntArray>, limit: Int): Int {
    // Find first empty cell
    var emptyRow = -1
    var emptyCol = -1
    outer@ for (r in 0..8) {
        for (c in 0..8) {
            if (board[r][c] == 0) {
                emptyRow = r
                emptyCol = c
                break@outer
            }
        }
    }

    // No empty cell = solved
    if (emptyRow == -1) return 1

    var count = 0
    for (num in 1..9) {
        if (isValidPlacement(board, emptyRow, emptyCol, num)) {
            board[emptyRow][emptyCol] = num
            count += countSolutions(board, limit)
            board[emptyRow][emptyCol] = 0

            // Early exit if we found more than one solution (for uniqueness check)
            if (count > 1) return count
        }
    }

    return count
}

private fun sharePuzzle(context: Context, board: Array<IntArray>) {
    // Create a shareable string representation
    val puzzleString = board.joinToString("\n") { row ->
        row.joinToString(" ") { if (it == 0) "." else it.toString() }
    }

    val shareText = """
🧩 Mein Sudoku-Rätsel:

$puzzleString

Erstellt mit Sudoku Online
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Puzzle teilen"))
}
