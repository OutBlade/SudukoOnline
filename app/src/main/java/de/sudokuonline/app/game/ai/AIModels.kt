package de.sudokuonline.app.game.ai

/**
 * AI Move types for TicTacToe
 * Note: AIMove and AIUltimateMove are still in TicTacToeAI.kt for backward compatibility
 */

/**
 * Result from AI computation
 */
sealed class AIResult {
    data class Success(
        val move: de.sudokuonline.app.game.AIMove,
        val evaluation: Int,      // Centipawn-like score (positive = AI winning)
        val depth: Int,           // Search depth achieved
        val nodesSearched: Long,  // Number of positions evaluated
        val timeMs: Long          // Time taken in milliseconds
    ) : AIResult()

    data class Timeout(
        val fallbackMove: de.sudokuonline.app.game.AIMove,
        val partialEvaluation: Int = 0
    ) : AIResult()

    object Cancelled : AIResult()

    data class Error(val message: String) : AIResult()
}

/**
 * Result from Ultimate TicTacToe AI computation
 */
sealed class AIUltimateResult {
    data class Success(
        val move: de.sudokuonline.app.game.AIUltimateMove,
        val evaluation: Int,
        val timeMs: Long
    ) : AIUltimateResult()

    data class Timeout(
        val fallbackMove: de.sudokuonline.app.game.AIUltimateMove
    ) : AIUltimateResult()

    data class Error(val message: String) : AIUltimateResult()
}

/**
 * AI State for UI observation
 */
sealed class AIState {
    object Idle : AIState()
    object Thinking : AIState()
    data class Progress(val depth: Int, val nodesSearched: Long) : AIState()
}

/**
 * Search result from the AI engine
 */
data class SearchResult(
    val bestMove: de.sudokuonline.app.game.AIMove,
    val evaluation: Int,
    val depth: Int,
    val nodesSearched: Long
)

/**
 * Ultimate search result
 */
data class UltimateSearchResult(
    val bestMove: de.sudokuonline.app.game.AIUltimateMove,
    val evaluation: Int
)
