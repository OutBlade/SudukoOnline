package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing achievements and badges
 */
class AchievementsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    // Lazy initialization to avoid circular dependency
    private val currencyRepository: CurrencyRepository by lazy {
        CurrencyRepository.getInstance(context)
    }

    private val _unlockedAchievements = MutableStateFlow(loadUnlockedAchievements())
    val unlockedAchievements: StateFlow<Set<String>> = _unlockedAchievements.asStateFlow()

    private val _newAchievement = MutableStateFlow<Achievement?>(null)
    val newAchievement: StateFlow<Achievement?> = _newAchievement.asStateFlow()

    private val _progress = MutableStateFlow(loadProgress())
    val progress: StateFlow<Map<String, Int>> = _progress.asStateFlow()

    private fun loadUnlockedAchievements(): Set<String> {
        val json = prefs.getString(KEY_UNLOCKED, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    private fun loadProgress(): Map<String, Int> {
        val json = prefs.getString(KEY_PROGRESS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    private fun saveUnlockedAchievements(achievements: Set<String>) {
        prefs.edit().putString(KEY_UNLOCKED, gson.toJson(achievements)).apply()
        _unlockedAchievements.value = achievements
    }

    private fun saveProgress(progress: Map<String, Int>) {
        prefs.edit().putString(KEY_PROGRESS, gson.toJson(progress)).apply()
        _progress.value = progress
    }

    /**
     * Check and unlock achievements based on game events
     */
    fun checkAchievements(event: AchievementEvent) {
        val currentUnlocked = _unlockedAchievements.value.toMutableSet()
        val currentProgress = _progress.value.toMutableMap()
        var newlyUnlocked: Achievement? = null

        when (event) {
            is AchievementEvent.GameWon -> {
                // First Win
                if (!currentUnlocked.contains(Achievement.FIRST_WIN.id)) {
                    currentUnlocked.add(Achievement.FIRST_WIN.id)
                    newlyUnlocked = Achievement.FIRST_WIN
                }

                // Win count achievements
                val wins = (currentProgress["total_wins"] ?: 0) + 1
                currentProgress["total_wins"] = wins

                if (wins >= 10 && !currentUnlocked.contains(Achievement.WIN_10.id)) {
                    currentUnlocked.add(Achievement.WIN_10.id)
                    newlyUnlocked = Achievement.WIN_10
                }
                if (wins >= 50 && !currentUnlocked.contains(Achievement.WIN_50.id)) {
                    currentUnlocked.add(Achievement.WIN_50.id)
                    newlyUnlocked = Achievement.WIN_50
                }
                if (wins >= 100 && !currentUnlocked.contains(Achievement.WIN_100.id)) {
                    currentUnlocked.add(Achievement.WIN_100.id)
                    newlyUnlocked = Achievement.WIN_100
                }

                // Perfect game (no errors, no hints)
                if (event.errors == 0 && event.hintsUsed == 0 && !currentUnlocked.contains(Achievement.PERFECT_GAME.id)) {
                    currentUnlocked.add(Achievement.PERFECT_GAME.id)
                    newlyUnlocked = Achievement.PERFECT_GAME
                }

                // Speed achievements
                if (event.timeSeconds < 180 && event.difficulty == "MEDIUM" && !currentUnlocked.contains(Achievement.SPEED_DEMON.id)) {
                    currentUnlocked.add(Achievement.SPEED_DEMON.id)
                    newlyUnlocked = Achievement.SPEED_DEMON
                }
                if (event.timeSeconds < 300 && event.difficulty == "HARD" && !currentUnlocked.contains(Achievement.HARD_MASTER.id)) {
                    currentUnlocked.add(Achievement.HARD_MASTER.id)
                    newlyUnlocked = Achievement.HARD_MASTER
                }
                if (event.timeSeconds < 600 && event.difficulty == "EXPERT" && !currentUnlocked.contains(Achievement.EXPERT_SOLVER.id)) {
                    currentUnlocked.add(Achievement.EXPERT_SOLVER.id)
                    newlyUnlocked = Achievement.EXPERT_SOLVER
                }
            }

            is AchievementEvent.StreakReached -> {
                if (event.streak >= 3 && !currentUnlocked.contains(Achievement.STREAK_3.id)) {
                    currentUnlocked.add(Achievement.STREAK_3.id)
                    newlyUnlocked = Achievement.STREAK_3
                }
                if (event.streak >= 7 && !currentUnlocked.contains(Achievement.STREAK_7.id)) {
                    currentUnlocked.add(Achievement.STREAK_7.id)
                    newlyUnlocked = Achievement.STREAK_7
                }
                if (event.streak >= 30 && !currentUnlocked.contains(Achievement.STREAK_30.id)) {
                    currentUnlocked.add(Achievement.STREAK_30.id)
                    newlyUnlocked = Achievement.STREAK_30
                }
            }

            is AchievementEvent.DailyChallengeCompleted -> {
                val dailies = (currentProgress["daily_completed"] ?: 0) + 1
                currentProgress["daily_completed"] = dailies

                if (!currentUnlocked.contains(Achievement.DAILY_FIRST.id)) {
                    currentUnlocked.add(Achievement.DAILY_FIRST.id)
                    newlyUnlocked = Achievement.DAILY_FIRST
                }
                if (dailies >= 7 && !currentUnlocked.contains(Achievement.DAILY_WEEK.id)) {
                    currentUnlocked.add(Achievement.DAILY_WEEK.id)
                    newlyUnlocked = Achievement.DAILY_WEEK
                }
                if (dailies >= 30 && !currentUnlocked.contains(Achievement.DAILY_MONTH.id)) {
                    currentUnlocked.add(Achievement.DAILY_MONTH.id)
                    newlyUnlocked = Achievement.DAILY_MONTH
                }
            }

            is AchievementEvent.TicTacToeWon -> {
                if (!currentUnlocked.contains(Achievement.TICTACTOE_FIRST.id)) {
                    currentUnlocked.add(Achievement.TICTACTOE_FIRST.id)
                    newlyUnlocked = Achievement.TICTACTOE_FIRST
                }

                val tttWins = (currentProgress["ttt_wins"] ?: 0) + 1
                currentProgress["ttt_wins"] = tttWins

                if (tttWins >= 10 && !currentUnlocked.contains(Achievement.TICTACTOE_MASTER.id)) {
                    currentUnlocked.add(Achievement.TICTACTOE_MASTER.id)
                    newlyUnlocked = Achievement.TICTACTOE_MASTER
                }

                if (event.againstAI && event.aiStrength >= 90 && !currentUnlocked.contains(Achievement.AI_BEATER.id)) {
                    currentUnlocked.add(Achievement.AI_BEATER.id)
                    newlyUnlocked = Achievement.AI_BEATER
                }
            }

            is AchievementEvent.MuhleWon -> {
                if (!currentUnlocked.contains(Achievement.MUHLE_FIRST.id)) {
                    currentUnlocked.add(Achievement.MUHLE_FIRST.id)
                    newlyUnlocked = Achievement.MUHLE_FIRST
                }

                val muhleWins = (currentProgress["muhle_wins"] ?: 0) + 1
                currentProgress["muhle_wins"] = muhleWins

                if (muhleWins >= 10 && !currentUnlocked.contains(Achievement.MUHLE_MASTER.id)) {
                    currentUnlocked.add(Achievement.MUHLE_MASTER.id)
                    newlyUnlocked = Achievement.MUHLE_MASTER
                }
            }

            is AchievementEvent.GamesPlayed -> {
                val total = (currentProgress["total_games"] ?: 0) + 1
                currentProgress["total_games"] = total

                if (total >= 100 && !currentUnlocked.contains(Achievement.DEDICATED.id)) {
                    currentUnlocked.add(Achievement.DEDICATED.id)
                    newlyUnlocked = Achievement.DEDICATED
                }
            }
        }

        saveUnlockedAchievements(currentUnlocked)
        saveProgress(currentProgress)

        if (newlyUnlocked != null) {
            _newAchievement.value = newlyUnlocked
            // Award coins for unlocking achievement
            awardCoinsForAchievement(newlyUnlocked)
        }
    }
    
    /**
     * Award coins when an achievement is unlocked
     */
    private fun awardCoinsForAchievement(achievement: Achievement) {
        // Award coins based on achievement points (roughly 1 coin per point)
        val coinReward = achievement.points
        currencyRepository.addCoins(coinReward, CoinReason.ACHIEVEMENT)
    }

    fun clearNewAchievement() {
        _newAchievement.value = null
    }

    fun getAchievementProgress(achievement: Achievement): Float {
        val progress = _progress.value
        return when (achievement) {
            Achievement.WIN_10 -> (progress["total_wins"] ?: 0) / 10f
            Achievement.WIN_50 -> (progress["total_wins"] ?: 0) / 50f
            Achievement.WIN_100 -> (progress["total_wins"] ?: 0) / 100f
            Achievement.DAILY_WEEK -> (progress["daily_completed"] ?: 0) / 7f
            Achievement.DAILY_MONTH -> (progress["daily_completed"] ?: 0) / 30f
            Achievement.TICTACTOE_MASTER -> (progress["ttt_wins"] ?: 0) / 10f
            Achievement.MUHLE_MASTER -> (progress["muhle_wins"] ?: 0) / 10f
            Achievement.DEDICATED -> (progress["total_games"] ?: 0) / 100f
            else -> if (_unlockedAchievements.value.contains(achievement.id)) 1f else 0f
        }.coerceIn(0f, 1f)
    }

    companion object {
        private const val PREFS_NAME = "achievements"
        private const val KEY_UNLOCKED = "unlocked"
        private const val KEY_PROGRESS = "progress"

        @Volatile
        private var instance: AchievementsRepository? = null

        fun getInstance(context: Context): AchievementsRepository {
            return instance ?: synchronized(this) {
                instance ?: AchievementsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * All available achievements
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: AchievementCategory,
    val points: Int
) {
    // Sudoku Achievements
    FIRST_WIN("first_win", "Erster Sieg", "Gewinne dein erstes Sudoku", "trophy", AchievementCategory.SUDOKU, 10),
    WIN_10("win_10", "Anfänger", "Gewinne 10 Sudoku-Spiele", "star", AchievementCategory.SUDOKU, 25),
    WIN_50("win_50", "Fortgeschritten", "Gewinne 50 Sudoku-Spiele", "star_half", AchievementCategory.SUDOKU, 50),
    WIN_100("win_100", "Sudoku-Meister", "Gewinne 100 Sudoku-Spiele", "stars", AchievementCategory.SUDOKU, 100),
    PERFECT_GAME("perfect", "Perfektion", "Beende ein Spiel ohne Fehler und Hinweise", "verified", AchievementCategory.SUDOKU, 50),
    SPEED_DEMON("speed", "Blitzschnell", "Löse ein Medium-Sudoku unter 3 Minuten", "bolt", AchievementCategory.SUDOKU, 30),
    HARD_MASTER("hard_master", "Schwer gemeistert", "Löse ein schweres Sudoku unter 5 Minuten", "psychology", AchievementCategory.SUDOKU, 50),
    EXPERT_SOLVER("expert", "Experte", "Löse ein Experten-Sudoku unter 10 Minuten", "military_tech", AchievementCategory.SUDOKU, 75),

    // Streak Achievements
    STREAK_3("streak_3", "Auf Siegeskurs", "Erreiche eine 3-Tage-Serie", "local_fire_department", AchievementCategory.STREAK, 15),
    STREAK_7("streak_7", "Wochenkrieger", "Erreiche eine 7-Tage-Serie", "whatshot", AchievementCategory.STREAK, 35),
    STREAK_30("streak_30", "Unaufhaltsam", "Erreiche eine 30-Tage-Serie", "celebration", AchievementCategory.STREAK, 100),

    // Daily Challenge
    DAILY_FIRST("daily_first", "Täglicher Held", "Schließe deine erste tägliche Herausforderung ab", "calendar_today", AchievementCategory.DAILY, 10),
    DAILY_WEEK("daily_week", "Woche geschafft", "Schließe 7 tägliche Herausforderungen ab", "date_range", AchievementCategory.DAILY, 30),
    DAILY_MONTH("daily_month", "Monatschampion", "Schließe 30 tägliche Herausforderungen ab", "event_available", AchievementCategory.DAILY, 75),

    // TicTacToe
    TICTACTOE_FIRST("ttt_first", "X oder O", "Gewinne dein erstes TicTacToe", "grid_3x3", AchievementCategory.TICTACTOE, 10),
    TICTACTOE_MASTER("ttt_master", "TicTacToe-Meister", "Gewinne 10 TicTacToe-Spiele", "emoji_events", AchievementCategory.TICTACTOE, 25),
    AI_BEATER("ai_beater", "KI-Bezwinger", "Besiege die KI auf höchster Stufe", "smart_toy", AchievementCategory.TICTACTOE, 50),

    // Muhle
    MUHLE_FIRST("muhle_first", "Mühlenbauer", "Gewinne dein erstes Mühle-Spiel", "circle", AchievementCategory.MUHLE, 10),
    MUHLE_MASTER("muhle_master", "Mühlen-Meister", "Gewinne 10 Mühle-Spiele", "workspace_premium", AchievementCategory.MUHLE, 25),

    // General
    DEDICATED("dedicated", "Hingabe", "Spiele 100 Spiele insgesamt", "favorite", AchievementCategory.GENERAL, 50)
}

enum class AchievementCategory(val displayName: String) {
    SUDOKU("Sudoku"),
    STREAK("Serien"),
    DAILY("Täglich"),
    TICTACTOE("TicTacToe"),
    MUHLE("Mühle"),
    GENERAL("Allgemein")
}

sealed class AchievementEvent {
    data class GameWon(
        val gameType: String,
        val difficulty: String,
        val timeSeconds: Int,
        val errors: Int,
        val hintsUsed: Int
    ) : AchievementEvent()

    data class StreakReached(val streak: Int) : AchievementEvent()
    object DailyChallengeCompleted : AchievementEvent()
    data class TicTacToeWon(val againstAI: Boolean, val aiStrength: Int = 0) : AchievementEvent()
    object MuhleWon : AchievementEvent()
    object GamesPlayed : AchievementEvent()
}
