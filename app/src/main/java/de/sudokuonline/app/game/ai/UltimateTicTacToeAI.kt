package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.TicTacToeBoard
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced AI for Ultimate TicTacToe
 *
 * Ultimate TicTacToe unique mechanics:
 * 1. 9 sub-boards, each a 3x3 TicTacToe
 * 2. Where you play determines where opponent must play
 * 3. Winning a sub-board claims it (like regular TicTacToe)
 * 4. Win 3 sub-boards in a row to win the game
 *
 * Key strategic elements:
 * - Board control: sending opponent to favorable/unfavorable boards
 * - Dual-layer tactics: local (sub-board) and global (meta-board)
 * - Tempo: forcing sequences that maintain initiative
 * - Sacrifice: sometimes losing a sub-board for strategic advantage
 */
object UltimateTicTacToeAI {

    // Board ownership: 0=empty, 1=X, 2=O, 3=draw
    private const val DRAW = 3

    // Evaluation weights
    private const val WIN_SCORE = 1000000
    private const val SUB_BOARD_WIN = 10000
    private const val META_THREAT = 50000
    private const val LOCAL_THREAT = 1000
    private const val BOARD_CONTROL = 500
    private const val CENTER_BONUS = 300

    data class UltimateMove(
        val boardRow: Int,    // Which sub-board (0-2)
        val boardCol: Int,
        val cellRow: Int,     // Which cell in sub-board (0-2)
        val cellCol: Int,
        val evaluation: Int,
        val reason: String
    )

    /**
     * Get best move for Ultimate TicTacToe
     *
     * @param boards 3x3 array of TicTacToeBoard (each is a sub-board)
     * @param metaBoard Board ownership array [row][col] -> owner (0, 1, 2, or 3 for draw)
     * @param activeBoard Which sub-board must be played in (null = any)
     * @param aiSymbol AI's symbol (1 or 2)
     */
    fun getBestMove(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        activeBoard: Pair<Int, Int>?,
        aiSymbol: Int,
        maxDepth: Int = 8,
        timeLimitMs: Long = 3000
    ): UltimateMove {
        val startTime = System.currentTimeMillis()

        // Generate valid moves
        val validMoves = generateValidMoves(boards, metaBoard, activeBoard, aiSymbol)

        if (validMoves.isEmpty()) {
            return UltimateMove(1, 1, 1, 1, 0, "Kein Zug möglich")
        }

        if (validMoves.size == 1) {
            val m = validMoves[0]
            return UltimateMove(m.first.first, m.first.second, m.second.first, m.second.second,
                0, "Einziger möglicher Zug")
        }

        // Check for immediate winning move on meta-board
        for (move in validMoves) {
            val (boardPos, cellPos) = move
            val subBoard = boards[boardPos.first][boardPos.second]

            // Check if this move wins the sub-board
            if (wouldWinSubBoard(subBoard, cellPos.first, cellPos.second, aiSymbol)) {
                // Check if winning this sub-board wins the game
                if (wouldWinMeta(metaBoard, boardPos.first, boardPos.second, aiSymbol)) {
                    return UltimateMove(
                        boardPos.first, boardPos.second, cellPos.first, cellPos.second,
                        WIN_SCORE, "Gewinnzug!"
                    )
                }
            }
        }

        // Block opponent's winning move
        val oppSymbol = 3 - aiSymbol
        for (move in validMoves) {
            val (boardPos, cellPos) = move
            val subBoard = boards[boardPos.first][boardPos.second]

            if (wouldWinSubBoard(subBoard, cellPos.first, cellPos.second, oppSymbol)) {
                if (wouldWinMeta(metaBoard, boardPos.first, boardPos.second, oppSymbol)) {
                    return UltimateMove(
                        boardPos.first, boardPos.second, cellPos.first, cellPos.second,
                        WIN_SCORE - 1000, "Blockiere Gegner-Sieg!"
                    )
                }
            }
        }

        // Use iterative deepening minimax
        var bestMove = validMoves[0]
        var bestScore = Int.MIN_VALUE

        for (depth in 1..maxDepth) {
            if (System.currentTimeMillis() - startTime > timeLimitMs * 0.7) break

            for (move in validMoves.sortedByDescending { preliminaryEval(boards, metaBoard, it, aiSymbol) }) {
                val (boardPos, cellPos) = move

                val score = evaluateMoveWithSearch(
                    boards, metaBoard, boardPos, cellPos, aiSymbol,
                    depth, Int.MIN_VALUE, Int.MAX_VALUE, startTime, timeLimitMs
                )

                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }

                if (System.currentTimeMillis() - startTime > timeLimitMs) break
            }
        }

        val reason = getMoveReason(boards, metaBoard, bestMove, aiSymbol)
        return UltimateMove(
            bestMove.first.first, bestMove.first.second,
            bestMove.second.first, bestMove.second.second,
            bestScore, reason
        )
    }

    /**
     * Generate all valid moves
     */
    private fun generateValidMoves(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        activeBoard: Pair<Int, Int>?,
        aiSymbol: Int
    ): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
        val moves = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()

        val boardsToCheck = if (activeBoard != null && metaBoard[activeBoard.first][activeBoard.second] == 0) {
            listOf(activeBoard)
        } else {
            // Can play in any unfinished board
            (0..2).flatMap { r -> (0..2).mapNotNull { c ->
                if (metaBoard[r][c] == 0) Pair(r, c) else null
            }}
        }

        for (boardPos in boardsToCheck) {
            val subBoard = boards[boardPos.first][boardPos.second]
            for (cellRow in 0..2) {
                for (cellCol in 0..2) {
                    if (subBoard.cells[cellRow][cellCol].isEmpty()) {
                        moves.add(Pair(boardPos, Pair(cellRow, cellCol)))
                    }
                }
            }
        }

        return moves
    }

    /**
     * Quick preliminary evaluation for move ordering
     */
    private fun preliminaryEval(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        move: Pair<Pair<Int, Int>, Pair<Int, Int>>,
        aiSymbol: Int
    ): Int {
        val (boardPos, cellPos) = move
        val subBoard = boards[boardPos.first][boardPos.second]
        var score = 0

        // Bonus for winning sub-board
        if (wouldWinSubBoard(subBoard, cellPos.first, cellPos.second, aiSymbol)) {
            score += SUB_BOARD_WIN

            // Extra bonus if this threatens meta-win
            if (createsThreat(metaBoard, boardPos.first, boardPos.second, aiSymbol)) {
                score += META_THREAT
            }
        }

        // Bonus for creating local threats
        score += countLocalThreats(subBoard, cellPos.first, cellPos.second, aiSymbol) * LOCAL_THREAT

        // Board control bonus
        val targetBoard = Pair(cellPos.first, cellPos.second)
        if (metaBoard[targetBoard.first][targetBoard.second] != 0) {
            // Sending to finished board gives opponent free choice - can be bad
            score -= 500
        } else {
            // Evaluate where we're sending opponent
            score += evaluateBoardToSend(boards, metaBoard, targetBoard, aiSymbol)
        }

        // Center positions are valuable
        if (cellPos.first == 1 && cellPos.second == 1) score += CENTER_BONUS
        if (boardPos.first == 1 && boardPos.second == 1) score += CENTER_BONUS / 2

        return score
    }

    /**
     * Evaluate board where we send opponent
     */
    private fun evaluateBoardToSend(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        targetBoard: Pair<Int, Int>,
        aiSymbol: Int
    ): Int {
        val oppSymbol = 3 - aiSymbol
        val subBoard = boards[targetBoard.first][targetBoard.second]
        var score = 0

        // Good: send to board where we're winning
        val aiProgress = countProgress(subBoard, aiSymbol)
        val oppProgress = countProgress(subBoard, oppSymbol)

        score += (aiProgress - oppProgress) * 200

        // Bad: send to board where opponent can win sub-board
        if (canWinImmediately(subBoard, oppSymbol)) {
            score -= 3000
        }

        // Good: send to board where opponent can't make progress
        val oppMoves = countEmptyCells(subBoard)
        if (oppMoves <= 2) {
            score += 500 // Limited options for opponent
        }

        return score
    }

    /**
     * Full minimax evaluation of a move
     */
    private fun evaluateMoveWithSearch(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        boardPos: Pair<Int, Int>,
        cellPos: Pair<Int, Int>,
        aiSymbol: Int,
        depth: Int,
        alpha: Int,
        beta: Int,
        startTime: Long,
        timeLimitMs: Long
    ): Int {
        // Make the move
        val newBoards = copyBoards(boards)
        val newMeta = metaBoard.map { it.copyOf() }.toTypedArray()

        newBoards[boardPos.first][boardPos.second] = makeMove(
            newBoards[boardPos.first][boardPos.second],
            cellPos.first, cellPos.second, aiSymbol
        )

        // Check if this won the sub-board
        if (isSubBoardWon(newBoards[boardPos.first][boardPos.second], aiSymbol)) {
            newMeta[boardPos.first][boardPos.second] = aiSymbol
        } else if (isSubBoardFull(newBoards[boardPos.first][boardPos.second])) {
            newMeta[boardPos.first][boardPos.second] = DRAW
        }

        // Check for game win
        if (isMetaWon(newMeta, aiSymbol)) {
            return WIN_SCORE
        }

        // Determine next active board
        val nextActive = if (newMeta[cellPos.first][cellPos.second] == 0) {
            cellPos
        } else {
            null // Free choice
        }

        return minimax(
            newBoards, newMeta, nextActive,
            3 - aiSymbol, aiSymbol, depth - 1,
            alpha, beta, false, startTime, timeLimitMs
        )
    }

    /**
     * Minimax with alpha-beta pruning
     */
    private fun minimax(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        activeBoard: Pair<Int, Int>?,
        currentPlayer: Int,
        aiSymbol: Int,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        startTime: Long,
        timeLimitMs: Long
    ): Int {
        // Time check
        if (System.currentTimeMillis() - startTime > timeLimitMs) {
            return evaluatePosition(boards, metaBoard, aiSymbol)
        }

        // Terminal checks
        if (isMetaWon(metaBoard, aiSymbol)) return WIN_SCORE - (8 - depth) * 1000
        if (isMetaWon(metaBoard, 3 - aiSymbol)) return -WIN_SCORE + (8 - depth) * 1000
        if (isMetaDraw(metaBoard)) return 0

        if (depth == 0) {
            return evaluatePosition(boards, metaBoard, aiSymbol)
        }

        val moves = generateValidMoves(boards, metaBoard, activeBoard, currentPlayer)
        if (moves.isEmpty()) {
            return evaluatePosition(boards, metaBoard, aiSymbol)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves.take(15)) { // Limit branching
                val (boardPos, cellPos) = move

                val newBoards = copyBoards(boards)
                val newMeta = metaBoard.map { it.copyOf() }.toTypedArray()

                newBoards[boardPos.first][boardPos.second] = makeMove(
                    newBoards[boardPos.first][boardPos.second],
                    cellPos.first, cellPos.second, currentPlayer
                )

                updateMetaBoard(newBoards, newMeta, boardPos, currentPlayer)

                val nextActive = getNextActiveBoard(newMeta, cellPos)

                val eval = minimax(
                    newBoards, newMeta, nextActive,
                    3 - currentPlayer, aiSymbol, depth - 1,
                    currentAlpha, currentBeta, false, startTime, timeLimitMs
                )

                maxEval = max(maxEval, eval)
                currentAlpha = max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves.take(15)) {
                val (boardPos, cellPos) = move

                val newBoards = copyBoards(boards)
                val newMeta = metaBoard.map { it.copyOf() }.toTypedArray()

                newBoards[boardPos.first][boardPos.second] = makeMove(
                    newBoards[boardPos.first][boardPos.second],
                    cellPos.first, cellPos.second, currentPlayer
                )

                updateMetaBoard(newBoards, newMeta, boardPos, currentPlayer)

                val nextActive = getNextActiveBoard(newMeta, cellPos)

                val eval = minimax(
                    newBoards, newMeta, nextActive,
                    3 - currentPlayer, aiSymbol, depth - 1,
                    currentAlpha, currentBeta, true, startTime, timeLimitMs
                )

                minEval = min(minEval, eval)
                currentBeta = min(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    /**
     * Evaluate full position
     */
    private fun evaluatePosition(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        aiSymbol: Int
    ): Int {
        val oppSymbol = 3 - aiSymbol
        var score = 0

        // Meta-board evaluation (most important)
        score += evaluateMetaBoard(metaBoard, aiSymbol) * 100

        // Sub-board evaluations
        for (row in 0..2) {
            for (col in 0..2) {
                if (metaBoard[row][col] == 0) {
                    val subScore = evaluateSubBoard(boards[row][col], aiSymbol)
                    // Weight by position importance
                    val posWeight = if (row == 1 && col == 1) 1.5f else 1.0f
                    score += (subScore * posWeight).toInt()
                }
            }
        }

        return score
    }

    /**
     * Evaluate meta-board
     */
    private fun evaluateMetaBoard(metaBoard: Array<IntArray>, aiSymbol: Int): Int {
        val oppSymbol = 3 - aiSymbol
        var score = 0

        // Count controlled boards
        var aiBoards = 0
        var oppBoards = 0
        for (row in 0..2) {
            for (col in 0..2) {
                when (metaBoard[row][col]) {
                    aiSymbol -> {
                        aiBoards++
                        // Center is most valuable
                        score += if (row == 1 && col == 1) 500 else 300
                        // Corners next
                        if ((row == 0 || row == 2) && (col == 0 || col == 2)) score += 50
                    }
                    oppSymbol -> {
                        oppBoards++
                        score -= if (row == 1 && col == 1) 500 else 300
                        if ((row == 0 || row == 2) && (col == 0 || col == 2)) score -= 50
                    }
                }
            }
        }

        // Two-in-a-row on meta-board (threats)
        score += countMetaThreats(metaBoard, aiSymbol) * 1000
        score -= countMetaThreats(metaBoard, oppSymbol) * 1000

        return score
    }

    /**
     * Count meta-board threats (2 in a row with empty third)
     */
    private fun countMetaThreats(metaBoard: Array<IntArray>, symbol: Int): Int {
        var threats = 0
        val lines = listOf(
            listOf(Pair(0,0), Pair(0,1), Pair(0,2)),
            listOf(Pair(1,0), Pair(1,1), Pair(1,2)),
            listOf(Pair(2,0), Pair(2,1), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,0), Pair(2,0)),
            listOf(Pair(0,1), Pair(1,1), Pair(2,1)),
            listOf(Pair(0,2), Pair(1,2), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,1), Pair(2,2)),
            listOf(Pair(0,2), Pair(1,1), Pair(2,0))
        )

        for (line in lines) {
            val values = line.map { metaBoard[it.first][it.second] }
            val myCount = values.count { it == symbol }
            val emptyCount = values.count { it == 0 }
            if (myCount == 2 && emptyCount == 1) threats++
        }

        return threats
    }

    /**
     * Evaluate a sub-board
     */
    private fun evaluateSubBoard(board: TicTacToeBoard, aiSymbol: Int): Int {
        val oppSymbol = 3 - aiSymbol
        var score = 0

        // Use perfect 3x3 evaluation
        for (row in 0..2) {
            for (col in 0..2) {
                val cell = board.cells[row][col]
                if (cell.value == aiSymbol) {
                    score += getPositionValue(row, col)
                } else if (cell.value == oppSymbol) {
                    score -= getPositionValue(row, col)
                }
            }
        }

        // Two-in-a-row threats
        score += countLocalThreats(board, 1, 1, aiSymbol) * 200 // Dummy pos
        score -= countLocalThreats(board, 1, 1, oppSymbol) * 200

        return score
    }

    private fun getPositionValue(row: Int, col: Int): Int {
        return when {
            row == 1 && col == 1 -> 30  // Center
            (row == 0 || row == 2) && (col == 0 || col == 2) -> 20  // Corner
            else -> 10  // Edge
        }
    }

    // Helper functions

    private fun wouldWinSubBoard(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        // Check all lines through this position
        val lines = mutableListOf<List<Pair<Int, Int>>>()

        // Row
        lines.add((0..2).map { Pair(row, it) })
        // Column
        lines.add((0..2).map { Pair(it, col) })
        // Diagonals if applicable
        if (row == col) lines.add((0..2).map { Pair(it, it) })
        if (row + col == 2) lines.add((0..2).map { Pair(it, 2 - it) })

        for (line in lines) {
            var count = 0
            for ((r, c) in line) {
                if (r == row && c == col) count++
                else if (board.cells[r][c].value == symbol) count++
            }
            if (count == 3) return true
        }
        return false
    }

    private fun wouldWinMeta(metaBoard: Array<IntArray>, row: Int, col: Int, symbol: Int): Boolean {
        val lines = mutableListOf<List<Pair<Int, Int>>>()
        lines.add((0..2).map { Pair(row, it) })
        lines.add((0..2).map { Pair(it, col) })
        if (row == col) lines.add((0..2).map { Pair(it, it) })
        if (row + col == 2) lines.add((0..2).map { Pair(it, 2 - it) })

        for (line in lines) {
            var count = 0
            for ((r, c) in line) {
                if (r == row && c == col) count++
                else if (metaBoard[r][c] == symbol) count++
            }
            if (count == 3) return true
        }
        return false
    }

    private fun createsThreat(metaBoard: Array<IntArray>, row: Int, col: Int, symbol: Int): Boolean {
        val lines = mutableListOf<List<Pair<Int, Int>>>()
        lines.add((0..2).map { Pair(row, it) })
        lines.add((0..2).map { Pair(it, col) })
        if (row == col) lines.add((0..2).map { Pair(it, it) })
        if (row + col == 2) lines.add((0..2).map { Pair(it, 2 - it) })

        for (line in lines) {
            var myCount = 0
            var emptyCount = 0
            for ((r, c) in line) {
                if (r == row && c == col) myCount++
                else if (metaBoard[r][c] == symbol) myCount++
                else if (metaBoard[r][c] == 0) emptyCount++
            }
            if (myCount == 2 && emptyCount == 1) return true
        }
        return false
    }

    private fun countLocalThreats(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Int {
        var threats = 0
        val lines = listOf(
            listOf(Pair(0,0), Pair(0,1), Pair(0,2)),
            listOf(Pair(1,0), Pair(1,1), Pair(1,2)),
            listOf(Pair(2,0), Pair(2,1), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,0), Pair(2,0)),
            listOf(Pair(0,1), Pair(1,1), Pair(2,1)),
            listOf(Pair(0,2), Pair(1,2), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,1), Pair(2,2)),
            listOf(Pair(0,2), Pair(1,1), Pair(2,0))
        )

        for (line in lines) {
            var myCount = 0
            var emptyCount = 0
            for ((r, c) in line) {
                if (board.cells[r][c].value == symbol) myCount++
                else if (board.cells[r][c].isEmpty()) emptyCount++
            }
            if (myCount == 2 && emptyCount == 1) threats++
        }

        return threats
    }

    private fun countProgress(board: TicTacToeBoard, symbol: Int): Int {
        var progress = 0
        for (row in 0..2) {
            for (col in 0..2) {
                if (board.cells[row][col].value == symbol) progress++
            }
        }
        return progress
    }

    private fun canWinImmediately(board: TicTacToeBoard, symbol: Int): Boolean {
        for (row in 0..2) {
            for (col in 0..2) {
                if (board.cells[row][col].isEmpty() &&
                    wouldWinSubBoard(board, row, col, symbol)) {
                    return true
                }
            }
        }
        return false
    }

    private fun countEmptyCells(board: TicTacToeBoard): Int {
        return board.cells.flatten().count { it.isEmpty() }
    }

    private fun isSubBoardWon(board: TicTacToeBoard, symbol: Int): Boolean {
        val lines = listOf(
            listOf(Pair(0,0), Pair(0,1), Pair(0,2)),
            listOf(Pair(1,0), Pair(1,1), Pair(1,2)),
            listOf(Pair(2,0), Pair(2,1), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,0), Pair(2,0)),
            listOf(Pair(0,1), Pair(1,1), Pair(2,1)),
            listOf(Pair(0,2), Pair(1,2), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,1), Pair(2,2)),
            listOf(Pair(0,2), Pair(1,1), Pair(2,0))
        )

        return lines.any { line -> line.all { board.cells[it.first][it.second].value == symbol }}
    }

    private fun isSubBoardFull(board: TicTacToeBoard): Boolean {
        return board.cells.flatten().none { it.isEmpty() }
    }

    private fun isMetaWon(metaBoard: Array<IntArray>, symbol: Int): Boolean {
        val lines = listOf(
            listOf(Pair(0,0), Pair(0,1), Pair(0,2)),
            listOf(Pair(1,0), Pair(1,1), Pair(1,2)),
            listOf(Pair(2,0), Pair(2,1), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,0), Pair(2,0)),
            listOf(Pair(0,1), Pair(1,1), Pair(2,1)),
            listOf(Pair(0,2), Pair(1,2), Pair(2,2)),
            listOf(Pair(0,0), Pair(1,1), Pair(2,2)),
            listOf(Pair(0,2), Pair(1,1), Pair(2,0))
        )

        return lines.any { line -> line.all { metaBoard[it.first][it.second] == symbol }}
    }

    private fun isMetaDraw(metaBoard: Array<IntArray>): Boolean {
        if (metaBoard.flatten().any { it == 0 }) return false
        return !isMetaWon(metaBoard, 1) && !isMetaWon(metaBoard, 2)
    }

    private fun copyBoards(boards: Array<Array<TicTacToeBoard>>): Array<Array<TicTacToeBoard>> {
        return Array(3) { row ->
            Array(3) { col ->
                boards[row][col].copy(
                    cells = boards[row][col].cells.map { rowCells ->
                        rowCells.map { it.copy() }
                    }
                )
            }
        }
    }

    private fun makeMove(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): TicTacToeBoard {
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(value = symbol) else cell
            }
        }
        return board.copy(cells = newCells)
    }

    private fun updateMetaBoard(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        boardPos: Pair<Int, Int>,
        player: Int
    ) {
        val subBoard = boards[boardPos.first][boardPos.second]
        if (isSubBoardWon(subBoard, player)) {
            metaBoard[boardPos.first][boardPos.second] = player
        } else if (isSubBoardFull(subBoard)) {
            metaBoard[boardPos.first][boardPos.second] = DRAW
        }
    }

    private fun getNextActiveBoard(metaBoard: Array<IntArray>, cellPos: Pair<Int, Int>): Pair<Int, Int>? {
        return if (metaBoard[cellPos.first][cellPos.second] == 0) {
            cellPos
        } else {
            null
        }
    }

    private fun getMoveReason(
        boards: Array<Array<TicTacToeBoard>>,
        metaBoard: Array<IntArray>,
        move: Pair<Pair<Int, Int>, Pair<Int, Int>>,
        aiSymbol: Int
    ): String {
        val (boardPos, cellPos) = move
        val subBoard = boards[boardPos.first][boardPos.second]

        return when {
            wouldWinSubBoard(subBoard, cellPos.first, cellPos.second, aiSymbol) -> {
                if (wouldWinMeta(metaBoard, boardPos.first, boardPos.second, aiSymbol)) {
                    "Spielgewinn!"
                } else if (createsThreat(metaBoard, boardPos.first, boardPos.second, aiSymbol)) {
                    "Gewinne Feld und schaffe Drohung!"
                } else {
                    "Gewinne dieses Feld"
                }
            }
            wouldWinSubBoard(subBoard, cellPos.first, cellPos.second, 3 - aiSymbol) ->
                "Verhindere Feldverlust"
            cellPos.first == 1 && cellPos.second == 1 ->
                "Kontrolliere Zentrum"
            metaBoard[cellPos.first][cellPos.second] != 0 ->
                "Sende Gegner zu beendetem Feld"
            else -> "Strategischer Zug"
        }
    }

    private fun Array<IntArray>.flatten(): List<Int> = this.flatMap { it.toList() }
}
