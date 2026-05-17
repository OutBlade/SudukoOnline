package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.*

/**
 * Muhle (Nine Men's Morris) game logic
 * Handles validation, mill detection, and move generation
 */
object MuhleLogic {

    /**
     * Board adjacency map - defines which positions are connected
     * This represents the lines on the Muhle board
     */
    private val ADJACENCIES: Map<Int, List<Int>> = mapOf(
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

    /**
     * All possible mills (lines of 3)
     */
    val MILLS: List<List<Int>> = listOf(
        // Outer square
        listOf(0, 1, 2),
        listOf(0, 9, 21),
        listOf(2, 14, 23),
        listOf(21, 22, 23),
        // Middle square
        listOf(3, 4, 5),
        listOf(3, 10, 18),
        listOf(5, 13, 20),
        listOf(18, 19, 20),
        // Inner square
        listOf(6, 7, 8),
        listOf(6, 11, 15),
        listOf(8, 12, 17),
        listOf(15, 16, 17),
        // Cross lines
        listOf(1, 4, 7),
        listOf(9, 10, 11),
        listOf(12, 13, 14),
        listOf(16, 19, 22)
    )

    /**
     * Check if a position is valid for placing a stone
     */
    fun isValidPlacement(board: MuhleBoard, position: Int): Boolean {
        if (position < 0 || position >= 24) return false
        return board.positions[position].isEmpty()
    }

    /**
     * Check if a move from one position to another is valid
     */
    fun isValidMove(board: MuhleBoard, from: Int, to: Int, canFly: Boolean): Boolean {
        if (from < 0 || from >= 24 || to < 0 || to >= 24) return false
        if (from == to) return false
        if (!board.positions[to].isEmpty()) return false

        // If flying, any empty position is valid
        if (canFly) return true

        // Otherwise, must be adjacent
        return ADJACENCIES[from]?.contains(to) == true
    }

    /**
     * Check if a stone can be removed (not in a mill, unless all stones are in mills)
     */
    fun canRemoveStone(board: MuhleBoard, position: Int, playerToRemoveFrom: Int): Boolean {
        if (position < 0 || position >= 24) return false

        val targetOwner = if (playerToRemoveFrom == 1)
            MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name

        // Must be opponent's stone
        if (board.positions[position].owner != targetOwner) return false

        // Check if stone is in a mill
        val isInMill = isPositionInMill(board, position)

        if (!isInMill) return true

        // If in mill, check if all opponent's stones are in mills
        val opponentPositions = board.positions.indices.filter {
            board.positions[it].owner == targetOwner
        }
        return opponentPositions.all { isPositionInMill(board, it) }
    }

    /**
     * Check if a position is part of a mill
     */
    fun isPositionInMill(board: MuhleBoard, position: Int): Boolean {
        val owner = board.positions[position].owner
        if (owner == MuhleStoneOwner.EMPTY.name) return false

        return MILLS.any { mill ->
            position in mill && mill.all { board.positions[it].owner == owner }
        }
    }

    /**
     * Get all mills that a position is part of (for highlighting)
     */
    fun getMillsContainingPosition(board: MuhleBoard, position: Int): List<List<Int>> {
        val owner = board.positions[position].owner
        if (owner == MuhleStoneOwner.EMPTY.name) return emptyList()

        return MILLS.filter { mill ->
            position in mill && mill.all { board.positions[it].owner == owner }
        }
    }

    /**
     * Check if placing/moving to a position forms a new mill
     */
    fun formsNewMill(board: MuhleBoard, position: Int, playerNumber: Int): Boolean {
        val owner = if (playerNumber == 1)
            MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name

        return MILLS.any { mill ->
            position in mill && mill.all { pos ->
                pos == position || board.positions[pos].owner == owner
            }
        }
    }

    /**
     * Place a stone on the board
     */
    fun placeStone(board: MuhleBoard, position: Int, playerNumber: Int): MuhleBoard {
        if (!isValidPlacement(board, position)) return board

        val owner = if (playerNumber == 1)
            MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name

        val newPositions = board.positions.mapIndexed { idx, pos ->
            if (idx == position) MuhlePosition(idx, owner) else pos
        }

        // Check for mills formed
        val millsFormed = MILLS.filter { mill ->
            position in mill && mill.all { newPositions[it].owner == owner }
        }.flatten().distinct()

        return board.copy(
            positions = newPositions,
            lastMillPositions = if (millsFormed.isNotEmpty()) millsFormed else emptyList()
        )
    }

    /**
     * Move a stone from one position to another
     */
    fun moveStone(board: MuhleBoard, from: Int, to: Int, playerNumber: Int): MuhleBoard {
        val owner = if (playerNumber == 1)
            MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name

        val newPositions = board.positions.mapIndexed { idx, pos ->
            when (idx) {
                from -> MuhlePosition(idx, MuhleStoneOwner.EMPTY.name)
                to -> MuhlePosition(idx, owner)
                else -> pos
            }
        }

        // Check for mills formed at new position
        val millsFormed = MILLS.filter { mill ->
            to in mill && mill.all { newPositions[it].owner == owner }
        }.flatten().distinct()

        return board.copy(
            positions = newPositions,
            lastMillPositions = if (millsFormed.isNotEmpty()) millsFormed else emptyList()
        )
    }

    /**
     * Remove a stone from the board
     */
    fun removeStone(board: MuhleBoard, position: Int): MuhleBoard {
        val newPositions = board.positions.mapIndexed { idx, pos ->
            if (idx == position) MuhlePosition(idx, MuhleStoneOwner.EMPTY.name) else pos
        }
        return board.copy(positions = newPositions, lastMillPositions = emptyList())
    }

    /**
     * Get all valid moves for a player
     */
    fun getValidMoves(board: MuhleBoard, playerNumber: Int, canFly: Boolean): List<Pair<Int, List<Int>>> {
        val owner = if (playerNumber == 1)
            MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name

        val playerPositions = board.positions.indices.filter {
            board.positions[it].owner == owner
        }

        return playerPositions.map { from ->
            val validDestinations = if (canFly) {
                board.positions.indices.filter { board.positions[it].isEmpty() }
            } else {
                ADJACENCIES[from]?.filter { board.positions[it].isEmpty() } ?: emptyList()
            }
            Pair(from, validDestinations)
        }.filter { it.second.isNotEmpty() }
    }

    /**
     * Get all empty positions (for placing phase)
     */
    fun getEmptyPositions(board: MuhleBoard): List<Int> {
        return board.positions.indices.filter { board.positions[it].isEmpty() }
    }

    /**
     * Get removable opponent stones
     */
    fun getRemovableStones(board: MuhleBoard, opponentNumber: Int): List<Int> {
        return board.positions.indices.filter {
            canRemoveStone(board, it, opponentNumber)
        }
    }

    /**
     * Check for win condition
     * Win if opponent has < 3 stones total OR opponent cannot make any move
     */
    fun checkWinner(board: MuhleBoard, players: Map<String, MuhleRoomPlayer>): Int? {
        players.values.forEach { player ->
            val totalStones = player.getTotalStones()

            // Win condition 1: Opponent has less than 3 stones (only after placing phase)
            if (player.stonesToPlace == 0 && totalStones < 3) {
                return if (player.playerNumber == 1) 2 else 1
            }

            // Win condition 2: Opponent cannot move (only in moving/flying phase)
            if (player.stonesToPlace == 0) {
                val canFly = player.stonesOnBoard <= 3
                val validMoves = getValidMoves(board, player.playerNumber, canFly)
                if (validMoves.isEmpty()) {
                    return if (player.playerNumber == 1) 2 else 1
                }
            }
        }
        return null
    }

    /**
     * Check if game is a draw (rare in Muhle, but possible)
     */
    fun isDraw(board: MuhleBoard, moveCount: Int): Boolean {
        // Draw after 50 moves without capturing (optional rule)
        return moveCount >= 100  // 50 moves per player
    }

    /**
     * Count stones for a player on the board
     */
    fun countStonesOnBoard(board: MuhleBoard, playerNumber: Int): Int {
        val owner = if (playerNumber == 1)
            MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        return board.positions.count { it.owner == owner }
    }

    /**
     * Get adjacent positions for a given position
     */
    fun getAdjacentPositions(position: Int): List<Int> {
        return ADJACENCIES[position] ?: emptyList()
    }

    /**
     * Calculate score for Muhle
     */
    fun calculateScore(
        won: Boolean,
        stonesRemaining: Int,
        millsFormed: Int,
        opponentStonesRemaining: Int
    ): Int {
        val baseScore = if (won) 1500 else 0
        val stoneBonus = stonesRemaining * 50
        val millBonus = millsFormed * 100
        val dominationBonus = if (won) (9 - opponentStonesRemaining) * 25 else 0

        return baseScore + stoneBonus + millBonus + dominationBonus
    }
}
