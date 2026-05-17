package de.sudokuonline.app.game.ai

import android.content.Context
import android.content.SharedPreferences
import de.sudokuonline.app.data.model.TicTacToeBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Position Cache for storing learned best moves
 *
 * Uses SharedPreferences for persistence.
 * Caches positions with their best moves and win rates.
 */
class PositionCache private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ai_position_cache",
        Context.MODE_PRIVATE
    )

    data class CachedPosition(
        val bestMoveRow: Int,
        val bestMoveCol: Int,
        val score: Int,
        val winCount: Int,
        val lossCount: Int,
        val drawCount: Int,
        val depth: Int
    ) {
        val totalGames: Int get() = winCount + lossCount + drawCount
        val winRate: Float get() = if (totalGames > 0) winCount.toFloat() / totalGames else 0.5f

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("row", bestMoveRow)
                put("col", bestMoveCol)
                put("score", score)
                put("wins", winCount)
                put("losses", lossCount)
                put("draws", drawCount)
                put("depth", depth)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): CachedPosition {
                return CachedPosition(
                    bestMoveRow = json.getInt("row"),
                    bestMoveCol = json.getInt("col"),
                    score = json.optInt("score", 0),
                    winCount = json.optInt("wins", 0),
                    lossCount = json.optInt("losses", 0),
                    drawCount = json.optInt("draws", 0),
                    depth = json.optInt("depth", 0)
                )
            }
        }
    }

    /**
     * Compute hash for a board position
     */
    fun computeHash(board: TicTacToeBoard): String {
        val sb = StringBuilder()
        sb.append("${board.size}_${board.winCondition}_")
        for (row in board.cells) {
            for (cell in row) {
                sb.append(cell.value)
            }
        }
        return md5(sb.toString())
    }

    /**
     * Get cached position if available
     */
    suspend fun get(board: TicTacToeBoard): CachedPosition? = withContext(Dispatchers.IO) {
        val hash = computeHash(board)
        val json = prefs.getString(hash, null) ?: return@withContext null
        try {
            CachedPosition.fromJson(JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Store a position with its best move
     */
    suspend fun store(
        board: TicTacToeBoard,
        bestMoveRow: Int,
        bestMoveCol: Int,
        score: Int,
        depth: Int
    ) = withContext(Dispatchers.IO) {
        val hash = computeHash(board)
        val existing = get(board)

        val cached = if (existing != null && existing.depth >= depth) {
            existing
        } else {
            CachedPosition(
                bestMoveRow = bestMoveRow,
                bestMoveCol = bestMoveCol,
                score = score,
                winCount = existing?.winCount ?: 0,
                lossCount = existing?.lossCount ?: 0,
                drawCount = existing?.drawCount ?: 0,
                depth = depth
            )
        }

        prefs.edit().putString(hash, cached.toJson().toString()).apply()
    }

    /**
     * Update win/loss/draw counts for a position
     */
    suspend fun updateResult(
        board: TicTacToeBoard,
        aiSymbol: Int,
        winner: Int  // 0=draw, 1=player1, 2=player2
    ) = withContext(Dispatchers.IO) {
        val hash = computeHash(board)
        val existing = get(board) ?: return@withContext

        val updated = when {
            winner == 0 -> existing.copy(drawCount = existing.drawCount + 1)
            winner == aiSymbol -> existing.copy(winCount = existing.winCount + 1)
            else -> existing.copy(lossCount = existing.lossCount + 1)
        }

        prefs.edit().putString(hash, updated.toJson().toString()).apply()
    }

    /**
     * Store multiple positions from a game
     */
    suspend fun storeGame(
        positions: List<Pair<TicTacToeBoard, Pair<Int, Int>>>,
        aiSymbol: Int,
        winner: Int
    ) = withContext(Dispatchers.IO) {
        for ((board, move) in positions) {
            val hash = computeHash(board)
            val existing = get(board)

            val cached = CachedPosition(
                bestMoveRow = move.first,
                bestMoveCol = move.second,
                score = existing?.score ?: 0,
                winCount = (existing?.winCount ?: 0) + if (winner == aiSymbol) 1 else 0,
                lossCount = (existing?.lossCount ?: 0) + if (winner != 0 && winner != aiSymbol) 1 else 0,
                drawCount = (existing?.drawCount ?: 0) + if (winner == 0) 1 else 0,
                depth = existing?.depth ?: 1
            )

            prefs.edit().putString(hash, cached.toJson().toString()).apply()
        }
    }

    /**
     * Get cache statistics
     */
    fun getStats(): CacheStats {
        val allEntries = prefs.all
        var totalGames = 0
        var totalWins = 0

        allEntries.values.forEach { value ->
            if (value is String) {
                try {
                    val cached = CachedPosition.fromJson(JSONObject(value))
                    totalGames += cached.totalGames
                    totalWins += cached.winCount
                } catch (e: Exception) { }
            }
        }

        return CacheStats(
            positionCount = allEntries.size,
            totalGames = totalGames,
            winRate = if (totalGames > 0) totalWins.toFloat() / totalGames else 0f
        )
    }

    /**
     * Clear all cached positions
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    data class CacheStats(
        val positionCount: Int,
        val totalGames: Int,
        val winRate: Float
    )

    companion object {
        @Volatile
        private var instance: PositionCache? = null

        fun getInstance(context: Context): PositionCache {
            return instance ?: synchronized(this) {
                instance ?: PositionCache(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
