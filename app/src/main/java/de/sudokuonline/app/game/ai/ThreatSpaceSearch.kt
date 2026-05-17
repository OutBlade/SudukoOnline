package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard
import kotlin.math.max
import kotlin.math.min

/**
 * Threat-Space Search for larger TicTacToe boards (4x4, 5x5)
 *
 * Instead of searching all moves, focuses on:
 * 1. Winning threats (immediate wins)
 * 2. Forcing sequences (threats that must be answered)
 * 3. Double threats (forks)
 * 4. Defensive necessities
 *
 * This dramatically reduces search space while maintaining strength.
 */
object ThreatSpaceSearch {

    // Threat types ordered by urgency
    enum class ThreatType(val priority: Int) {
        WINNING(1000),           // Immediate win
        BLOCK_LOSS(999),         // Must block or lose
        DOUBLE_THREAT(500),      // Fork - two winning threats
        BLOCK_DOUBLE(499),       // Block opponent's fork
        STRONG_THREAT(200),      // Creates 3-in-row with open end
        WEAK_THREAT(100),        // Creates 3-in-row blocked one side
        POSITIONAL(50),          // Good position
        NEUTRAL(0)
    }

    data class Threat(
        val type: ThreatType,
        val position: Int,       // Move that creates/blocks threat
        val line: List<Int>,     // The line involved
        val evaluation: Int
    )

    data class SearchResult(
        val bestMove: Int,
        val evaluation: Int,
        val principalVariation: List<Int>,
        val threatSequence: List<Threat>
    )

    // Line definitions for different board sizes
    private val lines4x4 = generateLines(4, 4)
    private val lines5x5 = generateLines(5, 4) // Need 4 in a row to win on 5x5

    private fun generateLines(size: Int, winLength: Int): List<List<Int>> {
        val lines = mutableListOf<List<Int>>()

        // Horizontal lines
        for (row in 0 until size) {
            for (startCol in 0..size - winLength) {
                lines.add((0 until winLength).map { row * size + startCol + it })
            }
        }

        // Vertical lines
        for (col in 0 until size) {
            for (startRow in 0..size - winLength) {
                lines.add((0 until winLength).map { (startRow + it) * size + col })
            }
        }

        // Diagonal lines (top-left to bottom-right)
        for (row in 0..size - winLength) {
            for (col in 0..size - winLength) {
                lines.add((0 until winLength).map { (row + it) * size + (col + it) })
            }
        }

        // Anti-diagonal lines (top-right to bottom-left)
        for (row in 0..size - winLength) {
            for (col in winLength - 1 until size) {
                lines.add((0 until winLength).map { (row + it) * size + (col - it) })
            }
        }

        return lines
    }

    /**
     * Main search function - finds best move using threat-space search
     */
    fun findBestMove(
        board: TicTacToeBoard,
        aiSymbol: Int,
        maxDepth: Int = 12,
        timeLimitMs: Long = 3000
    ): SearchResult {
        val size = board.size
        val winLength = if (size <= 4) size else 4
        val lines = when (size) {
            4 -> lines4x4
            5 -> lines5x5
            else -> generateLines(size, winLength)
        }

        val boardArray = boardToArray(board)
        val startTime = System.currentTimeMillis()

        // First, check for immediate wins or must-blocks
        val immediateMove = findImmediateMove(boardArray, size, aiSymbol, lines)
        if (immediateMove != null) {
            return SearchResult(
                bestMove = immediateMove.position,
                evaluation = immediateMove.evaluation,
                principalVariation = listOf(immediateMove.position),
                threatSequence = listOf(immediateMove)
            )
        }

        // Generate all threats for both players
        val aiThreats = findAllThreats(boardArray, size, aiSymbol, lines)
        val oppThreats = findAllThreats(boardArray, size, 3 - aiSymbol, lines)

        // Threat-space search with iterative deepening
        var bestMove = -1
        var bestEval = Int.MIN_VALUE
        var bestPV = listOf<Int>()
        var bestThreats = listOf<Threat>()

        for (depth in 1..maxDepth) {
            if (System.currentTimeMillis() - startTime > timeLimitMs * 0.8) break

            val result = threatSearch(
                boardArray, size, aiSymbol, aiSymbol,
                depth, Int.MIN_VALUE, Int.MAX_VALUE,
                lines, startTime, timeLimitMs
            )

            if (result.first >= 0) {
                bestMove = result.first
                bestEval = result.second
                bestPV = result.third
            }

            // Found winning sequence
            if (bestEval > 500000) break
        }

        // Fallback to best positional move if no threat-based move found
        if (bestMove < 0) {
            bestMove = findBestPositionalMove(boardArray, size, aiSymbol, lines)
        }

        return SearchResult(
            bestMove = bestMove,
            evaluation = bestEval,
            principalVariation = bestPV,
            threatSequence = bestThreats
        )
    }

    /**
     * Find immediate winning move or must-block
     */
    private fun findImmediateMove(
        board: IntArray,
        size: Int,
        aiSymbol: Int,
        lines: List<List<Int>>
    ): Threat? {
        val oppSymbol = 3 - aiSymbol

        // Check for winning move
        for (line in lines) {
            val aiCount = line.count { board[it] == aiSymbol }
            val emptyCount = line.count { board[it] == 0 }

            if (aiCount == line.size - 1 && emptyCount == 1) {
                val winPos = line.first { board[it] == 0 }
                return Threat(ThreatType.WINNING, winPos, line, 1000000)
            }
        }

        // Check for must-block
        for (line in lines) {
            val oppCount = line.count { board[it] == oppSymbol }
            val emptyCount = line.count { board[it] == 0 }

            if (oppCount == line.size - 1 && emptyCount == 1) {
                val blockPos = line.first { board[it] == 0 }
                return Threat(ThreatType.BLOCK_LOSS, blockPos, line, 999000)
            }
        }

        return null
    }

    /**
     * Find all threats for a player
     */
    private fun findAllThreats(
        board: IntArray,
        size: Int,
        symbol: Int,
        lines: List<List<Int>>
    ): List<Threat> {
        val threats = mutableListOf<Threat>()
        val oppSymbol = 3 - symbol

        for (line in lines) {
            val myCount = line.count { board[it] == symbol }
            val emptyCount = line.count { board[it] == 0 }
            val oppCount = line.count { board[it] == oppSymbol }

            if (oppCount > 0) continue // Line is blocked

            when {
                // Winning threat
                myCount == line.size - 1 && emptyCount == 1 -> {
                    val pos = line.first { board[it] == 0 }
                    threats.add(Threat(ThreatType.WINNING, pos, line, 1000000))
                }
                // Strong threat (n-2 in row with 2 empty)
                myCount == line.size - 2 && emptyCount == 2 -> {
                    val emptyPositions = line.filter { board[it] == 0 }
                    // Check if both ends are open
                    val isOpen = emptyPositions.size == 2
                    for (pos in emptyPositions) {
                        threats.add(Threat(
                            if (isOpen) ThreatType.STRONG_THREAT else ThreatType.WEAK_THREAT,
                            pos, line,
                            if (isOpen) 50000 else 10000
                        ))
                    }
                }
            }
        }

        // Find double threats (forks)
        val threatPositions = threats.groupBy { it.position }
        for ((pos, posThreats) in threatPositions) {
            if (posThreats.size >= 2 && posThreats.any { it.type == ThreatType.STRONG_THREAT }) {
                threats.add(Threat(ThreatType.DOUBLE_THREAT, pos, emptyList(), 100000))
            }
        }

        return threats.sortedByDescending { it.evaluation }
    }

    /**
     * Recursive threat-space search
     */
    private fun threatSearch(
        board: IntArray,
        size: Int,
        currentPlayer: Int,
        aiSymbol: Int,
        depth: Int,
        alpha: Int,
        beta: Int,
        lines: List<List<Int>>,
        startTime: Long,
        timeLimitMs: Long
    ): Triple<Int, Int, List<Int>> {
        // Time check
        if (System.currentTimeMillis() - startTime > timeLimitMs) {
            return Triple(-1, 0, emptyList())
        }

        // Check for terminal state
        val winner = checkWinner(board, size, lines)
        if (winner != 0) {
            val score = if (winner == aiSymbol) 1000000 - (12 - depth) * 1000 else -1000000 + (12 - depth) * 1000
            return Triple(-1, score, emptyList())
        }

        if (board.none { it == 0 }) {
            return Triple(-1, 0, emptyList()) // Draw
        }

        if (depth == 0) {
            return Triple(-1, evaluatePosition(board, size, aiSymbol, lines), emptyList())
        }

        val isMaximizing = currentPlayer == aiSymbol
        var bestMove = -1
        var bestScore = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE
        var bestPV = listOf<Int>()
        var currentAlpha = alpha
        var currentBeta = beta

        // Generate threat-ordered moves
        val moves = generateOrderedMoves(board, size, currentPlayer, lines)

        for (move in moves) {
            board[move] = currentPlayer

            val (_, score, pv) = threatSearch(
                board, size, 3 - currentPlayer, aiSymbol,
                depth - 1, currentAlpha, currentBeta,
                lines, startTime, timeLimitMs
            )

            board[move] = 0

            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                    bestPV = listOf(move) + pv
                }
                currentAlpha = max(currentAlpha, score)
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = move
                    bestPV = listOf(move) + pv
                }
                currentBeta = min(currentBeta, score)
            }

            if (currentBeta <= currentAlpha) break // Alpha-beta cutoff
        }

        return Triple(bestMove, bestScore, bestPV)
    }

    /**
     * Generate moves ordered by threat priority
     */
    private fun generateOrderedMoves(
        board: IntArray,
        size: Int,
        player: Int,
        lines: List<List<Int>>
    ): List<Int> {
        val oppSymbol = 3 - player
        val moveScores = mutableMapOf<Int, Int>()

        for (i in board.indices) {
            if (board[i] != 0) continue

            var score = 0

            // Check all lines through this position
            for (line in lines) {
                if (i !in line) continue

                val myCount = line.count { board[it] == player }
                val oppCount = line.count { board[it] == oppSymbol }
                val emptyCount = line.count { board[it] == 0 }

                // Winning move
                if (myCount == line.size - 1 && emptyCount == 1 && board[i] == 0) {
                    score += 1000000
                }
                // Block winning move
                else if (oppCount == line.size - 1 && emptyCount == 1 && board[i] == 0) {
                    score += 500000
                }
                // Create strong threat
                else if (myCount == line.size - 2 && emptyCount == 2 && oppCount == 0) {
                    score += 10000
                }
                // Block threat
                else if (oppCount == line.size - 2 && emptyCount == 2 && myCount == 0) {
                    score += 5000
                }
                // Develop position
                else if (myCount > 0 && oppCount == 0) {
                    score += myCount * 100
                }
            }

            // Center bonus
            val center = size / 2
            val row = i / size
            val col = i % size
            val distFromCenter = kotlin.math.abs(row - center) + kotlin.math.abs(col - center)
            score += (size - distFromCenter) * 10

            moveScores[i] = score
        }

        return moveScores.entries.sortedByDescending { it.value }.map { it.key }
    }

    /**
     * Evaluate board position
     */
    private fun evaluatePosition(
        board: IntArray,
        size: Int,
        aiSymbol: Int,
        lines: List<List<Int>>
    ): Int {
        val oppSymbol = 3 - aiSymbol
        var score = 0

        for (line in lines) {
            val aiCount = line.count { board[it] == aiSymbol }
            val oppCount = line.count { board[it] == oppSymbol }
            val emptyCount = line.count { board[it] == 0 }

            // Only consider lines that are still winnable
            if (aiCount > 0 && oppCount > 0) continue

            when {
                // Near win for AI
                aiCount == line.size - 1 && emptyCount == 1 -> score += 50000
                // Near win for opponent
                oppCount == line.size - 1 && emptyCount == 1 -> score -= 50000
                // Strong position for AI
                aiCount == line.size - 2 && emptyCount == 2 -> score += 5000
                // Strong position for opponent
                oppCount == line.size - 2 && emptyCount == 2 -> score -= 5000
                // Developing for AI
                aiCount > 0 && oppCount == 0 -> score += aiCount * aiCount * 100
                // Developing for opponent
                oppCount > 0 && aiCount == 0 -> score -= oppCount * oppCount * 100
            }
        }

        // Position bonus (center and corners)
        val center = size / 2
        for (i in board.indices) {
            if (board[i] == 0) continue

            val row = i / size
            val col = i % size
            val isCenter = row == center && col == center
            val isCorner = (row == 0 || row == size - 1) && (col == 0 || col == size - 1)

            val posBonus = when {
                isCenter -> 200
                isCorner -> 100
                else -> 50
            }

            if (board[i] == aiSymbol) score += posBonus
            else score -= posBonus
        }

        return score
    }

    /**
     * Check for winner
     */
    private fun checkWinner(board: IntArray, size: Int, lines: List<List<Int>>): Int {
        for (line in lines) {
            val first = board[line[0]]
            if (first != 0 && line.all { board[it] == first }) {
                return first
            }
        }
        return 0
    }

    /**
     * Find best positional move (fallback)
     */
    private fun findBestPositionalMove(
        board: IntArray,
        size: Int,
        aiSymbol: Int,
        lines: List<List<Int>>
    ): Int {
        val center = size / 2
        val centerPos = center * size + center

        // Prefer center
        if (board[centerPos] == 0) return centerPos

        // Then corners
        val corners = listOf(0, size - 1, size * (size - 1), size * size - 1)
        corners.shuffled().firstOrNull { board[it] == 0 }?.let { return it }

        // Then any empty
        return board.indices.firstOrNull { board[it] == 0 } ?: -1
    }

    private fun boardToArray(board: TicTacToeBoard): IntArray {
        val size = board.size
        return IntArray(size * size) { i ->
            board.cells[i / size][i % size].value
        }
    }
}
