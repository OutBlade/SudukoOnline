package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.DameRepository
import de.sudokuonline.app.game.DameLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DameGameState(
    val room: DameRoom? = null,
    val playerId: String = "",
    val playerName: String = "",
    val isMyTurn: Boolean = false,
    val myColor: DamePlayerColor = DamePlayerColor.WHITE,
    val selectedPiece: Pair<Int, Int>? = null,
    val validMoves: List<DameMove> = emptyList(),
    val allMyMoves: List<DameMove> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isComplete: Boolean = false,
    val winnerId: String? = null,
    val isDraw: Boolean = false,
    val showGameOver: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val rematchStatus: String = RematchStatus.NONE.name,
    val rematchRequestedBy: String? = null
)

class DameViewModel : ViewModel() {
    private val repository = DameRepository()
    private val _state = MutableStateFlow(DameGameState())
    val state: StateFlow<DameGameState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var timerJob: Job? = null

    fun observeRoom(roomId: String, playerId: String, playerName: String) {
        _state.value = _state.value.copy(playerId = playerId, playerName = playerName)

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeRoom(roomId).collect { room ->
                if (room != null) {
                    handleRoomUpdate(room)
                }
            }
        }
    }

    private fun handleRoomUpdate(room: DameRoom) {
        val playerId = _state.value.playerId
        val myColor = try {
            DamePlayerColor.valueOf(room.players[playerId]?.playerColor ?: DamePlayerColor.WHITE.name)
        } catch (e: Exception) { DamePlayerColor.WHITE }

        val isMyTurn = room.currentTurnPlayerId == playerId
        val isComplete = room.status == RoomStatus.FINISHED.name

        // Calculate valid moves if it's my turn
        val allMoves = if (isMyTurn && !isComplete) {
            if (room.mustContinueJump && room.continuingPieceRow >= 0) {
                val r = room.continuingPieceRow
                val c = room.continuingPieceCol
                val pt = DamePieceType.valueOf(room.board.cells[r][c].pieceType)
                DameLogic.getCapturesForPiece(room.board, r, c, pt, myColor)
            } else {
                DameLogic.getValidMoves(room.board, myColor)
            }
        } else emptyList()

        val selectedPiece = if (isMyTurn && room.mustContinueJump && room.continuingPieceRow >= 0) {
            Pair(room.continuingPieceRow, room.continuingPieceCol)
        } else null

        val validMoves = if (selectedPiece != null) {
            allMoves.filter { it.fromRow == selectedPiece.first && it.fromCol == selectedPiece.second }
        } else emptyList()

        _state.value = _state.value.copy(
            room = room,
            isMyTurn = isMyTurn,
            myColor = myColor,
            selectedPiece = selectedPiece,
            validMoves = validMoves,
            allMyMoves = allMoves,
            isComplete = isComplete,
            winnerId = room.winnerId,
            isDraw = room.isDraw,
            showGameOver = isComplete && (room.winnerId != null || room.isDraw),
            rematchStatus = room.rematchStatus,
            rematchRequestedBy = room.rematchRequestedBy
        )

        // Start timer
        if (room.status == RoomStatus.IN_PROGRESS.name && timerJob == null) {
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _state.value = _state.value.copy(elapsedSeconds = _state.value.elapsedSeconds + 1)
                }
            }
        }
        if (isComplete) {
            timerJob?.cancel()
            timerJob = null
        }
    }

    fun startGame() {
        val room = _state.value.room ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.startGame(room.id).onFailure {
                _state.value = _state.value.copy(error = it.message, isLoading = false)
            }.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun onCellClick(row: Int, col: Int) {
        val s = _state.value
        val room = s.room ?: return
        if (!s.isMyTurn || s.isComplete) return

        val board = room.board
        val cell = board.cells[row][col]
        val isOwnPiece = cell.owner == s.myColor.name

        if (s.selectedPiece != null) {
            // Try execute move
            val move = s.validMoves.firstOrNull { it.toRow == row && it.toCol == col }
            if (move != null) {
                executeMove(move)
                return
            }
            // Select different own piece (if not forced to continue)
            if (isOwnPiece && !room.mustContinueJump) {
                val pieceMoves = s.allMyMoves.filter { it.fromRow == row && it.fromCol == col }
                if (pieceMoves.isNotEmpty()) {
                    _state.value = s.copy(selectedPiece = Pair(row, col), validMoves = pieceMoves)
                }
                return
            }
            // Deselect
            if (row == s.selectedPiece.first && col == s.selectedPiece.second && !room.mustContinueJump) {
                _state.value = s.copy(selectedPiece = null, validMoves = emptyList())
                return
            }
        } else {
            // Select piece
            if (isOwnPiece) {
                val pieceMoves = s.allMyMoves.filter { it.fromRow == row && it.fromCol == col }
                if (pieceMoves.isNotEmpty()) {
                    _state.value = s.copy(selectedPiece = Pair(row, col), validMoves = pieceMoves)
                }
            }
        }
    }

    private fun executeMove(move: DameMove) {
        val s = _state.value
        val room = s.room ?: return

        viewModelScope.launch {
            val newBoard = DameLogic.executeMove(room.board, move)

            // Check multi-jump
            var mustContinue = false
            var contRow = -1
            var contCol = -1
            if (move.captures.isNotEmpty()) {
                val pt = DamePieceType.valueOf(newBoard.cells[move.toRow][move.toCol].pieceType)
                val further = DameLogic.getCapturesForPiece(newBoard, move.toRow, move.toCol, pt, s.myColor)
                if (further.isNotEmpty()) {
                    mustContinue = true
                    contRow = move.toRow
                    contCol = move.toCol
                }
            }

            val opponentColor = if (s.myColor == DamePlayerColor.WHITE) DamePlayerColor.BLACK else DamePlayerColor.WHITE
            val nextPlayerId: String
            val nextColor: DamePlayerColor
            if (mustContinue) {
                nextPlayerId = s.playerId
                nextColor = s.myColor
            } else {
                nextPlayerId = room.players.keys.firstOrNull { it != s.playerId } ?: s.playerId
                nextColor = opponentColor
            }

            val whitePieces = newBoard.cells.flatten().count { it.owner == DamePlayerColor.WHITE.name }
            val blackPieces = newBoard.cells.flatten().count { it.owner == DamePlayerColor.BLACK.name }

            repository.makeMove(
                roomId = room.id,
                newBoard = newBoard,
                nextPlayerId = nextPlayerId,
                nextColor = nextColor,
                mustContinueJump = mustContinue,
                continuingRow = contRow,
                continuingCol = contCol,
                whitePieces = whitePieces,
                blackPieces = blackPieces
            )

            // Check winner
            if (!mustContinue) {
                val winner = DameLogic.checkWinner(newBoard, opponentColor)
                if (winner != null) {
                    val winnerId = if (winner == s.myColor) s.playerId
                    else room.players.keys.firstOrNull { it != s.playerId }
                    repository.endGame(room.id, winnerId, isDraw = false)
                }
            }
        }
    }

    fun leaveRoom() {
        val s = _state.value
        val room = s.room ?: return
        viewModelScope.launch {
            repository.leaveRoom(room.id, s.playerId)
        }
        observeJob?.cancel()
        timerJob?.cancel()
    }

    fun requestRematch() {
        val room = _state.value.room ?: return
        viewModelScope.launch { repository.requestRematch(room.id, _state.value.playerId) }
    }

    fun acceptRematch() {
        val room = _state.value.room ?: return
        viewModelScope.launch {
            repository.acceptRematch(room.id, _state.value.playerId)
            _state.value = _state.value.copy(elapsedSeconds = 0, showGameOver = false)
        }
    }

    fun declineRematch() {
        val room = _state.value.room ?: return
        viewModelScope.launch { repository.declineRematch(room.id) }
    }

    fun didIRequestRematch(): Boolean {
        return _state.value.rematchRequestedBy == _state.value.playerId
    }

    fun dismissGameOver() {
        _state.value = _state.value.copy(showGameOver = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
        timerJob?.cancel()
    }
}
