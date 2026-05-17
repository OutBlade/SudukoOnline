package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.ai.BombModeAI
import de.sudokuonline.app.game.ai.OpeningBook
import de.sudokuonline.app.game.ai.PerfectTicTacToe
import de.sudokuonline.app.game.ai.ThreatSpaceSearch
import de.sudokuonline.app.game.ai.UltimateTicTacToeAI
import kotlin.math.max
import kotlin.math.min

/**
 * TicTacToe AI v4.0 - Professional-grade AI with specialized engines
 *
 * Key improvements:
 * - PERFECT play on 3x3 (mathematically cannot lose)
 * - Threat-space search for 5x5 (dramatically stronger)
 * - Specialized BombModeAI with resource management
 * - UltimateTicTacToeAI with board control strategy
 * - Opening book for instant, strong opening play
 * - All game modes have specialized, optimized strategies
 */
object TicTacToeAI {

    // Search depths - MUCH deeper for stronger play
    private const val MAX_DEPTH_3X3 = 15  // Effectively unlimited for 3x3
    private const val MAX_DEPTH_4X4 = 12
    private const val MAX_DEPTH_5X5 = 10
    private const val MAX_DEPTH_BOMB = 8
    private const val MAX_DEPTH_ULTIMATE = 8  // Increased for better play

    // Evaluation constants
    private const val WIN_SCORE = 1000000
    private const val THREAT_SCORE = 50000      // Can win next move
    private const val DOUBLE_THREAT = 100000    // Fork - 2+ winning threats
    private const val BLOCK_THREAT = 45000      // Must block opponent
    private const val TWO_IN_ROW = 1000
    private const val CENTER_BONUS = 500
    private const val CORNER_BONUS = 300

    // Bomb mode specific
    private const val BOMB_DESTROY_THREAT = 80000   // Destroy opponent's winning threat
    private const val BOMB_DESTROY_TWO = 30000      // Destroy opponent's 2-in-row
    private const val BOMB_SAVE_OWN = -20000        // Don't destroy own pieces (negative to avoid)

    /**
     * Get the best move for the AI - Main entry point
     * Routes to specialized AI based on game mode and board size
     */
    fun getBestMove(
        board: TicTacToeBoard,
        aiSymbol: Int,
        gameMode: TicTacToeGameMode,
        bombsRemaining: Int = 0,
        opponentBombs: Int = 0
    ): AIMove {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        // Route to specialized AI based on game mode
        return when {
            // Bomb modes - use specialized BombModeAI
            gameMode == TicTacToeGameMode.BOMB || gameMode == TicTacToeGameMode.L_BOMB -> {
                val bombMove = BombModeAI.getBestMove(
                    board = board,
                    aiSymbol = aiSymbol,
                    aiBombs = bombsRemaining,
                    opponentBombs = opponentBombs,
                    winLength = board.winCondition
                )
                if (bombMove.isBomb) {
                    AIMove.PlaceBomb(bombMove.row, bombMove.col)
                } else {
                    AIMove.PlaceSymbol(bombMove.row, bombMove.col)
                }
            }

            // 3x3 - use Perfect AI (never loses)
            board.size == 3 -> {
                // Try opening book first
                OpeningBook.getTicTacToe3x3Move(board, aiSymbol)?.let { pos ->
                    return AIMove.PlaceSymbol(pos / 3, pos % 3)
                }
                // Use perfect 3x3 AI
                val pos = PerfectTicTacToe.getBestMove(board, aiSymbol)
                if (pos >= 0) {
                    AIMove.PlaceSymbol(pos / 3, pos % 3)
                } else {
                    // Fallback
                    getStandardBestMove(board, aiSymbol)
                }
            }

            // 5x5 - use Threat-Space Search
            board.size == 5 -> {
                // Try opening book first
                OpeningBook.getTicTacToe5x5Move(board, aiSymbol)?.let { pos ->
                    return AIMove.PlaceSymbol(pos / 5, pos % 5)
                }
                // Use threat-space search
                val result = ThreatSpaceSearch.findBestMove(
                    board = board,
                    aiSymbol = aiSymbol,
                    maxDepth = 12,
                    timeLimitMs = 3000
                )
                AIMove.PlaceSymbol(result.bestMove / 5, result.bestMove % 5)
            }

            // 4x4 and others - use standard minimax with improvements
            else -> {
                getStandardBestMove(board, aiSymbol)
            }
        }
    }

    /**
     * Standard minimax-based move selection (for 4x4 and fallback)
     */
    private fun getStandardBestMove(board: TicTacToeBoard, aiSymbol: Int): AIMove {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        // 1. ALWAYS check for immediate win first
        val winMove = findWinningMove(board, aiSymbol)
        if (winMove != null) {
            return AIMove.PlaceSymbol(winMove.first, winMove.second)
        }

        // 2. ALWAYS block opponent's immediate win
        val blockMove = findWinningMove(board, opponentSymbol)
        if (blockMove != null) {
            return AIMove.PlaceSymbol(blockMove.first, blockMove.second)
        }

        // 3. Check for fork opportunity (create 2+ threats)
        val forkMove = findForkMove(board, aiSymbol)
        if (forkMove != null) {
            return AIMove.PlaceSymbol(forkMove.first, forkMove.second)
        }

        // 4. Block opponent's fork
        val blockFork = findForkMove(board, opponentSymbol)
        if (blockFork != null) {
            // Either block it or create our own threat that forces opponent to block
            val forcingMove = findForcingMove(board, aiSymbol, blockFork)
            if (forcingMove != null) {
                return AIMove.PlaceSymbol(forcingMove.first, forcingMove.second)
            }
            return AIMove.PlaceSymbol(blockFork.first, blockFork.second)
        }

        // 5. Use minimax for best strategic move
        val maxDepth = when (board.size) {
            3 -> MAX_DEPTH_3X3
            4 -> MAX_DEPTH_4X4
            else -> MAX_DEPTH_5X5
        }

        val bestMove = minimaxRoot(board, maxDepth, aiSymbol)
        return AIMove.PlaceSymbol(bestMove.row, bestMove.col)
    }

    /**
     * Find an immediate winning move
     */
    private fun findWinningMove(board: TicTacToeBoard, symbol: Int): Pair<Int, Int>? {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    if (wouldWinAt(board, row, col, symbol)) {
                        return Pair(row, col)
                    }
                }
            }
        }
        return null
    }

    /**
     * Find a fork move (creates 2+ winning threats)
     */
    private fun findForkMove(board: TicTacToeBoard, symbol: Int): Pair<Int, Int>? {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    val newBoard = makeMove(board, row, col, symbol)
                    val threats = countWinningThreats(newBoard, symbol)
                    if (threats >= 2) {
                        return Pair(row, col)
                    }
                }
            }
        }
        return null
    }

    /**
     * Find a move that creates a threat while not allowing opponent's fork
     */
    private fun findForcingMove(board: TicTacToeBoard, aiSymbol: Int, opponentFork: Pair<Int, Int>): Pair<Int, Int>? {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        // Find moves that create a threat (opponent must block) and don't enable their fork
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    val newBoard = makeMove(board, row, col, aiSymbol)
                    // Check if this creates a threat
                    val threats = countWinningThreats(newBoard, aiSymbol)
                    if (threats >= 1) {
                        // Find where opponent must block
                        val blockPos = findWinningMove(newBoard, aiSymbol)
                        if (blockPos != null && blockPos != opponentFork) {
                            // Opponent has to block somewhere else, ruining their fork
                            return Pair(row, col)
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Count how many winning threats exist
     */
    private fun countWinningThreats(board: TicTacToeBoard, symbol: Int): Int {
        var threats = 0
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    if (wouldWinAt(board, row, col, symbol)) {
                        threats++
                    }
                }
            }
        }
        return threats
    }

    /**
     * Check if placing at (row, col) would win
     */
    private fun wouldWinAt(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val size = board.size
        val win = board.winCondition

        // Check all 4 directions
        val directions = listOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal
            Pair(1, -1)   // Anti-diagonal
        )

        for ((dr, dc) in directions) {
            var count = 1  // The piece we're placing

            // Count in positive direction
            var r = row + dr
            var c = col + dc
            while (r in 0 until size && c in 0 until size && board.cells[r][c].value == symbol) {
                count++
                r += dr
                c += dc
            }

            // Count in negative direction
            r = row - dr
            c = col - dc
            while (r in 0 until size && c in 0 until size && board.cells[r][c].value == symbol) {
                count++
                r -= dr
                c -= dc
            }

            if (count >= win) return true
        }

        return false
    }

    /**
     * Root minimax call with move ordering
     */
    private fun minimaxRoot(board: TicTacToeBoard, maxDepth: Int, aiSymbol: Int): MoveResult {
        val orderedMoves = getOrderedMoves(board, aiSymbol)
        var bestMove = MoveResult(-1, -1, Int.MIN_VALUE)
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        for ((row, col) in orderedMoves) {
            if (board.cells[row][col].isEmpty()) {
                val newBoard = makeMove(board, row, col, aiSymbol)
                val score = minimax(newBoard, maxDepth - 1, false, aiSymbol, alpha, beta)

                if (score > bestMove.score) {
                    bestMove = MoveResult(row, col, score)
                }
                alpha = max(alpha, score)
            }
        }

        // Fallback to center or first available
        if (bestMove.row == -1) {
            val center = board.size / 2
            if (board.cells[center][center].isEmpty()) {
                return MoveResult(center, center, 0)
            }
            for (row in 0 until board.size) {
                for (col in 0 until board.size) {
                    if (board.cells[row][col].isEmpty()) {
                        return MoveResult(row, col, 0)
                    }
                }
            }
        }

        return bestMove
    }

    private data class MoveResult(val row: Int, val col: Int, val score: Int)

    private fun minimax(
        board: TicTacToeBoard,
        depth: Int,
        isMaximizing: Boolean,
        aiSymbol: Int,
        alpha: Int,
        beta: Int
    ): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        // Check terminal states
        val winner = TicTacToeLogic.checkWinner(board)
        if (winner == aiSymbol) return WIN_SCORE + depth
        if (winner == opponentSymbol) return -WIN_SCORE - depth
        if (TicTacToeLogic.isBoardFull(board) || depth == 0) {
            return evaluateBoard(board, aiSymbol)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            for ((row, col) in getOrderedMoves(board, aiSymbol)) {
                if (board.cells[row][col].isEmpty()) {
                    val newBoard = makeMove(board, row, col, aiSymbol)
                    val score = minimax(newBoard, depth - 1, false, aiSymbol, currentAlpha, currentBeta)
                    maxScore = max(maxScore, score)
                    currentAlpha = max(currentAlpha, score)
                    if (currentBeta <= currentAlpha) break
                }
            }
            return maxScore
        } else {
            var minScore = Int.MAX_VALUE
            for ((row, col) in getOrderedMoves(board, opponentSymbol)) {
                if (board.cells[row][col].isEmpty()) {
                    val newBoard = makeMove(board, row, col, opponentSymbol)
                    val score = minimax(newBoard, depth - 1, true, aiSymbol, currentAlpha, currentBeta)
                    minScore = min(minScore, score)
                    currentBeta = min(currentBeta, score)
                    if (currentBeta <= currentAlpha) break
                }
            }
            return minScore
        }
    }

    /**
     * Order moves by strategic importance for better pruning
     */
    private fun getOrderedMoves(board: TicTacToeBoard, forSymbol: Int): List<Pair<Int, Int>> {
        val size = board.size
        val center = size / 2
        val opponentSymbol = if (forSymbol == 1) 2 else 1

        val moves = mutableListOf<Pair<Pair<Int, Int>, Int>>()

        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board.cells[row][col].isEmpty()) {
                    var priority = 0

                    // Winning move - highest priority
                    if (wouldWinAt(board, row, col, forSymbol)) {
                        priority += 10000
                    }

                    // Blocking opponent's win
                    if (wouldWinAt(board, row, col, opponentSymbol)) {
                        priority += 9000
                    }

                    // Creating threats
                    val newBoard = makeMove(board, row, col, forSymbol)
                    val threats = countWinningThreats(newBoard, forSymbol)
                    priority += threats * 2000

                    // Center control
                    if (row == center && col == center) {
                        priority += 500
                    } else if (row in (center - 1)..(center + 1) && col in (center - 1)..(center + 1)) {
                        priority += 200
                    }

                    // Corners
                    if ((row == 0 || row == size - 1) && (col == 0 || col == size - 1)) {
                        priority += 300
                    }

                    moves.add(Pair(Pair(row, col), priority))
                }
            }
        }

        return moves.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Evaluate board position (used when not at terminal state)
     */
    private fun evaluateBoard(board: TicTacToeBoard, aiSymbol: Int): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        var score = 0

        // Count threats
        val aiThreats = countWinningThreats(board, aiSymbol)
        val oppThreats = countWinningThreats(board, opponentSymbol)

        // Multiple threats (fork) is extremely valuable
        if (aiThreats >= 2) score += DOUBLE_THREAT
        else if (aiThreats == 1) score += THREAT_SCORE

        if (oppThreats >= 2) score -= DOUBLE_THREAT
        else if (oppThreats == 1) score -= BLOCK_THREAT

        // Evaluate all lines
        val lines = getAllLines(board)
        for (line in lines) {
            score += evaluateLine(line, aiSymbol, opponentSymbol)
        }

        // Positional bonuses
        val size = board.size
        val center = size / 2

        // Center control
        if (board.cells[center][center].value == aiSymbol) score += CENTER_BONUS
        else if (board.cells[center][center].value == opponentSymbol) score -= CENTER_BONUS

        // Corner control (important in 3x3)
        val corners = listOf(
            Pair(0, 0), Pair(0, size - 1),
            Pair(size - 1, 0), Pair(size - 1, size - 1)
        )
        for ((r, c) in corners) {
            if (board.cells[r][c].value == aiSymbol) score += CORNER_BONUS
            else if (board.cells[r][c].value == opponentSymbol) score -= CORNER_BONUS
        }

        // For 5x5: inner square control
        if (size >= 5) {
            for (r in 1..3) {
                for (c in 1..3) {
                    if (board.cells[r][c].value == aiSymbol) score += 50
                    else if (board.cells[r][c].value == opponentSymbol) score -= 50
                }
            }
        }

        return score
    }

    private fun evaluateLine(line: List<Int>, aiSymbol: Int, opponentSymbol: Int): Int {
        val aiCount = line.count { it == aiSymbol }
        val opponentCount = line.count { it == opponentSymbol }
        val emptyCount = line.count { it == 0 }
        val lineSize = line.size

        // Line blocked by both - worthless
        if (aiCount > 0 && opponentCount > 0) return 0

        return when {
            aiCount == lineSize -> WIN_SCORE  // Win
            opponentCount == lineSize -> -WIN_SCORE  // Loss
            aiCount == lineSize - 1 && emptyCount == 1 -> 5000  // One away from win
            opponentCount == lineSize - 1 && emptyCount == 1 -> -5000  // Must block
            aiCount == lineSize - 2 && emptyCount == 2 -> TWO_IN_ROW
            opponentCount == lineSize - 2 && emptyCount == 2 -> -TWO_IN_ROW
            aiCount > 0 -> aiCount * 100
            opponentCount > 0 -> -opponentCount * 100
            else -> 0
        }
    }

    private fun getAllLines(board: TicTacToeBoard): List<List<Int>> {
        val lines = mutableListOf<List<Int>>()
        val size = board.size
        val winCondition = board.winCondition

        // Rows
        for (row in 0 until size) {
            for (startCol in 0..size - winCondition) {
                lines.add((startCol until startCol + winCondition).map { board.cells[row][it].value })
            }
        }

        // Columns
        for (col in 0 until size) {
            for (startRow in 0..size - winCondition) {
                lines.add((startRow until startRow + winCondition).map { board.cells[it][col].value })
            }
        }

        // Diagonals
        for (startRow in 0..size - winCondition) {
            for (startCol in 0..size - winCondition) {
                lines.add((0 until winCondition).map { board.cells[startRow + it][startCol + it].value })
                lines.add((0 until winCondition).map { board.cells[startRow + it][startCol + winCondition - 1 - it].value })
            }
        }

        return lines
    }

    // ============ BOMB MODE SPECIALIZED AI ============

    /**
     * Specialized AI for bomb modes - much smarter bomb usage
     */
    private fun getBestMoveWithBombs(
        board: TicTacToeBoard,
        aiSymbol: Int,
        gameMode: TicTacToeGameMode,
        bombsRemaining: Int
    ): AIMove {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        // Evaluate best symbol placement
        var bestSymbolMove: AIMove? = null
        var bestSymbolScore = Int.MIN_VALUE

        // Evaluate best bomb placement
        var bestBombMove: AIMove? = null
        var bestBombScore = Int.MIN_VALUE

        // Check all possible symbol placements
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    val newBoard = makeMove(board, row, col, aiSymbol)
                    var score = evaluateBombModePosition(newBoard, aiSymbol, gameMode)

                    // Bonus for creating threats
                    if (wouldWinAt(board, row, col, aiSymbol)) {
                        score += WIN_SCORE
                    } else if (countWinningThreats(newBoard, aiSymbol) >= 2) {
                        score += DOUBLE_THREAT
                    } else if (countWinningThreats(newBoard, aiSymbol) >= 1) {
                        score += THREAT_SCORE
                    }

                    // Block opponent threats
                    if (wouldWinAt(board, row, col, opponentSymbol)) {
                        score += BLOCK_THREAT
                    }

                    if (score > bestSymbolScore) {
                        bestSymbolScore = score
                        bestSymbolMove = AIMove.PlaceSymbol(row, col)
                    }
                }
            }
        }

        // Check all possible bomb placements
        if (bombsRemaining > 0) {
            for (row in 0 until board.size) {
                for (col in 0 until board.size) {
                    if (board.cells[row][col].isEmpty()) {
                        val bombedBoard = detonateBomb(board, row, col, gameMode)
                        var bombScore = evaluateBombPlacement(board, bombedBoard, row, col, aiSymbol, opponentSymbol, gameMode)

                        // Evaluate resulting position
                        bombScore += evaluateBombModePosition(bombedBoard, aiSymbol, gameMode) / 2

                        if (bombScore > bestBombScore) {
                            bestBombScore = bombScore
                            bestBombMove = AIMove.PlaceBomb(row, col)
                        }
                    }
                }
            }
        }

        // Choose between bomb and symbol
        // Only use bomb if it's significantly better (strategic bombs)
        if (bestBombMove != null && bestBombScore > bestSymbolScore + 20000) {
            return bestBombMove
        }

        return bestSymbolMove ?: AIMove.PlaceSymbol(board.size / 2, board.size / 2)
    }

    /**
     * Evaluate how good a bomb placement is
     */
    private fun evaluateBombPlacement(
        originalBoard: TicTacToeBoard,
        bombedBoard: TicTacToeBoard,
        bombRow: Int,
        bombCol: Int,
        aiSymbol: Int,
        opponentSymbol: Int,
        gameMode: TicTacToeGameMode
    ): Int {
        var score = 0

        // Count pieces destroyed
        var aiPiecesDestroyed = 0
        var opponentPiecesDestroyed = 0

        val affectedPositions = getAffectedPositions(bombRow, bombCol, originalBoard.size, gameMode)

        for ((r, c) in affectedPositions) {
            if (r in 0 until originalBoard.size && c in 0 until originalBoard.size) {
                when (originalBoard.cells[r][c].value) {
                    aiSymbol -> aiPiecesDestroyed++
                    opponentSymbol -> opponentPiecesDestroyed++
                }
            }
        }

        // Basic piece value
        score += (opponentPiecesDestroyed - aiPiecesDestroyed) * 5000

        // HUGE bonus for destroying opponent's winning threat
        for ((r, c) in affectedPositions) {
            if (r in 0 until originalBoard.size && c in 0 until originalBoard.size) {
                if (originalBoard.cells[r][c].value == opponentSymbol) {
                    // Check if this piece was part of a threat
                    val tempBoard = makeMove(originalBoard, r, c, 0) // Remove piece temporarily
                    val threatsBefore = countLineThreatsAt(originalBoard, r, c, opponentSymbol)
                    if (threatsBefore > 0) {
                        score += BOMB_DESTROY_THREAT
                    }
                    val twoInRowBefore = countTwoInRowAt(originalBoard, r, c, opponentSymbol)
                    if (twoInRowBefore > 0) {
                        score += BOMB_DESTROY_TWO
                    }
                }
            }
        }

        // Penalty for destroying our own winning threats
        for ((r, c) in affectedPositions) {
            if (r in 0 until originalBoard.size && c in 0 until originalBoard.size) {
                if (originalBoard.cells[r][c].value == aiSymbol) {
                    val ourThreats = countLineThreatsAt(originalBoard, r, c, aiSymbol)
                    if (ourThreats > 0) {
                        score += BOMB_SAVE_OWN * 2  // Very bad to destroy our own threats
                    }
                }
            }
        }

        // Don't waste bombs early - prefer to save them for critical moments
        val emptyCount = originalBoard.cells.flatten().count { it.isEmpty() }
        val totalCells = originalBoard.size * originalBoard.size
        if (emptyCount > totalCells * 0.6) {
            score -= 10000  // Penalty for early bomb use
        }

        return score
    }

    /**
     * Get positions affected by bomb
     */
    private fun getAffectedPositions(row: Int, col: Int, size: Int, gameMode: TicTacToeGameMode): List<Pair<Int, Int>> {
        return if (gameMode == TicTacToeGameMode.L_BOMB) {
            // L-shaped bomb
            listOf(
                Pair(row, col),
                Pair(row - 1, col), Pair(row + 1, col),
                Pair(row, col - 1), Pair(row, col + 1),
                Pair(row - 1, col - 1), Pair(row + 1, col + 1)
            )
        } else {
            // Standard 3x3 bomb
            listOf(
                Pair(row - 1, col - 1), Pair(row - 1, col), Pair(row - 1, col + 1),
                Pair(row, col - 1), Pair(row, col), Pair(row, col + 1),
                Pair(row + 1, col - 1), Pair(row + 1, col), Pair(row + 1, col + 1)
            )
        }
    }

    /**
     * Count threats at a specific position
     */
    private fun countLineThreatsAt(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Int {
        val size = board.size
        val win = board.winCondition
        var threats = 0

        // Check all directions through this point
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1)
        )

        for ((dr, dc) in directions) {
            // Check all windows that include this cell
            for (start in -(win - 1)..0) {
                var symbolCount = 0
                var emptyCount = 0
                var includesCell = false

                for (i in 0 until win) {
                    val r = row + (start + i) * dr
                    val c = col + (start + i) * dc

                    if (r !in 0 until size || c !in 0 until size) {
                        symbolCount = -1
                        break
                    }

                    if (r == row && c == col) includesCell = true

                    when (board.cells[r][c].value) {
                        symbol -> symbolCount++
                        0 -> emptyCount++
                        else -> {
                            symbolCount = -1
                            break
                        }
                    }
                }

                if (symbolCount >= 0 && includesCell && symbolCount == win - 1 && emptyCount == 1) {
                    threats++
                }
            }
        }

        return threats
    }

    /**
     * Count two-in-a-row patterns at position
     */
    private fun countTwoInRowAt(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Int {
        val size = board.size
        val win = board.winCondition
        var count = 0

        val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))

        for ((dr, dc) in directions) {
            for (start in -(win - 1)..0) {
                var symbolCount = 0
                var emptyCount = 0
                var includesCell = false

                for (i in 0 until win) {
                    val r = row + (start + i) * dr
                    val c = col + (start + i) * dc

                    if (r !in 0 until size || c !in 0 until size) {
                        symbolCount = -1
                        break
                    }

                    if (r == row && c == col) includesCell = true

                    when (board.cells[r][c].value) {
                        symbol -> symbolCount++
                        0 -> emptyCount++
                        else -> {
                            symbolCount = -1
                            break
                        }
                    }
                }

                if (symbolCount >= 0 && includesCell && symbolCount == win - 2 && emptyCount == 2) {
                    count++
                }
            }
        }

        return count
    }

    /**
     * Evaluate position for bomb mode
     */
    private fun evaluateBombModePosition(board: TicTacToeBoard, aiSymbol: Int, gameMode: TicTacToeGameMode): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        var score = 0

        // Piece count matters more in bomb mode (pieces can be destroyed)
        val aiPieces = board.cells.flatten().count { it.value == aiSymbol }
        val oppPieces = board.cells.flatten().count { it.value == opponentSymbol }
        score += (aiPieces - oppPieces) * 2000

        // Spread out pieces are safer in bomb mode
        score += evaluatePieceSpread(board, aiSymbol) * 500
        score -= evaluatePieceSpread(board, opponentSymbol) * 500

        // Regular position evaluation
        score += evaluateBoard(board, aiSymbol)

        return score
    }

    /**
     * Evaluate how spread out pieces are (spread = safer from bombs)
     */
    private fun evaluatePieceSpread(board: TicTacToeBoard, symbol: Int): Int {
        val positions = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].value == symbol) {
                    positions.add(Pair(row, col))
                }
            }
        }

        if (positions.size < 2) return 0

        // Count pieces that are NOT adjacent to each other
        var spreadScore = 0
        for (i in positions.indices) {
            var hasAdjacentAlly = false
            for (j in positions.indices) {
                if (i != j) {
                    val dist = maxOf(
                        kotlin.math.abs(positions[i].first - positions[j].first),
                        kotlin.math.abs(positions[i].second - positions[j].second)
                    )
                    if (dist <= 1) {
                        hasAdjacentAlly = true
                        break
                    }
                }
            }
            if (!hasAdjacentAlly) spreadScore++
        }

        return spreadScore
    }

    // ============ ULTIMATE TICTACTOE ============

    /**
     * Get best move for Ultimate TicTacToe - uses specialized AI
     */
    fun getBestUltimateMove(
        ultimateBoard: UltimateTicTacToeBoard,
        aiSymbol: Int,
        playableBoards: List<Pair<Int, Int>>
    ): AIUltimateMove {
        // Convert to format expected by UltimateTicTacToeAI
        val boards = Array(3) { row ->
            Array(3) { col ->
                TicTacToeBoard(
                    size = 3,
                    winCondition = 3,
                    cells = ultimateBoard.miniBoards[row][col].cells
                )
            }
        }

        val metaBoard = Array(3) { row ->
            IntArray(3) { col ->
                ultimateBoard.miniBoards[row][col].winner
            }
        }

        // Determine active board from playable boards
        val activeBoard = if (playableBoards.size == 1) {
            playableBoards[0]
        } else {
            null // Free choice
        }

        // Use specialized Ultimate AI
        val result = UltimateTicTacToeAI.getBestMove(
            boards = boards,
            metaBoard = metaBoard,
            activeBoard = activeBoard,
            aiSymbol = aiSymbol,
            maxDepth = MAX_DEPTH_ULTIMATE,
            timeLimitMs = 3000
        )

        return AIUltimateMove(
            boardRow = result.boardRow,
            boardCol = result.boardCol,
            cellRow = result.cellRow,
            cellCol = result.cellCol
        )
    }

    /**
     * Legacy Ultimate TicTacToe minimax (kept for compatibility)
     */
    @Deprecated("Use UltimateTicTacToeAI instead")
    fun getBestUltimateMoveLegacy(
        ultimateBoard: UltimateTicTacToeBoard,
        aiSymbol: Int,
        playableBoards: List<Pair<Int, Int>>
    ): AIUltimateMove {
        var bestScore = Int.MIN_VALUE
        var bestMove = AIUltimateMove(0, 0, 0, 0)

        for ((boardRow, boardCol) in playableBoards) {
            val miniBoard = ultimateBoard.miniBoards[boardRow][boardCol]
            if (miniBoard.isFinished()) continue

            for (cellRow in 0 until 3) {
                for (cellCol in 0 until 3) {
                    if (miniBoard.cells[cellRow][cellCol].isEmpty()) {
                        val newBoard = makeUltimateMove(ultimateBoard, boardRow, boardCol, cellRow, cellCol, aiSymbol)
                        val score = ultimateMinimax(
                            board = newBoard,
                            depth = MAX_DEPTH_ULTIMATE,
                            isMaximizing = false,
                            aiSymbol = aiSymbol,
                            alpha = Int.MIN_VALUE,
                            beta = Int.MAX_VALUE,
                            lastMoveCell = Pair(cellRow, cellCol)
                        )

                        if (score > bestScore) {
                            bestScore = score
                            bestMove = AIUltimateMove(boardRow, boardCol, cellRow, cellCol)
                        }
                    }
                }
            }
        }

        return bestMove
    }

    private fun ultimateMinimax(
        board: UltimateTicTacToeBoard,
        depth: Int,
        isMaximizing: Boolean,
        aiSymbol: Int,
        alpha: Int,
        beta: Int,
        lastMoveCell: Pair<Int, Int>
    ): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1

        val winner = checkUltimateWinner(board)
        if (winner == aiSymbol) return WIN_SCORE + depth
        if (winner == opponentSymbol) return -WIN_SCORE - depth
        if (isUltimateBoardFull(board) || depth == 0) {
            return evaluateUltimateBoard(board, aiSymbol)
        }

        var currentAlpha = alpha
        var currentBeta = beta
        val playableBoards = getPlayableBoards(board, lastMoveCell)

        if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            for ((boardRow, boardCol) in playableBoards) {
                val miniBoard = board.miniBoards[boardRow][boardCol]
                if (miniBoard.isFinished()) continue

                for (cellRow in 0 until 3) {
                    for (cellCol in 0 until 3) {
                        if (miniBoard.cells[cellRow][cellCol].isEmpty()) {
                            val newBoard = makeUltimateMove(board, boardRow, boardCol, cellRow, cellCol, aiSymbol)
                            val score = ultimateMinimax(
                                newBoard, depth - 1, false, aiSymbol,
                                currentAlpha, currentBeta, Pair(cellRow, cellCol)
                            )
                            maxScore = max(maxScore, score)
                            currentAlpha = max(currentAlpha, score)
                            if (currentBeta <= currentAlpha) return maxScore
                        }
                    }
                }
            }
            return maxScore
        } else {
            var minScore = Int.MAX_VALUE
            for ((boardRow, boardCol) in playableBoards) {
                val miniBoard = board.miniBoards[boardRow][boardCol]
                if (miniBoard.isFinished()) continue

                for (cellRow in 0 until 3) {
                    for (cellCol in 0 until 3) {
                        if (miniBoard.cells[cellRow][cellCol].isEmpty()) {
                            val newBoard = makeUltimateMove(board, boardRow, boardCol, cellRow, cellCol, opponentSymbol)
                            val score = ultimateMinimax(
                                newBoard, depth - 1, true, aiSymbol,
                                currentAlpha, currentBeta, Pair(cellRow, cellCol)
                            )
                            minScore = min(minScore, score)
                            currentBeta = min(currentBeta, score)
                            if (currentBeta <= currentAlpha) return minScore
                        }
                    }
                }
            }
            return minScore
        }
    }

    private fun evaluateUltimateBoard(board: UltimateTicTacToeBoard, aiSymbol: Int): Int {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        var score = 0

        val metaBoard = List(3) { row -> List(3) { col -> board.miniBoards[row][col].winner } }

        // Evaluate meta board lines
        val lines = listOf(
            listOf(metaBoard[0][0], metaBoard[0][1], metaBoard[0][2]),
            listOf(metaBoard[1][0], metaBoard[1][1], metaBoard[1][2]),
            listOf(metaBoard[2][0], metaBoard[2][1], metaBoard[2][2]),
            listOf(metaBoard[0][0], metaBoard[1][0], metaBoard[2][0]),
            listOf(metaBoard[0][1], metaBoard[1][1], metaBoard[2][1]),
            listOf(metaBoard[0][2], metaBoard[1][2], metaBoard[2][2]),
            listOf(metaBoard[0][0], metaBoard[1][1], metaBoard[2][2]),
            listOf(metaBoard[0][2], metaBoard[1][1], metaBoard[2][0])
        )

        for (line in lines) {
            val aiCount = line.count { it == aiSymbol }
            val opponentCount = line.count { it == opponentSymbol }
            val openCount = line.count { it == 0 }

            if (aiCount > 0 && opponentCount == 0) {
                score += when (aiCount) {
                    3 -> WIN_SCORE
                    2 -> if (openCount == 1) 50000 else 10000
                    else -> 1000
                }
            } else if (opponentCount > 0 && aiCount == 0) {
                score -= when (opponentCount) {
                    3 -> WIN_SCORE
                    2 -> if (openCount == 1) 50000 else 10000
                    else -> 1000
                }
            }
        }

        // Center control
        if (metaBoard[1][1] == aiSymbol) score += 20000
        else if (metaBoard[1][1] == opponentSymbol) score -= 20000

        // Corner control
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        for ((r, c) in corners) {
            if (metaBoard[r][c] == aiSymbol) score += 5000
            else if (metaBoard[r][c] == opponentSymbol) score -= 5000
        }

        return score
    }

    // ============ HELPER METHODS ============

    private fun makeMove(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): TicTacToeBoard {
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(value = symbol) else cell
            }
        }
        return board.copy(cells = newCells)
    }

    private fun makeUltimateMove(
        board: UltimateTicTacToeBoard,
        boardRow: Int,
        boardCol: Int,
        cellRow: Int,
        cellCol: Int,
        symbol: Int
    ): UltimateTicTacToeBoard {
        val newMiniBoards = board.miniBoards.mapIndexed { br, boardRowList ->
            boardRowList.mapIndexed { bc, miniBoard ->
                if (br == boardRow && bc == boardCol) {
                    val newCells = miniBoard.cells.mapIndexed { cr, cellRow_ ->
                        cellRow_.mapIndexed { cc, cell ->
                            if (cr == cellRow && cc == cellCol) cell.copy(value = symbol) else cell
                        }
                    }
                    val winner = checkMiniBoardWinner(newCells)
                    miniBoard.copy(cells = newCells, winner = winner)
                } else miniBoard
            }
        }
        return board.copy(miniBoards = newMiniBoards)
    }

    private fun detonateBomb(board: TicTacToeBoard, row: Int, col: Int, gameMode: TicTacToeGameMode): TicTacToeBoard {
        return if (gameMode == TicTacToeGameMode.L_BOMB) {
            TicTacToeLogic.detonateLBomb(board, row, col)
        } else {
            TicTacToeLogic.detonateStandardBomb(board, row, col)
        }
    }

    private fun checkMiniBoardWinner(cells: List<List<TicTacToeCell>>): Int {
        for (row in 0 until 3) {
            if (cells[row][0].value != 0 &&
                cells[row][0].value == cells[row][1].value &&
                cells[row][1].value == cells[row][2].value
            ) return cells[row][0].value
        }
        for (col in 0 until 3) {
            if (cells[0][col].value != 0 &&
                cells[0][col].value == cells[1][col].value &&
                cells[1][col].value == cells[2][col].value
            ) return cells[0][col].value
        }
        if (cells[0][0].value != 0 && cells[0][0].value == cells[1][1].value && cells[1][1].value == cells[2][2].value)
            return cells[0][0].value
        if (cells[0][2].value != 0 && cells[0][2].value == cells[1][1].value && cells[1][1].value == cells[2][0].value)
            return cells[0][2].value

        val isFull = cells.all { row -> row.all { it.value != 0 } }
        return if (isFull) 3 else 0
    }

    private fun checkUltimateWinner(board: UltimateTicTacToeBoard): Int {
        val metaBoard = List(3) { row -> List(3) { col -> board.miniBoards[row][col].winner } }

        for (row in 0 until 3) {
            if (metaBoard[row][0] != 0 && metaBoard[row][0] != 3 &&
                metaBoard[row][0] == metaBoard[row][1] && metaBoard[row][1] == metaBoard[row][2]
            ) return metaBoard[row][0]
        }
        for (col in 0 until 3) {
            if (metaBoard[0][col] != 0 && metaBoard[0][col] != 3 &&
                metaBoard[0][col] == metaBoard[1][col] && metaBoard[1][col] == metaBoard[2][col]
            ) return metaBoard[0][col]
        }
        if (metaBoard[0][0] != 0 && metaBoard[0][0] != 3 &&
            metaBoard[0][0] == metaBoard[1][1] && metaBoard[1][1] == metaBoard[2][2]
        ) return metaBoard[0][0]
        if (metaBoard[0][2] != 0 && metaBoard[0][2] != 3 &&
            metaBoard[0][2] == metaBoard[1][1] && metaBoard[1][1] == metaBoard[2][0]
        ) return metaBoard[0][2]

        return 0
    }

    private fun isUltimateBoardFull(board: UltimateTicTacToeBoard): Boolean {
        return board.miniBoards.all { row -> row.all { miniBoard -> miniBoard.isFinished() } }
    }

    private fun getPlayableBoards(board: UltimateTicTacToeBoard, lastMoveCell: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (targetRow, targetCol) = lastMoveCell
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

sealed class AIMove {
    data class PlaceSymbol(val row: Int, val col: Int) : AIMove()
    data class PlaceBomb(val row: Int, val col: Int) : AIMove()
}

data class AIUltimateMove(
    val boardRow: Int,
    val boardCol: Int,
    val cellRow: Int,
    val cellCol: Int
)
