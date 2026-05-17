package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.*

object DameAI {

    fun getBestMove(board: DameBoard, playerColor: DamePlayerColor, strength: Int): DameMove? {
        val depth = strengthToDepth(strength)
        val validMoves = DameLogic.getValidMoves(board, playerColor)
        if (validMoves.isEmpty()) return null

        val scoredMoves = validMoves.map { move ->
            val newBoard = DameLogic.executeMove(board, move)
            val opponentColor = if (playerColor == DamePlayerColor.WHITE) DamePlayerColor.BLACK else DamePlayerColor.WHITE
            val score = minimax(
                board = newBoard,
                depth = depth - 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                isMaximizing = false,
                maximizingColor = playerColor,
                currentColor = opponentColor
            )
            move to score
        }.sortedByDescending { it.second }

        if (strength <= 20) {
            val topMoves = scoredMoves.take(minOf(3, scoredMoves.size))
            return topMoves.random().first
        }

        return scoredMoves.first().first
    }

    private fun evaluateBoard(board: DameBoard, maximizingColor: DamePlayerColor): Int {
        val opponentColor = if (maximizingColor == DamePlayerColor.WHITE) DamePlayerColor.BLACK else DamePlayerColor.WHITE

        val ownMoves = DameLogic.getValidMoves(board, maximizingColor)
        val opponentMoves = DameLogic.getValidMoves(board, opponentColor)
        if (opponentMoves.isEmpty()) return 1000
        if (ownMoves.isEmpty()) return -1000

        var score = 0

        for (row in board.cells.indices) {
            for (col in board.cells[row].indices) {
                val cell = board.cells[row][col]
                if (cell.pieceType == DamePieceType.NONE.name) continue

                val isOwn = cell.owner == maximizingColor.name
                val sign = if (isOwn) 1 else -1
                val isKing = cell.pieceType == DamePieceType.KING.name

                score += sign * if (isKing) 250 else 100

                if (isOwn) {
                    if (row in 3..4 && col in 2..5) score += 10
                    if (col == 0 || col == 7 || row == 0 || row == 7) score += 5
                    if (!isKing) {
                        val promotionRow = if (maximizingColor == DamePlayerColor.WHITE) 0 else 7
                        val distance = kotlin.math.abs(row - promotionRow)
                        if (distance <= 2) score += 30
                    }
                }
            }
        }

        return score
    }

    private fun minimax(
        board: DameBoard,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        maximizingColor: DamePlayerColor,
        currentColor: DamePlayerColor
    ): Int {
        val winner = DameLogic.checkWinner(board, currentColor)
        if (winner != null) {
            return if (winner == maximizingColor) 1000 else -1000
        }
        if (depth == 0) {
            return evaluateBoard(board, maximizingColor)
        }

        val validMoves = DameLogic.getValidMoves(board, currentColor)
        if (validMoves.isEmpty()) {
            return if (isMaximizing) -1000 else 1000
        }

        val nextColor = if (currentColor == DamePlayerColor.WHITE) DamePlayerColor.BLACK else DamePlayerColor.WHITE
        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in validMoves) {
                val newBoard = DameLogic.executeMove(board, move)
                val eval = minimax(newBoard, depth - 1, currentAlpha, currentBeta, false, maximizingColor, nextColor)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in validMoves) {
                val newBoard = DameLogic.executeMove(board, move)
                val eval = minimax(newBoard, depth - 1, currentAlpha, currentBeta, true, maximizingColor, nextColor)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun strengthToDepth(strength: Int): Int {
        return when {
            strength <= 20 -> 1
            strength <= 40 -> 2
            strength <= 60 -> 3
            strength <= 80 -> 5
            else -> 7
        }
    }
}
