package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.*

/**
 * TicTacToe game logic - handles validation, win checking, and bomb mechanics
 */
object TicTacToeLogic {

    /**
     * Check if a move is valid
     */
    fun isValidMove(board: TicTacToeBoard, row: Int, col: Int): Boolean {
        if (row < 0 || row >= board.size || col < 0 || col >= board.size) return false
        return board.cells[row][col].isEmpty()
    }

    /**
     * Check if placing a bomb is valid
     */
    fun isValidBombPlacement(board: TicTacToeBoard, row: Int, col: Int): Boolean {
        if (row < 0 || row >= board.size || col < 0 || col >= board.size) return false
        // Bombs can be placed on empty cells only
        return board.cells[row][col].isEmpty()
    }

    /**
     * Make a move on the board
     */
    fun makeMove(
        board: TicTacToeBoard,
        row: Int,
        col: Int,
        symbol: Int,
        playerId: String
    ): TicTacToeBoard {
        if (!isValidMove(board, row, col)) return board

        val newCells = board.cells.mapIndexed { r, rowCells ->
            if (r == row) {
                rowCells.mapIndexed { c, cell ->
                    if (c == col) {
                        TicTacToeCell(
                            value = symbol,
                            playerId = playerId,
                            isBomb = false,
                            bombOwnerId = null
                        )
                    } else cell
                }
            } else rowCells
        }

        return board.copy(cells = newCells)
    }

    /**
     * Place a bomb on the board (for bomb modes)
     */
    fun placeBomb(
        board: TicTacToeBoard,
        row: Int,
        col: Int,
        playerId: String
    ): TicTacToeBoard {
        if (!isValidBombPlacement(board, row, col)) return board

        val newCells = board.cells.mapIndexed { r, rowCells ->
            if (r == row) {
                rowCells.mapIndexed { c, cell ->
                    if (c == col) {
                        TicTacToeCell(
                            value = 0,
                            playerId = null,
                            isBomb = true,
                            bombOwnerId = playerId
                        )
                    } else cell
                }
            } else rowCells
        }

        return board.copy(cells = newCells)
    }

    /**
     * Detonate a standard bomb - clears cells above, below, left, right, and the bomb itself
     * Pattern:
     *      [X]
     *   [X][💣][X]
     *      [X]
     */
    fun detonateStandardBomb(
        board: TicTacToeBoard,
        bombRow: Int,
        bombCol: Int
    ): TicTacToeBoard {
        val cellsToDelete = mutableListOf<Pair<Int, Int>>()

        // Bomb position itself
        cellsToDelete.add(Pair(bombRow, bombCol))

        // Above
        if (bombRow > 0) cellsToDelete.add(Pair(bombRow - 1, bombCol))
        // Below
        if (bombRow < board.size - 1) cellsToDelete.add(Pair(bombRow + 1, bombCol))
        // Left
        if (bombCol > 0) cellsToDelete.add(Pair(bombRow, bombCol - 1))
        // Right
        if (bombCol < board.size - 1) cellsToDelete.add(Pair(bombRow, bombCol + 1))

        return clearCells(board, cellsToDelete)
    }

    /**
     * Detonate an L-Bomb - clears all cells in the column above the bomb,
     * the bomb itself, and the cell to the right of the bomb
     * Pattern:
     *   [X]
     *   [X]
     *   [X]
     *   [💣][X]
     */
    fun detonateLBomb(
        board: TicTacToeBoard,
        bombRow: Int,
        bombCol: Int
    ): TicTacToeBoard {
        val cellsToDelete = mutableListOf<Pair<Int, Int>>()

        // Bomb position itself
        cellsToDelete.add(Pair(bombRow, bombCol))

        // All cells above the bomb (entire column upward)
        for (row in 0 until bombRow) {
            cellsToDelete.add(Pair(row, bombCol))
        }

        // Cell to the right of the bomb
        if (bombCol < board.size - 1) {
            cellsToDelete.add(Pair(bombRow, bombCol + 1))
        }

        return clearCells(board, cellsToDelete)
    }

    /**
     * Clear specified cells on the board
     */
    private fun clearCells(
        board: TicTacToeBoard,
        positions: List<Pair<Int, Int>>
    ): TicTacToeBoard {
        val positionSet = positions.toSet()

        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (Pair(r, c) in positionSet) {
                    TicTacToeCell() // Empty cell
                } else cell
            }
        }

        return board.copy(cells = newCells)
    }

    /**
     * Check for a winner
     * Returns: 1 for X wins, 2 for O wins, null for no winner yet
     */
    fun checkWinner(board: TicTacToeBoard): Int? {
        val size = board.size
        val winCondition = board.winCondition

        // Check all rows
        for (row in 0 until size) {
            val winner = checkLine(board, (0 until size).map { Pair(row, it) }, winCondition)
            if (winner != null) return winner
        }

        // Check all columns
        for (col in 0 until size) {
            val winner = checkLine(board, (0 until size).map { Pair(it, col) }, winCondition)
            if (winner != null) return winner
        }

        // Check main diagonals (top-left to bottom-right)
        for (startRow in 0 until size) {
            for (startCol in 0 until size) {
                val diagonal = mutableListOf<Pair<Int, Int>>()
                var r = startRow
                var c = startCol
                while (r < size && c < size) {
                    diagonal.add(Pair(r, c))
                    r++
                    c++
                }
                if (diagonal.size >= winCondition) {
                    val winner = checkLine(board, diagonal, winCondition)
                    if (winner != null) return winner
                }
            }
        }

        // Check anti-diagonals (top-right to bottom-left)
        for (startRow in 0 until size) {
            for (startCol in 0 until size) {
                val diagonal = mutableListOf<Pair<Int, Int>>()
                var r = startRow
                var c = startCol
                while (r < size && c >= 0) {
                    diagonal.add(Pair(r, c))
                    r++
                    c--
                }
                if (diagonal.size >= winCondition) {
                    val winner = checkLine(board, diagonal, winCondition)
                    if (winner != null) return winner
                }
            }
        }

        return null
    }

    /**
     * Check a line (row, column, or diagonal) for a winner
     */
    private fun checkLine(
        board: TicTacToeBoard,
        positions: List<Pair<Int, Int>>,
        winCondition: Int
    ): Int? {
        if (positions.size < winCondition) return null

        var consecutiveX = 0
        var consecutiveO = 0

        for ((row, col) in positions) {
            val cell = board.cells[row][col]

            when {
                cell.isX() && !cell.isBomb -> {
                    consecutiveX++
                    consecutiveO = 0
                    if (consecutiveX >= winCondition) return 1
                }
                cell.isO() && !cell.isBomb -> {
                    consecutiveO++
                    consecutiveX = 0
                    if (consecutiveO >= winCondition) return 2
                }
                else -> {
                    consecutiveX = 0
                    consecutiveO = 0
                }
            }
        }

        return null
    }

    /**
     * Get the winning line positions (for highlighting)
     */
    fun getWinningLine(board: TicTacToeBoard): List<Pair<Int, Int>>? {
        val size = board.size
        val winCondition = board.winCondition

        // Check all rows
        for (row in 0 until size) {
            val line = findWinningSequence(board, (0 until size).map { Pair(row, it) }, winCondition)
            if (line != null) return line
        }

        // Check all columns
        for (col in 0 until size) {
            val line = findWinningSequence(board, (0 until size).map { Pair(it, col) }, winCondition)
            if (line != null) return line
        }

        // Check main diagonals
        for (startRow in 0 until size) {
            for (startCol in 0 until size) {
                val diagonal = mutableListOf<Pair<Int, Int>>()
                var r = startRow
                var c = startCol
                while (r < size && c < size) {
                    diagonal.add(Pair(r, c))
                    r++
                    c++
                }
                if (diagonal.size >= winCondition) {
                    val line = findWinningSequence(board, diagonal, winCondition)
                    if (line != null) return line
                }
            }
        }

        // Check anti-diagonals
        for (startRow in 0 until size) {
            for (startCol in 0 until size) {
                val diagonal = mutableListOf<Pair<Int, Int>>()
                var r = startRow
                var c = startCol
                while (r < size && c >= 0) {
                    diagonal.add(Pair(r, c))
                    r++
                    c--
                }
                if (diagonal.size >= winCondition) {
                    val line = findWinningSequence(board, diagonal, winCondition)
                    if (line != null) return line
                }
            }
        }

        return null
    }

    /**
     * Find the winning sequence of positions in a line
     */
    private fun findWinningSequence(
        board: TicTacToeBoard,
        positions: List<Pair<Int, Int>>,
        winCondition: Int
    ): List<Pair<Int, Int>>? {
        if (positions.size < winCondition) return null

        var startIndex = 0
        var currentSymbol = 0
        var count = 0

        for (i in positions.indices) {
            val (row, col) = positions[i]
            val cell = board.cells[row][col]
            val cellSymbol = if (cell.isBomb) 0 else cell.value

            if (cellSymbol != 0 && cellSymbol == currentSymbol) {
                count++
                if (count >= winCondition) {
                    return positions.subList(startIndex, i + 1)
                }
            } else if (cellSymbol != 0) {
                currentSymbol = cellSymbol
                startIndex = i
                count = 1
            } else {
                currentSymbol = 0
                count = 0
            }
        }

        return null
    }

    /**
     * Check if the board is full (draw condition)
     */
    fun isBoardFull(board: TicTacToeBoard): Boolean {
        return board.cells.all { row ->
            row.all { cell -> !cell.isEmpty() }
        }
    }

    /**
     * Check if the game is a draw
     */
    fun isDraw(board: TicTacToeBoard): Boolean {
        return isBoardFull(board) && checkWinner(board) == null
    }

    /**
     * Get all available (empty) positions
     */
    fun getAvailableMoves(board: TicTacToeBoard): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    moves.add(Pair(row, col))
                }
            }
        }
        return moves
    }

    /**
     * Count moves made by a player
     */
    fun countMoves(board: TicTacToeBoard, symbol: Int): Int {
        return board.cells.sumOf { row ->
            row.count { cell -> cell.value == symbol && !cell.isBomb }
        }
    }

    /**
     * Get bombs on the board for a specific player
     */
    fun getBombsForPlayer(board: TicTacToeBoard, playerId: String): List<Pair<Int, Int>> {
        val bombs = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                val cell = board.cells[row][col]
                if (cell.isBomb && cell.bombOwnerId == playerId) {
                    bombs.add(Pair(row, col))
                }
            }
        }
        return bombs
    }

    /**
     * Get positions affected by a standard bomb
     */
    fun getStandardBombAffectedPositions(row: Int, col: Int, boardSize: Int): List<Pair<Int, Int>> {
        val positions = mutableListOf<Pair<Int, Int>>()
        positions.add(Pair(row, col))  // Bomb itself
        if (row > 0) positions.add(Pair(row - 1, col))  // Above
        if (row < boardSize - 1) positions.add(Pair(row + 1, col))  // Below
        if (col > 0) positions.add(Pair(row, col - 1))  // Left
        if (col < boardSize - 1) positions.add(Pair(row, col + 1))  // Right
        return positions
    }

    /**
     * Get positions affected by an L-bomb
     */
    fun getLBombAffectedPositions(row: Int, col: Int, boardSize: Int): List<Pair<Int, Int>> {
        val positions = mutableListOf<Pair<Int, Int>>()
        positions.add(Pair(row, col))  // Bomb itself
        // All cells above
        for (r in 0 until row) {
            positions.add(Pair(r, col))
        }
        // Cell to the right
        if (col < boardSize - 1) {
            positions.add(Pair(row, col + 1))
        }
        return positions
    }

    /**
     * Calculate score for TicTacToe
     */
    fun calculateScore(
        won: Boolean,
        isDraw: Boolean,
        movesCount: Int,
        bombsUsed: Int,
        boardSize: TicTacToeBoardSize
    ): Int {
        val baseScore = when {
            won -> 1000
            isDraw -> 250
            else -> 0
        }

        val sizeBonus = when (boardSize) {
            TicTacToeBoardSize.SMALL -> 0
            TicTacToeBoardSize.LARGE -> 500
        }

        // Bonus for winning quickly (fewer moves = more points)
        val speedBonus = if (won) {
            val maxMoves = boardSize.size * boardSize.size / 2
            val movesEfficiency = maxOf(0, maxMoves - movesCount)
            movesEfficiency * 50
        } else 0

        // Penalty for using bombs (encourages skillful play)
        val bombPenalty = bombsUsed * 25

        return maxOf(0, baseScore + sizeBonus + speedBonus - bombPenalty)
    }
}

/**
 * Ultimate TicTacToe game logic
 */
object UltimateTicTacToeLogic {

    /**
     * Check if a move is valid in Ultimate TicTacToe
     */
    fun isValidMove(
        board: UltimateTicTacToeBoard,
        boardRow: Int,
        boardCol: Int,
        cellRow: Int,
        cellCol: Int
    ): Boolean {
        // Check bounds
        if (boardRow !in 0..2 || boardCol !in 0..2) return false
        if (cellRow !in 0..2 || cellCol !in 0..2) return false

        // Check if mini board is already won
        val miniBoard = board.miniBoards[boardRow][boardCol]
        if (miniBoard.isFinished()) return false

        // Check if cell is empty
        if (!miniBoard.cells[cellRow][cellCol].isEmpty()) return false

        // Check if this is the active board (or any board is valid)
        val activeBoard = board.activeBoard
        if (activeBoard != null) {
            if (boardRow != activeBoard.first || boardCol != activeBoard.second) return false
        }

        return true
    }

    /**
     * Make a move in Ultimate TicTacToe
     */
    fun makeMove(
        board: UltimateTicTacToeBoard,
        boardRow: Int,
        boardCol: Int,
        cellRow: Int,
        cellCol: Int,
        symbol: Int,
        playerId: String
    ): UltimateTicTacToeBoard {
        if (!isValidMove(board, boardRow, boardCol, cellRow, cellCol)) return board

        // Update the cell in the mini board
        val miniBoard = board.miniBoards[boardRow][boardCol]
        val newCells = miniBoard.cells.mapIndexed { r, row ->
            if (r == cellRow) {
                row.mapIndexed { c, cell ->
                    if (c == cellCol) {
                        TicTacToeCell(value = symbol, playerId = playerId)
                    } else cell
                }
            } else row
        }

        // Check if mini board is now won
        val tempMiniBoard = TicTacToeBoard(cells = newCells, size = 3, winCondition = 3)
        val miniWinner = TicTacToeLogic.checkWinner(tempMiniBoard)
        val isMiniDraw = miniWinner == null && TicTacToeLogic.isBoardFull(tempMiniBoard)

        val newMiniBoard = UltimateMiniBoard(
            cells = newCells,
            winner = when {
                miniWinner != null -> miniWinner
                isMiniDraw -> 3  // Draw
                else -> 0
            }
        )

        // Update the board
        val newMiniBoards = board.miniBoards.mapIndexed { r, row ->
            if (r == boardRow) {
                row.mapIndexed { c, mb ->
                    if (c == boardCol) newMiniBoard else mb
                }
            } else row
        }

        // Determine next active board
        val nextActiveBoard = if (newMiniBoards[cellRow][cellCol].isFinished()) {
            null  // Target board is finished, can play anywhere
        } else {
            Pair(cellRow, cellCol)  // Must play in the board corresponding to cell position
        }

        return UltimateTicTacToeBoard(
            miniBoards = newMiniBoards,
            activeBoard = nextActiveBoard
        )
    }

    /**
     * Check for a winner in Ultimate TicTacToe (3 mini boards in a row)
     */
    fun checkWinner(board: UltimateTicTacToeBoard): Int? {
        val winners = board.miniBoards.map { row -> row.map { it.winner } }

        // Check rows
        for (row in 0..2) {
            if (winners[row][0] in 1..2 &&
                winners[row][0] == winners[row][1] &&
                winners[row][1] == winners[row][2]) {
                return winners[row][0]
            }
        }

        // Check columns
        for (col in 0..2) {
            if (winners[0][col] in 1..2 &&
                winners[0][col] == winners[1][col] &&
                winners[1][col] == winners[2][col]) {
                return winners[0][col]
            }
        }

        // Check diagonals
        if (winners[0][0] in 1..2 &&
            winners[0][0] == winners[1][1] &&
            winners[1][1] == winners[2][2]) {
            return winners[0][0]
        }
        if (winners[0][2] in 1..2 &&
            winners[0][2] == winners[1][1] &&
            winners[1][1] == winners[2][0]) {
            return winners[0][2]
        }

        return null
    }

    /**
     * Check if the game is a draw
     */
    fun isDraw(board: UltimateTicTacToeBoard): Boolean {
        // All mini boards must be finished and no winner
        val allFinished = board.miniBoards.all { row -> row.all { it.isFinished() } }
        return allFinished && checkWinner(board) == null
    }

    /**
     * Get all playable boards (for highlighting)
     */
    fun getPlayableBoards(board: UltimateTicTacToeBoard): List<Pair<Int, Int>> {
        val activeBoard = board.activeBoard
        return if (activeBoard != null && !board.miniBoards[activeBoard.first][activeBoard.second].isFinished()) {
            listOf(activeBoard)
        } else {
            // All unfinished boards are playable
            val playable = mutableListOf<Pair<Int, Int>>()
            for (row in 0..2) {
                for (col in 0..2) {
                    if (!board.miniBoards[row][col].isFinished()) {
                        playable.add(Pair(row, col))
                    }
                }
            }
            playable
        }
    }

    /**
     * Calculate score for Ultimate TicTacToe
     */
    fun calculateScore(won: Boolean, isDraw: Boolean, boardsWon: Int): Int {
        val baseScore = when {
            won -> 2000  // Higher base score for Ultimate
            isDraw -> 500
            else -> 0
        }

        // Bonus for each board won
        val boardBonus = boardsWon * 100

        return baseScore + boardBonus
    }
}
