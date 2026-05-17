package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard
import de.sudokuonline.app.data.model.UltimateTicTacToeBoard

/**
 * Advanced Position Evaluator - Stockfish-style scoring
 *
 * Uses centipawn-like values for precise evaluation:
 * - WIN: 100000 points (guaranteed win)
 * - NEAR_WIN: 50000+ points (winning position)
 * - THREAT: 8000+ points (immediate threat)
 *
 * Evaluates:
 * - Winning threats (N-1 in a row with open spot)
 * - Double threats (forks)
 * - Positional control (center, corners)
 * - Blocking value
 * - Line potential
 */
object PositionEvaluator {

    // Score constants (centipawn-like values)
    const val WIN_SCORE = 100000
    const val NEAR_WIN_SCORE = 50000
    const val THREAT_4 = 8000      // 4-in-a-row threat (5x5)
    const val THREAT_3 = 3000      // 3-in-a-row threat (3x3) or potential
    const val THREAT_2 = 800       // 2-in-a-row with 2 open
    const val FORK_BONUS = 5000    // Multiple winning threats
    const val CENTER_3X3 = 300     // Center control (3x3)
    const val CENTER_5X5 = 200     // Center control (5x5)
    const val CORNER_BONUS = 150   // Corner control
    const val EDGE_BONUS = 50      // Edge control
    const val BLOCK_MULTIPLIER = 1.1f  // Slight bonus for blocking

    /**
     * Full position evaluation
     * Returns score from AI's perspective (positive = good for AI)
     */
    fun evaluate(board: TicTacToeBoard, aiSymbol: Int): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        // Check for immediate wins
        val aiWinCheck = checkImmediateWin(board, aiSymbol)
        if (aiWinCheck != null) return WIN_SCORE

        val opponentWinCheck = checkImmediateWin(board, opponentSymbol)
        if (opponentWinCheck != null) return -WIN_SCORE

        var score = 0

        // Threat analysis
        val aiThreats = countThreats(board, aiSymbol)
        val opponentThreats = countThreats(board, opponentSymbol)

        // AI winning threats
        score += when {
            aiThreats.immediateThreat > 1 -> FORK_BONUS + aiThreats.immediateThreat * THREAT_4
            aiThreats.immediateThreat == 1 -> THREAT_4
            else -> 0
        }
        score += aiThreats.potentialThreats * THREAT_2

        // Opponent threats (we need to block)
        score -= when {
            opponentThreats.immediateThreat > 1 -> (FORK_BONUS * BLOCK_MULTIPLIER).toInt()
            opponentThreats.immediateThreat == 1 -> (THREAT_4 * BLOCK_MULTIPLIER).toInt()
            else -> 0
        }
        score -= (opponentThreats.potentialThreats * THREAT_2 * BLOCK_MULTIPLIER).toInt()

        // Positional evaluation
        score += evaluatePosition(board, aiSymbol)
        score -= evaluatePosition(board, opponentSymbol)

        // Line potential (how many lines can still be won)
        score += evaluateLinePotential(board, aiSymbol) * 50
        score -= evaluateLinePotential(board, opponentSymbol) * 50

        return score
    }

    /**
     * Quick static evaluation for leaf nodes
     */
    fun quickEvaluate(board: TicTacToeBoard, aiSymbol: Int): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        var score = 0

        // Count pieces
        val aiPieces = countPieces(board, aiSymbol)
        val oppPieces = countPieces(board, opponentSymbol)

        // Simple threat count
        val aiThreats = countThreats(board, aiSymbol)
        val oppThreats = countThreats(board, opponentSymbol)

        score += aiThreats.immediateThreat * 5000
        score -= oppThreats.immediateThreat * 5000
        score += aiThreats.potentialThreats * 500
        score -= oppThreats.potentialThreats * 500

        // Positional
        score += evaluatePosition(board, aiSymbol)
        score -= evaluatePosition(board, opponentSymbol)

        return score
    }

    /**
     * Check if there's an immediate winning move
     * Returns the winning position if found
     */
    fun checkImmediateWin(board: TicTacToeBoard, symbol: Int): Pair<Int, Int>? {
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
     * Check if placing at (row, col) would win
     */
    fun wouldWin(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val winCondition = board.winCondition
        val size = board.size

        // Check row
        var count = 1
        var c = col - 1
        while (c >= 0 && (board.cells[row][c].value == symbol || c == col)) {
            if (board.cells[row][c].value == symbol) count++
            c--
        }
        c = col + 1
        while (c < size && (board.cells[row][c].value == symbol || c == col)) {
            if (board.cells[row][c].value == symbol) count++
            c++
        }
        if (count >= winCondition) return true

        // Check column
        count = 1
        var r = row - 1
        while (r >= 0 && (board.cells[r][col].value == symbol || r == row)) {
            if (board.cells[r][col].value == symbol) count++
            r--
        }
        r = row + 1
        while (r < size && (board.cells[r][col].value == symbol || r == row)) {
            if (board.cells[r][col].value == symbol) count++
            r++
        }
        if (count >= winCondition) return true

        // Check diagonal (top-left to bottom-right)
        count = 1
        r = row - 1
        c = col - 1
        while (r >= 0 && c >= 0 && (board.cells[r][c].value == symbol || (r == row && c == col))) {
            if (board.cells[r][c].value == symbol) count++
            r--
            c--
        }
        r = row + 1
        c = col + 1
        while (r < size && c < size && (board.cells[r][c].value == symbol || (r == row && c == col))) {
            if (board.cells[r][c].value == symbol) count++
            r++
            c++
        }
        if (count >= winCondition) return true

        // Check anti-diagonal (top-right to bottom-left)
        count = 1
        r = row - 1
        c = col + 1
        while (r >= 0 && c < size && (board.cells[r][c].value == symbol || (r == row && c == col))) {
            if (board.cells[r][c].value == symbol) count++
            r--
            c++
        }
        r = row + 1
        c = col - 1
        while (r < size && c >= 0 && (board.cells[r][c].value == symbol || (r == row && c == col))) {
            if (board.cells[r][c].value == symbol) count++
            r++
            c--
        }
        if (count >= winCondition) return true

        return false
    }

    data class ThreatCount(
        val immediateThreat: Int,  // Can win next move
        val potentialThreats: Int  // 2 in a row with 2 open
    )

    /**
     * Count threats for a player
     */
    fun countThreats(board: TicTacToeBoard, symbol: Int): ThreatCount {
        var immediate = 0
        var potential = 0

        val lines = getAllLines(board)
        val winCondition = board.winCondition

        for (line in lines) {
            val symbolCount = line.count { it == symbol }
            val emptyCount = line.count { it == 0 }
            val otherCount = line.size - symbolCount - emptyCount

            if (otherCount > 0) continue  // Line is blocked

            when {
                symbolCount == winCondition - 1 && emptyCount >= 1 -> immediate++
                symbolCount == winCondition - 2 && emptyCount >= 2 -> potential++
            }
        }

        return ThreatCount(immediate, potential)
    }

    /**
     * Evaluate positional control
     */
    private fun evaluatePosition(board: TicTacToeBoard, symbol: Int): Int {
        var score = 0
        val size = board.size
        val center = size / 2

        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board.cells[row][col].value == symbol) {
                    // Center
                    if (row == center && col == center) {
                        score += if (size == 3) CENTER_3X3 else CENTER_5X5
                    }
                    // Near center (for 5x5)
                    else if (size == 5 && row in 1..3 && col in 1..3) {
                        score += CENTER_5X5 / 2
                    }
                    // Corners
                    else if ((row == 0 || row == size - 1) && (col == 0 || col == size - 1)) {
                        score += CORNER_BONUS
                    }
                    // Edges
                    else if (row == 0 || row == size - 1 || col == 0 || col == size - 1) {
                        score += EDGE_BONUS
                    }
                }
            }
        }

        return score
    }

    /**
     * Count how many lines can still potentially be won
     */
    private fun evaluateLinePotential(board: TicTacToeBoard, symbol: Int): Int {
        val opponent = if (symbol == 1) 2 else 1
        var openLines = 0

        val lines = getAllLines(board)
        for (line in lines) {
            val hasOpponent = line.any { it == opponent }
            if (!hasOpponent) {
                openLines++
            }
        }

        return openLines
    }

    private fun countPieces(board: TicTacToeBoard, symbol: Int): Int {
        return board.cells.flatten().count { it.value == symbol }
    }

    /**
     * Get all winning lines on the board
     */
    private fun getAllLines(board: TicTacToeBoard): List<List<Int>> {
        val lines = mutableListOf<List<Int>>()
        val size = board.size
        val winCondition = board.winCondition

        // Rows
        for (row in 0 until size) {
            for (startCol in 0..size - winCondition) {
                lines.add((startCol until startCol + winCondition).map {
                    board.cells[row][it].value
                })
            }
        }

        // Columns
        for (col in 0 until size) {
            for (startRow in 0..size - winCondition) {
                lines.add((startRow until startRow + winCondition).map {
                    board.cells[it][col].value
                })
            }
        }

        // Diagonals
        for (startRow in 0..size - winCondition) {
            for (startCol in 0..size - winCondition) {
                // Main diagonal
                lines.add((0 until winCondition).map {
                    board.cells[startRow + it][startCol + it].value
                })
                // Anti-diagonal
                lines.add((0 until winCondition).map {
                    board.cells[startRow + it][startCol + winCondition - 1 - it].value
                })
            }
        }

        return lines
    }

    /**
     * Evaluate Ultimate TicTacToe position
     */
    fun evaluateUltimate(board: UltimateTicTacToeBoard, aiSymbol: Int): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        var score = 0

        // Create meta-board
        val metaBoard = Array(3) { row ->
            IntArray(3) { col ->
                board.miniBoards[row][col].winner
            }
        }

        // Check meta-board threats
        score += evaluateMetaThreats(metaBoard, aiSymbol) * 1000
        score -= evaluateMetaThreats(metaBoard, opponentSymbol) * 1000

        // Center mini-board control
        if (metaBoard[1][1] == aiSymbol) score += 500
        else if (metaBoard[1][1] == opponentSymbol) score -= 500

        // Corner mini-boards
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        for ((r, c) in corners) {
            if (metaBoard[r][c] == aiSymbol) score += 200
            else if (metaBoard[r][c] == opponentSymbol) score -= 200
        }

        // Evaluate individual mini-boards
        for (row in 0..2) {
            for (col in 0..2) {
                val miniBoard = board.miniBoards[row][col]
                if (!miniBoard.isFinished()) {
                    // Create a TicTacToeBoard for the mini-board
                    val tempBoard = de.sudokuonline.app.data.model.TicTacToeBoard(
                        cells = miniBoard.cells,
                        size = 3,
                        winCondition = 3
                    )
                    val miniBoardScore = quickEvaluate(tempBoard, aiSymbol)
                    // Weight by position importance
                    val weight = if (row == 1 && col == 1) 1.5f else 1.0f
                    score += (miniBoardScore * weight * 0.1f).toInt()
                }
            }
        }

        return score
    }

    private fun evaluateMetaThreats(metaBoard: Array<IntArray>, symbol: Int): Int {
        var threats = 0

        // Rows
        for (row in 0..2) {
            threats += evaluateMetaLine(listOf(metaBoard[row][0], metaBoard[row][1], metaBoard[row][2]), symbol)
        }

        // Columns
        for (col in 0..2) {
            threats += evaluateMetaLine(listOf(metaBoard[0][col], metaBoard[1][col], metaBoard[2][col]), symbol)
        }

        // Diagonals
        threats += evaluateMetaLine(listOf(metaBoard[0][0], metaBoard[1][1], metaBoard[2][2]), symbol)
        threats += evaluateMetaLine(listOf(metaBoard[0][2], metaBoard[1][1], metaBoard[2][0]), symbol)

        return threats
    }

    private fun evaluateMetaLine(line: List<Int>, symbol: Int): Int {
        val symbolCount = line.count { it == symbol }
        val emptyCount = line.count { it == 0 }
        val otherCount = line.count { it != symbol && it != 0 && it != 3 }  // 3 = draw

        if (otherCount > 0) return 0  // Blocked by opponent

        return when {
            symbolCount == 3 -> 100  // Won
            symbolCount == 2 && emptyCount == 1 -> 10  // Threat
            symbolCount == 1 && emptyCount == 2 -> 2  // Potential
            else -> 0
        }
    }
}
