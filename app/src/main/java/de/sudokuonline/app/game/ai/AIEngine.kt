package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard
import de.sudokuonline.app.data.model.TicTacToeGameMode
import de.sudokuonline.app.data.model.UltimateTicTacToeBoard
import de.sudokuonline.app.game.AIMove
import de.sudokuonline.app.game.AIUltimateMove
import de.sudokuonline.app.game.TicTacToeLogic
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

/**
 * Perfect AI Engine - Plays optimally in all modes
 *
 * Features:
 * - Perfect play for 3x3 (always draws against perfect opponent)
 * - Deep search for 5x5 with iterative deepening
 * - Smart bomb usage (only when it removes 2+ opponent stones)
 * - Transposition table for speed
 */
class AIEngine {

    private val transpositionTable = TranspositionTable()
    private var nodesSearched = 0L
    private var startTime = 0L
    private var timeLimit = 0L
    private var searchCancelled = false

    // Killer moves for each depth
    private val killerMoves = Array(30) { arrayOfNulls<Pair<Int, Int>>(2) }

    companion object {
        const val WIN_SCORE = 1000000
        const val DRAW_SCORE = 0
    }

    data class SearchResult(
        val bestMove: AIMove,
        val score: Int,
        val depth: Int,
        val nodesSearched: Long,
        val timeMs: Long
    )

    /**
     * Find best move with perfect play
     */
    suspend fun findBestMove(
        board: TicTacToeBoard,
        aiSymbol: Int,
        gameMode: TicTacToeGameMode,
        timeLimitMs: Long = 4000,
        maxDepth: Int = 20
    ): SearchResult {
        startTime = System.currentTimeMillis()
        timeLimit = timeLimitMs
        nodesSearched = 0
        searchCancelled = false
        transpositionTable.newSearch()
        clearKillerMoves()

        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        // === IMMEDIATE WIN CHECK ===
        val winMove = findWinningMove(board, aiSymbol)
        if (winMove != null) {
            return SearchResult(
                bestMove = AIMove.PlaceSymbol(winMove.first, winMove.second),
                score = WIN_SCORE,
                depth = 1,
                nodesSearched = 1,
                timeMs = System.currentTimeMillis() - startTime
            )
        }

        // === MUST BLOCK CHECK ===
        val blockMove = findWinningMove(board, opponentSymbol)
        if (blockMove != null) {
            return SearchResult(
                bestMove = AIMove.PlaceSymbol(blockMove.first, blockMove.second),
                score = 0,
                depth = 1,
                nodesSearched = 1,
                timeMs = System.currentTimeMillis() - startTime
            )
        }

        // === FORK DETECTION (create two winning threats) ===
        val forkMove = findForkMove(board, aiSymbol)
        if (forkMove != null) {
            return SearchResult(
                bestMove = AIMove.PlaceSymbol(forkMove.first, forkMove.second),
                score = WIN_SCORE / 2,
                depth = 2,
                nodesSearched = 10,
                timeMs = System.currentTimeMillis() - startTime
            )
        }

        // === BLOCK OPPONENT FORK ===
        val blockFork = findForkMove(board, opponentSymbol)
        if (blockFork != null) {
            // Either block the fork or force opponent to defend
            val forceMove = findForcingMove(board, aiSymbol, blockFork)
            if (forceMove != null) {
                return SearchResult(
                    bestMove = AIMove.PlaceSymbol(forceMove.first, forceMove.second),
                    score = 100,
                    depth = 2,
                    nodesSearched = 20,
                    timeMs = System.currentTimeMillis() - startTime
                )
            }
            return SearchResult(
                bestMove = AIMove.PlaceSymbol(blockFork.first, blockFork.second),
                score = 50,
                depth = 2,
                nodesSearched = 20,
                timeMs = System.currentTimeMillis() - startTime
            )
        }

        // === 3x3 SPECIFIC: PERFECT STRATEGY ===
        if (board.size == 3) {
            // Take center if available (highest priority after win/block/fork)
            if (board.cells[1][1].isEmpty()) {
                return SearchResult(
                    bestMove = AIMove.PlaceSymbol(1, 1),
                    score = 80,
                    depth = 1,
                    nodesSearched = 1,
                    timeMs = System.currentTimeMillis() - startTime
                )
            }

            // Take opposite corner if opponent has a corner
            val cornerPairs = listOf(
                Triple(0, 0, 8), Triple(0, 2, 6), Triple(2, 0, 2), Triple(2, 2, 0)
            )
            for ((row, col, opposite) in cornerPairs) {
                if (board.cells[row][col].value == opponentSymbol) {
                    val oppRow = opposite / 3
                    val oppCol = opposite % 3
                    if (board.cells[oppRow][oppCol].isEmpty()) {
                        return SearchResult(
                            bestMove = AIMove.PlaceSymbol(oppRow, oppCol),
                            score = 70,
                            depth = 1,
                            nodesSearched = 1,
                            timeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
            }

            // Take any corner
            for (corner in listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))) {
                if (board.cells[corner.first][corner.second].isEmpty()) {
                    return SearchResult(
                        bestMove = AIMove.PlaceSymbol(corner.first, corner.second),
                        score = 60,
                        depth = 1,
                        nodesSearched = 1,
                        timeMs = System.currentTimeMillis() - startTime
                    )
                }
            }

            // Take any edge
            for (edge in listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))) {
                if (board.cells[edge.first][edge.second].isEmpty()) {
                    return SearchResult(
                        bestMove = AIMove.PlaceSymbol(edge.first, edge.second),
                        score = 50,
                        depth = 1,
                        nodesSearched = 1,
                        timeMs = System.currentTimeMillis() - startTime
                    )
                }
            }
        }

        // === ITERATIVE DEEPENING SEARCH ===
        var bestMove: AIMove = findFirstLegalMove(board)
        var bestScore = Int.MIN_VALUE
        var completedDepth = 0

        // For 3x3, we can solve completely
        val effectiveMaxDepth = if (board.size == 3) 15 else maxDepth

        for (depth in 1..effectiveMaxDepth) {
            if (isTimeUp() || searchCancelled || !coroutineContext.isActive) break

            val result = searchRoot(board, aiSymbol, depth)

            if (!isTimeUp() && !searchCancelled && coroutineContext.isActive) {
                if (result.score > bestScore || completedDepth == 0) {
                    bestScore = result.score
                    bestMove = result.move
                    completedDepth = depth
                }

                // Found guaranteed win
                if (result.score >= WIN_SCORE - 100) break
            }

            // Check time for next iteration
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed * 3 > timeLimit) break
        }

        return SearchResult(
            bestMove = bestMove,
            score = bestScore,
            depth = completedDepth,
            nodesSearched = nodesSearched,
            timeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Find a move that wins immediately
     */
    private fun findWinningMove(board: TicTacToeBoard, symbol: Int): Pair<Int, Int>? {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    if (wouldWin(board, row, col, symbol)) {
                        return Pair(row, col)
                    }
                }
            }
        }
        return null
    }

    /**
     * Find a fork move (creates two winning threats)
     */
    private fun findForkMove(board: TicTacToeBoard, symbol: Int): Pair<Int, Int>? {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    val newBoard = makeMove(board, row, col, symbol)
                    val winningMoves = countWinningMoves(newBoard, symbol)
                    if (winningMoves >= 2) {
                        return Pair(row, col)
                    }
                }
            }
        }
        return null
    }

    /**
     * Find a move that forces opponent to defend instead of creating fork
     */
    private fun findForcingMove(board: TicTacToeBoard, symbol: Int, avoidPos: Pair<Int, Int>): Pair<Int, Int>? {
        val opponent = if (symbol == 1) 2 else 1

        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty() && (row != avoidPos.first || col != avoidPos.second)) {
                    val newBoard = makeMove(board, row, col, symbol)

                    // Check if this creates a threat
                    val ourWinMove = findWinningMove(newBoard, symbol)
                    if (ourWinMove != null) {
                        // Opponent must block at ourWinMove
                        // Make sure that's not the fork position
                        if (ourWinMove.first != avoidPos.first || ourWinMove.second != avoidPos.second) {
                            // This is a good forcing move
                            return Pair(row, col)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun countWinningMoves(board: TicTacToeBoard, symbol: Int): Int {
        var count = 0
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    if (wouldWin(board, row, col, symbol)) {
                        count++
                    }
                }
            }
        }
        return count
    }

    private fun wouldWin(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val size = board.size
        val winCondition = board.winCondition

        // Check row
        var count = 1
        var c = col - 1
        while (c >= 0 && board.cells[row][c].value == symbol) { count++; c-- }
        c = col + 1
        while (c < size && board.cells[row][c].value == symbol) { count++; c++ }
        if (count >= winCondition) return true

        // Check column
        count = 1
        var r = row - 1
        while (r >= 0 && board.cells[r][col].value == symbol) { count++; r-- }
        r = row + 1
        while (r < size && board.cells[r][col].value == symbol) { count++; r++ }
        if (count >= winCondition) return true

        // Check diagonal
        count = 1
        r = row - 1; c = col - 1
        while (r >= 0 && c >= 0 && board.cells[r][c].value == symbol) { count++; r--; c-- }
        r = row + 1; c = col + 1
        while (r < size && c < size && board.cells[r][c].value == symbol) { count++; r++; c++ }
        if (count >= winCondition) return true

        // Check anti-diagonal
        count = 1
        r = row - 1; c = col + 1
        while (r >= 0 && c < size && board.cells[r][c].value == symbol) { count++; r--; c++ }
        r = row + 1; c = col - 1
        while (r < size && c >= 0 && board.cells[r][c].value == symbol) { count++; r++; c-- }
        if (count >= winCondition) return true

        return false
    }

    private data class RootResult(val move: AIMove, val score: Int)

    private suspend fun searchRoot(board: TicTacToeBoard, aiSymbol: Int, depth: Int): RootResult {
        val moves = generateOrderedMoves(board, transpositionTable.computeHash(board))

        var bestScore = Int.MIN_VALUE
        var bestMove = moves.firstOrNull()?.let { AIMove.PlaceSymbol(it.first, it.second) }
            ?: findFirstLegalMove(board)
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        for ((row, col) in moves) {
            if (isTimeUp() || searchCancelled || !coroutineContext.isActive) break

            val newBoard = makeMove(board, row, col, aiSymbol)
            val score = -negamax(newBoard, depth - 1, -beta, -alpha, if (aiSymbol == 1) 2 else 1, aiSymbol)

            if (score > bestScore) {
                bestScore = score
                bestMove = AIMove.PlaceSymbol(row, col)
            }

            alpha = max(alpha, score)
        }

        return RootResult(bestMove, bestScore)
    }

    private suspend fun negamax(
        board: TicTacToeBoard,
        depth: Int,
        alphaIn: Int,
        beta: Int,
        currentPlayer: Int,
        aiSymbol: Int
    ): Int {
        if (isTimeUp() || searchCancelled || !coroutineContext.isActive) return 0

        nodesSearched++
        var alpha = alphaIn

        // Terminal check
        val winner = TicTacToeLogic.checkWinner(board)
        if (winner != null) {
            return if (winner == currentPlayer) WIN_SCORE + depth else -WIN_SCORE - depth
        }
        if (TicTacToeLogic.isBoardFull(board)) return DRAW_SCORE

        if (depth <= 0) {
            return evaluate(board, currentPlayer)
        }

        // Transposition table lookup
        val hash = transpositionTable.computeHash(board)
        when (val probe = transpositionTable.probe(hash, depth, alpha, beta)) {
            is TranspositionTable.ProbeResult.Hit -> return probe.score
            else -> { }
        }

        val moves = generateOrderedMoves(board, hash, depth)
        if (moves.isEmpty()) return DRAW_SCORE

        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null
        var flag = TranspositionTable.Flag.ALPHA

        val opponent = if (currentPlayer == 1) 2 else 1

        for ((index, move) in moves.withIndex()) {
            val (row, col) = move

            // Late move reduction
            val reduction = if (index > 4 && depth >= 4) 1 else 0

            val newBoard = makeMove(board, row, col, currentPlayer)
            var score = -negamax(newBoard, depth - 1 - reduction, -beta, -alpha, opponent, aiSymbol)

            // Re-search if reduced search found good move
            if (reduction > 0 && score > alpha) {
                score = -negamax(newBoard, depth - 1, -beta, -alpha, opponent, aiSymbol)
            }

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }

            if (score > alpha) {
                alpha = score
                flag = TranspositionTable.Flag.EXACT
            }

            if (alpha >= beta) {
                storeKillerMove(depth, move)
                flag = TranspositionTable.Flag.BETA
                break
            }
        }

        transpositionTable.store(hash, bestScore, depth, flag, bestMove)
        return bestScore
    }

    /**
     * Static evaluation for leaf nodes
     */
    private fun evaluate(board: TicTacToeBoard, player: Int): Int {
        val opponent = if (player == 1) 2 else 1
        var score = 0

        // Count threats
        val myThreats = countThreats(board, player)
        val oppThreats = countThreats(board, opponent)

        // Immediate threat is very valuable
        score += myThreats.immediate * 5000
        score -= oppThreats.immediate * 5000

        // Two-in-a-row with open space
        score += myThreats.potential * 500
        score -= oppThreats.potential * 500

        // Center control
        val center = board.size / 2
        if (board.cells[center][center].value == player) score += 300
        else if (board.cells[center][center].value == opponent) score -= 300

        // For 5x5, near-center is also valuable
        if (board.size == 5) {
            for (r in 1..3) {
                for (c in 1..3) {
                    if (board.cells[r][c].value == player) score += 50
                    else if (board.cells[r][c].value == opponent) score -= 50
                }
            }
        }

        // Corner control
        val corners = listOf(
            Pair(0, 0), Pair(0, board.size - 1),
            Pair(board.size - 1, 0), Pair(board.size - 1, board.size - 1)
        )
        for ((r, c) in corners) {
            if (board.cells[r][c].value == player) score += 100
            else if (board.cells[r][c].value == opponent) score -= 100
        }

        return score
    }

    private data class ThreatCount(val immediate: Int, val potential: Int)

    private fun countThreats(board: TicTacToeBoard, symbol: Int): ThreatCount {
        var immediate = 0
        var potential = 0

        val size = board.size
        val win = board.winCondition

        // Check all lines
        val lines = mutableListOf<List<Pair<Int, Int>>>()

        // Rows
        for (row in 0 until size) {
            for (startCol in 0..size - win) {
                lines.add((0 until win).map { Pair(row, startCol + it) })
            }
        }

        // Columns
        for (col in 0 until size) {
            for (startRow in 0..size - win) {
                lines.add((0 until win).map { Pair(startRow + it, col) })
            }
        }

        // Diagonals
        for (startRow in 0..size - win) {
            for (startCol in 0..size - win) {
                lines.add((0 until win).map { Pair(startRow + it, startCol + it) })
                lines.add((0 until win).map { Pair(startRow + it, startCol + win - 1 - it) })
            }
        }

        for (line in lines) {
            var symbolCount = 0
            var emptyCount = 0
            var blocked = false

            for ((r, c) in line) {
                when (board.cells[r][c].value) {
                    symbol -> symbolCount++
                    0 -> emptyCount++
                    else -> blocked = true
                }
            }

            if (!blocked) {
                when {
                    symbolCount == win - 1 && emptyCount == 1 -> immediate++
                    symbolCount == win - 2 && emptyCount == 2 -> potential++
                }
            }
        }

        return ThreatCount(immediate, potential)
    }

    private fun generateOrderedMoves(board: TicTacToeBoard, hash: Long, depth: Int = 0): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        val size = board.size

        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board.cells[row][col].isEmpty()) {
                    moves.add(Pair(row, col))
                }
            }
        }

        if (moves.isEmpty()) return moves

        val ttMove = transpositionTable.getBestMove(hash)
        val center = size / 2

        return moves.sortedByDescending { move ->
            var score = 0

            // TT move first
            if (move == ttMove) score += 10000

            // Killer moves
            if (depth < killerMoves.size) {
                if (killerMoves[depth][0] == move) score += 900
                if (killerMoves[depth][1] == move) score += 800
            }

            // Center preference
            val distFromCenter = kotlin.math.abs(move.first - center) + kotlin.math.abs(move.second - center)
            score += (size - distFromCenter) * 100

            // Corners
            if ((move.first == 0 || move.first == size - 1) &&
                (move.second == 0 || move.second == size - 1)) {
                score += 200
            }

            score
        }
    }

    private fun storeKillerMove(depth: Int, move: Pair<Int, Int>) {
        if (depth >= killerMoves.size) return
        if (killerMoves[depth][0] != move) {
            killerMoves[depth][1] = killerMoves[depth][0]
            killerMoves[depth][0] = move
        }
    }

    private fun clearKillerMoves() {
        for (i in killerMoves.indices) {
            killerMoves[i][0] = null
            killerMoves[i][1] = null
        }
    }

    private fun makeMove(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): TicTacToeBoard {
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(value = symbol) else cell
            }
        }
        return board.copy(cells = newCells)
    }

    private fun findFirstLegalMove(board: TicTacToeBoard): AIMove {
        val center = board.size / 2
        if (board.cells[center][center].isEmpty()) {
            return AIMove.PlaceSymbol(center, center)
        }

        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    return AIMove.PlaceSymbol(row, col)
                }
            }
        }
        return AIMove.PlaceSymbol(0, 0)
    }

    private fun isTimeUp(): Boolean = System.currentTimeMillis() - startTime >= timeLimit

    fun cancel() { searchCancelled = true }

    fun clearCache() { transpositionTable.clear() }

    fun getTableStats() = transpositionTable.getStats()

    // ========== ULTIMATE TICTACTOE ==========

    suspend fun findBestUltimateMove(
        board: UltimateTicTacToeBoard,
        aiSymbol: Int,
        playableBoards: List<Pair<Int, Int>>,
        timeLimitMs: Long = 3000
    ): AIUltimateMove {
        startTime = System.currentTimeMillis()
        timeLimit = timeLimitMs
        nodesSearched = 0
        searchCancelled = false

        val opponent = if (aiSymbol == 1) 2 else 1

        // Check for immediate winning moves first
        for ((boardRow, boardCol) in playableBoards) {
            val miniBoard = board.miniBoards[boardRow][boardCol]
            if (miniBoard.isFinished()) continue

            for (cellRow in 0 until 3) {
                for (cellCol in 0 until 3) {
                    if (miniBoard.cells[cellRow][cellCol].isEmpty()) {
                        val move = AIUltimateMove(boardRow, boardCol, cellRow, cellCol)
                        val newBoard = makeUltimateMove(board, move, aiSymbol)

                        // Check if this wins the mini-board
                        if (newBoard.miniBoards[boardRow][boardCol].winner == aiSymbol) {
                            // Check if winning this board wins the game
                            if (checkUltimateWinner(newBoard) == aiSymbol) {
                                return move
                            }
                        }
                    }
                }
            }
        }

        // Iterative deepening with minimax
        var bestMove: AIUltimateMove? = null
        var bestScore = Int.MIN_VALUE

        for (depth in 1..6) {
            if (isTimeUp() || searchCancelled || !coroutineContext.isActive) break

            for ((boardRow, boardCol) in playableBoards) {
                val miniBoard = board.miniBoards[boardRow][boardCol]
                if (miniBoard.isFinished()) continue

                for (cellRow in 0 until 3) {
                    for (cellCol in 0 until 3) {
                        if (miniBoard.cells[cellRow][cellCol].isEmpty()) {
                            if (isTimeUp() || searchCancelled) break

                            val move = AIUltimateMove(boardRow, boardCol, cellRow, cellCol)
                            val newBoard = makeUltimateMove(board, move, aiSymbol)

                            val score = -ultimateMinimax(
                                newBoard, depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE,
                                opponent, aiSymbol, Pair(cellRow, cellCol)
                            )

                            if (score > bestScore) {
                                bestScore = score
                                bestMove = move
                            }
                        }
                    }
                }
            }

            // Early exit if found winning move
            if (bestScore >= WIN_SCORE - 100) break

            // Check if we have enough time for next iteration
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed * 3 > timeLimit) break
        }

        return bestMove ?: AIUltimateMove(
            playableBoards.firstOrNull()?.first ?: 0,
            playableBoards.firstOrNull()?.second ?: 0, 1, 1
        )
    }

    private suspend fun ultimateMinimax(
        board: UltimateTicTacToeBoard,
        depth: Int,
        alphaIn: Int,
        beta: Int,
        currentPlayer: Int,
        aiSymbol: Int,
        lastMove: Pair<Int, Int>
    ): Int {
        if (isTimeUp() || searchCancelled || !coroutineContext.isActive) return 0
        nodesSearched++

        // Terminal checks
        val winner = checkUltimateWinner(board)
        if (winner == currentPlayer) return WIN_SCORE + depth
        if (winner != 0 && winner != 3) return -WIN_SCORE - depth
        if (isUltimateFull(board)) return DRAW_SCORE

        if (depth <= 0) {
            return evaluateUltimatePosition(board, currentPlayer, aiSymbol)
        }

        var alpha = alphaIn
        val opponent = if (currentPlayer == 1) 2 else 1
        val playable = getNextPlayableBoards(board, lastMove)

        if (playable.isEmpty()) return DRAW_SCORE

        var bestScore = Int.MIN_VALUE

        for ((br, bc) in playable) {
            val mini = board.miniBoards[br][bc]
            if (mini.isFinished()) continue

            for (cr in 0..2) {
                for (cc in 0..2) {
                    if (mini.cells[cr][cc].isEmpty()) {
                        val move = AIUltimateMove(br, bc, cr, cc)
                        val newBoard = makeUltimateMove(board, move, currentPlayer)

                        val score = -ultimateMinimax(
                            newBoard, depth - 1, -beta, -alpha,
                            opponent, aiSymbol, Pair(cr, cc)
                        )

                        bestScore = maxOf(bestScore, score)
                        alpha = maxOf(alpha, score)

                        if (alpha >= beta) return bestScore
                    }
                }
            }
        }

        return if (bestScore == Int.MIN_VALUE) DRAW_SCORE else bestScore
    }

    private fun evaluateUltimatePosition(board: UltimateTicTacToeBoard, currentPlayer: Int, aiSymbol: Int): Int {
        val opponent = if (currentPlayer == 1) 2 else 1
        var score = 0

        // Meta-board analysis
        val metaBoard = Array(3) { r -> Array(3) { c -> board.miniBoards[r][c].winner } }

        // Check meta-board lines
        val lines = listOf(
            listOf(Triple(0,0, metaBoard[0][0]), Triple(0,1, metaBoard[0][1]), Triple(0,2, metaBoard[0][2])),
            listOf(Triple(1,0, metaBoard[1][0]), Triple(1,1, metaBoard[1][1]), Triple(1,2, metaBoard[1][2])),
            listOf(Triple(2,0, metaBoard[2][0]), Triple(2,1, metaBoard[2][1]), Triple(2,2, metaBoard[2][2])),
            listOf(Triple(0,0, metaBoard[0][0]), Triple(1,0, metaBoard[1][0]), Triple(2,0, metaBoard[2][0])),
            listOf(Triple(0,1, metaBoard[0][1]), Triple(1,1, metaBoard[1][1]), Triple(2,1, metaBoard[2][1])),
            listOf(Triple(0,2, metaBoard[0][2]), Triple(1,2, metaBoard[1][2]), Triple(2,2, metaBoard[2][2])),
            listOf(Triple(0,0, metaBoard[0][0]), Triple(1,1, metaBoard[1][1]), Triple(2,2, metaBoard[2][2])),
            listOf(Triple(0,2, metaBoard[0][2]), Triple(1,1, metaBoard[1][1]), Triple(2,0, metaBoard[2][0]))
        )

        for (line in lines) {
            val currentCount = line.count { it.third == currentPlayer }
            val opponentCount = line.count { it.third == opponent }
            val openCount = line.count { it.third == 0 }

            if (currentCount > 0 && opponentCount == 0) {
                score += when (currentCount) {
                    3 -> 50000
                    2 -> if (openCount == 1) 2000 else 500
                    1 -> 50
                    else -> 0
                }
            } else if (opponentCount > 0 && currentCount == 0) {
                score -= when (opponentCount) {
                    3 -> 50000
                    2 -> if (openCount == 1) 2000 else 500
                    1 -> 50
                    else -> 0
                }
            }
        }

        // Center control is crucial
        if (metaBoard[1][1] == currentPlayer) score += 800
        else if (metaBoard[1][1] == opponent) score -= 800

        // Corner control
        val corners = listOf(Pair(0,0), Pair(0,2), Pair(2,0), Pair(2,2))
        for ((r, c) in corners) {
            if (metaBoard[r][c] == currentPlayer) score += 200
            else if (metaBoard[r][c] == opponent) score -= 200
        }

        // Evaluate potential in open mini-boards
        for (r in 0..2) {
            for (c in 0..2) {
                if (metaBoard[r][c] == 0) {
                    val mini = board.miniBoards[r][c]
                    score += evaluateMiniBoard(mini, currentPlayer, opponent) / 5
                }
            }
        }

        return score
    }

    private fun evaluateMiniBoard(mini: de.sudokuonline.app.data.model.UltimateMiniBoard, player: Int, opponent: Int): Int {
        var score = 0

        // Center
        if (mini.cells[1][1].value == player) score += 40
        else if (mini.cells[1][1].value == opponent) score -= 40

        // Corners
        val corners = listOf(Pair(0,0), Pair(0,2), Pair(2,0), Pair(2,2))
        for ((r, c) in corners) {
            if (mini.cells[r][c].value == player) score += 15
            else if (mini.cells[r][c].value == opponent) score -= 15
        }

        // Check for threats (2 in a row)
        val miniLines: List<List<Int>> = listOf(
            listOf(mini.cells[0][0].value, mini.cells[0][1].value, mini.cells[0][2].value),
            listOf(mini.cells[1][0].value, mini.cells[1][1].value, mini.cells[1][2].value),
            listOf(mini.cells[2][0].value, mini.cells[2][1].value, mini.cells[2][2].value),
            listOf(mini.cells[0][0].value, mini.cells[1][0].value, mini.cells[2][0].value),
            listOf(mini.cells[0][1].value, mini.cells[1][1].value, mini.cells[2][1].value),
            listOf(mini.cells[0][2].value, mini.cells[1][2].value, mini.cells[2][2].value),
            listOf(mini.cells[0][0].value, mini.cells[1][1].value, mini.cells[2][2].value),
            listOf(mini.cells[0][2].value, mini.cells[1][1].value, mini.cells[2][0].value)
        )

        for (line in miniLines) {
            val playerCount = line.count { value -> value == player }
            val opponentCount = line.count { value -> value == opponent }
            val emptyCount = line.count { value -> value == 0 }

            if (playerCount == 2 && emptyCount == 1) score += 100
            if (opponentCount == 2 && emptyCount == 1) score -= 100
        }

        return score
    }

    private fun makeUltimateMove(board: UltimateTicTacToeBoard, move: AIUltimateMove, symbol: Int): UltimateTicTacToeBoard {
        val newMiniBoards = board.miniBoards.mapIndexed { br, boardRow ->
            boardRow.mapIndexed { bc, miniBoard ->
                if (br == move.boardRow && bc == move.boardCol) {
                    val newCells = miniBoard.cells.mapIndexed { cr, cellRow ->
                        cellRow.mapIndexed { cc, cell ->
                            if (cr == move.cellRow && cc == move.cellCol) {
                                cell.copy(value = symbol)
                            } else cell
                        }
                    }
                    val winner = checkMiniBoardWinner(newCells)
                    miniBoard.copy(cells = newCells, winner = winner)
                } else miniBoard
            }
        }
        return board.copy(miniBoards = newMiniBoards)
    }

    private fun checkMiniBoardWinner(cells: List<List<de.sudokuonline.app.data.model.TicTacToeCell>>): Int {
        // Check rows
        for (row in 0..2) {
            if (cells[row][0].value != 0 &&
                cells[row][0].value == cells[row][1].value &&
                cells[row][1].value == cells[row][2].value) {
                return cells[row][0].value
            }
        }
        // Check columns
        for (col in 0..2) {
            if (cells[0][col].value != 0 &&
                cells[0][col].value == cells[1][col].value &&
                cells[1][col].value == cells[2][col].value) {
                return cells[0][col].value
            }
        }
        // Check diagonals
        if (cells[0][0].value != 0 && cells[0][0].value == cells[1][1].value && cells[1][1].value == cells[2][2].value) {
            return cells[0][0].value
        }
        if (cells[0][2].value != 0 && cells[0][2].value == cells[1][1].value && cells[1][1].value == cells[2][0].value) {
            return cells[0][2].value
        }
        // Check for draw
        if (cells.all { row -> row.all { it.value != 0 } }) return 3
        return 0
    }

    private fun checkUltimateWinner(board: UltimateTicTacToeBoard): Int {
        val meta = Array(3) { r -> Array(3) { c -> board.miniBoards[r][c].winner } }

        // Check rows
        for (r in 0..2) {
            if (meta[r][0] != 0 && meta[r][0] != 3 && meta[r][0] == meta[r][1] && meta[r][1] == meta[r][2]) {
                return meta[r][0]
            }
        }
        // Check columns
        for (c in 0..2) {
            if (meta[0][c] != 0 && meta[0][c] != 3 && meta[0][c] == meta[1][c] && meta[1][c] == meta[2][c]) {
                return meta[0][c]
            }
        }
        // Check diagonals
        if (meta[0][0] != 0 && meta[0][0] != 3 && meta[0][0] == meta[1][1] && meta[1][1] == meta[2][2]) {
            return meta[0][0]
        }
        if (meta[0][2] != 0 && meta[0][2] != 3 && meta[0][2] == meta[1][1] && meta[1][1] == meta[2][0]) {
            return meta[0][2]
        }
        return 0
    }

    private fun isUltimateFull(board: UltimateTicTacToeBoard): Boolean {
        return board.miniBoards.all { row -> row.all { it.isFinished() } }
    }

    private fun getNextPlayableBoards(board: UltimateTicTacToeBoard, lastMove: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (targetRow, targetCol) = lastMove
        val targetBoard = board.miniBoards[targetRow][targetCol]

        return if (targetBoard.isFinished()) {
            board.miniBoards.flatMapIndexed { row, boardRow ->
                boardRow.mapIndexedNotNull { col, miniBoard ->
                    if (!miniBoard.isFinished()) Pair(row, col) else null
                }
            }
        } else {
            listOf(Pair(targetRow, targetCol))
        }
    }
}
