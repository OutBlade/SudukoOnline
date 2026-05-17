package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.*

object DameLogic {

    private const val BOARD_SIZE = 8

    private fun getCell(board: DameBoard, row: Int, col: Int): DameCell = board.cells[row][col]

    private fun hasPiece(cell: DameCell): Boolean = cell.pieceType != DamePieceType.NONE.name

    private fun isOwnedBy(cell: DameCell, color: DamePlayerColor): Boolean = cell.owner == color.name

    private fun updateCell(board: DameBoard, row: Int, col: Int, cell: DameCell): DameBoard {
        val newCells = board.cells.mapIndexed { r, rowCells ->
            if (r == row) rowCells.mapIndexed { c, existing -> if (c == col) cell else existing }
            else rowCells
        }
        return DameBoard(newCells)
    }

    private fun clearCell(board: DameBoard, row: Int, col: Int): DameBoard {
        return updateCell(board, row, col, DameCell(row = row, col = col))
    }

    fun getValidMoves(board: DameBoard, playerColor: DamePlayerColor): List<DameMove> {
        val allCaptures = mutableListOf<DameMove>()
        val allSimpleMoves = mutableListOf<DameMove>()

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val cell = getCell(board, row, col)
                if (!hasPiece(cell) || !isOwnedBy(cell, playerColor)) continue

                val pt = DamePieceType.valueOf(cell.pieceType)
                val captures = getCapturesForPiece(board, row, col, pt, playerColor)
                if (captures.isNotEmpty()) {
                    allCaptures.addAll(captures)
                } else {
                    allSimpleMoves.addAll(getSimpleMovesForPiece(board, row, col, pt, playerColor))
                }
            }
        }

        return if (allCaptures.isNotEmpty()) allCaptures else allSimpleMoves
    }

    fun getMovesForPiece(board: DameBoard, row: Int, col: Int): List<DameMove> {
        val cell = getCell(board, row, col)
        if (!hasPiece(cell)) return emptyList()
        val playerColor = DamePlayerColor.valueOf(cell.owner)
        val pt = DamePieceType.valueOf(cell.pieceType)

        // Check if any piece of this player can capture (mandatory capture rule)
        val anyCapture = (0 until BOARD_SIZE).any { r ->
            (0 until BOARD_SIZE).any { c ->
                val cc = getCell(board, r, c)
                hasPiece(cc) && isOwnedBy(cc, playerColor) &&
                        getCapturesForPiece(board, r, c, DamePieceType.valueOf(cc.pieceType), playerColor).isNotEmpty()
            }
        }

        val captures = getCapturesForPiece(board, row, col, pt, playerColor)
        if (anyCapture) return captures

        return if (captures.isNotEmpty()) captures
        else getSimpleMovesForPiece(board, row, col, pt, playerColor)
    }

    fun getCapturesForPiece(
        board: DameBoard,
        row: Int,
        col: Int,
        pieceType: DamePieceType,
        owner: DamePlayerColor
    ): List<DameMove> {
        val singleCaptures = getCaptureMoves(board, row, col, pieceType, owner)
        if (singleCaptures.isEmpty()) return emptyList()

        val result = mutableListOf<DameMove>()

        for (capture in singleCaptures) {
            val landRow = capture.toRow
            val landCol = capture.toCol

            val tempBoard = applySingleCapture(board, row, col, landRow, landCol, capture.captures, pieceType, owner)

            val landedType = if (pieceType == DamePieceType.MAN && isPromotionRow(landRow, owner)) {
                DamePieceType.KING
            } else {
                pieceType
            }

            val continuing = getContinuingCaptures(tempBoard, landRow, landCol, capture.captures, landedType, owner)

            if (continuing.isEmpty()) {
                result.add(DameMove(fromRow = row, fromCol = col, toRow = landRow, toCol = landCol, captures = capture.captures))
            } else {
                for (cont in continuing) {
                    result.add(DameMove(fromRow = row, fromCol = col, toRow = cont.toRow, toCol = cont.toCol, captures = capture.captures + cont.captures))
                }
            }
        }

        return result
    }

    fun executeMove(board: DameBoard, move: DameMove): DameBoard {
        val cell = getCell(board, move.fromRow, move.fromCol)
        if (!hasPiece(cell)) return board

        var newBoard = clearCell(board, move.fromRow, move.fromCol)

        for ((capRow, capCol) in move.captures) {
            newBoard = clearCell(newBoard, capRow, capCol)
        }

        val owner = DamePlayerColor.valueOf(cell.owner)
        val currentType = DamePieceType.valueOf(cell.pieceType)
        val newType = if (currentType == DamePieceType.MAN && isPromotionRow(move.toRow, owner)) {
            DamePieceType.KING
        } else {
            currentType
        }

        newBoard = updateCell(newBoard, move.toRow, move.toCol, DameCell(
            row = move.toRow,
            col = move.toCol,
            pieceType = newType.name,
            owner = cell.owner
        ))

        return newBoard
    }

    fun checkWinner(board: DameBoard, currentPlayerColor: DamePlayerColor): DamePlayerColor? {
        val hasPieces = board.cells.flatten().any { hasPiece(it) && isOwnedBy(it, currentPlayerColor) }

        if (!hasPieces) {
            return if (currentPlayerColor == DamePlayerColor.WHITE) DamePlayerColor.BLACK else DamePlayerColor.WHITE
        }

        val hasMoves = getValidMoves(board, currentPlayerColor).isNotEmpty()
        if (!hasMoves) {
            return if (currentPlayerColor == DamePlayerColor.WHITE) DamePlayerColor.BLACK else DamePlayerColor.WHITE
        }

        return null
    }

    fun isDraw(board: DameBoard, moveHistory: List<DameMove>): Boolean {
        if (moveHistory.size < 40) return false

        val allPieces = board.cells.flatten().filter { hasPiece(it) }
        val allKings = allPieces.all { it.pieceType == DamePieceType.KING.name }
        if (!allKings) return false

        val last40 = moveHistory.takeLast(40)
        return last40.all { it.captures.isEmpty() }
    }

    private fun getCaptureMoves(
        board: DameBoard,
        row: Int,
        col: Int,
        pieceType: DamePieceType,
        owner: DamePlayerColor
    ): List<DameMove> {
        val directions = getCaptureDirections(pieceType, owner)
        val result = mutableListOf<DameMove>()

        for ((dr, dc) in directions) {
            val midRow = row + dr
            val midCol = col + dc
            val landRow = row + 2 * dr
            val landCol = col + 2 * dc

            if (!inBounds(landRow, landCol) || !inBounds(midRow, midCol)) continue

            val midCell = getCell(board, midRow, midCol)
            if (!hasPiece(midCell) || isOwnedBy(midCell, owner)) continue

            val landCell = getCell(board, landRow, landCol)
            if (hasPiece(landCell)) continue

            result.add(DameMove(fromRow = row, fromCol = col, toRow = landRow, toCol = landCol, captures = listOf(Pair(midRow, midCol))))
        }

        return result
    }

    fun getContinuingCaptures(
        board: DameBoard,
        row: Int,
        col: Int,
        alreadyCaptured: List<Pair<Int, Int>>,
        pieceType: DamePieceType,
        owner: DamePlayerColor
    ): List<DameMove> {
        val directions = getCaptureDirections(pieceType, owner)
        val result = mutableListOf<DameMove>()

        for ((dr, dc) in directions) {
            val midRow = row + dr
            val midCol = col + dc
            val landRow = row + 2 * dr
            val landCol = col + 2 * dc

            if (!inBounds(landRow, landCol) || !inBounds(midRow, midCol)) continue
            if (alreadyCaptured.contains(Pair(midRow, midCol))) continue

            val midCell = getCell(board, midRow, midCol)
            if (!hasPiece(midCell) || isOwnedBy(midCell, owner)) continue

            val landCell = getCell(board, landRow, landCol)
            if (hasPiece(landCell)) continue

            val newCaptured = alreadyCaptured + Pair(midRow, midCol)
            val tempBoard = applySingleCapture(board, row, col, landRow, landCol, listOf(Pair(midRow, midCol)), pieceType, owner)

            val further = getContinuingCaptures(tempBoard, landRow, landCol, newCaptured, pieceType, owner)

            if (further.isEmpty()) {
                result.add(DameMove(fromRow = row, fromCol = col, toRow = landRow, toCol = landCol, captures = listOf(Pair(midRow, midCol))))
            } else {
                for (cont in further) {
                    result.add(DameMove(fromRow = row, fromCol = col, toRow = cont.toRow, toCol = cont.toCol, captures = listOf(Pair(midRow, midCol)) + cont.captures))
                }
            }
        }

        return result
    }

    private fun getSimpleMovesForPiece(
        board: DameBoard,
        row: Int,
        col: Int,
        pieceType: DamePieceType,
        owner: DamePlayerColor
    ): List<DameMove> {
        val directions = getMoveDirections(pieceType, owner)
        val result = mutableListOf<DameMove>()

        for ((dr, dc) in directions) {
            val newRow = row + dr
            val newCol = col + dc
            if (!inBounds(newRow, newCol)) continue
            if (hasPiece(getCell(board, newRow, newCol))) continue

            result.add(DameMove(fromRow = row, fromCol = col, toRow = newRow, toCol = newCol, captures = emptyList()))
        }

        return result
    }

    private fun getMoveDirections(pieceType: DamePieceType, owner: DamePlayerColor): List<Pair<Int, Int>> {
        return if (pieceType == DamePieceType.KING) {
            listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        } else {
            val forward = if (owner == DamePlayerColor.WHITE) -1 else 1
            listOf(Pair(forward, -1), Pair(forward, 1))
        }
    }

    private fun getCaptureDirections(pieceType: DamePieceType, owner: DamePlayerColor): List<Pair<Int, Int>> {
        return if (pieceType == DamePieceType.KING) {
            listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        } else {
            val forward = if (owner == DamePlayerColor.WHITE) -1 else 1
            listOf(Pair(forward, -1), Pair(forward, 1))
        }
    }

    private fun inBounds(row: Int, col: Int): Boolean = row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE

    private fun isPromotionRow(row: Int, owner: DamePlayerColor): Boolean {
        return (owner == DamePlayerColor.WHITE && row == 0) ||
                (owner == DamePlayerColor.BLACK && row == BOARD_SIZE - 1)
    }

    private fun applySingleCapture(
        board: DameBoard,
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int,
        captured: List<Pair<Int, Int>>,
        pieceType: DamePieceType,
        owner: DamePlayerColor
    ): DameBoard {
        var newBoard = clearCell(board, fromRow, fromCol)
        for ((cr, cc) in captured) {
            newBoard = clearCell(newBoard, cr, cc)
        }
        newBoard = updateCell(newBoard, toRow, toCol, DameCell(
            row = toRow,
            col = toCol,
            pieceType = pieceType.name,
            owner = owner.name
        ))
        return newBoard
    }
}
