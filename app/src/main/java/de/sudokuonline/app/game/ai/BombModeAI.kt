package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard
import de.sudokuonline.app.data.model.TicTacToeCell
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Specialized AI for Bomb Mode TicTacToe v2.0
 *
 * KEY INSIGHT: In bomb mode, the player who starts with perfect play can always win
 * on 3x3 because bombs allow breaking any defensive line. The AI must understand:
 *
 * 1. BOMB RESOURCE MANAGEMENT is critical - bombs are the most valuable resource
 * 2. BOMB PARITY determines advantage - more bombs = control of the game
 * 3. When opponent has bombs, SPREAD pieces to minimize bomb damage
 * 4. When you have bomb advantage, FORCE positions that require opponent to use bombs
 * 5. EARLY BOMBS can be better than late bombs in some situations
 *
 * Strategy adjustments:
 * - If we have more bombs: aggressive play, create double threats
 * - If equal bombs: focus on positional advantage
 * - If fewer bombs: defensive play, avoid clusters, force trades
 *
 * The AI uses minimax with bomb states included in evaluation.
 */
object BombModeAI {

    data class BombMove(
        val row: Int,
        val col: Int,
        val isBomb: Boolean,
        val evaluation: Int,
        val reason: String
    )

    // Evaluation weights - tuned for bomb mode dynamics
    private const val WIN_SCORE = 1000000
    private const val LOSS_SCORE = -1000000
    
    // Bomb-specific values
    private const val BOMB_ADVANTAGE_MULTIPLIER = 8000  // Value per bomb advantage
    private const val BOMB_LAST_ONE_VALUE = 15000       // Having the last bomb is huge
    private const val BOMB_DESTROY_THREAT = 25000       // Using bomb to destroy winning threat
    private const val BOMB_WASTE_PENALTY = -12000       // Penalty for wasting a bomb
    
    // Positional values
    private const val CENTER_VALUE = 800
    private const val CORNER_VALUE = 400
    private const val THREAT_VALUE = 3000               // Creating a winning threat
    private const val DOUBLE_THREAT_VALUE = 50000       // Fork - usually wins
    private const val BLOCK_THREAT_VALUE = 2500
    
    // Clustering (important for bomb defense)
    private const val CLUSTER_PENALTY = 600             // Adjacent pieces are bomb targets
    private const val ISOLATION_BONUS = 400             // Isolated pieces are safer
    
    // Search depth - deeper for bomb mode due to complexity
    private const val MAX_DEPTH_3X3 = 12
    private const val MAX_DEPTH_5X5 = 8

    /**
     * Main entry point - get best move considering bomb mechanics
     * 
     * The key innovation: we search the full game tree including bomb usage
     * and evaluate positions based on bomb resource advantage
     */
    fun getBestMove(
        board: TicTacToeBoard,
        aiSymbol: Int,
        aiBombs: Int,
        opponentBombs: Int,
        winLength: Int = if (board.size <= 4) board.size else 4
    ): BombMove {
        val size = board.size
        val oppSymbol = 3 - aiSymbol
        
        // Phase 1: Check for immediate win (symbol placement)
        val winMove = findWinningMove(board, aiSymbol, winLength)
        if (winMove != null) {
            return BombMove(winMove.first, winMove.second, false, WIN_SCORE, "Gewinne!")
        }

        // Phase 2: Check if opponent has winning threat
        val oppWinMove = findWinningMove(board, oppSymbol, winLength)
        if (oppWinMove != null) {
            // Option A: Block with symbol
            // Option B: If we have bombs and blocking creates worse position, consider bomb
            
            // For now, prefer blocking with symbol to save bombs
            return BombMove(oppWinMove.first, oppWinMove.second, false, WIN_SCORE - 1000, "Blockiere Sieg!")
        }

        // Phase 3: Calculate bomb advantage and adjust strategy
        val bombAdvantage = aiBombs - opponentBombs
        
        // Phase 4: Full minimax search with bomb states
        val maxDepth = if (size <= 3) MAX_DEPTH_3X3 else MAX_DEPTH_5X5
        
        val result = minimaxRoot(
            board = board,
            aiSymbol = aiSymbol,
            aiBombs = aiBombs,
            opponentBombs = opponentBombs,
            winLength = winLength,
            maxDepth = maxDepth,
            bombAdvantage = bombAdvantage
        )
        
        return result
    }

    /**
     * Root minimax - evaluates all possible moves including bombs
     */
    private fun minimaxRoot(
        board: TicTacToeBoard,
        aiSymbol: Int,
        aiBombs: Int,
        opponentBombs: Int,
        winLength: Int,
        maxDepth: Int,
        bombAdvantage: Int
    ): BombMove {
        val size = board.size
        val oppSymbol = 3 - aiSymbol
        var bestMove: BombMove? = null
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE
        
        // Generate all possible moves (symbol placements + bomb placements)
        val moves = generateAllMoves(board, aiSymbol, aiBombs, opponentBombs, winLength, bombAdvantage)
        
        // Sort moves by estimated value for better alpha-beta pruning
        val sortedMoves = moves.sortedByDescending { it.evaluation }
        
        for (move in sortedMoves) {
            val newBoard: TicTacToeBoard
            val newAiBombs: Int
            
            if (move.isBomb) {
                // Apply bomb
                newBoard = applyBomb(board, move.row, move.col, size)
                newAiBombs = aiBombs - 1
            } else {
                // Place symbol
                newBoard = placeSymbol(board, move.row, move.col, aiSymbol)
                newAiBombs = aiBombs
            }
            
            // Check if this move wins
            if (!move.isBomb && wouldWin(board, move.row, move.col, aiSymbol, winLength)) {
                return move.copy(evaluation = WIN_SCORE)
            }
            
            // Minimax search
            val score = -minimax(
                board = newBoard,
                currentSymbol = oppSymbol,
                aiSymbol = aiSymbol,
                aiBombs = newAiBombs,
                opponentBombs = opponentBombs,
                winLength = winLength,
                depth = maxDepth - 1,
                alpha = -beta,
                beta = -alpha,
                isMaximizing = false
            )
            
            if (score > bestScore) {
                bestScore = score
                bestMove = move.copy(evaluation = score)
            }
            
            alpha = max(alpha, score)
        }
        
        return bestMove ?: BombMove(size / 2, size / 2, false, 0, "Fallback: Zentrum")
    }

    /**
     * Minimax with alpha-beta pruning including bomb states
     */
    private fun minimax(
        board: TicTacToeBoard,
        currentSymbol: Int,
        aiSymbol: Int,
        aiBombs: Int,
        opponentBombs: Int,
        winLength: Int,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean
    ): Int {
        val size = board.size
        val oppSymbol = 3 - currentSymbol
        
        // Terminal conditions
        val winner = checkWinner(board, winLength)
        if (winner == aiSymbol) return WIN_SCORE + depth
        if (winner == oppSymbol) return LOSS_SCORE - depth
        if (isBoardFull(board)) return 0
        if (depth == 0) {
            return evaluate(board, aiSymbol, aiBombs, opponentBombs, winLength)
        }
        
        val currentBombs = if (currentSymbol == aiSymbol) aiBombs else opponentBombs
        val otherBombs = if (currentSymbol == aiSymbol) opponentBombs else aiBombs
        val bombAdvantage = if (currentSymbol == aiSymbol) aiBombs - opponentBombs else opponentBombs - aiBombs
        
        var currentAlpha = alpha
        var currentBeta = beta
        
        if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            
            // Try symbol placements first (usually better to not waste bombs)
            for (row in 0 until size) {
                for (col in 0 until size) {
                    if (board.cells[row][col].isEmpty()) {
                        val newBoard = placeSymbol(board, row, col, currentSymbol)
                        
                        val newAiBombs = if (currentSymbol == aiSymbol) aiBombs else aiBombs
                        val newOppBombs = if (currentSymbol == aiSymbol) opponentBombs else opponentBombs
                        
                        val score = -minimax(
                            newBoard, oppSymbol, aiSymbol, newAiBombs, newOppBombs,
                            winLength, depth - 1, -currentBeta, -currentAlpha, false
                        )
                        
                        maxScore = max(maxScore, score)
                        currentAlpha = max(currentAlpha, score)
                        if (currentBeta <= currentAlpha) break
                    }
                }
                if (currentBeta <= currentAlpha) break
            }
            
            // Try bomb placements if beneficial
            if (currentBombs > 0 && shouldConsiderBomb(board, currentSymbol, oppSymbol, currentBombs, otherBombs, winLength)) {
                for (row in 0 until size) {
                    for (col in 0 until size) {
                        if (board.cells[row][col].isEmpty()) {
                            val bombValue = evaluateBombPlacement(board, row, col, currentSymbol, oppSymbol, winLength)
                            
                            // Only consider bomb if it's actually useful
                            if (bombValue > BOMB_WASTE_PENALTY) {
                                val newBoard = applyBomb(board, row, col, size)
                                val newAiBombs = if (currentSymbol == aiSymbol) aiBombs - 1 else aiBombs
                                val newOppBombs = if (currentSymbol == aiSymbol) opponentBombs else opponentBombs - 1
                                
                                val score = -minimax(
                                    newBoard, oppSymbol, aiSymbol, newAiBombs, newOppBombs,
                                    winLength, depth - 1, -currentBeta, -currentAlpha, false
                                )
                                
                                maxScore = max(maxScore, score)
                                currentAlpha = max(currentAlpha, score)
                                if (currentBeta <= currentAlpha) break
                            }
                        }
                    }
                    if (currentBeta <= currentAlpha) break
                }
            }
            
            return maxScore
        } else {
            var minScore = Int.MAX_VALUE
            
            for (row in 0 until size) {
                for (col in 0 until size) {
                    if (board.cells[row][col].isEmpty()) {
                        val newBoard = placeSymbol(board, row, col, currentSymbol)
                        val newAiBombs = if (currentSymbol == aiSymbol) aiBombs else aiBombs
                        val newOppBombs = if (currentSymbol == aiSymbol) opponentBombs else opponentBombs
                        
                        val score = -minimax(
                            newBoard, oppSymbol, aiSymbol, newAiBombs, newOppBombs,
                            winLength, depth - 1, -currentBeta, -currentAlpha, true
                        )
                        
                        minScore = min(minScore, score)
                        currentBeta = min(currentBeta, score)
                        if (currentBeta <= currentAlpha) break
                    }
                }
                if (currentBeta <= currentAlpha) break
            }
            
            // Consider opponent bombs
            if (currentBombs > 0) {
                for (row in 0 until size) {
                    for (col in 0 until size) {
                        if (board.cells[row][col].isEmpty()) {
                            val bombValue = evaluateBombPlacement(board, row, col, currentSymbol, oppSymbol, winLength)
                            if (bombValue > BOMB_WASTE_PENALTY) {
                                val newBoard = applyBomb(board, row, col, size)
                                val newAiBombs = if (currentSymbol == aiSymbol) aiBombs - 1 else aiBombs
                                val newOppBombs = if (currentSymbol == aiSymbol) opponentBombs else opponentBombs - 1
                                
                                val score = -minimax(
                                    newBoard, oppSymbol, aiSymbol, newAiBombs, newOppBombs,
                                    winLength, depth - 1, -currentBeta, -currentAlpha, true
                                )
                                
                                minScore = min(minScore, score)
                                currentBeta = min(currentBeta, score)
                                if (currentBeta <= currentAlpha) break
                            }
                        }
                    }
                    if (currentBeta <= currentAlpha) break
                }
            }
            
            return minScore
        }
    }

    /**
     * Generate all possible moves with initial evaluation for move ordering
     */
    private fun generateAllMoves(
        board: TicTacToeBoard,
        aiSymbol: Int,
        aiBombs: Int,
        opponentBombs: Int,
        winLength: Int,
        bombAdvantage: Int
    ): List<BombMove> {
        val size = board.size
        val oppSymbol = 3 - aiSymbol
        val moves = mutableListOf<BombMove>()
        
        // 1. Symbol placements
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board.cells[row][col].isEmpty()) {
                    val eval = evaluateSymbolPlacement(board, row, col, aiSymbol, oppSymbol, winLength, opponentBombs)
                    val reason = getMoveReason(board, row, col, aiSymbol, winLength)
                    moves.add(BombMove(row, col, false, eval, reason))
                }
            }
        }
        
        // 2. Bomb placements (only if we have bombs and it makes sense)
        if (aiBombs > 0) {
            for (row in 0 until size) {
                for (col in 0 until size) {
                    if (board.cells[row][col].isEmpty()) {
                        val bombEval = evaluateBombPlacement(board, row, col, aiSymbol, oppSymbol, winLength)
                        
                        // Only include bomb moves that are potentially good
                        if (bombEval > 0) {
                            val reason = when {
                                bombEval > 20000 -> "Zerstoere Gewinndrohung!"
                                bombEval > 10000 -> "Strategische Bombe"
                                else -> "Taktische Bombe"
                            }
                            moves.add(BombMove(row, col, true, bombEval, reason))
                        }
                    }
                }
            }
        }
        
        return moves
    }

    /**
     * Evaluate a symbol placement considering bomb dynamics
     */
    private fun evaluateSymbolPlacement(
        board: TicTacToeBoard,
        row: Int,
        col: Int,
        aiSymbol: Int,
        oppSymbol: Int,
        winLength: Int,
        opponentBombs: Int
    ): Int {
        val size = board.size
        var score = 0
        
        // Immediate win check
        if (wouldWin(board, row, col, aiSymbol, winLength)) {
            return WIN_SCORE
        }
        
        // Block opponent win
        if (wouldWin(board, row, col, oppSymbol, winLength)) {
            score += 100000
        }
        
        // Check for creating double threat (fork)
        val newBoard = placeSymbol(board, row, col, aiSymbol)
        val threats = countWinningThreats(newBoard, aiSymbol, winLength)
        if (threats >= 2) {
            score += DOUBLE_THREAT_VALUE
        } else if (threats == 1) {
            score += THREAT_VALUE
        }
        
        // Block opponent's double threat opportunity
        val oppNewBoard = placeSymbol(board, row, col, oppSymbol)
        val oppThreats = countWinningThreats(oppNewBoard, oppSymbol, winLength)
        if (oppThreats >= 2) {
            score += DOUBLE_THREAT_VALUE / 2
        }
        
        // Positional value
        val center = size / 2
        if (row == center && col == center) {
            score += CENTER_VALUE
        } else if ((row == 0 || row == size - 1) && (col == 0 || col == size - 1)) {
            score += CORNER_VALUE
        }
        
        // BOMB DEFENSE: If opponent has bombs, avoid clustering
        if (opponentBombs > 0) {
            val adjacentOwn = countAdjacentPieces(board, row, col, aiSymbol)
            score -= adjacentOwn * CLUSTER_PENALTY
            
            if (adjacentOwn == 0) {
                score += ISOLATION_BONUS
            }
        }
        
        // Line contribution
        score += evaluateLineContribution(board, row, col, aiSymbol, winLength) * 100
        
        return score
    }

    /**
     * Evaluate a bomb placement - ONLY positive if it destroys valuable opponent pieces
     */
    private fun evaluateBombPlacement(
        board: TicTacToeBoard,
        row: Int,
        col: Int,
        aiSymbol: Int,
        oppSymbol: Int,
        winLength: Int
    ): Int {
        val size = board.size
        var value = 0
        
        // Get affected positions (standard 3x3 bomb pattern)
        val affected = getAffectedPositions(row, col, size)
        
        var opponentHits = 0
        var ownHits = 0
        var destroysOpponentThreat = false
        var destroysOwnThreat = false
        
        for ((r, c) in affected) {
            if (r < 0 || r >= size || c < 0 || c >= size) continue
            
            when (board.cells[r][c].value) {
                oppSymbol -> {
                    opponentHits++
                    // Check if this piece is part of a threat
                    if (isPartOfThreat(board, r, c, oppSymbol, winLength)) {
                        destroysOpponentThreat = true
                    }
                }
                aiSymbol -> {
                    ownHits++
                    if (isPartOfThreat(board, r, c, aiSymbol, winLength)) {
                        destroysOwnThreat = true
                    }
                }
            }
        }
        
        // Calculate value
        // Positive only if we hit more opponent pieces than own
        value = (opponentHits - ownHits) * 5000
        
        // Big bonus for destroying opponent threats
        if (destroysOpponentThreat) {
            value += BOMB_DESTROY_THREAT
        }
        
        // Big penalty for destroying own threats
        if (destroysOwnThreat) {
            value -= BOMB_DESTROY_THREAT
        }
        
        // Penalty for using bomb early (save for critical moments)
        val filledCells = board.cells.flatten().count { it.value != 0 }
        val totalCells = size * size
        if (filledCells < totalCells / 3) {
            value -= 5000  // Early game bomb penalty
        }
        
        // Must hit at least 2 opponent pieces to be worthwhile (unless destroying a threat)
        if (opponentHits < 2 && !destroysOpponentThreat) {
            value = BOMB_WASTE_PENALTY
        }
        
        return value
    }

    /**
     * Comprehensive position evaluation for leaf nodes
     */
    private fun evaluate(
        board: TicTacToeBoard,
        aiSymbol: Int,
        aiBombs: Int,
        opponentBombs: Int,
        winLength: Int
    ): Int {
        val oppSymbol = 3 - aiSymbol
        val size = board.size
        var score = 0
        
        // 1. BOMB ADVANTAGE - The most important factor in bomb mode
        val bombAdvantage = aiBombs - opponentBombs
        score += bombAdvantage * BOMB_ADVANTAGE_MULTIPLIER
        
        // Having the last bomb is extremely valuable
        if (aiBombs > 0 && opponentBombs == 0) {
            score += BOMB_LAST_ONE_VALUE
        } else if (aiBombs == 0 && opponentBombs > 0) {
            score -= BOMB_LAST_ONE_VALUE
        }
        
        // 2. Winning threats
        val aiThreats = countWinningThreats(board, aiSymbol, winLength)
        val oppThreats = countWinningThreats(board, oppSymbol, winLength)
        
        // Double threat usually wins (unless opponent has bomb)
        if (aiThreats >= 2) {
            if (opponentBombs == 0) {
                score += DOUBLE_THREAT_VALUE * 2  // Guaranteed win
            } else {
                score += DOUBLE_THREAT_VALUE  // Still very good
            }
        } else {
            score += aiThreats * THREAT_VALUE
        }
        
        if (oppThreats >= 2) {
            if (aiBombs == 0) {
                score -= DOUBLE_THREAT_VALUE * 2
            } else {
                score -= DOUBLE_THREAT_VALUE
            }
        } else {
            score -= oppThreats * THREAT_VALUE
        }
        
        // 3. Piece count and positioning
        var aiPieces = 0
        var oppPieces = 0
        var aiCluster = 0
        var oppCluster = 0
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                when (board.cells[row][col].value) {
                    aiSymbol -> {
                        aiPieces++
                        if (opponentBombs > 0) {
                            aiCluster += countAdjacentPieces(board, row, col, aiSymbol)
                        }
                        // Positional value
                        val center = size / 2
                        if (row == center && col == center) score += CENTER_VALUE
                    }
                    oppSymbol -> {
                        oppPieces++
                        if (aiBombs > 0) {
                            oppCluster += countAdjacentPieces(board, row, col, oppSymbol)
                        }
                        val center = size / 2
                        if (row == center && col == center) score -= CENTER_VALUE
                    }
                }
            }
        }
        
        // Clustering penalty (if opponent has bombs, spread is safer)
        if (opponentBombs > 0) {
            score -= aiCluster * CLUSTER_PENALTY
        }
        if (aiBombs > 0) {
            score += oppCluster * CLUSTER_PENALTY / 2  // Opponent clusters are targets
        }
        
        // 4. Line potential
        val lines = getAllLines(board, winLength)
        for (line in lines) {
            score += evaluateLine(line, aiSymbol, oppSymbol, winLength)
        }
        
        return score
    }

    /**
     * Determine if we should even consider using a bomb in this position
     */
    private fun shouldConsiderBomb(
        board: TicTacToeBoard,
        currentSymbol: Int,
        oppSymbol: Int,
        currentBombs: Int,
        opponentBombs: Int,
        winLength: Int
    ): Boolean {
        // Always consider if opponent has a winning threat we need to destroy
        val oppWinMove = findWinningMove(board, oppSymbol, winLength)
        if (oppWinMove != null) return true
        
        // Consider if we have bomb advantage and opponent is clustering
        if (currentBombs > opponentBombs) {
            val oppCluster = board.cells.flatten()
                .filter { it.value == oppSymbol }
                .count()
            if (oppCluster >= 3) return true
        }
        
        // Don't waste bombs early if equal or behind
        val filledCells = board.cells.flatten().count { it.value != 0 }
        if (filledCells < 4) return false
        
        return currentBombs >= 2  // Only consider bombs if we have spare
    }

    // ============ HELPER METHODS ============

    private fun findWinningMove(board: TicTacToeBoard, symbol: Int, winLength: Int): Pair<Int, Int>? {
        val size = board.size
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board.cells[row][col].isEmpty() && wouldWin(board, row, col, symbol, winLength)) {
                    return Pair(row, col)
                }
            }
        }
        return null
    }

    private fun wouldWin(board: TicTacToeBoard, row: Int, col: Int, symbol: Int, winLength: Int): Boolean {
        val size = board.size
        val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))
        
        for ((dr, dc) in directions) {
            var count = 1
            
            var r = row + dr
            var c = col + dc
            while (r in 0 until size && c in 0 until size && board.cells[r][c].value == symbol) {
                count++
                r += dr
                c += dc
            }
            
            r = row - dr
            c = col - dc
            while (r in 0 until size && c in 0 until size && board.cells[r][c].value == symbol) {
                count++
                r -= dr
                c -= dc
            }
            
            if (count >= winLength) return true
        }
        return false
    }

    private fun countWinningThreats(board: TicTacToeBoard, symbol: Int, winLength: Int): Int {
        val size = board.size
        var threats = 0
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board.cells[row][col].isEmpty() && wouldWin(board, row, col, symbol, winLength)) {
                    threats++
                }
            }
        }
        return threats
    }

    private fun isPartOfThreat(board: TicTacToeBoard, row: Int, col: Int, symbol: Int, winLength: Int): Boolean {
        val size = board.size
        val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))
        
        for ((dr, dc) in directions) {
            var count = 1
            var emptyCount = 0
            
            var r = row + dr
            var c = col + dc
            while (r in 0 until size && c in 0 until size) {
                when (board.cells[r][c].value) {
                    symbol -> count++
                    0 -> emptyCount++
                    else -> break
                }
                r += dr
                c += dc
            }
            
            r = row - dr
            c = col - dc
            while (r in 0 until size && c in 0 until size) {
                when (board.cells[r][c].value) {
                    symbol -> count++
                    0 -> emptyCount++
                    else -> break
                }
                r -= dr
                c -= dc
            }
            
            // Part of a threat if one more piece would win
            if (count == winLength - 1 && emptyCount >= 1) {
                return true
            }
        }
        return false
    }

    private fun countAdjacentPieces(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Int {
        val size = board.size
        var count = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = row + dr
                val c = col + dc
                if (r in 0 until size && c in 0 until size && board.cells[r][c].value == symbol) {
                    count++
                }
            }
        }
        return count
    }

    private fun evaluateLineContribution(board: TicTacToeBoard, row: Int, col: Int, symbol: Int, winLength: Int): Int {
        val size = board.size
        var contribution = 0
        val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))
        
        for ((dr, dc) in directions) {
            var lineCount = 1
            var emptyCount = 0
            var blocked = 0
            
            for (sign in listOf(1, -1)) {
                var r = row + dr * sign
                var c = col + dc * sign
                while (r in 0 until size && c in 0 until size) {
                    when (board.cells[r][c].value) {
                        symbol -> lineCount++
                        0 -> emptyCount++
                        else -> { blocked++; break }
                    }
                    if (lineCount + emptyCount >= winLength) break
                    r += dr * sign
                    c += dc * sign
                }
            }
            
            if (lineCount + emptyCount >= winLength && blocked < 2) {
                contribution += when (lineCount) {
                    winLength - 1 -> 50
                    else -> lineCount * 5
                }
            }
        }
        return contribution
    }

    private fun getAffectedPositions(row: Int, col: Int, size: Int): List<Pair<Int, Int>> {
        return listOf(
            Pair(row - 1, col - 1), Pair(row - 1, col), Pair(row - 1, col + 1),
            Pair(row, col - 1), Pair(row, col), Pair(row, col + 1),
            Pair(row + 1, col - 1), Pair(row + 1, col), Pair(row + 1, col + 1)
        )
    }

    private fun placeSymbol(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): TicTacToeBoard {
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(value = symbol) else cell
            }
        }
        return board.copy(cells = newCells)
    }

    private fun applyBomb(board: TicTacToeBoard, row: Int, col: Int, size: Int): TicTacToeBoard {
        val affected = getAffectedPositions(row, col, size)
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (affected.any { it.first == r && it.second == c } && 
                    r in 0 until size && c in 0 until size) {
                    TicTacToeCell(value = 0, playerId = null, isBomb = false, bombOwnerId = null)
                } else cell
            }
        }
        return board.copy(cells = newCells)
    }

    private fun checkWinner(board: TicTacToeBoard, winLength: Int): Int {
        val size = board.size
        for (symbol in listOf(1, 2)) {
            for (row in 0 until size) {
                for (col in 0 until size) {
                    if (board.cells[row][col].value == symbol && 
                        wouldWinExisting(board, row, col, symbol, winLength)) {
                        return symbol
                    }
                }
            }
        }
        return 0
    }

    private fun wouldWinExisting(board: TicTacToeBoard, row: Int, col: Int, symbol: Int, winLength: Int): Boolean {
        if (board.cells[row][col].value != symbol) return false
        return wouldWin(board.copy(
            cells = board.cells.mapIndexed { r, rowCells ->
                rowCells.mapIndexed { c, cell ->
                    if (r == row && c == col) cell.copy(value = 0) else cell
                }
            }
        ), row, col, symbol, winLength)
    }

    private fun isBoardFull(board: TicTacToeBoard): Boolean {
        return board.cells.all { row -> row.all { it.value != 0 } }
    }

    private fun getAllLines(board: TicTacToeBoard, winLength: Int): List<List<Int>> {
        val lines = mutableListOf<List<Int>>()
        val size = board.size
        
        // Rows
        for (row in 0 until size) {
            for (startCol in 0..size - winLength) {
                lines.add((startCol until startCol + winLength).map { board.cells[row][it].value })
            }
        }
        
        // Columns
        for (col in 0 until size) {
            for (startRow in 0..size - winLength) {
                lines.add((startRow until startRow + winLength).map { board.cells[it][col].value })
            }
        }
        
        // Diagonals
        for (startRow in 0..size - winLength) {
            for (startCol in 0..size - winLength) {
                lines.add((0 until winLength).map { board.cells[startRow + it][startCol + it].value })
                lines.add((0 until winLength).map { board.cells[startRow + it][startCol + winLength - 1 - it].value })
            }
        }
        
        return lines
    }

    private fun evaluateLine(line: List<Int>, aiSymbol: Int, oppSymbol: Int, winLength: Int): Int {
        val aiCount = line.count { it == aiSymbol }
        val oppCount = line.count { it == oppSymbol }
        
        if (aiCount > 0 && oppCount > 0) return 0  // Blocked line
        
        return when {
            aiCount == winLength -> WIN_SCORE / 10
            oppCount == winLength -> -WIN_SCORE / 10
            aiCount == winLength - 1 -> 500
            oppCount == winLength - 1 -> -500
            aiCount > 0 -> aiCount * 50
            oppCount > 0 -> -oppCount * 50
            else -> 0
        }
    }

    private fun getMoveReason(board: TicTacToeBoard, row: Int, col: Int, aiSymbol: Int, winLength: Int): String {
        val size = board.size
        val center = size / 2
        
        if (countWinningThreats(placeSymbol(board, row, col, aiSymbol), aiSymbol, winLength) >= 2) {
            return "Doppeldrohung (Gabel)!"
        }
        
        return when {
            row == center && col == center -> "Kontrolliere Zentrum"
            (row == 0 || row == size - 1) && (col == 0 || col == size - 1) -> "Sichere Ecke"
            else -> "Strategische Position"
        }
    }
}
