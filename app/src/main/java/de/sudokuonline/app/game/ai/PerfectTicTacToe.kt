package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard

/**
 * Perfect TicTacToe 3x3 AI - NEVER loses
 *
 * Board layout:
 *   0 | 1 | 2
 *  ---+---+---
 *   3 | 4 | 5
 *  ---+---+---
 *   6 | 7 | 8
 *
 * Perfect Strategy (in priority order):
 * 1. WIN: If you can win, win
 * 2. BLOCK: If opponent can win next move, block them
 * 3. FORK: Create a position with 2 winning threats
 * 4. BLOCK FORK: Prevent opponent from creating a fork
 * 5. CENTER: Take center if available
 * 6. OPPOSITE CORNER: If opponent has corner, take opposite
 * 7. CORNER: Take any empty corner
 * 8. EDGE: Take any empty edge
 */
object PerfectTicTacToe {

    /**
     * Get the PERFECT move for 3x3 TicTacToe
     * Returns position 0-8, or -1 if no move available
     */
    fun getBestMove(board: TicTacToeBoard, aiSymbol: Int): Int {
        if (board.size != 3) return -1

        val oppSymbol = 3 - aiSymbol
        val cells = board.cells

        // 1. WIN - Check for winning move
        for (pos in 0 until 9) {
            val row = pos / 3
            val col = pos % 3
            if (cells[row][col].isEmpty() && wouldWin(cells, row, col, aiSymbol)) {
                return pos
            }
        }

        // 2. BLOCK - Check for opponent's winning move
        for (pos in 0 until 9) {
            val row = pos / 3
            val col = pos % 3
            if (cells[row][col].isEmpty() && wouldWin(cells, row, col, oppSymbol)) {
                return pos
            }
        }

        // 3. FORK - Create a fork (2 winning threats)
        for (pos in 0 until 9) {
            val row = pos / 3
            val col = pos % 3
            if (cells[row][col].isEmpty() && createsFork(board, row, col, aiSymbol)) {
                return pos
            }
        }

        // 4. BLOCK FORK - Prevent opponent's fork
        val oppForks = mutableListOf<Int>()
        for (pos in 0 until 9) {
            val row = pos / 3
            val col = pos % 3
            if (cells[row][col].isEmpty() && createsFork(board, row, col, oppSymbol)) {
                oppForks.add(pos)
            }
        }

        if (oppForks.size == 1) {
            // Single fork - just block it
            return oppForks[0]
        } else if (oppForks.size > 1) {
            // Multiple forks - create a threat that forces block elsewhere
            for (pos in 0 until 9) {
                val row = pos / 3
                val col = pos % 3
                if (cells[row][col].isEmpty()) {
                    // Check if this creates a threat
                    val newBoard = makeMove(board, row, col, aiSymbol)
                    val threatPos = findWinningPos(newBoard, aiSymbol)
                    if (threatPos != null && threatPos !in oppForks) {
                        // This forces opponent to block at threatPos, not at a fork
                        return pos
                    }
                }
            }
            // Can't force, just block one fork
            return oppForks[0]
        }

        // 5. CENTER - Take center if available
        if (cells[1][1].isEmpty()) {
            return 4
        }

        // 6. OPPOSITE CORNER - If opponent has corner, take opposite
        val cornerPairs = listOf(
            0 to 8, 2 to 6, 6 to 2, 8 to 0
        )
        for ((corner, opposite) in cornerPairs) {
            val cRow = corner / 3
            val cCol = corner % 3
            val oRow = opposite / 3
            val oCol = opposite % 3
            if (cells[cRow][cCol].value == oppSymbol && cells[oRow][oCol].isEmpty()) {
                return opposite
            }
        }

        // 7. CORNER - Take any empty corner
        for (corner in listOf(0, 2, 6, 8)) {
            val row = corner / 3
            val col = corner % 3
            if (cells[row][col].isEmpty()) {
                return corner
            }
        }

        // 8. EDGE - Take any empty edge
        for (edge in listOf(1, 3, 5, 7)) {
            val row = edge / 3
            val col = edge % 3
            if (cells[row][col].isEmpty()) {
                return edge
            }
        }

        return -1 // No move available
    }

    /**
     * Check if placing symbol at (row, col) would win
     */
    private fun wouldWin(cells: List<List<de.sudokuonline.app.data.model.TicTacToeCell>>, row: Int, col: Int, symbol: Int): Boolean {
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

        // Check main diagonal (0,0 -> 2,2)
        if (row == col) {
            count = 0
            for (i in 0 until 3) {
                if (i == row || cells[i][i].value == symbol) count++
            }
            if (count == 3) return true
        }

        // Check anti-diagonal (0,2 -> 2,0)
        if (row + col == 2) {
            count = 0
            for (i in 0 until 3) {
                if (i == row || cells[i][2 - i].value == symbol) count++
            }
            if (count == 3) return true
        }

        return false
    }

    /**
     * Check if placing creates a fork (2+ winning threats)
     */
    private fun createsFork(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val newBoard = makeMove(board, row, col, symbol)
        return countWinningThreats(newBoard, symbol) >= 2
    }

    /**
     * Count how many ways the player can win next move
     */
    private fun countWinningThreats(board: TicTacToeBoard, symbol: Int): Int {
        var threats = 0
        for (pos in 0 until 9) {
            val r = pos / 3
            val c = pos % 3
            if (board.cells[r][c].isEmpty() && wouldWin(board.cells, r, c, symbol)) {
                threats++
            }
        }
        return threats
    }

    /**
     * Find a position where symbol can win
     */
    private fun findWinningPos(board: TicTacToeBoard, symbol: Int): Int? {
        for (pos in 0 until 9) {
            val r = pos / 3
            val c = pos % 3
            if (board.cells[r][c].isEmpty() && wouldWin(board.cells, r, c, symbol)) {
                return pos
            }
        }
        return null
    }

    /**
     * Create a new board with a move applied
     */
    private fun makeMove(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): TicTacToeBoard {
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(value = symbol) else cell
            }
        }
        return board.copy(cells = newCells)
    }
}
