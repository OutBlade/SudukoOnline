package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.MuhleAI.MuhleAIMove.*
import de.sudokuonline.app.game.ai.MuhleEndgame
import de.sudokuonline.app.game.ai.OpeningBook
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import java.util.concurrent.ConcurrentHashMap

/**
 * Mühle AI v4.0 - Professional-grade with specialized endgame and opening support
 *
 * Key improvements:
 * - Much deeper search (8-14 ply)
 * - Opening book for strong early play
 * - Endgame tables for perfect play with few stones
 * - Aggressive mill creation and blocking
 * - Double mill (Zwickmühle) detection and creation
 * - Phase-specific evaluation
 * - Better mobility management
 */
object MuhleAI {

    // Much deeper search for stronger play
    private const val MAX_DEPTH_PLACING = 8
    private const val MAX_DEPTH_MOVING = 10
    private const val MAX_DEPTH_FLYING = 12
    private const val MAX_DEPTH_ENDGAME = 14  // When close to winning/losing

    // Evaluation constants - carefully tuned
    private const val WIN_SCORE = 1000000
    private const val LOSS_SCORE = -1000000

    // Mill-related scoring
    private const val MILL_VALUE = 500               // Completed mill
    private const val POTENTIAL_MILL = 200           // 2 stones with 1 empty (can complete)
    private const val DOUBLE_MILL_VALUE = 1500       // Zwickmühle - can form mill repeatedly
    private const val BLOCKING_MILL = 180            // Blocking opponent's potential mill

    // Stone and mobility
    private const val STONE_VALUE = 100              // Each stone on board
    private const val MOBILITY_VALUE = 15            // Each possible move
    private const val BLOCKED_STONE = -30            // Stone that can't move

    // Strategic positions
    private const val CROSSPOINT_VALUE = 40          // Position with 4 connections
    private const val TPOINT_VALUE = 25              // Position with 3 connections
    private const val CORNER_VALUE = 15              // Corner position

    // Phase bonuses
    private const val EARLY_AGGRESSION = 50          // Bonus for offensive play early
    private const val ENDGAME_STONE_VALUE = 200      // Stones worth more in endgame

    // Transposition table
    private val transpositionTable = ConcurrentHashMap<Long, TTEntry>()
    private var searchAge = 0

    data class TTEntry(
        val score: Int,
        val depth: Int,
        val flag: TTFlag,
        val bestMove: MuhleAIMove?,
        val age: Int
    )

    enum class TTFlag { EXACT, LOWER_BOUND, UPPER_BOUND }

    sealed class MuhleAIMove {
        data class Place(val position: Int) : MuhleAIMove()
        data class Move(val from: Int, val to: Int) : MuhleAIMove()
        data class Remove(val position: Int) : MuhleAIMove()
    }

    /**
     * Main entry point - get the best move
     */
    suspend fun getBestMove(
        board: MuhleBoard,
        aiPlayerNumber: Int,
        gamePhase: MuhleGamePhase,
        strength: Int = 100,
        timeLimitMs: Long = 4000,
        moveNumber: Int = 0
    ): MuhleAIMove {
        searchAge++
        val startTime = System.currentTimeMillis()
        val opponent = 3 - aiPlayerNumber

        // 0. Opening book for early game
        if (gamePhase == MuhleGamePhase.PLACING && moveNumber <= 6 && strength >= 70) {
            val openingMove = OpeningBook.getMuhleOpeningMove(board, aiPlayerNumber, moveNumber)
            if (openingMove != null) {
                return MuhleAIMove.Place(openingMove)
            }
        }

        // 1. Check for immediate winning move (form a mill)
        val winningMove = findImmediateMillMove(board, aiPlayerNumber, gamePhase)
        if (winningMove != null) return winningMove

        // 2. Block opponent's immediate mill threat
        val blockMove = findImmediateMillMove(board, opponent, gamePhase)
        if (blockMove != null) {
            // Convert opponent's winning move to our blocking move
            val actualBlockMove: MuhleAIMove? = when (blockMove) {
                is MuhleAIMove.Place -> blockMove
                is MuhleAIMove.Move -> {
                    // Block by moving to their destination if possible
                    val myMoves = getMoves(board, aiPlayerNumber, gamePhase)
                    val blockingMove = myMoves.find { it.second == blockMove.to }
                    if (blockingMove != null) {
                        MuhleAIMove.Move(blockingMove.first, blockingMove.second)
                    } else {
                        // Can't block with move, proceed with normal search
                        null
                    }
                }
                else -> null
            }
            if (actualBlockMove != null) return actualBlockMove
        }

        // 3. Use endgame tables when appropriate (few stones, high strength)
        val aiStones = MuhleLogic.countStonesOnBoard(board, aiPlayerNumber)
        val oppStones = MuhleLogic.countStonesOnBoard(board, opponent)

        if (gamePhase != MuhleGamePhase.PLACING &&
            (aiStones <= 4 || oppStones <= 4) &&
            strength >= 80) {
            val isFlying = aiStones == 3
            val endgameResult = MuhleEndgame.analyzeEndgame(board, aiPlayerNumber, isFlying)
            if (endgameResult.bestMove != null) {
                return when (val egMove = endgameResult.bestMove) {
                    is MuhleEndgame.MuhleEndgameMove.Move -> Move(egMove.from, egMove.to)
                    is MuhleEndgame.MuhleEndgameMove.Remove -> Remove(egMove.position)
                    null -> TODO()
                }
            }
        }

        // 4. Determine search depth based on phase and strength
        val baseDepth = when (gamePhase) {
            MuhleGamePhase.PLACING -> MAX_DEPTH_PLACING
            MuhleGamePhase.MOVING -> MAX_DEPTH_MOVING
            MuhleGamePhase.FLYING -> MAX_DEPTH_FLYING
        }

        // Adjust depth based on strength
        val searchDepth = ((baseDepth * strength) / 100).coerceIn(3, baseDepth)

        // 5. Weak AI makes random moves sometimes
        if (strength < 30) {
            if (Random.nextInt(100) > strength * 2) {
                return getRandomMove(board, aiPlayerNumber, gamePhase)
            }
        }

        // 6. Iterative deepening search
        var bestMove: MuhleAIMove = getFirstValidMove(board, aiPlayerNumber, gamePhase)
        var bestScore = Int.MIN_VALUE

        for (depth in 2..searchDepth) {
            if (System.currentTimeMillis() - startTime > timeLimitMs * 0.8) break
            if (!coroutineContext.isActive) break

            val result = searchRoot(board, aiPlayerNumber, gamePhase, depth, startTime, timeLimitMs)
            if (result != null && result.second > bestScore) {
                bestMove = result.first
                bestScore = result.second
            }

            // Found winning move
            if (bestScore >= WIN_SCORE - 1000) break
        }

        // 7. Apply strength-based mistakes
        return applyStrengthAdjustment(bestMove, board, aiPlayerNumber, gamePhase, strength)
    }

    /**
     * Find immediate mill-forming move
     */
    private fun findImmediateMillMove(
        board: MuhleBoard,
        playerNumber: Int,
        phase: MuhleGamePhase
    ): MuhleAIMove? {
        when (phase) {
            MuhleGamePhase.PLACING -> {
                for (pos in MuhleLogic.getEmptyPositions(board)) {
                    if (MuhleLogic.formsNewMill(board, pos, playerNumber)) {
                        return MuhleAIMove.Place(pos)
                    }
                }
            }
            MuhleGamePhase.MOVING, MuhleGamePhase.FLYING -> {
                val canFly = phase == MuhleGamePhase.FLYING
                val moves = MuhleLogic.getValidMoves(board, playerNumber, canFly)

                for ((from, destinations) in moves) {
                    for (to in destinations) {
                        // Simulate moving the stone
                        val tempBoard = simulateMove(board, from, to, playerNumber)
                        if (MuhleLogic.formsNewMill(tempBoard, to, playerNumber)) {
                            return MuhleAIMove.Move(from, to)
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Root search with move ordering
     */
    private fun searchRoot(
        board: MuhleBoard,
        playerNumber: Int,
        phase: MuhleGamePhase,
        maxDepth: Int,
        startTime: Long,
        timeLimit: Long
    ): Pair<MuhleAIMove, Int>? {
        val moves = getOrderedMoves(board, playerNumber, phase)
        if (moves.isEmpty()) return null

        var alpha = Int.MIN_VALUE + 1
        val beta = Int.MAX_VALUE - 1
        var bestScore = Int.MIN_VALUE
        var bestMove = moves.first()

        for (move in moves) {
            if (System.currentTimeMillis() - startTime > timeLimit * 0.9) break

            val newBoard = applyMove(board, move, playerNumber)
            val newPhase = determinePhase(newBoard, playerNumber)

            // Check if this move forms a mill (need to handle removal)
            val formedMill = when (move) {
                is MuhleAIMove.Place -> MuhleLogic.formsNewMill(board, move.position, playerNumber)
                is MuhleAIMove.Move -> {
                    val temp = simulateMove(board, move.from, move.to, playerNumber)
                    MuhleLogic.formsNewMill(temp, move.to, playerNumber)
                }
                else -> false
            }

            val score = if (formedMill) {
                // Add bonus for forming a mill and search from opponent's perspective
                MILL_VALUE + alphaBeta(
                    newBoard, maxDepth - 1, -beta, -alpha,
                    3 - playerNumber, playerNumber, newPhase, startTime, timeLimit
                ).let { -it }
            } else {
                -alphaBeta(
                    newBoard, maxDepth - 1, -beta, -alpha,
                    3 - playerNumber, playerNumber, newPhase, startTime, timeLimit
                )
            }

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = max(alpha, score)
        }

        return Pair(bestMove, bestScore)
    }

    /**
     * Alpha-beta search with transposition table
     */
    private fun alphaBeta(
        board: MuhleBoard,
        depth: Int,
        alphaIn: Int,
        betaIn: Int,
        currentPlayer: Int,
        aiPlayer: Int,
        phase: MuhleGamePhase,
        startTime: Long,
        timeLimit: Long
    ): Int {
        // Time check
        if (System.currentTimeMillis() - startTime > timeLimit) {
            return evaluate(board, aiPlayer, phase)
        }

        // Terminal condition check
        val myStones = MuhleLogic.countStonesOnBoard(board, currentPlayer)
        val oppStones = MuhleLogic.countStonesOnBoard(board, 3 - currentPlayer)

        if (phase != MuhleGamePhase.PLACING) {
            if (myStones < 3) return if (currentPlayer == aiPlayer) LOSS_SCORE + depth else WIN_SCORE - depth
            if (oppStones < 3) return if (currentPlayer == aiPlayer) WIN_SCORE - depth else LOSS_SCORE + depth
        }

        // Leaf node
        if (depth == 0) {
            return evaluate(board, aiPlayer, phase)
        }

        // Transposition table lookup
        val hash = computeHash(board, currentPlayer)
        val ttEntry = transpositionTable[hash]
        if (ttEntry != null && ttEntry.depth >= depth && ttEntry.age >= searchAge - 2) {
            when (ttEntry.flag) {
                TTFlag.EXACT -> return ttEntry.score
                TTFlag.LOWER_BOUND -> if (ttEntry.score >= betaIn) return ttEntry.score
                TTFlag.UPPER_BOUND -> if (ttEntry.score <= alphaIn) return ttEntry.score
            }
        }

        // Generate and order moves
        val moves = getOrderedMoves(board, currentPlayer, phase)
        if (moves.isEmpty()) {
            // No moves = loss
            return if (currentPlayer == aiPlayer) LOSS_SCORE + depth else WIN_SCORE - depth
        }

        var alpha = alphaIn
        var bestScore = Int.MIN_VALUE + 1
        var bestMove: MuhleAIMove? = null
        var flag = TTFlag.UPPER_BOUND

        for (move in moves) {
            val newBoard = applyMove(board, move, currentPlayer)
            val newPhase = determinePhase(newBoard, currentPlayer)

            val score = -alphaBeta(
                newBoard, depth - 1, -betaIn, -alpha,
                3 - currentPlayer, aiPlayer, newPhase, startTime, timeLimit
            )

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }

            if (score > alpha) {
                alpha = score
                flag = TTFlag.EXACT
            }

            if (alpha >= betaIn) {
                flag = TTFlag.LOWER_BOUND
                break
            }
        }

        // Store in transposition table
        if (transpositionTable.size < 1000000) {
            transpositionTable[hash] = TTEntry(bestScore, depth, flag, bestMove, searchAge)
        }

        return bestScore
    }

    /**
     * Get moves ordered by strategic value (best first for better pruning)
     */
    private fun getOrderedMoves(
        board: MuhleBoard,
        playerNumber: Int,
        phase: MuhleGamePhase
    ): List<MuhleAIMove> {
        val opponent = 3 - playerNumber
        val moves = mutableListOf<Pair<MuhleAIMove, Int>>()

        when (phase) {
            MuhleGamePhase.PLACING -> {
                for (pos in MuhleLogic.getEmptyPositions(board)) {
                    var priority = 0

                    // Forms mill - highest priority
                    if (MuhleLogic.formsNewMill(board, pos, playerNumber)) {
                        priority += 10000
                    }

                    // Blocks opponent's mill
                    if (MuhleLogic.formsNewMill(board, pos, opponent)) {
                        priority += 9000
                    }

                    // Creates double mill opportunity
                    val potentialMills = countPotentialMillsForPosition(board, pos, playerNumber)
                    priority += potentialMills * 500

                    // Blocks opponent's double mill
                    val oppPotential = countPotentialMillsForPosition(board, pos, opponent)
                    priority += oppPotential * 400

                    // Crosspoints (4 connections) are strategically valuable
                    val connections = MuhleLogic.getAdjacentPositions(pos).size
                    priority += connections * 50

                    moves.add(Pair(MuhleAIMove.Place(pos), priority))
                }
            }
            MuhleGamePhase.MOVING, MuhleGamePhase.FLYING -> {
                val canFly = phase == MuhleGamePhase.FLYING
                val validMoves = MuhleLogic.getValidMoves(board, playerNumber, canFly)

                for ((from, destinations) in validMoves) {
                    for (to in destinations) {
                        var priority = 0

                        // Simulate the move
                        val tempBoard = simulateMove(board, from, to, playerNumber)

                        // Forms mill
                        if (MuhleLogic.formsNewMill(tempBoard, to, playerNumber)) {
                            priority += 10000
                        }

                        // Blocks opponent's mill
                        if (MuhleLogic.formsNewMill(board, to, opponent)) {
                            priority += 9000
                        }

                        // Creates/maintains double mill position
                        val doubleMill = isPartOfDoubleMill(tempBoard, to, playerNumber)
                        if (doubleMill) {
                            priority += 5000
                        }

                        // Moving to position with more potential
                        val newPotential = countPotentialMillsForPosition(tempBoard, to, playerNumber)
                        val oldPotential = countPotentialMillsForPosition(board, from, playerNumber)
                        priority += (newPotential - oldPotential) * 200

                        // Prefer moves that increase mobility
                        val connections = MuhleLogic.getAdjacentPositions(to).count {
                            tempBoard.positions[it].isEmpty()
                        }
                        priority += connections * 30

                        moves.add(Pair(MuhleAIMove.Move(from, to), priority))
                    }
                }
            }
        }

        return moves.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Comprehensive position evaluation
     */
    private fun evaluate(board: MuhleBoard, aiPlayer: Int, phase: MuhleGamePhase): Int {
        val opponent = 3 - aiPlayer
        var score = 0

        val aiStones = MuhleLogic.countStonesOnBoard(board, aiPlayer)
        val oppStones = MuhleLogic.countStonesOnBoard(board, opponent)

        // Win/loss detection
        if (phase != MuhleGamePhase.PLACING) {
            if (oppStones < 3) return WIN_SCORE
            if (aiStones < 3) return LOSS_SCORE
        }

        // Stone count - more valuable in endgame
        val stoneMultiplier = if (aiStones <= 4 || oppStones <= 4) ENDGAME_STONE_VALUE else STONE_VALUE
        score += (aiStones - oppStones) * stoneMultiplier

        // Completed mills
        val aiMills = countMills(board, aiPlayer)
        val oppMills = countMills(board, opponent)
        score += (aiMills - oppMills) * MILL_VALUE

        // Potential mills (2 in a row with 1 empty)
        val aiPotential = countPotentialMills(board, aiPlayer)
        val oppPotential = countPotentialMills(board, opponent)
        score += (aiPotential - oppPotential) * POTENTIAL_MILL

        // Double mills (Zwickmühle) - extremely valuable
        val aiDoubleMills = countDoubleMills(board, aiPlayer)
        val oppDoubleMills = countDoubleMills(board, opponent)
        score += (aiDoubleMills - oppDoubleMills) * DOUBLE_MILL_VALUE

        // Mobility (number of legal moves)
        if (phase != MuhleGamePhase.PLACING) {
            val aiCanFly = aiStones <= 3
            val oppCanFly = oppStones <= 3
            val aiMobility = MuhleLogic.getValidMoves(board, aiPlayer, aiCanFly).sumOf { it.second.size }
            val oppMobility = MuhleLogic.getValidMoves(board, opponent, oppCanFly).sumOf { it.second.size }
            score += (aiMobility - oppMobility) * MOBILITY_VALUE

            // Blocked stones penalty
            score -= countBlockedStones(board, aiPlayer) * (-BLOCKED_STONE)
            score += countBlockedStones(board, opponent) * (-BLOCKED_STONE)

            // Check for no-move situation (loss)
            if (aiMobility == 0 && !aiCanFly) return LOSS_SCORE
            if (oppMobility == 0 && !oppCanFly) return WIN_SCORE
        }

        // Strategic position control
        score += evaluatePositionalControl(board, aiPlayer)
        score -= evaluatePositionalControl(board, opponent)

        // Early game: bonus for aggression
        if (phase == MuhleGamePhase.PLACING) {
            // Aggressive piece placement
            val aiCenterControl = countCrossPointControl(board, aiPlayer)
            score += aiCenterControl * EARLY_AGGRESSION
        }

        return score
    }

    /**
     * Evaluate strategic position control
     */
    private fun evaluatePositionalControl(board: MuhleBoard, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        var score = 0

        // Crosspoints (positions with 4 connections) - most valuable
        val crosspoints = setOf(4, 10, 13, 19)
        for (pos in crosspoints) {
            if (board.positions[pos].owner == owner) {
                score += CROSSPOINT_VALUE
            }
        }

        // T-points (positions with 3 connections)
        val tpoints = setOf(1, 7, 9, 11, 12, 14, 16, 22)
        for (pos in tpoints) {
            if (board.positions[pos].owner == owner) {
                score += TPOINT_VALUE
            }
        }

        // Corners (positions with 2 connections)
        val corners = setOf(0, 2, 3, 5, 6, 8, 15, 17, 18, 20, 21, 23)
        for (pos in corners) {
            if (board.positions[pos].owner == owner) {
                score += CORNER_VALUE
            }
        }

        return score
    }

    private fun countMills(board: MuhleBoard, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        return MuhleLogic.MILLS.count { mill ->
            mill.all { board.positions[it].owner == owner }
        }
    }

    private fun countPotentialMills(board: MuhleBoard, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        return MuhleLogic.MILLS.count { mill ->
            val playerCount = mill.count { board.positions[it].owner == owner }
            val emptyCount = mill.count { board.positions[it].isEmpty() }
            playerCount == 2 && emptyCount == 1
        }
    }

    private fun countPotentialMillsForPosition(board: MuhleBoard, pos: Int, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        return MuhleLogic.MILLS.count { mill ->
            pos in mill &&
            mill.count { board.positions[it].owner == owner } >= 1 &&
            mill.all { p -> board.positions[p].owner == owner || board.positions[p].isEmpty() }
        }
    }

    private fun countDoubleMills(board: MuhleBoard, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        var count = 0

        for (pos in 0 until 24) {
            if (board.positions[pos].owner == owner) {
                // Check if this stone is part of two potential mills
                val millsWithThisStone = MuhleLogic.MILLS.filter { mill ->
                    pos in mill &&
                    mill.count { board.positions[it].owner == owner } == 2 &&
                    mill.count { board.positions[it].isEmpty() } == 1
                }
                if (millsWithThisStone.size >= 2) {
                    count++
                }
            }
        }
        return count
    }

    private fun isPartOfDoubleMill(board: MuhleBoard, pos: Int, playerNumber: Int): Boolean {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name

        val millsWithPos = MuhleLogic.MILLS.filter { mill ->
            pos in mill && mill.all { board.positions[it].owner == owner || it == pos }
        }
        return millsWithPos.size >= 2
    }

    private fun countBlockedStones(board: MuhleBoard, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        var blocked = 0

        for (pos in 0 until 24) {
            if (board.positions[pos].owner == owner) {
                val adjacent = MuhleLogic.getAdjacentPositions(pos)
                if (adjacent.none { board.positions[it].isEmpty() }) {
                    blocked++
                }
            }
        }
        return blocked
    }

    private fun countCrossPointControl(board: MuhleBoard, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val crosspoints = setOf(4, 10, 13, 19)
        return crosspoints.count { board.positions[it].owner == owner }
    }

    /**
     * Get best stone to remove after forming a mill
     */
    fun getBestRemoval(
        board: MuhleBoard,
        opponentNumber: Int,
        strength: Int = 100
    ): MuhleAIMove.Remove {
        val removable = MuhleLogic.getRemovableStones(board, opponentNumber)
        if (removable.isEmpty()) return MuhleAIMove.Remove(0)

        // Weak AI sometimes picks randomly
        if (strength < 50 && Random.nextInt(100) > strength) {
            return MuhleAIMove.Remove(removable.random())
        }

        // Evaluate each removal option
        val evaluatedRemovals = removable.map { pos ->
            var score = 0
            val myPlayer = 3 - opponentNumber
            val oppOwner = if (opponentNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name

            // Prefer removing from double mill positions (breaks Zwickmühle)
            val millsWithPosition = MuhleLogic.MILLS.count { mill ->
                pos in mill && mill.count { board.positions[it].owner == oppOwner } >= 2
            }
            score += millsWithPosition * 100

            // Prefer removing from potential mills
            val potentialMills = MuhleLogic.MILLS.count { mill ->
                pos in mill &&
                mill.count { board.positions[it].owner == oppOwner } == 2 &&
                mill.count { board.positions[it].isEmpty() } == 1
            }
            score += potentialMills * 80

            // Prefer removing from strategic positions
            val connections = MuhleLogic.getAdjacentPositions(pos).size
            score += connections * 20

            // Prefer removing stones that block our mills
            val myOwner = if (myPlayer == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
            for (mill in MuhleLogic.MILLS) {
                if (pos in mill) {
                    val myCount = mill.count { board.positions[it].owner == myOwner }
                    if (myCount == 2) {
                        score += 150  // This stone blocks our potential mill
                    }
                }
            }

            Pair(pos, score)
        }

        val best = evaluatedRemovals.maxByOrNull { it.second }?.first ?: removable.first()
        return MuhleAIMove.Remove(best)
    }

    // ============ HELPER METHODS ============

    private fun getMoves(board: MuhleBoard, playerNumber: Int, phase: MuhleGamePhase): List<Pair<Int, Int>> {
        return when (phase) {
            MuhleGamePhase.PLACING -> MuhleLogic.getEmptyPositions(board).map { Pair(-1, it) }
            else -> {
                val canFly = MuhleLogic.countStonesOnBoard(board, playerNumber) <= 3
                MuhleLogic.getValidMoves(board, playerNumber, canFly).flatMap { (from, tos) ->
                    tos.map { Pair(from, it) }
                }
            }
        }
    }

    private fun simulateMove(board: MuhleBoard, from: Int, to: Int, playerNumber: Int): MuhleBoard {
        val newPositions = board.positions.mapIndexed { idx, pos ->
            when (idx) {
                from -> MuhlePosition(idx, MuhleStoneOwner.EMPTY.name)
                to -> MuhlePosition(idx, if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name)
                else -> pos
            }
        }
        return board.copy(positions = newPositions)
    }

    private fun applyMove(board: MuhleBoard, move: MuhleAIMove, playerNumber: Int): MuhleBoard {
        return when (move) {
            is MuhleAIMove.Place -> MuhleLogic.placeStone(board, move.position, playerNumber)
            is MuhleAIMove.Move -> MuhleLogic.moveStone(board, move.from, move.to, playerNumber)
            is MuhleAIMove.Remove -> {
                val newPositions = board.positions.mapIndexed { idx, pos ->
                    if (idx == move.position) MuhlePosition(idx, MuhleStoneOwner.EMPTY.name) else pos
                }
                board.copy(positions = newPositions)
            }
        }
    }

    private fun determinePhase(board: MuhleBoard, justPlayed: Int): MuhleGamePhase {
        val totalPlaced = board.positions.count { !it.isEmpty() }
        if (totalPlaced < 18) return MuhleGamePhase.PLACING

        val stones = MuhleLogic.countStonesOnBoard(board, justPlayed)
        return if (stones <= 3) MuhleGamePhase.FLYING else MuhleGamePhase.MOVING
    }

    private fun computeHash(board: MuhleBoard, currentPlayer: Int): Long {
        var hash = currentPlayer.toLong()
        for (i in 0 until 24) {
            val ownerValue = when (board.positions[i].owner) {
                MuhleStoneOwner.PLAYER_1.name -> 1L
                MuhleStoneOwner.PLAYER_2.name -> 2L
                else -> 0L
            }
            hash = hash xor (ownerValue shl (i * 2 + 2))
        }
        return hash
    }

    private fun getRandomMove(board: MuhleBoard, playerNumber: Int, phase: MuhleGamePhase): MuhleAIMove {
        return when (phase) {
            MuhleGamePhase.PLACING -> {
                val empty = MuhleLogic.getEmptyPositions(board)
                MuhleAIMove.Place(empty.randomOrNull() ?: 0)
            }
            else -> {
                val canFly = phase == MuhleGamePhase.FLYING
                val moves = MuhleLogic.getValidMoves(board, playerNumber, canFly)
                if (moves.isEmpty()) {
                    MuhleAIMove.Move(0, 1)
                } else {
                    val (from, destinations) = moves.random()
                    MuhleAIMove.Move(from, destinations.randomOrNull() ?: 0)
                }
            }
        }
    }

    private fun getFirstValidMove(board: MuhleBoard, playerNumber: Int, phase: MuhleGamePhase): MuhleAIMove {
        return when (phase) {
            MuhleGamePhase.PLACING -> {
                MuhleAIMove.Place(MuhleLogic.getEmptyPositions(board).firstOrNull() ?: 0)
            }
            else -> {
                val canFly = phase == MuhleGamePhase.FLYING
                val moves = MuhleLogic.getValidMoves(board, playerNumber, canFly)
                if (moves.isEmpty()) {
                    MuhleAIMove.Move(0, 1)
                } else {
                    val (from, destinations) = moves.first()
                    MuhleAIMove.Move(from, destinations.firstOrNull() ?: 0)
                }
            }
        }
    }

    private fun applyStrengthAdjustment(
        optimalMove: MuhleAIMove,
        board: MuhleBoard,
        playerNumber: Int,
        phase: MuhleGamePhase,
        strength: Int
    ): MuhleAIMove {
        if (strength >= 95) return optimalMove

        val mistakeProbability = when {
            strength <= 20 -> 0.55f
            strength <= 40 -> 0.35f
            strength <= 60 -> 0.20f
            strength <= 80 -> 0.10f
            else -> 0.03f
        }

        if (Random.nextFloat() < mistakeProbability) {
            return getRandomMove(board, playerNumber, phase)
        }

        return optimalMove
    }

    /**
     * Clear transposition table (call between games)
     */
    fun clearCache() {
        transpositionTable.clear()
        searchAge = 0
    }
}
