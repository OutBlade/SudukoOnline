package de.sudokuonline.app.game.ai

import kotlin.math.max
import kotlin.math.min

/**
 * Advanced Search Engine - Professional-grade game tree search
 *
 * Implements state-of-the-art techniques used in chess engines:
 * - Iterative Deepening with time management
 * - Principal Variation Search (PVS)
 * - Killer Move Heuristic
 * - History Heuristic
 * - Late Move Reductions (LMR)
 * - Aspiration Windows
 * - Transposition Table with Zobrist Hashing
 */
object AdvancedSearchEngine {

    // Search statistics
    var nodesSearched = 0L
        private set
    var cutoffs = 0L
        private set
    var ttHits = 0L
        private set

    // Killer moves - moves that caused beta cutoffs at each depth
    private val killerMoves = Array(64) { arrayOfNulls<Any>(2) }

    // History heuristic - how often each move improves position
    private val historyTable = mutableMapOf<Any, Int>()

    // Constants
    const val INFINITY = 1_000_000
    const val MATE_SCORE = 900_000
    const val MAX_DEPTH = 64

    /**
     * Generic interface for game states
     */
    interface GameState<M> {
        fun generateMoves(): List<M>
        fun makeMove(move: M): GameState<M>
        fun evaluate(maximizingPlayer: Boolean): Int
        fun isTerminal(): Boolean
        fun hash(): Long
    }

    /**
     * Transposition Table Entry
     */
    data class TTEntry(
        val hash: Long,
        val depth: Int,
        val score: Int,
        val flag: TTFlag,
        val bestMove: Any?,
        val age: Int
    )

    enum class TTFlag {
        EXACT,      // Exact score
        ALPHA,      // Upper bound (failed low)
        BETA        // Lower bound (failed high)
    }

    // Transposition table
    private val transpositionTable = HashMap<Long, TTEntry>(1_000_000)
    private var searchAge = 0

    /**
     * Main search function with iterative deepening
     */
    fun <M> search(
        state: GameState<M>,
        maxDepth: Int,
        timeLimitMs: Long,
        maximizingPlayer: Boolean = true
    ): SearchResult<M> {
        resetStatistics()
        searchAge++

        val startTime = System.currentTimeMillis()
        var bestMove: M? = null
        var bestScore = if (maximizingPlayer) -INFINITY else INFINITY
        var completedDepth = 0

        // Iterative deepening
        for (depth in 1..maxDepth) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > timeLimitMs * 0.8) break

            // Aspiration window search
            val (score, move) = aspirationSearch(
                state, depth, bestScore, maximizingPlayer, startTime, timeLimitMs
            )

            if (move != null) {
                bestMove = move
                bestScore = score
                completedDepth = depth
            }

            // Found mate - no need to search deeper
            if (kotlin.math.abs(score) > MATE_SCORE - 100) break
        }

        return SearchResult(
            bestMove = bestMove,
            score = bestScore,
            depth = completedDepth,
            nodes = nodesSearched,
            timeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Aspiration window search - narrow search window for efficiency
     */
    private fun <M> aspirationSearch(
        state: GameState<M>,
        depth: Int,
        previousScore: Int,
        maximizingPlayer: Boolean,
        startTime: Long,
        timeLimitMs: Long
    ): Pair<Int, M?> {
        var alpha = -INFINITY
        var beta = INFINITY

        // Use aspiration window after depth 3
        if (depth > 3 && kotlin.math.abs(previousScore) < MATE_SCORE - 100) {
            val window = 50
            alpha = previousScore - window
            beta = previousScore + window
        }

        var score = pvSearch(state, depth, alpha, beta, maximizingPlayer, 0, startTime, timeLimitMs)
        var bestMove = getBestMoveFromTT<M>(state.hash())

        // Re-search with full window if outside aspiration window
        if (score <= alpha || score >= beta) {
            score = pvSearch(state, depth, -INFINITY, INFINITY, maximizingPlayer, 0, startTime, timeLimitMs)
            bestMove = getBestMoveFromTT(state.hash())
        }

        return Pair(score, bestMove)
    }

    /**
     * Principal Variation Search (PVS) - More efficient than standard alpha-beta
     */
    private fun <M> pvSearch(
        state: GameState<M>,
        depth: Int,
        alphaIn: Int,
        betaIn: Int,
        maximizingPlayer: Boolean,
        ply: Int,
        startTime: Long,
        timeLimitMs: Long
    ): Int {
        nodesSearched++

        // Time check
        if (nodesSearched % 10000 == 0L) {
            if (System.currentTimeMillis() - startTime > timeLimitMs) {
                return if (maximizingPlayer) -INFINITY else INFINITY
            }
        }

        // Terminal check
        if (state.isTerminal() || depth == 0) {
            return state.evaluate(maximizingPlayer)
        }

        val hash = state.hash()
        var alpha = alphaIn
        var beta = betaIn

        // Transposition table lookup
        val ttEntry = transpositionTable[hash]
        if (ttEntry != null && ttEntry.depth >= depth) {
            ttHits++
            when (ttEntry.flag) {
                TTFlag.EXACT -> return ttEntry.score
                TTFlag.ALPHA -> if (ttEntry.score <= alpha) return ttEntry.score
                TTFlag.BETA -> if (ttEntry.score >= beta) return ttEntry.score
            }

            // Use TT move for ordering
            when (ttEntry.flag) {
                TTFlag.BETA -> alpha = max(alpha, ttEntry.score)
                TTFlag.ALPHA -> beta = min(beta, ttEntry.score)
                else -> {}
            }
        }

        // Generate and order moves
        val moves = orderMoves(state.generateMoves(), ply, hash)
        if (moves.isEmpty()) {
            return state.evaluate(maximizingPlayer)
        }

        var bestScore = -INFINITY
        var bestMove: M? = null
        var flag = TTFlag.ALPHA
        var searchedMoves = 0

        for (move in moves) {
            val newState = state.makeMove(move)
            var score: Int

            if (searchedMoves == 0) {
                // First move - full window search
                score = -pvSearch(
                    newState, depth - 1, -beta, -alpha,
                    !maximizingPlayer, ply + 1, startTime, timeLimitMs
                )
            } else {
                // Late Move Reductions
                var reduction = 0
                if (searchedMoves >= 4 && depth >= 3 && !state.isTerminal()) {
                    reduction = 1
                    if (searchedMoves >= 8) reduction = 2
                }

                // Null window search
                score = -pvSearch(
                    newState, depth - 1 - reduction, -alpha - 1, -alpha,
                    !maximizingPlayer, ply + 1, startTime, timeLimitMs
                )

                // Re-search with full window if improved
                if (score > alpha && (reduction > 0 || score < beta)) {
                    score = -pvSearch(
                        newState, depth - 1, -beta, -alpha,
                        !maximizingPlayer, ply + 1, startTime, timeLimitMs
                    )
                }
            }

            searchedMoves++

            if (score > bestScore) {
                bestScore = score
                bestMove = move

                if (score > alpha) {
                    alpha = score
                    flag = TTFlag.EXACT

                    if (score >= beta) {
                        // Beta cutoff
                        cutoffs++
                        flag = TTFlag.BETA

                        // Update killer moves
                        updateKillerMove(move, ply)

                        // Update history
                        updateHistory(move, depth)

                        break
                    }
                }
            }
        }

        // Store in transposition table
        if (transpositionTable.size < 2_000_000) {
            transpositionTable[hash] = TTEntry(
                hash = hash,
                depth = depth,
                score = bestScore,
                flag = flag,
                bestMove = bestMove,
                age = searchAge
            )
        }

        return bestScore
    }

    /**
     * Order moves for better pruning:
     * 1. TT best move
     * 2. Killer moves
     * 3. History heuristic
     */
    @Suppress("UNCHECKED_CAST")
    private fun <M> orderMoves(moves: List<M>, ply: Int, hash: Long): List<M> {
        val ttEntry = transpositionTable[hash]
        val ttMove = ttEntry?.bestMove as? M

        return moves.sortedByDescending { move ->
            var score = 0

            // TT move gets highest priority
            if (move == ttMove) {
                score += 10000
            }

            // Killer moves
            if (ply < killerMoves.size) {
                if (move == killerMoves[ply][0]) score += 900
                if (move == killerMoves[ply][1]) score += 800
            }

            // History heuristic
            score += historyTable.get<Any?, Int>(move) ?: 0

            score
        }
    }

    private fun <M> updateKillerMove(move: M, ply: Int) {
        if (ply >= killerMoves.size) return

        // Don't store if already killer
        if (move != killerMoves[ply][0]) {
            killerMoves[ply][1] = killerMoves[ply][0]
            killerMoves[ply][0] = move
        }
    }

    private fun <M> updateHistory(move: M, depth: Int) {
        val current = historyTable[move as Any] ?: 0
        historyTable[move] = current + depth * depth
    }

    @Suppress("UNCHECKED_CAST")
    private fun <M> getBestMoveFromTT(hash: Long): M? {
        return transpositionTable[hash]?.bestMove as? M
    }

    private fun resetStatistics() {
        nodesSearched = 0
        cutoffs = 0
        ttHits = 0
    }

    /**
     * Clear all caches (call between games)
     */
    fun clearAll() {
        transpositionTable.clear()
        historyTable.clear()
        for (i in killerMoves.indices) {
            killerMoves[i][0] = null
            killerMoves[i][1] = null
        }
        searchAge = 0
    }

    /**
     * Search result
     */
    data class SearchResult<M>(
        val bestMove: M?,
        val score: Int,
        val depth: Int,
        val nodes: Long,
        val timeMs: Long
    )
}
