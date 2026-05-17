package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard
import de.sudokuonline.app.data.model.MuhleBoard
import de.sudokuonline.app.data.model.MuhleStoneOwner

/**
 * Opening Book System - PERFECT opening moves for TicTacToe
 *
 * 3x3 TicTacToe Perfect Strategy:
 * ================================
 * Board positions:
 *   0 | 1 | 2
 *  ---+---+---
 *   3 | 4 | 5
 *  ---+---+---
 *   6 | 7 | 8
 *
 * PERFECT PLAY RULES:
 * 1. First move: ALWAYS take center (4) if available, else take corner (0,2,6,8)
 * 2. If opponent took center first: ALWAYS take a corner
 * 3. If opponent took corner first: ALWAYS take center
 * 4. If opponent took edge first: ALWAYS take center
 *
 * With perfect play, the first player can always DRAW (never lose).
 * With perfect play, neither player can WIN against perfect defense.
 */
object OpeningBook {

    // ============ TICTACTOE 3x3 - PERFECT OPENING BOOK ============

    /**
     * Get PERFECT opening move for 3x3 TicTacToe
     * This handles the first 2-3 moves which are critical
     */
    fun getTicTacToe3x3Move(board: TicTacToeBoard, playerSymbol: Int): Int? {
        if (board.size != 3) return null

        val cells = board.cells
        val oppSymbol = 3 - playerSymbol

        // Count pieces
        val myPieces = cells.flatten().count { it.value == playerSymbol }
        val oppPieces = cells.flatten().count { it.value == oppSymbol }
        val totalPieces = myPieces + oppPieces

        // === MOVE 1: Empty board - I go first ===
        if (totalPieces == 0) {
            // ALWAYS take center on first move - mathematically optimal
            return 4
        }

        // === MOVE 1: One opponent piece - I go second ===
        if (myPieces == 0 && oppPieces == 1) {
            // Opponent made first move
            return when {
                // Opponent took center -> take ANY corner (0 is fine)
                cells[1][1].value == oppSymbol -> 0
                
                // Opponent took corner -> take center
                cells[0][0].value == oppSymbol -> 4
                cells[0][2].value == oppSymbol -> 4
                cells[2][0].value == oppSymbol -> 4
                cells[2][2].value == oppSymbol -> 4
                
                // Opponent took edge -> take center
                cells[0][1].value == oppSymbol -> 4
                cells[1][0].value == oppSymbol -> 4
                cells[1][2].value == oppSymbol -> 4
                cells[2][1].value == oppSymbol -> 4
                
                else -> 4
            }
        }

        // === MOVE 2: I made first move (center), opponent responded ===
        if (myPieces == 1 && oppPieces == 1 && cells[1][1].value == playerSymbol) {
            // I have center, opponent has something else
            // ALWAYS take a corner, preferably opposite to opponent if they took corner
            return when {
                // Opponent took a corner -> take OPPOSITE corner (creates fork threat)
                cells[0][0].value == oppSymbol -> 8
                cells[0][2].value == oppSymbol -> 6
                cells[2][0].value == oppSymbol -> 2
                cells[2][2].value == oppSymbol -> 0
                
                // Opponent took an edge -> take any corner
                cells[0][1].value == oppSymbol -> 0  // Take corner
                cells[1][0].value == oppSymbol -> 0
                cells[1][2].value == oppSymbol -> 2
                cells[2][1].value == oppSymbol -> 6
                
                else -> 0
            }
        }

        // === MOVE 2: I went second (took corner), opponent has center ===
        if (myPieces == 1 && oppPieces == 1 && cells[1][1].value == oppSymbol) {
            // I have a corner, opponent has center
            // Take OPPOSITE corner to create potential fork
            return when {
                cells[0][0].value == playerSymbol -> 8
                cells[0][2].value == playerSymbol -> 6
                cells[2][0].value == playerSymbol -> 2
                cells[2][2].value == playerSymbol -> 0
                else -> null // Shouldn't happen
            }
        }

        // === MOVE 2: Opponent took corner first, I took center ===
        if (myPieces == 1 && oppPieces == 1 && cells[1][1].value == playerSymbol) {
            // Already handled above
            return null
        }

        // === MOVE 3+: Use direct strategy checks ===
        if (totalPieces >= 2) {
            // Check for winning move
            for (pos in 0 until 9) {
                val row = pos / 3
                val col = pos % 3
                if (cells[row][col].isEmpty() && wouldWin(board, row, col, playerSymbol)) {
                    return pos
                }
            }

            // Check for blocking move
            for (pos in 0 until 9) {
                val row = pos / 3
                val col = pos % 3
                if (cells[row][col].isEmpty() && wouldWin(board, row, col, oppSymbol)) {
                    return pos
                }
            }

            // Check for fork creation
            for (pos in 0 until 9) {
                val row = pos / 3
                val col = pos % 3
                if (cells[row][col].isEmpty() && createsFork(board, row, col, playerSymbol)) {
                    return pos
                }
            }

            // Block opponent's fork
            for (pos in 0 until 9) {
                val row = pos / 3
                val col = pos % 3
                if (cells[row][col].isEmpty() && createsFork(board, row, col, oppSymbol)) {
                    return pos
                }
            }

            // Take center if available
            if (cells[1][1].isEmpty()) {
                return 4
            }

            // Take corner if available
            for (corner in listOf(0, 2, 6, 8)) {
                if (cells[corner / 3][corner % 3].isEmpty()) {
                    return corner
                }
            }

            // Take edge
            for (edge in listOf(1, 3, 5, 7)) {
                if (cells[edge / 3][edge % 3].isEmpty()) {
                    return edge
                }
            }
        }

        return null // Let minimax handle it
    }

    /**
     * Check if placing at (row, col) would win
     */
    private fun wouldWin(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val cells = board.cells

        // Check row
        var count = 0
        for (c in 0 until 3) {
            if (c == col || cells[row][c].value == symbol) count++
        }
        if (count == 3) return true

        // Check column
        count = 0
        for (r in 0 until 3) {
            if (r == row || cells[r][col].value == symbol) count++
        }
        if (count == 3) return true

        // Check main diagonal
        if (row == col) {
            count = 0
            for (i in 0 until 3) {
                if ((i == row) || cells[i][i].value == symbol) count++
            }
            if (count == 3) return true
        }

        // Check anti-diagonal
        if (row + col == 2) {
            count = 0
            for (i in 0 until 3) {
                if ((i == row && 2 - i == col) || cells[i][2 - i].value == symbol) count++
            }
            if (count == 3) return true
        }

        return false
    }

    /**
     * Check if placing creates a fork (2+ winning threats)
     */
    private fun createsFork(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        // Simulate placing the piece
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(value = symbol) else cell
            }
        }
        val newBoard = board.copy(cells = newCells)

        // Count winning threats
        var threats = 0
        for (pos in 0 until 9) {
            val r = pos / 3
            val c = pos % 3
            if (newBoard.cells[r][c].isEmpty() && wouldWin(newBoard, r, c, symbol)) {
                threats++
            }
        }
        return threats >= 2
    }

    // ============ TICTACTOE 5x5 OPENING BOOK ============

    /**
     * Get opening move for 5x5 TicTacToe
     */
    fun getTicTacToe5x5Move(board: TicTacToeBoard, playerSymbol: Int): Int? {
        if (board.size != 5) return null

        val cells = board.cells
        val oppSymbol = 3 - playerSymbol
        val totalPieces = cells.flatten().count { it.value != 0 }

        return when (totalPieces) {
            0 -> 12  // Center (2,2)
            1 -> {
                if (cells[2][2].value != 0) {
                    // Opponent took center, take a corner
                    0
                } else {
                    // Take center
                    12
                }
            }
            2 -> {
                // Prioritize center, then corners near center
                if (cells[2][2].isEmpty()) return 12
                listOf(6, 8, 16, 18).firstOrNull { pos ->
                    cells[pos / 5][pos % 5].isEmpty()
                }
            }
            else -> null
        }
    }

    // ============ MÜHLE OPENING BOOK ============

    /**
     * Get opening move for Mühle
     */
    fun getMuhleOpeningMove(board: MuhleBoard, playerNumber: Int, moveNumber: Int): Int? {
        if (moveNumber > 6) return null

        // Crosspoints (4 connections) are most valuable: 4, 10, 13, 19
        // T-points (3 connections): 1, 7, 9, 11, 12, 14, 16, 22

        val crosspoints = listOf(4, 10, 13, 19).shuffled()
        val tpoints = listOf(1, 7, 9, 11, 12, 14, 16, 22).shuffled()

        // First few moves: prioritize crosspoints
        if (moveNumber <= 2) {
            crosspoints.firstOrNull { board.positions[it].isEmpty() }?.let { return it }
        }

        // Check for mill opportunities
        val myOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val oppOwner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_2.name else MuhleStoneOwner.PLAYER_1.name

        // Block opponent's mills
        for (mill in MUHLE_MILLS) {
            val oppCount = mill.count { board.positions[it].owner == oppOwner }
            val emptyCount = mill.count { board.positions[it].isEmpty() }
            if (oppCount == 2 && emptyCount == 1) {
                mill.firstOrNull { board.positions[it].isEmpty() }?.let { return it }
            }
        }

        // Try to form own mills
        for (mill in MUHLE_MILLS) {
            val myCount = mill.count { board.positions[it].owner == myOwner }
            val emptyCount = mill.count { board.positions[it].isEmpty() }
            if (myCount == 2 && emptyCount == 1) {
                mill.firstOrNull { board.positions[it].isEmpty() }?.let { return it }
            }
        }

        // Take strategic positions
        crosspoints.firstOrNull { board.positions[it].isEmpty() }?.let { return it }
        tpoints.firstOrNull { board.positions[it].isEmpty() }?.let { return it }

        // Any empty position
        return (0 until 24).firstOrNull { board.positions[it].isEmpty() }
    }

    private val MUHLE_MILLS = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(15, 16, 17), listOf(18, 19, 20), listOf(21, 22, 23),
        listOf(0, 9, 21), listOf(3, 10, 18), listOf(6, 11, 15),
        listOf(1, 4, 7), listOf(16, 19, 22), listOf(8, 12, 17),
        listOf(5, 13, 20), listOf(2, 14, 23)
    )
}
