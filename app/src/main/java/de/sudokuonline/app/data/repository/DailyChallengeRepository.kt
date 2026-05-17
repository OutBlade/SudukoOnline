package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.game.SudokuGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for Daily Challenge feature.
 * Generates a consistent puzzle for each day that all players share.
 */
class DailyChallengeRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _challengeState = MutableStateFlow(loadChallengeState())
    val challengeState: StateFlow<DailyChallengeState> = _challengeState.asStateFlow()

    private val _streakInfo = MutableStateFlow(loadStreakInfo())
    val streakInfo: StateFlow<StreakInfo> = _streakInfo.asStateFlow()

    private fun loadChallengeState(): DailyChallengeState {
        val today = getTodayString()
        val lastPlayedDate = prefs.getString(KEY_LAST_PLAYED_DATE, null)
        val completed = prefs.getBoolean(KEY_COMPLETED, false) && lastPlayedDate == today
        val bestTime = prefs.getInt(KEY_BEST_TIME, 0)
        val savedBoard = prefs.getString(KEY_SAVED_BOARD, null)
        val savedSolution = prefs.getString(KEY_SAVED_SOLUTION, null)

        // Check if we need a new puzzle
        if (lastPlayedDate != today) {
            // Generate new puzzle for today
            val (board, solution) = generateDailyPuzzle(today)
            return DailyChallengeState(
                date = today,
                board = board,
                solution = solution,
                completed = false,
                bestTime = 0,
                difficulty = getDifficultyForDay()
            )
        }

        return DailyChallengeState(
            date = today,
            board = savedBoard ?: "",
            solution = savedSolution ?: "",
            completed = completed,
            bestTime = if (completed) bestTime else 0,
            difficulty = getDifficultyForDay()
        )
    }

    private fun loadStreakInfo(): StreakInfo {
        return StreakInfo(
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0),
            totalCompleted = prefs.getInt(KEY_TOTAL_COMPLETED, 0),
            lastCompletedDate = prefs.getString(KEY_LAST_COMPLETED_DATE, null)
        )
    }

    /**
     * Generate a deterministic puzzle based on the date.
     * All players get the same puzzle on the same day.
     */
    private fun generateDailyPuzzle(dateString: String): Pair<String, String> {
        // Use date as seed for consistent puzzle generation
        val seed = dateString.hashCode().toLong()

        val difficulty = getDifficultyForDay()
        val generator = SudokuGenerator()

        // Generate puzzle with seeded random
        val (board, solution) = generator.generateWithSeed(difficulty, seed)

        // Convert to string for storage
        val boardString = board.flatten().joinToString("")
        val solutionString = solution.flatten().joinToString("")

        // Save for later
        prefs.edit()
            .putString(KEY_SAVED_BOARD, boardString)
            .putString(KEY_SAVED_SOLUTION, solutionString)
            .putString(KEY_LAST_PLAYED_DATE, dateString)
            .putBoolean(KEY_COMPLETED, false)
            .apply()

        return Pair(boardString, solutionString)
    }

    /**
     * Get difficulty that rotates based on day of week
     */
    private fun getDifficultyForDay(): Difficulty {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> Difficulty.EASY
            Calendar.TUESDAY -> Difficulty.MEDIUM
            Calendar.WEDNESDAY -> Difficulty.MEDIUM
            Calendar.THURSDAY -> Difficulty.HARD
            Calendar.FRIDAY -> Difficulty.HARD
            Calendar.SATURDAY -> Difficulty.EXPERT
            Calendar.SUNDAY -> Difficulty.EXPERT
            else -> Difficulty.MEDIUM
        }
    }

    /**
     * Mark today's challenge as completed
     */
    fun completeChallenge(timeSeconds: Int): CompletionResult {
        val today = getTodayString()
        val currentState = _challengeState.value
        val currentStreak = _streakInfo.value

        if (currentState.completed) {
            // Already completed today
            return CompletionResult(
                isNewRecord = false,
                previousBest = currentState.bestTime,
                newStreak = currentStreak.currentStreak
            )
        }

        val isNewRecord = currentState.bestTime == 0 || timeSeconds < currentState.bestTime

        // Update streak
        val yesterday = getYesterdayString()
        val newStreak = if (currentStreak.lastCompletedDate == yesterday) {
            currentStreak.currentStreak + 1
        } else if (currentStreak.lastCompletedDate == today) {
            currentStreak.currentStreak
        } else {
            1 // Start new streak
        }

        val newBestStreak = maxOf(currentStreak.bestStreak, newStreak)

        // Save completion
        prefs.edit()
            .putBoolean(KEY_COMPLETED, true)
            .putInt(KEY_BEST_TIME, timeSeconds)
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putInt(KEY_BEST_STREAK, newBestStreak)
            .putInt(KEY_TOTAL_COMPLETED, currentStreak.totalCompleted + 1)
            .putString(KEY_LAST_COMPLETED_DATE, today)
            .apply()

        // Update state
        _challengeState.value = currentState.copy(
            completed = true,
            bestTime = timeSeconds
        )

        _streakInfo.value = StreakInfo(
            currentStreak = newStreak,
            bestStreak = newBestStreak,
            totalCompleted = currentStreak.totalCompleted + 1,
            lastCompletedDate = today
        )

        return CompletionResult(
            isNewRecord = isNewRecord,
            previousBest = currentState.bestTime,
            newStreak = newStreak
        )
    }

    /**
     * Get the board as 2D array for game screen
     */
    fun getBoardArray(): List<List<Int>> {
        val boardString = _challengeState.value.board
        if (boardString.length != 81) {
            // Generate new if invalid
            val today = getTodayString()
            val (board, _) = generateDailyPuzzle(today)
            return board.chunked(9).map { row ->
                row.map { it.digitToIntOrNull() ?: 0 }
            }
        }
        return boardString.chunked(9).map { row ->
            row.map { it.digitToIntOrNull() ?: 0 }
        }
    }

    /**
     * Get the solution as 2D array
     */
    fun getSolutionArray(): List<List<Int>> {
        val solutionString = _challengeState.value.solution
        if (solutionString.length != 81) return emptyList()
        return solutionString.chunked(9).map { row ->
            row.map { it.digitToIntOrNull() ?: 0 }
        }
    }

    /**
     * Get time until next challenge
     */
    fun getTimeUntilNextChallenge(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Set to midnight tomorrow
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis - now
    }

    /**
     * Check if today's challenge is available (not completed)
     */
    fun isChallengeAvailable(): Boolean {
        val today = getTodayString()
        val lastPlayed = prefs.getString(KEY_LAST_PLAYED_DATE, null)
        val completed = prefs.getBoolean(KEY_COMPLETED, false)

        return lastPlayed != today || !completed
    }

    /**
     * Refresh state (for when returning to screen)
     */
    fun refresh() {
        _challengeState.value = loadChallengeState()
        _streakInfo.value = loadStreakInfo()
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getYesterdayString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    companion object {
        private const val PREFS_NAME = "daily_challenge"
        private const val KEY_LAST_PLAYED_DATE = "last_played_date"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_BEST_TIME = "best_time"
        private const val KEY_SAVED_BOARD = "saved_board"
        private const val KEY_SAVED_SOLUTION = "saved_solution"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_TOTAL_COMPLETED = "total_completed"
        private const val KEY_LAST_COMPLETED_DATE = "last_completed_date"

        @Volatile
        private var instance: DailyChallengeRepository? = null

        fun getInstance(context: Context): DailyChallengeRepository {
            return instance ?: synchronized(this) {
                instance ?: DailyChallengeRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

data class DailyChallengeState(
    val date: String = "",
    val board: String = "",
    val solution: String = "",
    val completed: Boolean = false,
    val bestTime: Int = 0,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

data class StreakInfo(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompleted: Int = 0,
    val lastCompletedDate: String? = null
)

data class CompletionResult(
    val isNewRecord: Boolean,
    val previousBest: Int,
    val newStreak: Int
)
