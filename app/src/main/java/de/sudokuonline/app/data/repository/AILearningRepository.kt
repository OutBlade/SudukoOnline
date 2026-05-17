package de.sudokuonline.app.data.repository

import android.content.Context
import de.sudokuonline.app.data.local.*
import de.sudokuonline.app.data.model.TicTacToeBoard
import de.sudokuonline.app.data.model.TicTacToeGameMode
import de.sudokuonline.app.game.AIMove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Repository for AI Learning
 *
 * Manages:
 * - Position learning from games
 * - Opening book queries
 * - Game history storage
 * - Pattern recognition for avoiding losses
 *
 * Similar to how chess engines like Stockfish use opening books
 * and learn from games.
 */
class AILearningRepository private constructor(context: Context) {

    private val database = AIDatabase.getInstance(context)
    private val positionDao = database.learnedPositionDao()
    private val historyDao = database.gameHistoryDao()

    // Cache of positions to avoid (loaded at startup)
    private var positionsToAvoid: Set<String> = emptySet()
    private var sequencesToAvoid: List<MoveSequenceEntity> = emptyList()

    /**
     * Initialize by loading positions to avoid
     */
    suspend fun initialize(gameMode: TicTacToeGameMode, boardSize: Int) = withContext(Dispatchers.IO) {
        positionsToAvoid = positionDao.getPositionsToAvoid(
            gameMode.name,
            boardSize
        ).toSet()

        sequencesToAvoid = historyDao.getSequencesToAvoid(
            gameMode.name,
            boardSize
        )
    }

    // ========== Position Learning ==========

    /**
     * Check if we have a learned move for this position
     */
    suspend fun getLearnedMove(board: TicTacToeBoard, gameMode: TicTacToeGameMode): LearnedMove? =
        withContext(Dispatchers.IO) {
            val hash = computeBoardHash(board, gameMode)
            val position = positionDao.getBestMoveForPosition(hash)

            if (position != null && position.confidence > 0.4) {
                LearnedMove(
                    row = position.bestMoveRow,
                    col = position.bestMoveCol,
                    moveType = if (position.moveType == "BOMB") MoveType.BOMB else MoveType.SYMBOL,
                    winRate = position.winRate,
                    confidence = position.confidence,
                    timesPlayed = position.timesPlayed
                )
            } else null
        }

    /**
     * Check if a position should be avoided (leads to losses)
     */
    suspend fun shouldAvoidPosition(board: TicTacToeBoard, gameMode: TicTacToeGameMode): Boolean {
        val hash = computeBoardHash(board, gameMode)
        return hash in positionsToAvoid
    }

    /**
     * Get win rate for a potential move (for move ordering)
     */
    suspend fun getMoveWinRate(
        board: TicTacToeBoard,
        gameMode: TicTacToeGameMode,
        row: Int,
        col: Int,
        symbol: Int
    ): Float = withContext(Dispatchers.IO) {
        // Simulate the move
        val newBoard = board.copy(
            cells = board.cells.mapIndexed { r, rowCells ->
                rowCells.mapIndexed { c, cell ->
                    if (r == row && c == col) cell.copy(value = symbol)
                    else cell
                }
            }
        )
        val hash = computeBoardHash(newBoard, gameMode)
        val position = positionDao.getByHash(hash)
        position?.winRate ?: 0.5f  // Default to neutral if unknown
    }

    /**
     * Store a position with its best move
     */
    suspend fun storePosition(
        board: TicTacToeBoard,
        gameMode: TicTacToeGameMode,
        bestMove: AIMove,
        evaluation: Int,
        searchDepth: Int
    ) = withContext(Dispatchers.IO) {
        val hash = computeBoardHash(board, gameMode)
        val existing = positionDao.getByHash(hash)

        val (row, col, type) = when (bestMove) {
            is AIMove.PlaceSymbol -> Triple(bestMove.row, bestMove.col, "SYMBOL")
            is AIMove.PlaceBomb -> Triple(bestMove.row, bestMove.col, "BOMB")
        }

        if (existing != null) {
            // Update if our search is deeper or evaluation is better
            positionDao.updateBestMoveIfBetter(hash, row, col, type, evaluation, searchDepth)
        } else {
            // Insert new position
            positionDao.insert(
                LearnedPositionEntity(
                    positionHash = hash,
                    boardSize = board.size,
                    gameMode = gameMode.name,
                    bestMoveRow = row,
                    bestMoveCol = col,
                    moveType = type,
                    evaluation = evaluation,
                    searchDepth = searchDepth
                )
            )
        }
    }

    // ========== Game Recording ==========

    /**
     * Record a completed game for learning
     */
    suspend fun recordGame(
        gameMode: TicTacToeGameMode,
        boardSize: Int,
        aiStrength: Int,
        aiPlayedAs: Int,
        result: GameResult,
        moves: List<GameMove>,
        positionHashes: List<String>,
        durationSeconds: Int
    ) = withContext(Dispatchers.IO) {
        // Store game history
        val moveJson = JSONArray().apply {
            moves.forEach { move ->
                put(JSONObject().apply {
                    put("row", move.row)
                    put("col", move.col)
                    put("symbol", move.symbol)
                    put("type", move.type)
                })
            }
        }.toString()

        val hashJson = JSONArray().apply {
            positionHashes.forEach { put(it) }
        }.toString()

        historyDao.insert(
            GameHistoryEntity(
                gameMode = gameMode.name,
                boardSize = boardSize,
                aiStrength = aiStrength,
                aiPlayedAs = aiPlayedAs,
                result = result.name,
                moveSequence = moveJson,
                positionHashes = hashJson,
                totalMoves = moves.size,
                durationSeconds = durationSeconds
            )
        )

        // Update position statistics
        val resultMod = when (result) {
            GameResult.WIN -> Triple(1, 0, 0)
            GameResult.LOSS -> Triple(0, 1, 0)
            GameResult.DRAW -> Triple(0, 0, 1)
        }

        positionHashes.forEach { hash ->
            positionDao.updateStats(
                hash = hash,
                won = resultMod.first,
                lost = resultMod.second,
                draw = resultMod.third
            )
        }

        // Learn move sequences (first N moves)
        if (moves.size >= 2) {
            learnMoveSequence(gameMode, boardSize, moves.take(4), result, positionHashes.lastOrNull() ?: "")
        }

        // Refresh avoid cache if AI lost
        if (result == GameResult.LOSS) {
            positionsToAvoid = positionDao.getPositionsToAvoid(gameMode.name, boardSize).toSet()
        }
    }

    /**
     * Learn from a move sequence (opening book style)
     */
    private suspend fun learnMoveSequence(
        gameMode: TicTacToeGameMode,
        boardSize: Int,
        moves: List<GameMove>,
        result: GameResult,
        endPositionHash: String
    ) {
        val sequenceJson = JSONArray().apply {
            moves.forEach { move ->
                put(JSONObject().apply {
                    put("row", move.row)
                    put("col", move.col)
                    put("symbol", move.symbol)
                })
            }
        }.toString()

        val sequenceHash = md5(sequenceJson)
        val existing = historyDao.getSequence(sequenceHash)

        if (existing != null) {
            historyDao.updateSequenceStats(sequenceHash)
        } else {
            historyDao.insertSequence(
                MoveSequenceEntity(
                    sequenceHash = sequenceHash,
                    gameMode = gameMode.name,
                    boardSize = boardSize,
                    moveSequence = sequenceJson,
                    sequenceLength = moves.size,
                    outcome = result.name,
                    endPositionHash = endPositionHash
                )
            )
        }
    }

    // ========== Statistics ==========

    /**
     * Get learning statistics
     */
    suspend fun getStatistics(gameMode: TicTacToeGameMode, boardSize: Int): AILearningStats =
        withContext(Dispatchers.IO) {
            val positionStats = positionDao.getStats(gameMode.name, boardSize)
            val resultCounts = historyDao.getResultCounts(gameMode.name, boardSize)
            val totalGames = historyDao.getCount()

            AILearningStats(
                totalPositions = positionStats?.totalPositions ?: 0,
                totalGames = totalGames,
                averageWinRate = positionStats?.avgWinRate ?: 0.5f,
                wins = resultCounts.find { it.result == "WIN" }?.count ?: 0,
                losses = resultCounts.find { it.result == "LOSS" }?.count ?: 0,
                draws = resultCounts.find { it.result == "DRAW" }?.count ?: 0,
                positionsToAvoid = positionsToAvoid.size
            )
        }

    // ========== Helpers ==========

    /**
     * Compute hash for a board position
     */
    fun computeBoardHash(board: TicTacToeBoard, gameMode: TicTacToeGameMode): String {
        val sb = StringBuilder()
        sb.append("${board.size}_${board.winCondition}_${gameMode.name}_")
        for (row in board.cells) {
            for (cell in row) {
                sb.append(cell.value)
            }
        }
        return md5(sb.toString())
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        @Volatile
        private var instance: AILearningRepository? = null

        fun getInstance(context: Context): AILearningRepository {
            return instance ?: synchronized(this) {
                instance ?: AILearningRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

// ========== Data Classes ==========

data class LearnedMove(
    val row: Int,
    val col: Int,
    val moveType: MoveType,
    val winRate: Float,
    val confidence: Float,
    val timesPlayed: Int
)

enum class MoveType {
    SYMBOL, BOMB
}

data class GameMove(
    val row: Int,
    val col: Int,
    val symbol: Int,
    val type: String = "SYMBOL"
)

data class AILearningStats(
    val totalPositions: Int,
    val totalGames: Int,
    val averageWinRate: Float,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val positionsToAvoid: Int
)
