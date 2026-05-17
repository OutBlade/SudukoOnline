package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.sudokuonline.app.data.model.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for storing and retrieving game statistics
 */
class StatisticsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val _statistics = MutableStateFlow(loadStatistics())
    val statistics: StateFlow<GameStatistics> = _statistics.asStateFlow()

    private val _gameHistory = MutableStateFlow(loadGameHistory())
    val gameHistory: StateFlow<List<GameHistoryItem>> = _gameHistory.asStateFlow()

    private fun loadStatistics(): GameStatistics {
        val json = prefs.getString(KEY_STATISTICS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, GameStatistics::class.java)
            } catch (e: Exception) {
                GameStatistics()
            }
        } else {
            GameStatistics()
        }
    }

    private fun loadGameHistory(): List<GameHistoryItem> {
        val json = prefs.getString(KEY_GAME_HISTORY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<GameHistoryItem>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun saveStatistics(stats: GameStatistics) {
        prefs.edit().putString(KEY_STATISTICS, gson.toJson(stats)).apply()
        _statistics.value = stats
    }

    private fun saveGameHistory(history: List<GameHistoryItem>) {
        // Keep only last 100 games
        val trimmedHistory = history.takeLast(100)
        prefs.edit().putString(KEY_GAME_HISTORY, gson.toJson(trimmedHistory)).apply()
        _gameHistory.value = trimmedHistory
    }

    /**
     * Record a completed Sudoku game
     */
    fun recordSudokuGame(
        difficulty: Difficulty,
        won: Boolean,
        timeSeconds: Int,
        errors: Int,
        hintsUsed: Int
    ) {
        val currentStats = _statistics.value
        val currentHistory = _gameHistory.value.toMutableList()

        // Update statistics
        val newStats = currentStats.copy(
            sudokuGamesPlayed = currentStats.sudokuGamesPlayed + 1,
            sudokuGamesWon = currentStats.sudokuGamesWon + if (won) 1 else 0,
            sudokuTotalTime = currentStats.sudokuTotalTime + timeSeconds,
            sudokuBestTime = if (won && (currentStats.sudokuBestTime == 0 || timeSeconds < currentStats.sudokuBestTime)) {
                timeSeconds
            } else {
                currentStats.sudokuBestTime
            },
            sudokuCurrentStreak = if (won) currentStats.sudokuCurrentStreak + 1 else 0,
            sudokuBestStreak = maxOf(
                currentStats.sudokuBestStreak,
                if (won) currentStats.sudokuCurrentStreak + 1 else currentStats.sudokuCurrentStreak
            ),
            sudokuByDifficulty = currentStats.sudokuByDifficulty.toMutableMap().apply {
                val key = difficulty.name
                val current = this[key] ?: DifficultyStats()
                this[key] = current.copy(
                    played = current.played + 1,
                    won = current.won + if (won) 1 else 0,
                    bestTime = if (won && (current.bestTime == 0 || timeSeconds < current.bestTime)) {
                        timeSeconds
                    } else {
                        current.bestTime
                    }
                )
            }
        )

        // Add to history
        currentHistory.add(
            GameHistoryItem(
                gameType = "SUDOKU",
                difficulty = difficulty.name,
                won = won,
                timeSeconds = timeSeconds,
                errors = errors,
                hintsUsed = hintsUsed,
                timestamp = System.currentTimeMillis()
            )
        )

        saveStatistics(newStats)
        saveGameHistory(currentHistory)
    }

    /**
     * Record a completed TicTacToe game
     */
    fun recordTicTacToeGame(
        gameMode: String,
        boardSize: String,
        won: Boolean,
        isDraw: Boolean,
        timeSeconds: Int
    ) {
        val currentStats = _statistics.value
        val currentHistory = _gameHistory.value.toMutableList()

        val newStats = currentStats.copy(
            ticTacToeGamesPlayed = currentStats.ticTacToeGamesPlayed + 1,
            ticTacToeGamesWon = currentStats.ticTacToeGamesWon + if (won) 1 else 0,
            ticTacToeDraws = currentStats.ticTacToeDraws + if (isDraw) 1 else 0,
            ticTacToeCurrentStreak = if (won) currentStats.ticTacToeCurrentStreak + 1 else 0,
            ticTacToeBestStreak = maxOf(
                currentStats.ticTacToeBestStreak,
                if (won) currentStats.ticTacToeCurrentStreak + 1 else currentStats.ticTacToeCurrentStreak
            )
        )

        currentHistory.add(
            GameHistoryItem(
                gameType = "TICTACTOE",
                difficulty = "$gameMode - $boardSize",
                won = won,
                isDraw = isDraw,
                timeSeconds = timeSeconds,
                timestamp = System.currentTimeMillis()
            )
        )

        saveStatistics(newStats)
        saveGameHistory(currentHistory)
    }

    /**
     * Record a completed Muhle game
     */
    fun recordMuhleGame(
        won: Boolean,
        timeSeconds: Int
    ) {
        val currentStats = _statistics.value
        val currentHistory = _gameHistory.value.toMutableList()

        val newStats = currentStats.copy(
            muhleGamesPlayed = currentStats.muhleGamesPlayed + 1,
            muhleGamesWon = currentStats.muhleGamesWon + if (won) 1 else 0,
            muhleCurrentStreak = if (won) currentStats.muhleCurrentStreak + 1 else 0,
            muhleBestStreak = maxOf(
                currentStats.muhleBestStreak,
                if (won) currentStats.muhleCurrentStreak + 1 else currentStats.muhleCurrentStreak
            )
        )

        currentHistory.add(
            GameHistoryItem(
                gameType = "MUHLE",
                won = won,
                timeSeconds = timeSeconds,
                timestamp = System.currentTimeMillis()
            )
        )

        saveStatistics(newStats)
        saveGameHistory(currentHistory)
    }

    /**
     * Get statistics for today's games
     */
    fun getTodayStats(): TodayStats {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayGames = _gameHistory.value.filter { it.timestamp >= today }

        return TodayStats(
            gamesPlayed = todayGames.size,
            gamesWon = todayGames.count { it.won },
            totalTime = todayGames.sumOf { it.timeSeconds }
        )
    }

    /**
     * Clear all statistics (for testing/reset)
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        _statistics.value = GameStatistics()
        _gameHistory.value = emptyList()
    }
    
    /**
     * Get extended statistics for dashboard
     */
    fun getExtendedStats(): ExtendedStats {
        val history = _gameHistory.value
        val stats = _statistics.value
        
        // Calculate weekly games (last 7 days)
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val weeklyGames = MutableList(7) { 0 }
        val weeklyWins = MutableList(7) { 0 }
        
        history.forEach { game ->
            val daysAgo = ((now - game.timestamp) / dayMs).toInt()
            if (daysAgo in 0..6) {
                weeklyGames[6 - daysAgo]++
                if (game.won) weeklyWins[6 - daysAgo]++
            }
        }
        
        // Calculate average game time
        val avgTime = if (history.isNotEmpty()) {
            history.map { it.timeSeconds }.average().toInt()
        } else 0
        
        // Find favorite game type
        val gameTypeCounts = history.groupingBy { it.gameType }.eachCount()
        val favoriteType = gameTypeCounts.maxByOrNull { it.value }?.key ?: "Sudoku"
        
        // Total play time
        val totalTime = history.sumOf { it.timeSeconds } / 60
        
        // Perfect games (Sudoku with 0 errors)
        val perfectGames = history.count { it.gameType == "SUDOKU" && it.won && it.errors == 0 }
        
        // Current overall streak
        val sortedHistory = history.sortedByDescending { it.timestamp }
        var streak = 0
        for (game in sortedHistory) {
            if (game.won) streak++ else break
        }
        
        // Longest session (approximate by consecutive games within 30 min)
        var longestSession = 0
        var currentSession = 0
        val sortedByTime = history.sortedBy { it.timestamp }
        for (i in sortedByTime.indices) {
            currentSession += sortedByTime[i].timeSeconds
            if (i < sortedByTime.size - 1) {
                val gap = sortedByTime[i + 1].timestamp - sortedByTime[i].timestamp
                if (gap > 30 * 60 * 1000) { // 30 min gap
                    longestSession = maxOf(longestSession, currentSession)
                    currentSession = 0
                }
            }
        }
        longestSession = maxOf(longestSession, currentSession)
        
        return ExtendedStats(
            weeklyGames = weeklyGames,
            weeklyWins = weeklyWins,
            averageGameTime = avgTime,
            favoriteGameType = when (favoriteType) {
                "SUDOKU" -> "Sudoku"
                "TICTACTOE" -> "TicTacToe"
                "MUHLE" -> "Mühle"
                else -> favoriteType
            },
            totalPlayTimeMinutes = totalTime,
            perfectGamesCount = perfectGames,
            currentOverallStreak = streak,
            longestSession = longestSession / 60
        )
    }
    
    /**
     * Get win rate trend (last 10 games vs previous 10)
     */
    fun getWinRateTrend(): Float {
        val history = _gameHistory.value.sortedByDescending { it.timestamp }
        if (history.size < 10) return 0f
        
        val recent10 = history.take(10)
        val previous10 = history.drop(10).take(10)
        
        if (previous10.isEmpty()) return 0f
        
        val recentRate = recent10.count { it.won } / 10f
        val previousRate = previous10.count { it.won } / previous10.size.toFloat()
        
        return recentRate - previousRate
    }

    companion object {
        private const val PREFS_NAME = "game_statistics"
        private const val KEY_STATISTICS = "statistics"
        private const val KEY_GAME_HISTORY = "game_history"

        @Volatile
        private var instance: StatisticsRepository? = null

        fun getInstance(context: Context): StatisticsRepository {
            return instance ?: synchronized(this) {
                instance ?: StatisticsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Overall game statistics
 */
data class GameStatistics(
    // Sudoku stats
    val sudokuGamesPlayed: Int = 0,
    val sudokuGamesWon: Int = 0,
    val sudokuTotalTime: Int = 0,
    val sudokuBestTime: Int = 0,
    val sudokuCurrentStreak: Int = 0,
    val sudokuBestStreak: Int = 0,
    val sudokuByDifficulty: Map<String, DifficultyStats> = emptyMap(),

    // TicTacToe stats
    val ticTacToeGamesPlayed: Int = 0,
    val ticTacToeGamesWon: Int = 0,
    val ticTacToeDraws: Int = 0,
    val ticTacToeCurrentStreak: Int = 0,
    val ticTacToeBestStreak: Int = 0,

    // Muhle stats
    val muhleGamesPlayed: Int = 0,
    val muhleGamesWon: Int = 0,
    val muhleCurrentStreak: Int = 0,
    val muhleBestStreak: Int = 0
) {
    val sudokuWinRate: Float
        get() = if (sudokuGamesPlayed > 0) sudokuGamesWon.toFloat() / sudokuGamesPlayed else 0f

    val ticTacToeWinRate: Float
        get() = if (ticTacToeGamesPlayed > 0) ticTacToeGamesWon.toFloat() / ticTacToeGamesPlayed else 0f

    val muhleWinRate: Float
        get() = if (muhleGamesPlayed > 0) muhleGamesWon.toFloat() / muhleGamesPlayed else 0f

    val totalGamesPlayed: Int
        get() = sudokuGamesPlayed + ticTacToeGamesPlayed + muhleGamesPlayed

    val totalGamesWon: Int
        get() = sudokuGamesWon + ticTacToeGamesWon + muhleGamesWon
}

data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestTime: Int = 0
) {
    val winRate: Float
        get() = if (played > 0) won.toFloat() / played else 0f
}

data class GameHistoryItem(
    val gameType: String = "",
    val difficulty: String = "",
    val won: Boolean = false,
    val isDraw: Boolean = false,
    val timeSeconds: Int = 0,
    val errors: Int = 0,
    val hintsUsed: Int = 0,
    val timestamp: Long = 0
)

data class TodayStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val totalTime: Int = 0
)

/**
 * Extended statistics for dashboard
 */
data class ExtendedStats(
    val weeklyGames: List<Int> = List(7) { 0 },
    val weeklyWins: List<Int> = List(7) { 0 },
    val averageGameTime: Int = 0,
    val favoriteGameType: String = "Sudoku",
    val totalPlayTimeMinutes: Int = 0,
    val perfectGamesCount: Int = 0,
    val currentOverallStreak: Int = 0,
    val longestSession: Int = 0
)
