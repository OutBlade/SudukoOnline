package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.MuhleBoard
import de.sudokuonline.app.data.model.MuhleStoneOwner

/**
 * Mühle (Nine Men's Morris) Endgame Engine
 *
 * Provides perfect play in endgame situations where:
 * - One or both players have 3-4 stones
 * - Flying phase is active (3 stones can move anywhere)
 * - Mill formations are critical
 *
 * Key endgame principles:
 * 1. With 3 stones: mobility is everything (can jump to any empty)
 * 2. Zwickmühle (double mill) is winning if achievable
 * 3. Opponent with 3 stones vs 4+ is usually losing
 * 4. Block opponent's flying options
 */
object MuhleEndgame {

    // Board adjacency map (position -> adjacent positions)
    private val adjacency = mapOf(
        0 to listOf(1, 9),
        1 to listOf(0, 2, 4),
        2 to listOf(1, 14),
        3 to listOf(4, 10),
        4 to listOf(1, 3, 5, 7),
        5 to listOf(4, 13),
        6 to listOf(7, 11),
        7 to listOf(4, 6, 8),
        8 to listOf(7, 12),
        9 to listOf(0, 10, 21),
        10 to listOf(3, 9, 11, 18),
        11 to listOf(6, 10, 15),
        12 to listOf(8, 13, 17),
        13 to listOf(5, 12, 14, 20),
        14 to listOf(2, 13, 23),
        15 to listOf(11, 16),
        16 to listOf(15, 17, 19),
        17 to listOf(12, 16),
        18 to listOf(10, 19),
        19 to listOf(16, 18, 20, 22),
        20 to listOf(13, 19),
        21 to listOf(9, 22),
        22 to listOf(19, 21, 23),
        23 to listOf(14, 22)
    )

    // All possible mills
    private val mills = listOf(
        // Outer square
        listOf(0, 1, 2),    // Top
        listOf(0, 9, 21),   // Left
        listOf(2, 14, 23),  // Right
        listOf(21, 22, 23), // Bottom
        // Middle square
        listOf(3, 4, 5),    // Top
        listOf(3, 10, 18),  // Left
        listOf(5, 13, 20),  // Right
        listOf(18, 19, 20), // Bottom
        // Inner square
        listOf(6, 7, 8),    // Top
        listOf(6, 11, 15),  // Left
        listOf(8, 12, 17),  // Right
        listOf(15, 16, 17), // Bottom
        // Cross connections
        listOf(1, 4, 7),    // Top vertical
        listOf(9, 10, 11),  // Left horizontal
        listOf(12, 13, 14), // Right horizontal
        listOf(16, 19, 22)  // Bottom vertical
    )

    // Endgame pattern cache
    private val endgameCache = HashMap<Long, EndgameResult>(10000)

    data class EndgameResult(
        val bestMove: MuhleEndgameMove?,
        val evaluation: Int,  // Positive = winning for player, negative = losing
        val movesToWin: Int   // Number of moves to forced win (0 if draw/loss)
    )

    sealed class MuhleEndgameMove {
        data class Move(val from: Int, val to: Int) : MuhleEndgameMove()
        data class Remove(val position: Int) : MuhleEndgameMove()
    }

    /**
     * Analyze endgame position and return best move
     */
    fun analyzeEndgame(
        board: MuhleBoard,
        playerNumber: Int,  // 1 or 2
        isFlying: Boolean   // Whether current player can fly (has 3 stones)
    ): EndgameResult {
        val hash = hashPosition(board, playerNumber)

        // Check cache
        endgameCache[hash]?.let { return it }

        val playerOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val oppOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_2.name else MuhleStoneOwner.PLAYER_1.name

        val myStones = countStones(board, playerOwner)
        val oppStones = countStones(board, oppOwner)

        // Terminal conditions
        if (myStones < 3) {
            return EndgameResult(null, -1000000, 0) // We lost
        }
        if (oppStones < 3) {
            return EndgameResult(null, 1000000, 0) // We won
        }

        // Check for no legal moves (losing)
        val legalMoves = generateMoves(board, playerNumber, isFlying)
        if (legalMoves.isEmpty()) {
            return EndgameResult(null, -1000000, 0)
        }

        // Endgame search with limited depth
        val result = endgameSearch(board, playerNumber, isFlying, 12, Int.MIN_VALUE, Int.MAX_VALUE)

        // Cache result
        endgameCache[hash] = result

        return result
    }

    /**
     * Deep endgame search with alpha-beta
     */
    private fun endgameSearch(
        board: MuhleBoard,
        playerNumber: Int,
        isFlying: Boolean,
        depth: Int,
        alpha: Int,
        beta: Int
    ): EndgameResult {
        val playerOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val oppOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_2.name else MuhleStoneOwner.PLAYER_1.name

        val myStones = countStones(board, playerOwner)
        val oppStones = countStones(board, oppOwner)

        // Terminal checks
        if (oppStones < 3) return EndgameResult(null, 1000000, 0)
        if (myStones < 3) return EndgameResult(null, -1000000, 0)

        val moves = generateMoves(board, playerNumber, isFlying)
        if (moves.isEmpty()) {
            return EndgameResult(null, -1000000, 0) // Can't move = lose
        }

        if (depth == 0) {
            return EndgameResult(moves.firstOrNull(), evaluateEndgame(board, playerNumber), 0)
        }

        var currentAlpha = alpha
        var bestMove: MuhleEndgameMove? = null
        var bestEval = Int.MIN_VALUE
        var bestMovesToWin = Int.MAX_VALUE

        // Order moves: mill-forming first, then Zwickmühle moves
        val orderedMoves = moves.sortedByDescending { move ->
            getMoveValue(board, move, playerOwner)
        }

        for (move in orderedMoves) {
            val (newBoard, formsMill) = applyMove(board, move, playerOwner)

            var eval: Int
            var movesToWin = 0

            if (formsMill) {
                // Must remove an opponent stone
                val removablePieces = getRemovablePieces(newBoard, oppOwner)
                if (removablePieces.isEmpty()) {
                    // No pieces to remove (shouldn't happen normally)
                    continue
                }

                var bestRemoveEval = Int.MIN_VALUE
                for (removePos in removablePieces) {
                    val boardAfterRemove = removePiece(newBoard, removePos)
                    val oppIsFlying = countStones(boardAfterRemove, oppOwner) == 3

                    val result = endgameSearch(
                        boardAfterRemove,
                        3 - playerNumber,
                        oppIsFlying,
                        depth - 1,
                        -beta,
                        -currentAlpha
                    )

                    val removeEval = -result.evaluation
                    if (removeEval > bestRemoveEval) {
                        bestRemoveEval = removeEval
                        movesToWin = result.movesToWin + 1
                    }
                }
                eval = bestRemoveEval
            } else {
                val oppIsFlying = countStones(newBoard, oppOwner) == 3
                val result = endgameSearch(
                    newBoard,
                    3 - playerNumber,
                    oppIsFlying,
                    depth - 1,
                    -beta,
                    -currentAlpha
                )
                eval = -result.evaluation
                movesToWin = result.movesToWin + 1
            }

            if (eval > bestEval || (eval == bestEval && movesToWin < bestMovesToWin)) {
                bestEval = eval
                bestMove = move
                bestMovesToWin = movesToWin
            }

            currentAlpha = maxOf(currentAlpha, eval)
            if (currentAlpha >= beta) break // Cutoff
        }

        return EndgameResult(bestMove, bestEval, bestMovesToWin)
    }

    /**
     * Evaluate endgame position heuristically
     */
    private fun evaluateEndgame(board: MuhleBoard, playerNumber: Int): Int {
        val playerOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val oppOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_2.name else MuhleStoneOwner.PLAYER_1.name

        var score = 0

        val myStones = countStones(board, playerOwner)
        val oppStones = countStones(board, oppOwner)

        // Stone count difference is critical
        score += (myStones - oppStones) * 10000

        // Mobility (number of legal moves)
        val myMobility = generateMoves(board, playerNumber, myStones == 3).size
        val oppMobility = generateMoves(board, 3 - playerNumber, oppStones == 3).size
        score += (myMobility - oppMobility) * 500

        // Potential mills (2 in a row with empty third)
        score += countPotentialMills(board, playerOwner) * 2000
        score -= countPotentialMills(board, oppOwner) * 2000

        // Closed mills
        score += countClosedMills(board, playerOwner) * 1000
        score -= countClosedMills(board, oppOwner) * 1000

        // Zwickmühle potential (two mills sharing a stone)
        score += detectZwickmuhle(board, playerOwner) * 5000
        score -= detectZwickmuhle(board, oppOwner) * 5000

        // Position value (crosspoints are best)
        score += evaluatePositions(board, playerOwner)
        score -= evaluatePositions(board, oppOwner)

        return score
    }

    /**
     * Generate all legal moves
     */
    private fun generateMoves(
        board: MuhleBoard,
        playerNumber: Int,
        isFlying: Boolean
    ): List<MuhleEndgameMove> {
        val playerOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val moves = mutableListOf<MuhleEndgameMove>()

        val myPositions = (0..23).filter { board.positions[it].owner == playerOwner }
        val emptyPositions = (0..23).filter { board.positions[it].owner == MuhleStoneOwner.EMPTY.name }

        for (from in myPositions) {
            if (isFlying) {
                // Can move to any empty position
                for (to in emptyPositions) {
                    moves.add(MuhleEndgameMove.Move(from, to))
                }
            } else {
                // Can only move to adjacent positions
                for (to in adjacency[from] ?: emptyList()) {
                    if (board.positions[to].owner == MuhleStoneOwner.EMPTY.name) {
                        moves.add(MuhleEndgameMove.Move(from, to))
                    }
                }
            }
        }

        return moves
    }

    /**
     * Get value of a move for ordering
     */
    private fun getMoveValue(board: MuhleBoard, move: MuhleEndgameMove, playerOwner: String): Int {
        if (move !is MuhleEndgameMove.Move) return 0

        var value = 0

        // Check if move forms a mill
        val testBoard = board.positions.toMutableList()
        testBoard[move.from] = testBoard[move.from].copy(owner = MuhleStoneOwner.EMPTY.name)
        testBoard[move.to] = testBoard[move.to].copy(owner = playerOwner)

        if (isInMill(testBoard.map { it.owner }, move.to, playerOwner)) {
            value += 10000
        }

        // Check if it's a Zwickmühle move (can create mill back and forth)
        val millsContainingTo = mills.filter { move.to in it && it.all { pos ->
            pos == move.to || testBoard[pos].owner == playerOwner
        }}
        val millsContainingFrom = mills.filter { move.from in it }

        if (millsContainingTo.isNotEmpty() && millsContainingFrom.isNotEmpty()) {
            // Potential back-and-forth mill
            value += 5000
        }

        // Crosspoint positions are valuable
        val crosspoints = listOf(4, 10, 13, 19)
        if (move.to in crosspoints) value += 1000

        return value
    }

    /**
     * Apply a move and return new board + whether it forms a mill
     */
    private fun applyMove(
        board: MuhleBoard,
        move: MuhleEndgameMove,
        playerOwner: String
    ): Pair<MuhleBoard, Boolean> {
        if (move !is MuhleEndgameMove.Move) return Pair(board, false)

        val newPositions = board.positions.mapIndexed { index, pos ->
            when (index) {
                move.from -> pos.copy(owner = MuhleStoneOwner.EMPTY.name)
                move.to -> pos.copy(owner = playerOwner)
                else -> pos
            }
        }

        val newBoard = board.copy(positions = newPositions)
        val formsMill = isInMill(newPositions.map { it.owner }, move.to, playerOwner)

        return Pair(newBoard, formsMill)
    }

    /**
     * Check if position is part of a mill
     */
    private fun isInMill(owners: List<String>, position: Int, playerOwner: String): Boolean {
        return mills.any { mill ->
            position in mill && mill.all { owners[it] == playerOwner }
        }
    }

    /**
     * Get removable pieces (not in mill, or all in mill)
     */
    private fun getRemovablePieces(board: MuhleBoard, oppOwner: String): List<Int> {
        val oppPositions = (0..23).filter { board.positions[it].owner == oppOwner }
        val owners = board.positions.map { it.owner }

        // First, try pieces not in mills
        val notInMill = oppPositions.filter { pos ->
            !mills.any { mill -> pos in mill && mill.all { owners[it] == oppOwner } }
        }

        return if (notInMill.isNotEmpty()) notInMill else oppPositions
    }

    /**
     * Remove a piece from the board
     */
    private fun removePiece(board: MuhleBoard, position: Int): MuhleBoard {
        val newPositions = board.positions.mapIndexed { index, pos ->
            if (index == position) pos.copy(owner = MuhleStoneOwner.EMPTY.name) else pos
        }
        return board.copy(positions = newPositions)
    }

    /**
     * Count stones for a player
     */
    private fun countStones(board: MuhleBoard, owner: String): Int {
        return board.positions.count { it.owner == owner }
    }

    /**
     * Count potential mills (2 in a row with empty third)
     */
    private fun countPotentialMills(board: MuhleBoard, owner: String): Int {
        val owners = board.positions.map { it.owner }
        return mills.count { mill ->
            mill.count { owners[it] == owner } == 2 &&
            mill.count { owners[it] == MuhleStoneOwner.EMPTY.name } == 1
        }
    }

    /**
     * Count closed mills
     */
    private fun countClosedMills(board: MuhleBoard, owner: String): Int {
        val owners = board.positions.map { it.owner }
        return mills.count { mill -> mill.all { owners[it] == owner } }
    }

    /**
     * Detect Zwickmühle (double mill) positions
     */
    private fun detectZwickmuhle(board: MuhleBoard, owner: String): Int {
        val owners = board.positions.map { it.owner }
        var count = 0

        // For each position, check if it's part of multiple potential mills
        for (pos in 0..23) {
            if (owners[pos] == owner) {
                val millsContaining = mills.filter { pos in it }
                val potentialMills = millsContaining.count { mill ->
                    mill.count { owners[it] == owner } >= 2 &&
                    mill.none { owners[it] != owner && owners[it] != MuhleStoneOwner.EMPTY.name }
                }
                if (potentialMills >= 2) count++
            }
        }

        return count
    }

    /**
     * Evaluate positions based on strategic value
     */
    private fun evaluatePositions(board: MuhleBoard, owner: String): Int {
        var score = 0
        val owners = board.positions.map { it.owner }

        // Crosspoints (4 connections) - most valuable
        val crosspoints = listOf(4, 10, 13, 19)
        score += crosspoints.count { owners[it] == owner } * 150

        // T-points (3 connections)
        val tpoints = listOf(1, 7, 9, 11, 12, 14, 16, 22)
        score += tpoints.count { owners[it] == owner } * 100

        // Corner points (2 connections) - least valuable in endgame
        // (remaining positions)

        return score
    }

    /**
     * Hash position for caching
     */
    private fun hashPosition(board: MuhleBoard, playerNumber: Int): Long {
        var hash = playerNumber.toLong()
        for (i in 0..23) {
            val value = when (board.positions[i].owner) {
                MuhleStoneOwner.PLAYER_1.name -> 1L
                MuhleStoneOwner.PLAYER_2.name -> 2L
                else -> 0L
            }
            hash = hash * 3 + value
        }
        return hash
    }

    /**
     * Get strategic advice for the player
     */
    fun getEndgameAdvice(board: MuhleBoard, playerNumber: Int): String {
        val playerOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val oppOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_2.name else MuhleStoneOwner.PLAYER_1.name

        val myStones = countStones(board, playerOwner)
        val oppStones = countStones(board, oppOwner)

        return when {
            myStones == 3 && oppStones > 3 -> "Nutze die Sprungfähigkeit! Du kannst überall hin ziehen."
            oppStones == 3 && myStones > 3 -> "Blockiere den Gegner! Er kann überall hin springen."
            myStones == 3 && oppStones == 3 -> "Beide können springen - versuche eine Zwickmühle aufzubauen!"
            countPotentialMills(board, playerOwner) > countPotentialMills(board, oppOwner) ->
                "Du hast mehr potentielle Mühlen - halte den Druck aufrecht!"
            detectZwickmuhle(board, playerOwner) > 0 -> "Du hast eine Zwickmühle - nutze sie aus!"
            detectZwickmuhle(board, oppOwner) > 0 -> "Vorsicht! Gegner hat eine Zwickmühle!"
            else -> "Versuche deine Steine zu den Kreuzpunkten zu bewegen."
        }
    }
}
