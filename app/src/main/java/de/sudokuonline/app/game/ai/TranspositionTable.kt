package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard
import java.util.concurrent.ConcurrentHashMap

/**
 * Transposition Table for caching evaluated positions
 *
 * Uses Zobrist hashing for fast position lookups.
 * Dramatically improves search speed by avoiding re-evaluation.
 */
class TranspositionTable(private val maxSize: Int = 1_000_000) {

    data class Entry(
        val score: Int,
        val depth: Int,
        val flag: Flag,
        val bestMove: Pair<Int, Int>?,
        val age: Int = 0
    )

    enum class Flag {
        EXACT,      // Exact score
        ALPHA,      // Upper bound (score <= alpha)
        BETA        // Lower bound (score >= beta)
    }

    private val table = ConcurrentHashMap<Long, Entry>()
    private var currentAge = 0

    // Zobrist keys for hashing
    private val zobristTable: Array<Array<LongArray>> = Array(5) {
        Array(5) {
            LongArray(3) { kotlin.random.Random.nextLong() }
        }
    }

    /**
     * Compute Zobrist hash for a board position
     */
    fun computeHash(board: TicTacToeBoard): Long {
        var hash = 0L
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                val value = board.cells[row][col].value
                if (value != 0) {
                    hash = hash xor zobristTable[row][col][value]
                }
            }
        }
        return hash
    }

    /**
     * Incrementally update hash after a move (much faster than recomputing)
     */
    fun updateHash(hash: Long, row: Int, col: Int, oldValue: Int, newValue: Int): Long {
        var newHash = hash
        if (oldValue != 0) {
            newHash = newHash xor zobristTable[row][col][oldValue]
        }
        if (newValue != 0) {
            newHash = newHash xor zobristTable[row][col][newValue]
        }
        return newHash
    }

    /**
     * Store an entry in the table
     */
    fun store(
        hash: Long,
        score: Int,
        depth: Int,
        flag: Flag,
        bestMove: Pair<Int, Int>?
    ) {
        // Replace if deeper search or newer entry
        val existing = table[hash]
        if (existing == null || existing.depth <= depth || existing.age < currentAge) {
            table[hash] = Entry(score, depth, flag, bestMove, currentAge)

            // Evict old entries if table is too large
            if (table.size > maxSize) {
                evictOldEntries()
            }
        }
    }

    /**
     * Probe the table for an entry
     */
    fun probe(hash: Long, depth: Int, alpha: Int, beta: Int): ProbeResult {
        val entry = table[hash] ?: return ProbeResult.Miss

        // Only use if searched to at least the required depth
        if (entry.depth < depth) {
            return ProbeResult.Shallow(entry.bestMove)
        }

        return when (entry.flag) {
            Flag.EXACT -> ProbeResult.Hit(entry.score, entry.bestMove)
            Flag.ALPHA -> {
                if (entry.score <= alpha) {
                    ProbeResult.Hit(entry.score, entry.bestMove)
                } else {
                    ProbeResult.Shallow(entry.bestMove)
                }
            }
            Flag.BETA -> {
                if (entry.score >= beta) {
                    ProbeResult.Hit(entry.score, entry.bestMove)
                } else {
                    ProbeResult.Shallow(entry.bestMove)
                }
            }
        }
    }

    /**
     * Get best move from previous search (for move ordering)
     */
    fun getBestMove(hash: Long): Pair<Int, Int>? {
        return table[hash]?.bestMove
    }

    /**
     * Increment age for new search
     */
    fun newSearch() {
        currentAge++
    }

    /**
     * Clear the table
     */
    fun clear() {
        table.clear()
        currentAge = 0
    }

    /**
     * Evict oldest entries when table is full
     */
    private fun evictOldEntries() {
        val entriesToRemove = table.entries
            .filter { it.value.age < currentAge - 2 }
            .take(table.size / 4)
            .map { it.key }

        entriesToRemove.forEach { table.remove(it) }
    }

    /**
     * Get table statistics
     */
    fun getStats(): TableStats {
        return TableStats(
            size = table.size,
            maxSize = maxSize,
            fillRate = table.size.toFloat() / maxSize
        )
    }

    data class TableStats(
        val size: Int,
        val maxSize: Int,
        val fillRate: Float
    )

    sealed class ProbeResult {
        data class Hit(val score: Int, val bestMove: Pair<Int, Int>?) : ProbeResult()
        data class Shallow(val bestMove: Pair<Int, Int>?) : ProbeResult()
        object Miss : ProbeResult()
    }
}
