package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.TicTacToeRepository
import de.sudokuonline.app.game.TicTacToeLogic
import de.sudokuonline.app.game.UltimateTicTacToeLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Game action mode - either placing a symbol or a bomb
 */
enum class TicTacToeActionMode {
    SYMBOL,  // Normal move - place X or O
    BOMB     // Place a bomb
}

/**
 * State for TicTacToe game
 */
data class TicTacToeGameState(
    // Board state
    val board: TicTacToeBoard = TicTacToeBoard(),
    val winningLine: List<Pair<Int, Int>>? = null,

    // Ultimate TicTacToe specific
    val ultimateBoard: UltimateTicTacToeBoard = UltimateTicTacToeBoard(),
    val playableBoards: List<Pair<Int, Int>> = emptyList(),

    // Game settings
    val gameMode: TicTacToeGameMode = TicTacToeGameMode.CLASSIC,
    val boardSize: TicTacToeBoardSize = TicTacToeBoardSize.SMALL,

    // Player info
    val playerId: String = "",
    val playerName: String = "",
    val mySymbol: Int = 0,  // 1=X, 2=O
    val bombsRemaining: Int = 3,

    // Turn info
    val isMyTurn: Boolean = false,
    val currentTurnSymbol: Int = 1,  // X always starts

    // Action mode
    val actionMode: TicTacToeActionMode = TicTacToeActionMode.SYMBOL,

    // Game status
    val elapsedSeconds: Int = 0,
    val isComplete: Boolean = false,
    val winnerId: String? = null,
    val isDraw: Boolean = false,
    val showGameOver: Boolean = false,

    // Multiplayer
    val room: TicTacToeRoom? = null,
    val opponentName: String = "",
    val opponentBombsRemaining: Int = 3,

    // Rematch system
    val rematchStatus: RematchStatus = RematchStatus.NONE,
    val rematchRequestedBy: String? = null,
    val mySeriesWins: Int = 0,
    val opponentSeriesWins: Int = 0,
    val roundNumber: Int = 1,

    // Loading states
    val isLoading: Boolean = false,
    val error: String? = null
)

class TicTacToeViewModel : ViewModel() {
    private val repository = TicTacToeRepository()

    private val _state = MutableStateFlow(TicTacToeGameState())
    val state: StateFlow<TicTacToeGameState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var roomObserverJob: Job? = null

    /**
     * Create a new multiplayer room
     */
    fun createRoom(
        playerId: String,
        playerName: String,
        gameMode: TicTacToeGameMode,
        boardSize: TicTacToeBoardSize,
        isPrivate: Boolean
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = repository.createRoom(
                hostId = playerId,
                hostName = playerName,
                gameMode = gameMode,
                boardSize = boardSize,
                isPrivate = isPrivate
            )

            result.onSuccess { room ->
                _state.value = _state.value.copy(
                    room = room,
                    board = room.board,
                    playerId = playerId,
                    playerName = playerName,
                    mySymbol = 1, // Host is always X
                    gameMode = room.getGameModeEnum(),
                    boardSize = room.getBoardSizeEnum(),
                    bombsRemaining = if (gameMode != TicTacToeGameMode.CLASSIC) 3 else 0,
                    isLoading = false
                )
                observeRoom(room.id)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Fehler beim Erstellen des Raums"
                )
            }
        }
    }

    /**
     * Join a room by code
     */
    fun joinRoomByCode(code: String, playerId: String, playerName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = repository.joinRoomByCode(
                code = code.uppercase(),
                playerId = playerId,
                playerName = playerName
            )

            result.onSuccess { room ->
                val gameMode = room.getGameModeEnum()
                _state.value = _state.value.copy(
                    room = room,
                    board = room.board,
                    playerId = playerId,
                    playerName = playerName,
                    mySymbol = 2, // Joiner is always O
                    gameMode = gameMode,
                    boardSize = room.getBoardSizeEnum(),
                    bombsRemaining = if (gameMode != TicTacToeGameMode.CLASSIC) 3 else 0,
                    isLoading = false
                )
                observeRoom(room.id)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Fehler beim Beitreten"
                )
            }
        }
    }

    /**
     * Join a room by ID
     */
    fun joinRoom(roomId: String, playerId: String, playerName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = repository.joinRoom(
                roomId = roomId,
                playerId = playerId,
                playerName = playerName
            )

            result.onSuccess { room ->
                val gameMode = room.getGameModeEnum()
                _state.value = _state.value.copy(
                    room = room,
                    board = room.board,
                    playerId = playerId,
                    playerName = playerName,
                    mySymbol = 2, // Joiner is always O
                    gameMode = gameMode,
                    boardSize = room.getBoardSizeEnum(),
                    bombsRemaining = if (gameMode != TicTacToeGameMode.CLASSIC) 3 else 0,
                    isLoading = false
                )
                observeRoom(room.id)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Fehler beim Beitreten"
                )
            }
        }
    }

    /**
     * Observe room without joining (for when already in the room)
     */
    fun observeRoomOnly(roomId: String, playerId: String, playerName: String) {
        _state.value = _state.value.copy(
            playerId = playerId,
            playerName = playerName
        )
        observeRoom(roomId)
    }

    /**
     * Start observing room updates
     */
    private fun observeRoom(roomId: String) {
        roomObserverJob?.cancel()
        roomObserverJob = viewModelScope.launch {
            repository.observeRoom(roomId).collect { room ->
                room?.let { handleRoomUpdate(it) }
            }
        }
    }

    /**
     * Handle room updates from Firebase
     */
    private fun handleRoomUpdate(room: TicTacToeRoom) {
        val currentState = _state.value

        // Determine my symbol if not set
        val mySymbol = if (currentState.mySymbol == 0) {
            room.players[currentState.playerId]?.symbol ?: 0
        } else currentState.mySymbol

        // Get opponent info
        val opponent = room.getOpponent(currentState.playerId)

        // Check if game started
        if (room.status == RoomStatus.IN_PROGRESS.name && timerJob == null) {
            startTimer()
        }

        val gameMode = room.getGameModeEnum()

        // Handle different game modes
        val (winner, winningLine, isDraw, playableBoards) = if (gameMode == TicTacToeGameMode.ULTIMATE) {
            // Ultimate TicTacToe logic
            val ultimateWinner = UltimateTicTacToeLogic.checkWinner(room.ultimateBoard)
            val ultimateDraw = ultimateWinner == null && UltimateTicTacToeLogic.isDraw(room.ultimateBoard)
            val boards = UltimateTicTacToeLogic.getPlayableBoards(room.ultimateBoard)
            Quadruple(ultimateWinner, null, ultimateDraw, boards)
        } else {
            // Regular TicTacToe logic
            val normalWinner = TicTacToeLogic.checkWinner(room.board)
            val normalWinningLine = if (normalWinner != null) TicTacToeLogic.getWinningLine(room.board) else null
            val normalDraw = normalWinner == null && TicTacToeLogic.isDraw(room.board)
            Quadruple(normalWinner, normalWinningLine, normalDraw, emptyList())
        }

        // Determine if game is complete
        val isComplete = room.status == RoomStatus.FINISHED.name || winner != null || isDraw

        // Get bombs remaining
        val myBombs = room.players[currentState.playerId]?.bombsRemaining ?: 3
        val opponentBombs = opponent?.bombsRemaining ?: 3

        // Parse rematch status
        val rematchStatus = try {
            RematchStatus.valueOf(room.rematchStatus)
        } catch (e: Exception) {
            RematchStatus.NONE
        }
        
        // Get series scores
        val mySeriesWins = room.seriesScore[currentState.playerId] ?: 0
        val opponentSeriesWins = opponent?.let { room.seriesScore[it.playerId] } ?: 0

        _state.value = currentState.copy(
            room = room,
            board = room.board,
            ultimateBoard = room.ultimateBoard,
            playableBoards = playableBoards,
            gameMode = gameMode,
            boardSize = room.getBoardSizeEnum(),
            mySymbol = mySymbol,
            isMyTurn = room.currentTurnPlayerId == currentState.playerId,
            currentTurnSymbol = room.currentTurnSymbol,
            bombsRemaining = myBombs,
            opponentName = opponent?.displayName ?: "",
            opponentBombsRemaining = opponentBombs,
            winningLine = winningLine,
            isComplete = isComplete,
            winnerId = room.winnerId,
            isDraw = room.isDraw || isDraw,
            showGameOver = isComplete && rematchStatus != RematchStatus.ACCEPTED,
            rematchStatus = rematchStatus,
            rematchRequestedBy = room.rematchRequestedBy,
            mySeriesWins = mySeriesWins,
            opponentSeriesWins = opponentSeriesWins,
            roundNumber = room.roundNumber
        )

        // Stop timer when game ends
        if (isComplete) {
            stopTimer()
        }
    }

    // Helper data class for multiple return values
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * Start the game (host only)
     */
    fun startGame() {
        val room = _state.value.room ?: return

        viewModelScope.launch {
            repository.startGame(room.id)
        }
    }

    /**
     * Toggle action mode between symbol and bomb
     */
    fun toggleActionMode() {
        val currentState = _state.value
        if (currentState.gameMode == TicTacToeGameMode.CLASSIC) return
        if (currentState.bombsRemaining <= 0) return

        val newMode = if (currentState.actionMode == TicTacToeActionMode.SYMBOL) {
            TicTacToeActionMode.BOMB
        } else {
            TicTacToeActionMode.SYMBOL
        }

        _state.value = currentState.copy(actionMode = newMode)
    }

    /**
     * Set action mode explicitly
     */
    fun setActionMode(mode: TicTacToeActionMode) {
        val currentState = _state.value
        if (currentState.gameMode == TicTacToeGameMode.CLASSIC && mode == TicTacToeActionMode.BOMB) return
        if (currentState.bombsRemaining <= 0 && mode == TicTacToeActionMode.BOMB) return

        _state.value = currentState.copy(actionMode = mode)
    }

    /**
     * Handle cell click
     */
    fun onCellClick(row: Int, col: Int) {
        val currentState = _state.value

        // Check if it's my turn
        if (!currentState.isMyTurn) return

        // Check if game is over
        if (currentState.isComplete) return

        when (currentState.actionMode) {
            TicTacToeActionMode.SYMBOL -> makeMove(row, col)
            TicTacToeActionMode.BOMB -> placeBomb(row, col)
        }
    }

    /**
     * Handle cell click for Ultimate TicTacToe
     */
    fun onUltimateCellClick(boardRow: Int, boardCol: Int, cellRow: Int, cellCol: Int) {
        val currentState = _state.value

        // Check if it's my turn
        if (!currentState.isMyTurn) return

        // Check if game is over
        if (currentState.isComplete) return

        // Check if this is Ultimate mode
        if (currentState.gameMode != TicTacToeGameMode.ULTIMATE) return

        makeUltimateMove(boardRow, boardCol, cellRow, cellCol)
    }

    /**
     * Make a move in Ultimate TicTacToe
     */
    private fun makeUltimateMove(boardRow: Int, boardCol: Int, cellRow: Int, cellCol: Int) {
        val currentState = _state.value
        val room = currentState.room ?: return

        if (!UltimateTicTacToeLogic.isValidMove(
                currentState.ultimateBoard, boardRow, boardCol, cellRow, cellCol
            )) return

        viewModelScope.launch {
            // Update board locally first
            val newBoard = UltimateTicTacToeLogic.makeMove(
                currentState.ultimateBoard,
                boardRow, boardCol, cellRow, cellCol,
                currentState.mySymbol,
                currentState.playerId
            )

            val newPlayableBoards = UltimateTicTacToeLogic.getPlayableBoards(newBoard)

            _state.value = currentState.copy(
                ultimateBoard = newBoard,
                playableBoards = newPlayableBoards,
                isMyTurn = false
            )

            // Check for winner
            val winner = UltimateTicTacToeLogic.checkWinner(newBoard)
            val isDraw = winner == null && UltimateTicTacToeLogic.isDraw(newBoard)

            // Sync to server
            repository.makeUltimateMove(
                roomId = room.id,
                playerId = currentState.playerId,
                boardRow = boardRow,
                boardCol = boardCol,
                cellRow = cellRow,
                cellCol = cellCol,
                symbol = currentState.mySymbol,
                newBoard = newBoard
            )

            // Handle game end
            if (winner != null) {
                val winnerId = if (winner == currentState.mySymbol) {
                    currentState.playerId
                } else {
                    room.getOpponent(currentState.playerId)?.playerId
                }
                repository.endGame(room.id, winnerId, false)
            } else if (isDraw) {
                repository.endGame(room.id, null, true)
            }
        }
    }

    /**
     * Make a normal move (place X or O)
     */
    private fun makeMove(row: Int, col: Int) {
        val currentState = _state.value
        val room = currentState.room ?: return

        if (!TicTacToeLogic.isValidMove(currentState.board, row, col)) return

        viewModelScope.launch {
            // Update board locally first for responsiveness
            val newBoard = TicTacToeLogic.makeMove(
                currentState.board,
                row, col,
                currentState.mySymbol,
                currentState.playerId
            )

            _state.value = currentState.copy(board = newBoard, isMyTurn = false)

            // Check for winner after move
            val winner = TicTacToeLogic.checkWinner(newBoard)
            val isDraw = winner == null && TicTacToeLogic.isDraw(newBoard)

            // Sync to server
            repository.makeMove(
                roomId = room.id,
                playerId = currentState.playerId,
                row = row,
                col = col,
                symbol = currentState.mySymbol
            )

            // Handle game end
            if (winner != null) {
                val winnerId = if (winner == currentState.mySymbol) {
                    currentState.playerId
                } else {
                    room.getOpponent(currentState.playerId)?.playerId
                }
                repository.endGame(room.id, winnerId, false)
            } else if (isDraw) {
                repository.endGame(room.id, null, true)
            }
        }
    }

    /**
     * Place a bomb
     */
    private fun placeBomb(row: Int, col: Int) {
        val currentState = _state.value
        val room = currentState.room ?: return

        // Check if player has bombs remaining
        if (currentState.bombsRemaining <= 0) return

        // Check if placement is valid
        if (!TicTacToeLogic.isValidBombPlacement(currentState.board, row, col)) return

        viewModelScope.launch {
            // Place bomb locally
            val boardWithBomb = TicTacToeLogic.placeBomb(
                currentState.board,
                row, col,
                currentState.playerId
            )

            // Detonate based on game mode
            val newBoard = when (currentState.gameMode) {
                TicTacToeGameMode.BOMB -> TicTacToeLogic.detonateStandardBomb(boardWithBomb, row, col)
                TicTacToeGameMode.L_BOMB -> TicTacToeLogic.detonateLBomb(boardWithBomb, row, col)
                else -> boardWithBomb
            }

            _state.value = currentState.copy(
                board = newBoard,
                bombsRemaining = currentState.bombsRemaining - 1,
                actionMode = TicTacToeActionMode.SYMBOL,  // Reset to symbol mode after bomb
                isMyTurn = false
            )

            // Sync to server
            repository.placeBomb(
                roomId = room.id,
                playerId = currentState.playerId,
                row = row,
                col = col,
                gameMode = currentState.gameMode,
                newBoard = newBoard
            )

            // Check for winner after bomb
            val winner = TicTacToeLogic.checkWinner(newBoard)
            val isDraw = winner == null && TicTacToeLogic.isDraw(newBoard)

            if (winner != null) {
                val winnerId = if (winner == currentState.mySymbol) {
                    currentState.playerId
                } else {
                    room.getOpponent(currentState.playerId)?.playerId
                }
                repository.endGame(room.id, winnerId, false)
            } else if (isDraw) {
                repository.endGame(room.id, null, true)
            }
        }
    }

    /**
     * Start game timer
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_state.value.isComplete) {
                    _state.value = _state.value.copy(
                        elapsedSeconds = _state.value.elapsedSeconds + 1
                    )
                }
            }
        }
    }

    /**
     * Stop game timer
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Leave current room
     */
    fun leaveRoom() {
        val currentState = _state.value
        val room = currentState.room ?: return

        roomObserverJob?.cancel()
        stopTimer()

        viewModelScope.launch {
            repository.leaveRoom(room.id, currentState.playerId)
        }

        _state.value = TicTacToeGameState()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Reset game state
     */
    fun resetGame() {
        stopTimer()
        roomObserverJob?.cancel()
        _state.value = TicTacToeGameState()
    }

    /**
     * Dismiss game over dialog
     */
    fun dismissGameOver() {
        _state.value = _state.value.copy(showGameOver = false)
    }

    /**
     * Check if I won
     */
    fun didIWin(): Boolean {
        val currentState = _state.value
        return currentState.winnerId == currentState.playerId
    }

    /**
     * Get result message
     */
    fun getResultMessage(): String {
        val currentState = _state.value
        return when {
            currentState.isDraw -> "Unentschieden!"
            currentState.winnerId == currentState.playerId -> "Du hast gewonnen!"
            currentState.winnerId != null -> "Du hast verloren!"
            else -> ""
        }
    }
    
    /**
     * Request a rematch
     */
    fun requestRematch() {
        val currentState = _state.value
        val room = currentState.room ?: return
        
        viewModelScope.launch {
            repository.requestRematch(room.id, currentState.playerId)
        }
    }
    
    /**
     * Accept a rematch request
     */
    fun acceptRematch() {
        val currentState = _state.value
        val room = currentState.room ?: return
        
        viewModelScope.launch {
            val result = repository.acceptRematch(room.id, currentState.playerId)
            result.onSuccess {
                // Reset local state for new game
                _state.value = currentState.copy(
                    showGameOver = false,
                    isComplete = false,
                    winnerId = null,
                    isDraw = false,
                    winningLine = null,
                    elapsedSeconds = 0
                )
                // Timer will be started when room update arrives
            }
        }
    }
    
    /**
     * Decline a rematch request
     */
    fun declineRematch() {
        val currentState = _state.value
        val room = currentState.room ?: return
        
        viewModelScope.launch {
            repository.declineRematch(room.id)
        }
    }
    
    /**
     * Check if I requested the rematch
     */
    fun didIRequestRematch(): Boolean {
        val currentState = _state.value
        return currentState.rematchRequestedBy == currentState.playerId
    }
    
    /**
     * Check if opponent requested rematch
     */
    fun opponentRequestedRematch(): Boolean {
        val currentState = _state.value
        return currentState.rematchStatus == RematchStatus.PENDING && 
               currentState.rematchRequestedBy != null &&
               currentState.rematchRequestedBy != currentState.playerId
    }
    
    /**
     * Get series score string
     */
    fun getSeriesScore(): String {
        val currentState = _state.value
        return "${currentState.mySeriesWins} - ${currentState.opponentSeriesWins}"
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        roomObserverJob?.cancel()
    }
}
