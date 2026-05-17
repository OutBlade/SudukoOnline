package de.sudokuonline.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for game history.
 * Stores complete games for analysis and learning.
 */
@Dao
interface GameHistoryDao {

    /**
     * Insert a new game
     */
    @Insert
    suspend fun insert(game: GameHistoryEntity): Long

    /**
     * Get recent games
     */
    @Query("""
        SELECT * FROM game_history
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentGames(limit: Int = 50): List<GameHistoryEntity>

    /**
     * Get games by mode and result
     */
    @Query("""
        SELECT * FROM game_history
        WHERE gameMode = :gameMode
        AND boardSize = :boardSize
        AND result = :result
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getGamesByResult(
        gameMode: String,
        boardSize: Int,
        result: String,
        limit: Int = 20
    ): List<GameHistoryEntity>

    /**
     * Get win/loss/draw counts
     */
    @Query("""
        SELECT result, COUNT(*) as count
        FROM game_history
        WHERE gameMode = :gameMode AND boardSize = :boardSize
        GROUP BY result
    """)
    suspend fun getResultCounts(gameMode: String, boardSize: Int): List<ResultCount>

    /**
     * Get all games as Flow
     */
    @Query("SELECT * FROM game_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<GameHistoryEntity>>

    /**
     * Get game count
     */
    @Query("SELECT COUNT(*) FROM game_history")
    suspend fun getCount(): Int

    /**
     * Get average game duration
     */
    @Query("""
        SELECT AVG(durationSeconds) FROM game_history
        WHERE gameMode = :gameMode AND boardSize = :boardSize
    """)
    suspend fun getAverageDuration(gameMode: String, boardSize: Int): Float?

    /**
     * Delete old games (keep last N)
     */
    @Query("""
        DELETE FROM game_history
        WHERE id NOT IN (
            SELECT id FROM game_history
            ORDER BY timestamp DESC
            LIMIT :keepCount
        )
    """)
    suspend fun deleteOldGames(keepCount: Int = 1000)

    // ========== Move Sequences ==========

    /**
     * Insert a move sequence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSequence(sequence: MoveSequenceEntity)

    /**
     * Get sequence by hash
     */
    @Query("SELECT * FROM move_sequences WHERE sequenceHash = :hash LIMIT 1")
    suspend fun getSequence(hash: String): MoveSequenceEntity?

    /**
     * Get sequences to avoid (often lead to losses)
     */
    @Query("""
        SELECT * FROM move_sequences
        WHERE gameMode = :gameMode
        AND boardSize = :boardSize
        AND shouldAvoid = 1
        ORDER BY timesOccurred DESC
        LIMIT :limit
    """)
    suspend fun getSequencesToAvoid(gameMode: String, boardSize: Int, limit: Int = 50): List<MoveSequenceEntity>

    /**
     * Get preferred opening sequences
     */
    @Query("""
        SELECT * FROM move_sequences
        WHERE gameMode = :gameMode
        AND boardSize = :boardSize
        AND shouldPrefer = 1
        AND sequenceLength <= :maxLength
        ORDER BY timesOccurred DESC
        LIMIT :limit
    """)
    suspend fun getPreferredOpenings(
        gameMode: String,
        boardSize: Int,
        maxLength: Int = 4,
        limit: Int = 20
    ): List<MoveSequenceEntity>

    /**
     * Update sequence statistics
     */
    @Query("""
        UPDATE move_sequences
        SET timesOccurred = timesOccurred + 1,
            shouldAvoid = CASE WHEN outcome = 'LOSS' AND timesOccurred >= 3 THEN 1 ELSE shouldAvoid END,
            shouldPrefer = CASE WHEN outcome = 'WIN' AND timesOccurred >= 3 THEN 1 ELSE shouldPrefer END,
            lastSeen = :timestamp
        WHERE sequenceHash = :hash
    """)
    suspend fun updateSequenceStats(hash: String, timestamp: Long = System.currentTimeMillis())
}

/**
 * Result count for statistics
 */
data class ResultCount(
    val result: String,
    val count: Int
)
