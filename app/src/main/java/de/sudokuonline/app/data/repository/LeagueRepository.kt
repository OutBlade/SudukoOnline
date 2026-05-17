package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

/**
 * Repository for managing leagues, rankings, and seasonal rewards
 */
class LeagueRepository private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("league_prefs", Context.MODE_PRIVATE)
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    
    private val _playerData = MutableStateFlow(loadLocalPlayerData())
    val playerData: StateFlow<PlayerLeagueData> = _playerData.asStateFlow()
    
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()
    
    private val _weeklyLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val weeklyLeaderboard: StateFlow<List<LeaderboardEntry>> = _weeklyLeaderboard.asStateFlow()
    
    private val _seasonInfo = MutableStateFlow(getCurrentSeasonInfo())
    val seasonInfo: StateFlow<SeasonInfo> = _seasonInfo.asStateFlow()
    
    init {
        checkSeasonReset()
        loadLeaderboards()
    }
    
    /**
     * Add rating points after a game
     */
    fun addRatingPoints(points: Int, gameType: String, won: Boolean) {
        val current = _playerData.value
        val newRating = (current.rating + points).coerceAtLeast(0)
        val newLeague = calculateLeague(newRating)
        val promoted = newLeague.tier > current.league.tier
        
        val updated = current.copy(
            rating = newRating,
            league = newLeague,
            weeklyPoints = current.weeklyPoints + points,
            seasonPoints = current.seasonPoints + points,
            gamesThisWeek = current.gamesThisWeek + 1,
            winsThisWeek = current.winsThisWeek + if (won) 1 else 0,
            totalGames = current.totalGames + 1,
            totalWins = current.totalWins + if (won) 1 else 0
        )
        
        _playerData.value = updated
        saveLocalPlayerData(updated)
        
        // Update Firebase
        updateFirebaseRating(updated)
        
        // Check for league promotion reward
        if (promoted) {
            // Award promotion bonus (handled by CurrencyRepository)
        }
    }
    
    /**
     * Calculate league based on rating
     */
    private fun calculateLeague(rating: Int): League {
        return when {
            rating >= 3000 -> League.GRANDMASTER
            rating >= 2500 -> League.MASTER
            rating >= 2000 -> League.DIAMOND
            rating >= 1500 -> League.PLATINUM
            rating >= 1000 -> League.GOLD
            rating >= 500 -> League.SILVER
            else -> League.BRONZE
        }
    }
    
    /**
     * Get points for a game result
     */
    fun calculatePointsEarned(
        won: Boolean,
        difficulty: String,
        timeSeconds: Int,
        perfect: Boolean,
        vsHigherRated: Boolean = false
    ): Int {
        if (!won) return -10 // Lose points for losing
        
        var points = when (difficulty) {
            "EASY" -> 10
            "MEDIUM" -> 20
            "HARD" -> 35
            "EXPERT" -> 50
            else -> 15
        }
        
        // Time bonus (faster = more points)
        val timeBonus = when {
            timeSeconds < 120 -> 15
            timeSeconds < 180 -> 10
            timeSeconds < 300 -> 5
            else -> 0
        }
        points += timeBonus
        
        // Perfect game bonus
        if (perfect) points += 20
        
        // Bonus for beating higher-rated opponent
        if (vsHigherRated) points = (points * 1.5).toInt()
        
        return points
    }
    
    /**
     * Check and handle season/week reset
     */
    private fun checkSeasonReset() {
        val current = _playerData.value
        val today = LocalDate.now()
        val currentWeek = today.get(WeekFields.ISO.weekOfYear())
        val currentSeason = getSeason(today)
        
        var updated = current
        
        // Weekly reset
        if (current.lastWeek != currentWeek) {
            // Claim weekly rewards based on position
            claimWeeklyRewards(current.weeklyPoints)
            
            updated = updated.copy(
                weeklyPoints = 0,
                gamesThisWeek = 0,
                winsThisWeek = 0,
                lastWeek = currentWeek
            )
        }
        
        // Season reset
        if (current.currentSeason != currentSeason) {
            // Claim season rewards
            claimSeasonRewards(current.seasonPoints, current.league)
            
            // Soft reset rating (keep some progress)
            val newRating = (current.rating * 0.7).toInt().coerceAtLeast(100)
            
            updated = updated.copy(
                seasonPoints = 0,
                rating = newRating,
                league = calculateLeague(newRating),
                currentSeason = currentSeason
            )
        }
        
        if (updated != current) {
            _playerData.value = updated
            saveLocalPlayerData(updated)
        }
    }
    
    private fun getSeason(date: LocalDate): Int {
        // 4 seasons per year
        return date.year * 4 + (date.monthValue - 1) / 3
    }
    
    private fun claimWeeklyRewards(points: Int) {
        // Award coins based on weekly performance
        // This would integrate with CurrencyRepository
    }
    
    private fun claimSeasonRewards(points: Int, league: League) {
        // Award coins and special rewards based on final league position
        // This would integrate with CurrencyRepository and maybe unlock special themes
    }
    
    /**
     * Load leaderboards from Firebase
     */
    private fun loadLeaderboards() {
        // Global leaderboard (top 100)
        database.child("leaderboard")
            .orderByChild("rating")
            .limitToLast(100)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = mutableListOf<LeaderboardEntry>()
                    snapshot.children.forEach { child ->
                        child.getValue(LeaderboardEntry::class.java)?.let {
                            entries.add(it)
                        }
                    }
                    _leaderboard.value = entries.sortedByDescending { it.rating }
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
        
        // Weekly leaderboard
        val weekKey = getWeekKey()
        database.child("weekly_leaderboard").child(weekKey)
            .orderByChild("weeklyPoints")
            .limitToLast(100)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = mutableListOf<LeaderboardEntry>()
                    snapshot.children.forEach { child ->
                        child.getValue(LeaderboardEntry::class.java)?.let {
                            entries.add(it)
                        }
                    }
                    _weeklyLeaderboard.value = entries.sortedByDescending { it.weeklyPoints }
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    private fun updateFirebaseRating(data: PlayerLeagueData) {
        val playerId = prefs.getString("player_id", null) ?: return
        val playerName = prefs.getString("player_name", "Spieler") ?: "Spieler"
        
        val entry = LeaderboardEntry(
            id = playerId,
            name = playerName,
            rating = data.rating,
            league = data.league.name,
            weeklyPoints = data.weeklyPoints,
            seasonPoints = data.seasonPoints,
            totalGames = data.totalGames,
            totalWins = data.totalWins
        )
        
        // Update global leaderboard
        database.child("leaderboard").child(playerId).setValue(entry)
        
        // Update weekly leaderboard
        val weekKey = getWeekKey()
        database.child("weekly_leaderboard").child(weekKey).child(playerId).setValue(entry)
    }
    
    private fun getWeekKey(): String {
        val today = LocalDate.now()
        return "${today.year}_${today.get(WeekFields.ISO.weekOfYear())}"
    }
    
    private fun getCurrentSeasonInfo(): SeasonInfo {
        val today = LocalDate.now()
        val seasonNumber = getSeason(today)
        val seasonMonth = (today.monthValue - 1) / 3 * 3 + 1
        val seasonStart = LocalDate.of(today.year, seasonMonth, 1)
        val seasonEnd = seasonStart.plusMonths(3).minusDays(1)
        val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, seasonEnd).toInt()
        
        val seasonName = when ((today.monthValue - 1) / 3) {
            0 -> "Frühling"
            1 -> "Sommer"
            2 -> "Herbst"
            else -> "Winter"
        }
        
        return SeasonInfo(
            number = seasonNumber,
            name = "$seasonName ${today.year}",
            daysRemaining = daysRemaining.coerceAtLeast(0),
            endDate = seasonEnd.toString()
        )
    }
    
    private fun loadLocalPlayerData(): PlayerLeagueData {
        return PlayerLeagueData(
            rating = prefs.getInt("rating", 100),
            league = League.valueOf(prefs.getString("league", "BRONZE") ?: "BRONZE"),
            weeklyPoints = prefs.getInt("weekly_points", 0),
            seasonPoints = prefs.getInt("season_points", 0),
            gamesThisWeek = prefs.getInt("games_this_week", 0),
            winsThisWeek = prefs.getInt("wins_this_week", 0),
            totalGames = prefs.getInt("total_games", 0),
            totalWins = prefs.getInt("total_wins", 0),
            lastWeek = prefs.getInt("last_week", 0),
            currentSeason = prefs.getInt("current_season", 0)
        )
    }
    
    private fun saveLocalPlayerData(data: PlayerLeagueData) {
        prefs.edit()
            .putInt("rating", data.rating)
            .putString("league", data.league.name)
            .putInt("weekly_points", data.weeklyPoints)
            .putInt("season_points", data.seasonPoints)
            .putInt("games_this_week", data.gamesThisWeek)
            .putInt("wins_this_week", data.winsThisWeek)
            .putInt("total_games", data.totalGames)
            .putInt("total_wins", data.totalWins)
            .putInt("last_week", data.lastWeek)
            .putInt("current_season", data.currentSeason)
            .apply()
    }
    
    /**
     * Get player's current rank in leaderboard
     */
    fun getPlayerRank(): Int {
        val playerId = prefs.getString("player_id", null) ?: return 0
        return _leaderboard.value.indexOfFirst { it.id == playerId } + 1
    }
    
    /**
     * Get player's weekly rank
     */
    fun getWeeklyRank(): Int {
        val playerId = prefs.getString("player_id", null) ?: return 0
        return _weeklyLeaderboard.value.indexOfFirst { it.id == playerId } + 1
    }
    
    companion object {
        @Volatile
        private var instance: LeagueRepository? = null
        
        fun getInstance(context: Context): LeagueRepository {
            return instance ?: synchronized(this) {
                instance ?: LeagueRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

// Data classes
enum class League(val tier: Int, val displayName: String, val color: Long, val icon: String) {
    BRONZE(1, "Bronze", 0xFFCD7F32, "🥉"),
    SILVER(2, "Silber", 0xFFC0C0C0, "🥈"),
    GOLD(3, "Gold", 0xFFFFD700, "🥇"),
    PLATINUM(4, "Platin", 0xFFE5E4E2, "💎"),
    DIAMOND(5, "Diamant", 0xFFB9F2FF, "💠"),
    MASTER(6, "Meister", 0xFF9B30FF, "👑"),
    GRANDMASTER(7, "Großmeister", 0xFFFF4500, "🏆")
}

data class PlayerLeagueData(
    val rating: Int = 100,
    val league: League = League.BRONZE,
    val weeklyPoints: Int = 0,
    val seasonPoints: Int = 0,
    val gamesThisWeek: Int = 0,
    val winsThisWeek: Int = 0,
    val totalGames: Int = 0,
    val totalWins: Int = 0,
    val lastWeek: Int = 0,
    val currentSeason: Int = 0
)

data class LeaderboardEntry(
    val id: String = "",
    val name: String = "",
    val rating: Int = 0,
    val league: String = "BRONZE",
    val weeklyPoints: Int = 0,
    val seasonPoints: Int = 0,
    val totalGames: Int = 0,
    val totalWins: Int = 0
)

data class SeasonInfo(
    val number: Int = 0,
    val name: String = "",
    val daysRemaining: Int = 0,
    val endDate: String = ""
)
