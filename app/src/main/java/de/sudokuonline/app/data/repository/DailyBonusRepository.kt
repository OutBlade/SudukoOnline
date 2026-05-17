package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for managing daily login bonus
 */
class DailyBonusRepository private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("daily_bonus_prefs", Context.MODE_PRIVATE)
    
    private val _loginStreak = MutableStateFlow(loadLoginStreak())
    val loginStreak: StateFlow<Int> = _loginStreak.asStateFlow()
    
    private val _canClaimBonus = MutableStateFlow(checkCanClaimBonus())
    val canClaimBonus: StateFlow<Boolean> = _canClaimBonus.asStateFlow()
    
    private val _lastClaimDate = MutableStateFlow(loadLastClaimDate())
    val lastClaimDate: StateFlow<String?> = _lastClaimDate.asStateFlow()
    
    /**
     * Check if user can claim daily bonus today
     */
    fun checkCanClaimBonus(): Boolean {
        val lastClaim = loadLastClaimDate() ?: return true
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return lastClaim != today
    }
    
    /**
     * Claim daily bonus and return the amount earned
     * Returns null if already claimed today
     */
    fun claimDailyBonus(currencyRepository: CurrencyRepository): DailyBonusResult? {
        if (!checkCanClaimBonus()) return null
        
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lastClaimStr = loadLastClaimDate()
        
        // Check if this continues a streak
        val newStreak = if (lastClaimStr != null) {
            val lastClaim = LocalDate.parse(lastClaimStr, DateTimeFormatter.ISO_LOCAL_DATE)
            val daysSinceLast = today.toEpochDay() - lastClaim.toEpochDay()
            
            when {
                daysSinceLast == 1L -> _loginStreak.value + 1 // Continue streak
                daysSinceLast > 1L -> 1 // Streak broken, start new
                else -> _loginStreak.value // Same day (shouldn't happen)
            }
        } else {
            1 // First ever claim
        }
        
        // Calculate bonus based on streak
        val bonusAmount = calculateBonusAmount(newStreak)
        
        // Award coins
        currencyRepository.addCoins(bonusAmount, CoinReason.DAILY_BONUS)
        
        // Save state
        _loginStreak.value = newStreak
        _lastClaimDate.value = todayStr
        _canClaimBonus.value = false
        
        prefs.edit()
            .putInt("login_streak", newStreak)
            .putString("last_claim_date", todayStr)
            .apply()
        
        return DailyBonusResult(
            coinsEarned = bonusAmount,
            currentStreak = newStreak,
            nextDayBonus = calculateBonusAmount(newStreak + 1)
        )
    }
    
    /**
     * Calculate bonus amount based on streak
     * Base: 25 coins, +5 per streak day, max 100
     */
    private fun calculateBonusAmount(streak: Int): Int {
        val base = CoinReason.DAILY_BONUS.baseAmount // 25
        val bonus = (streak - 1) * 5
        return minOf(base + bonus, 100) // Max 100 coins
    }
    
    /**
     * Get the bonus amount for current streak (for display)
     */
    fun getCurrentBonusAmount(): Int {
        return calculateBonusAmount(_loginStreak.value + 1)
    }
    
    /**
     * Refresh the can claim status (call when app opens)
     */
    fun refresh() {
        _canClaimBonus.value = checkCanClaimBonus()
    }
    
    private fun loadLoginStreak(): Int = prefs.getInt("login_streak", 0)
    
    private fun loadLastClaimDate(): String? = prefs.getString("last_claim_date", null)
    
    companion object {
        @Volatile
        private var instance: DailyBonusRepository? = null
        
        fun getInstance(context: Context): DailyBonusRepository {
            return instance ?: synchronized(this) {
                instance ?: DailyBonusRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Result of claiming daily bonus
 */
data class DailyBonusResult(
    val coinsEarned: Int,
    val currentStreak: Int,
    val nextDayBonus: Int
)
