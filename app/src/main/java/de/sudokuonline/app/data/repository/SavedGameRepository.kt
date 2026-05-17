package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.model.SudokuBoard
import de.sudokuonline.app.data.model.SudokuCell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for saving and loading game state
 * Allows players to pause and resume games later
 */
class SavedGameRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val _hasSavedGame = MutableStateFlow(checkHasSavedGame())
    val hasSavedGame: StateFlow<Boolean> = _hasSavedGame.asStateFlow()

    private val _savedGameInfo = MutableStateFlow(loadSavedGameInfo())
    val savedGameInfo: StateFlow<SavedGameInfo?> = _savedGameInfo.asStateFlow()

    private fun checkHasSavedGame(): Boolean {
        return prefs.contains(KEY_SAVED_BOARD) && prefs.contains(KEY_SAVED_SOLUTION)
    }

    private fun loadSavedGameInfo(): SavedGameInfo? {
        if (!checkHasSavedGame()) return null

        return try {
            SavedGameInfo(
                difficulty = Difficulty.valueOf(prefs.getString(KEY_DIFFICULTY, "MEDIUM") ?: "MEDIUM"),
                elapsedSeconds = prefs.getInt(KEY_ELAPSED_TIME, 0),
                errors = prefs.getInt(KEY_ERRORS, 0),
                hintsUsed = prefs.getInt(KEY_HINTS_USED, 0),
                progress = prefs.getInt(KEY_PROGRESS, 0),
                savedAt = prefs.getLong(KEY_SAVED_AT, System.currentTimeMillis()),
                gameType = prefs.getString(KEY_GAME_TYPE, "SUDOKU") ?: "SUDOKU"
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save the current Sudoku game state
     */
    fun saveSudokuGame(
        board: SudokuBoard,
        difficulty: Difficulty,
        elapsedSeconds: Int,
        errors: Int,
        hintsUsed: Int,
        selectedCell: Pair<Int, Int>?,
        isNotesMode: Boolean
    ) {
        val boardData = SavedBoardData(
            cells = board.cells.map { row ->
                row.map { cell ->
                    SavedCellData(
                        value = cell.value,
                        isFixed = cell.isFixed,
                        notes = cell.notes,
                        isError = cell.isError
                    )
                }
            },
            solution = board.solution
        )

        val progress = calculateProgress(board)

        prefs.edit()
            .putString(KEY_SAVED_BOARD, gson.toJson(boardData))
            .putString(KEY_SAVED_SOLUTION, gson.toJson(board.solution))
            .putString(KEY_DIFFICULTY, difficulty.name)
            .putInt(KEY_ELAPSED_TIME, elapsedSeconds)
            .putInt(KEY_ERRORS, errors)
            .putInt(KEY_HINTS_USED, hintsUsed)
            .putInt(KEY_PROGRESS, progress)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .putString(KEY_GAME_TYPE, "SUDOKU")
            .putInt(KEY_SELECTED_ROW, selectedCell?.first ?: -1)
            .putInt(KEY_SELECTED_COL, selectedCell?.second ?: -1)
            .putBoolean(KEY_NOTES_MODE, isNotesMode)
            .apply()

        _hasSavedGame.value = true
        _savedGameInfo.value = SavedGameInfo(
            difficulty = difficulty,
            elapsedSeconds = elapsedSeconds,
            errors = errors,
            hintsUsed = hintsUsed,
            progress = progress,
            savedAt = System.currentTimeMillis(),
            gameType = "SUDOKU"
        )
    }

    /**
     * Load the saved Sudoku game
     */
    fun loadSudokuGame(): SavedSudokuGame? {
        if (!checkHasSavedGame()) return null

        return try {
            val boardJson = prefs.getString(KEY_SAVED_BOARD, null) ?: return null
            val boardData = gson.fromJson(boardJson, SavedBoardData::class.java)

            val cells = boardData.cells.map { row ->
                row.map { cellData ->
                    SudokuCell(
                        value = cellData.value,
                        isFixed = cellData.isFixed,
                        notes = cellData.notes,
                        isError = cellData.isError,
                        enteredBy = null
                    )
                }
            }

            val board = SudokuBoard(cells = cells, solution = boardData.solution)

            val selectedRow = prefs.getInt(KEY_SELECTED_ROW, -1)
            val selectedCol = prefs.getInt(KEY_SELECTED_COL, -1)
            val selectedCell = if (selectedRow >= 0 && selectedCol >= 0) {
                Pair(selectedRow, selectedCol)
            } else null

            SavedSudokuGame(
                board = board,
                difficulty = Difficulty.valueOf(prefs.getString(KEY_DIFFICULTY, "MEDIUM") ?: "MEDIUM"),
                elapsedSeconds = prefs.getInt(KEY_ELAPSED_TIME, 0),
                errors = prefs.getInt(KEY_ERRORS, 0),
                hintsUsed = prefs.getInt(KEY_HINTS_USED, 0),
                selectedCell = selectedCell,
                isNotesMode = prefs.getBoolean(KEY_NOTES_MODE, false)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete the saved game
     */
    fun deleteSavedGame() {
        prefs.edit()
            .remove(KEY_SAVED_BOARD)
            .remove(KEY_SAVED_SOLUTION)
            .remove(KEY_DIFFICULTY)
            .remove(KEY_ELAPSED_TIME)
            .remove(KEY_ERRORS)
            .remove(KEY_HINTS_USED)
            .remove(KEY_PROGRESS)
            .remove(KEY_SAVED_AT)
            .remove(KEY_GAME_TYPE)
            .remove(KEY_SELECTED_ROW)
            .remove(KEY_SELECTED_COL)
            .remove(KEY_NOTES_MODE)
            .apply()

        _hasSavedGame.value = false
        _savedGameInfo.value = null
    }

    /**
     * Auto-save on pause or exit
     */
    fun autoSave(
        board: SudokuBoard,
        difficulty: Difficulty,
        elapsedSeconds: Int,
        errors: Int,
        hintsUsed: Int,
        selectedCell: Pair<Int, Int>?,
        isNotesMode: Boolean
    ) {
        // Only save if game is not complete
        if (!isGameComplete(board)) {
            saveSudokuGame(board, difficulty, elapsedSeconds, errors, hintsUsed, selectedCell, isNotesMode)
        }
    }

    private fun calculateProgress(board: SudokuBoard): Int {
        var filled = 0
        var total = 81

        for (row in board.cells) {
            for (cell in row) {
                if (cell.value != 0) {
                    filled++
                }
            }
        }

        return (filled * 100) / total
    }

    private fun isGameComplete(board: SudokuBoard): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board.cells[row][col].value != board.solution[row][col]) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        private const val PREFS_NAME = "saved_game"
        private const val KEY_SAVED_BOARD = "saved_board"
        private const val KEY_SAVED_SOLUTION = "saved_solution"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_ELAPSED_TIME = "elapsed_time"
        private const val KEY_ERRORS = "errors"
        private const val KEY_HINTS_USED = "hints_used"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_SAVED_AT = "saved_at"
        private const val KEY_GAME_TYPE = "game_type"
        private const val KEY_SELECTED_ROW = "selected_row"
        private const val KEY_SELECTED_COL = "selected_col"
        private const val KEY_NOTES_MODE = "notes_mode"

        @Volatile
        private var instance: SavedGameRepository? = null

        fun getInstance(context: Context): SavedGameRepository {
            return instance ?: synchronized(this) {
                instance ?: SavedGameRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Information about a saved game (for display without loading full game)
 */
data class SavedGameInfo(
    val difficulty: Difficulty,
    val elapsedSeconds: Int,
    val errors: Int,
    val hintsUsed: Int,
    val progress: Int,
    val savedAt: Long,
    val gameType: String
)

/**
 * Complete saved Sudoku game data
 */
data class SavedSudokuGame(
    val board: SudokuBoard,
    val difficulty: Difficulty,
    val elapsedSeconds: Int,
    val errors: Int,
    val hintsUsed: Int,
    val selectedCell: Pair<Int, Int>?,
    val isNotesMode: Boolean
)

/**
 * Internal data class for JSON serialization
 */
data class SavedBoardData(
    val cells: List<List<SavedCellData>>,
    val solution: List<List<Int>>
)

data class SavedCellData(
    val value: Int,
    val isFixed: Boolean,
    val notes: List<Int>,
    val isError: Boolean
)
