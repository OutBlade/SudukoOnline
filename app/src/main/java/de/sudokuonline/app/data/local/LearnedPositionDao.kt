package de.sudokuonline.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for learned positions.
 * Provides methods to query and update the AI's knowledge base.
 */
@Dao
interface LearnedPositionDao {

    /**
     * Get a learned position by its hash
     */
    @Query("SELECT * FROM learned_positions WHERE positionHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): LearnedPositionEntity?

    /**
     * Get best move for a position (highest win rate with confidence)
     */
    @Query("""
        SELECT * FROM learned_positions
        WHERE positionHash = :hash
        AND confidence > 0.3
        ORDER BY winRate DESC, timesPlayed DESC
        LIMIT 1
    """)
    suspend fun getBestMoveForPosition(hash: String): LearnedPositionEntity?

    /**
     * Get positions to avoid (low win rate)
     */
    @Query("""
        SELECT positionHash FROM learned_positions
        WHERE gameMode = :gameMode
        AND boardSize = :boardSize
        AND winRate < 0.3
        AND timesPlayed >= 5
    """)
    suspend fun getPositionsToAvoid(gameMode: String, boardSize: Int): List<String>

    /**
     * Get high-confidence positions (good sample size)
     */
    @Query("""
        SELECT * FROM learned_positions
        WHERE gameMode = :gameMode
        AND boardSize = :boardSize
        AND confidence > 0.5
        ORDER BY winRate DESC
        LIMIT :limit
    """)
    suspend fun getHighConfidencePositions(gameMode: String, boardSize: Int, limit: Int = 100): List<LearnedPositionEntity>

    /**
     * Insert or update a position
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: LearnedPositionEntity)

    /**
     * Insert multiple positions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(positions: List<LearnedPositionEntity>)

    /**
     * Update statistics for a position after a game
     */
    @Query("""
        UPDATE learned_positions
        SET timesPlayed = timesPlayed + 1,
            timesWon = timesWon + :won,
            timesLost = timesLost + :lost,
            timesDraw = timesDraw + :draw,
            winRate = CAST((timesWon + :won) AS FLOAT) / (timesPlayed + 1),
            confidence = MIN(1.0, (timesPlayed + 1) / 20.0),
            lastUpdated = :timestamp
        WHERE positionHash = :hash
    """)
    suspend fun updateStats(hash: String, won: Int, lost: Int, draw: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Update best move if new evaluation is better
     */
    @Query("""
        UPDATE learned_positions
        SET bestMoveRow = :row,
            bestMoveCol = :col,
            moveType = :moveType,
            evaluation = :evaluation,
            searchDepth = :depth,
            lastUpdated = :timestamp
        WHERE positionHash = :hash
        AND (searchDepth < :depth OR (searchDepth = :depth AND evaluation < :evaluation))
    """)
    suspend fun updateBestMoveIfBetter(
        hash: String,
        row: Int,
        col: Int,
        moveType: String,
        evaluation: Int,
        depth: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get total count of learned positions
     */
    @Query("SELECT COUNT(*) FROM learned_positions")
    suspend fun getCount(): Int

    /**
     * Get statistics for a game mode
     */
    @Query("""
        SELECT AVG(winRate) as avgWinRate, COUNT(*) as totalPositions,
               SUM(timesPlayed) as totalGames
        FROM learned_positions
        WHERE gameMode = :gameMode AND boardSize = :boardSize
    """)
    suspend fun getStats(gameMode: String, boardSize: Int): LearningStats?

    /**
     * Clear old positions with low confidence
     */
    @Query("""
        DELETE FROM learned_positions
        WHERE confidence < 0.1
        AND timesPlayed < 3
        AND lastUpdated < :cutoffTime
    """)
    suspend fun cleanupOldPositions(cutoffTime: Long)

    /**
     * Get all positions as Flow for observing
     */
    @Query("SELECT * FROM learned_positions ORDER BY lastUpdated DESC")
    fun observeAll(): Flow<List<LearnedPositionEntity>>
}

/**
 * Statistics about learning
 */
data class LearningStats(
    val avgWinRate: Float?,
    val totalPositions: Int,
    val totalGames: Int
)
