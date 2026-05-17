package de.sudokuonline.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores learned positions with their evaluation and statistics.
 * Similar to an opening book in chess engines.
 *
 * The AI will:
 * 1. Check this table before calculating a move
 * 2. Use positions with high win rates
 * 3. Avoid positions that led to losses
 */
@Entity(
    tableName = "learned_positions",
    indices = [
        Index(value = ["positionHash"], unique = true),
        Index(value = ["gameMode", "boardSize"]),
        Index(value = ["winRate"])
    ]
)
data class LearnedPositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Position identification
    val positionHash: String,           // MD5 hash of board state
    val boardSize: Int,                 // 3 or 5
    val gameMode: String,               // CLASSIC, BOMB, L_BOMB, ULTIMATE

    // Best move from this position
    val bestMoveRow: Int,
    val bestMoveCol: Int,
    val moveType: String,               // SYMBOL, BOMB

    // Evaluation score from AI
    val evaluation: Int,
    val searchDepth: Int,

    // Learning statistics
    val timesPlayed: Int = 0,
    val timesWon: Int = 0,
    val timesLost: Int = 0,
    val timesDraw: Int = 0,

    // Calculated win rate (0.0 - 1.0)
    val winRate: Float = 0.5f,

    // Confidence score based on sample size
    val confidence: Float = 0f,

    // When position was last seen
    val lastUpdated: Long = System.currentTimeMillis(),

    // Source of the position
    val source: String = "SELF_PLAY"    // SELF_PLAY, USER_GAME, IMPORTED
)

/**
 * Stores complete game histories for analysis.
 * Allows the AI to learn from entire game sequences.
 */
@Entity(
    tableName = "game_history",
    indices = [
        Index(value = ["gameMode", "boardSize"]),
        Index(value = ["result"]),
        Index(value = ["timestamp"])
    ]
)
data class GameHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Game metadata
    val gameMode: String,
    val boardSize: Int,
    val aiStrength: Int,
    val aiPlayedAs: Int,                // 1 = X (first), 2 = O (second)

    // Result: WIN, LOSS, DRAW
    val result: String,

    // Move sequence as JSON array
    val moveSequence: String,           // JSON: [{"row":1,"col":1,"symbol":1}, ...]

    // Position hashes at each move (for learning)
    val positionHashes: String,         // JSON: ["hash1", "hash2", ...]

    // Game duration
    val totalMoves: Int,
    val durationSeconds: Int,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Stores specific move sequences that led to wins or losses.
 * Helps identify winning/losing patterns.
 */
@Entity(
    tableName = "move_sequences",
    indices = [
        Index(value = ["sequenceHash"], unique = true),
        Index(value = ["outcome"]),
        Index(value = ["gameMode", "boardSize"])
    ]
)
data class MoveSequenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Sequence identification
    val sequenceHash: String,           // Hash of the move sequence
    val gameMode: String,
    val boardSize: Int,

    // The sequence of moves (first N moves)
    val moveSequence: String,           // JSON array of moves
    val sequenceLength: Int,            // How many moves in sequence

    // Outcome statistics
    val outcome: String,                // WIN, LOSS, DRAW
    val timesOccurred: Int = 1,

    // Position at end of sequence
    val endPositionHash: String,

    // Learning data
    val shouldAvoid: Boolean = false,   // True if this sequence often leads to loss
    val shouldPrefer: Boolean = false,  // True if this sequence often leads to win

    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * Result type for game outcomes
 */
enum class GameResult {
    WIN, LOSS, DRAW
}

/**
 * Move type for stored moves
 */
enum class StoredMoveType {
    SYMBOL, BOMB
}
