package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class for coin notification
 */
data class CoinEarnedEvent(
    val amount: Int,
    val reason: CoinReason,
    val id: Long = System.currentTimeMillis()
)

/**
 * Repository for managing in-app currency (coins)
 */
class CurrencyRepository private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
    
    private val _coins = MutableStateFlow(loadCoins())
    val coins: StateFlow<Int> = _coins.asStateFlow()
    
    private val _totalEarned = MutableStateFlow(loadTotalEarned())
    val totalEarned: StateFlow<Int> = _totalEarned.asStateFlow()
    
    // Event for showing coin earned notification
    private val _coinEarnedEvent = MutableStateFlow<CoinEarnedEvent?>(null)
    val coinEarnedEvent: StateFlow<CoinEarnedEvent?> = _coinEarnedEvent.asStateFlow()
    
    /**
     * Add coins (from watching ads, completing games, etc.)
     */
    fun addCoins(amount: Int, reason: CoinReason) {
        val newBalance = _coins.value + amount
        _coins.value = newBalance
        _totalEarned.value = _totalEarned.value + amount
        saveCoins(newBalance)
        saveTotalEarned(_totalEarned.value)
        
        // Emit coin earned event for notification
        _coinEarnedEvent.value = CoinEarnedEvent(amount, reason)
        
        // Track earning history
        trackEarning(amount, reason)
    }
    
    /**
     * Clear the coin earned event (after showing notification)
     */
    fun clearCoinEarnedEvent() {
        _coinEarnedEvent.value = null
    }
    
    /**
     * Spend coins (for purchasing themes, etc.)
     * Returns true if successful, false if insufficient funds
     */
    fun spendCoins(amount: Int): Boolean {
        if (_coins.value < amount) return false
        
        val newBalance = _coins.value - amount
        _coins.value = newBalance
        saveCoins(newBalance)
        return true
    }
    
    /**
     * Check if user can afford an item
     */
    fun canAfford(amount: Int): Boolean = _coins.value >= amount
    
    /**
     * Get current balance
     */
    fun getBalance(): Int = _coins.value
    
    private fun loadCoins(): Int = prefs.getInt("coins", 100) // Start with 100 free coins
    
    private fun saveCoins(amount: Int) {
        prefs.edit().putInt("coins", amount).apply()
    }
    
    private fun loadTotalEarned(): Int = prefs.getInt("total_earned", 100)
    
    private fun saveTotalEarned(amount: Int) {
        prefs.edit().putInt("total_earned", amount).apply()
    }
    
    private fun trackEarning(amount: Int, reason: CoinReason) {
        // Could log to analytics or save history
    }
    
    companion object {
        @Volatile
        private var instance: CurrencyRepository? = null
        
        fun getInstance(context: Context): CurrencyRepository {
            return instance ?: synchronized(this) {
                instance ?: CurrencyRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Reasons for earning coins
 */
enum class CoinReason(val displayName: String, val baseAmount: Int) {
    WATCH_AD("Werbung geschaut", 50),
    COMPLETE_EASY("Einfaches Sudoku gelöst", 10),
    COMPLETE_MEDIUM("Mittleres Sudoku gelöst", 25),
    COMPLETE_HARD("Schweres Sudoku gelöst", 50),
    COMPLETE_EXPERT("Experten-Sudoku gelöst", 100),
    DAILY_BONUS("Täglicher Bonus", 25),
    ACHIEVEMENT("Erfolg freigeschaltet", 50),
    FIRST_WIN_OF_DAY("Erster Sieg des Tages", 30),
    WIN_STREAK("Siegesserie", 20),
    PERFECT_GAME("Perfektes Spiel (keine Fehler)", 50),
    TICTACTOE_WIN("TicTacToe gewonnen", 15),
    MUHLE_WIN("Mühle gewonnen", 20),
    DAME_WIN("Dame gewonnen", 20),
    LOOTBOX("Lootbox", 0)
}
