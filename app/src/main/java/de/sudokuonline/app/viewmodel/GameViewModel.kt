package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.GameRepository
import de.sudokuonline.app.game.SudokuGenerator
import de.sudokuonline.app.game.SudokuValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameState(
    val board: SudokuBoard = SudokuBoard(),
    val selectedCell: Pair<Int, Int>? = null,
    val isNotesMode: Boolean = false,
    val errors: Int = 0,
    val maxErrors: Int = 3,
    val hintsUsed: Int = 0,
    val maxHints: Int = 3,
    val bonusHints: Int = 0,
    val showAdForHintDialog: Boolean = false,
    val elapsedSeconds: Int = 0,
    val isComplete: Boolean = false,
    val isGameOver: Boolean = false,  // true when max errors reached
    val isPaused: Boolean = false,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val gameMode: GameMode = GameMode.SINGLE_PLAYER,

    // Multiplayer
    val room: GameRoom? = null,
    val playerId: String = "",
    val playerName: String = "",
    val opponentProgress: Int = 0,
    val isWinner: Boolean? = null,
    val showGameOver: Boolean = false,

    // Loading states
    val isLoading: Boolean = false,
    val error: String? = null
)

class GameViewModel : ViewModel() {
    private val gameRepository = GameRepository()
    
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    private var roomObserverJob: Job? = null
    
    // Saved game repository - will be initialized when context is available
    private var savedGameRepository: de.sudokuonline.app.data.repository.SavedGameRepository? = null
    
    /**
     * Initialize with context for saved game functionality
     */
    fun initWithContext(context: android.content.Context) {
        savedGameRepository = de.sudokuonline.app.data.repository.SavedGameRepository.getInstance(context)
    }
    
    /**
     * Start a new single player game
     */
    fun startSinglePlayerGame(difficulty: Difficulty) {
        // Delete any existing saved game when starting a new one
        savedGameRepository?.deleteSavedGame()
        
        val board = SudokuGenerator().generate(difficulty)
        _state.value = GameState(
            board = board,
            difficulty = difficulty,
            gameMode = GameMode.SINGLE_PLAYER
        )
        startTimer()
    }
    
    /**
     * Resume a saved single player game
     */
    fun resumeSavedGame(): Boolean {
        val savedGame = savedGameRepository?.loadSudokuGame() ?: return false
        
        _state.value = GameState(
            board = savedGame.board,
            difficulty = savedGame.difficulty,
            gameMode = GameMode.SINGLE_PLAYER,
            elapsedSeconds = savedGame.elapsedSeconds,
            errors = savedGame.errors,
            hintsUsed = savedGame.hintsUsed,
            selectedCell = savedGame.selectedCell,
            isNotesMode = savedGame.isNotesMode
        )
        
        // Delete the saved game after loading (will auto-save on pause/exit)
        savedGameRepository?.deleteSavedGame()
        
        startTimer()
        return true
    }
    
    /**
     * Save current game state
     */
    fun saveCurrentGame() {
        val currentState = _state.value
        
        // Only save single player games that are not complete
        if (currentState.gameMode != GameMode.SINGLE_PLAYER) return
        if (currentState.isComplete || currentState.isGameOver) return
        
        savedGameRepository?.autoSave(
            board = currentState.board,
            difficulty = currentState.difficulty,
            elapsedSeconds = currentState.elapsedSeconds,
            errors = currentState.errors,
            hintsUsed = currentState.hintsUsed,
            selectedCell = currentState.selectedCell,
            isNotesMode = currentState.isNotesMode
        )
    }
    
    /**
     * Check if there's a saved game available
     */
    fun hasSavedGame(): Boolean {
        return savedGameRepository?.hasSavedGame?.value == true
    }
    
    /**
     * Create a multiplayer room
     */
    fun createRoom(
        playerId: String,
        playerName: String,
        gameMode: GameMode,
        difficulty: Difficulty,
        isPrivate: Boolean
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = gameRepository.createRoom(
                hostId = playerId,
                hostName = playerName,
                gameMode = gameMode,
                difficulty = difficulty,
                isPrivate = isPrivate
            )
            
            result.onSuccess { room ->
                _state.value = _state.value.copy(
                    room = room,
                    board = room.board,
                    playerId = playerId,
                    playerName = playerName,
                    gameMode = gameMode,
                    difficulty = difficulty,
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
            
            val result = gameRepository.joinRoomByCode(
                code = code.uppercase(),
                playerId = playerId,
                playerName = playerName
            )
            
            result.onSuccess { room ->
                _state.value = _state.value.copy(
                    room = room,
                    board = room.board,
                    playerId = playerId,
                    playerName = playerName,
                    gameMode = GameMode.valueOf(room.gameMode),
                    difficulty = Difficulty.valueOf(room.difficulty),
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
            
            val result = gameRepository.joinRoom(
                roomId = roomId,
                playerId = playerId,
                playerName = playerName
            )
            
            result.onSuccess { room ->
                _state.value = _state.value.copy(
                    room = room,
                    board = room.board,
                    playerId = playerId,
                    playerName = playerName,
                    gameMode = GameMode.valueOf(room.gameMode),
                    difficulty = Difficulty.valueOf(room.difficulty),
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
     * Observe room for real-time updates
     */
    private fun observeRoom(roomId: String) {
        roomObserverJob?.cancel()
        roomObserverJob = viewModelScope.launch {
            try {
                gameRepository.observeRoom(roomId).collect { room ->
                    room?.let { handleRoomUpdate(it) }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Verbindung unterbrochen. Bitte erneut versuchen."
                )
            }
        }
    }
    
    /**
     * Just observe a room without joining (for when already in the room)
     */
    fun observeRoomOnly(roomId: String, playerId: String, playerName: String) {
        _state.value = _state.value.copy(
            playerId = playerId,
            playerName = playerName
        )
        observeRoom(roomId)
    }
    
    /**
     * Handle room updates from Firebase
     */
    private fun handleRoomUpdate(room: GameRoom) {
        val currentState = _state.value
        
        // Update board - always use server board if local is empty
        val board = if (currentState.board.solution.flatten().all { it == 0 }) {
            // Local board is empty, use server board
            room.board
        } else if (currentState.gameMode == GameMode.COOP) {
            // Coop mode: always sync from server
            room.board
        } else {
            // Competitive: keep local board for own moves
            currentState.board
        }
        
        // Calculate opponent progress
        val opponentProgress = room.players
            .filter { it.key != currentState.playerId }
            .values
            .maxOfOrNull { it.progress } ?: 0
        
        // Check if game started
        if (room.status == RoomStatus.IN_PROGRESS.name && timerJob == null) {
            startTimer()
        }
        
        // Check if game ended
        val isWinner = if (room.status == RoomStatus.FINISHED.name) {
            room.winnerId == currentState.playerId
        } else null
        
        _state.value = currentState.copy(
            room = room,
            board = board,
            difficulty = Difficulty.valueOf(room.difficulty),
            gameMode = GameMode.valueOf(room.gameMode),
            opponentProgress = opponentProgress,
            isWinner = isWinner,
            showGameOver = room.status == RoomStatus.FINISHED.name
        )
    }
    
    /**
     * Start the game (host only)
     */
    fun startGame() {
        val room = _state.value.room ?: return
        
        viewModelScope.launch {
            gameRepository.startGame(room.id)
        }
    }
    
    /**
     * Select a cell
     */
    fun selectCell(row: Int, col: Int) {
        val currentState = _state.value
        if (currentState.board.cells[row][col].isFixed) {
            // Allow selecting but not editing fixed cells
        }
        _state.value = currentState.copy(selectedCell = Pair(row, col))
    }
    
    /**
     * Enter a number in the selected cell
     */
    fun enterNumber(number: Int) {
        val currentState = _state.value
        val (row, col) = currentState.selectedCell ?: return
        
        val cell = currentState.board.cells[row][col]
        if (cell.isFixed) return
        
        if (currentState.isNotesMode && number != 0) {
            // Toggle note
            val newNotes = if (number in cell.notes) {
                cell.notes - number
            } else {
                cell.notes + number
            }
            updateCellLocally(row, col, cell.copy(notes = newNotes.sorted()))
        } else {
            // Enter number
            val isCorrect = SudokuValidator.isValidMove(currentState.board, row, col, number)
            val newErrors = if (number != 0 && !isCorrect) currentState.errors + 1 else currentState.errors
            
            val newCell = cell.copy(
                value = number,
                notes = if (number != 0) emptyList() else cell.notes,
                isError = number != 0 && !isCorrect,
                enteredBy = currentState.playerId.ifEmpty { null }
            )
            
            updateCellLocally(row, col, newCell, newErrors)
            
            // Sync to Firebase for multiplayer
            if (currentState.room != null) {
                syncCellToServer(row, col, number)
            }
        }
    }
    
    /**
     * Update cell locally
     */
    private fun updateCellLocally(row: Int, col: Int, cell: SudokuCell, errors: Int? = null) {
        val currentState = _state.value
        val newCells = currentState.board.cells.mapIndexed { r, rowCells ->
            if (r == row) {
                rowCells.mapIndexed { c, existingCell ->
                    if (c == col) cell else existingCell
                }
            } else rowCells
        }
        
        val newBoard = currentState.board.copy(cells = newCells)
        val isComplete = SudokuValidator.isComplete(newBoard)
        val newErrors = errors ?: currentState.errors
        val isGameOver = newErrors >= currentState.maxErrors
        
        _state.value = currentState.copy(
            board = newBoard,
            errors = newErrors,
            isComplete = isComplete,
            isGameOver = isGameOver,
            showGameOver = isComplete || isGameOver,
            isWinner = if (isGameOver && !isComplete) false else if (isComplete) true else null
        )
        
        // Handle game completion or game over
        if (isComplete) {
            handleGameComplete()
        } else if (isGameOver) {
            handleGameOver()
        }
    }
    
    /**
     * Handle game over (too many errors)
     */
    private fun handleGameOver() {
        stopTimer()
        
        val currentState = _state.value
        val room = currentState.room
        
        if (room != null && currentState.gameMode == GameMode.COMPETITIVE) {
            viewModelScope.launch {
                // Mark player as finished with loss
                gameRepository.finishGame(
                    roomId = room.id,
                    playerId = currentState.playerId,
                    score = 0
                )
                
                // Update player stats
                gameRepository.updatePlayerStats(
                    playerId = currentState.playerId,
                    won = false,
                    score = 0
                )
            }
        }
    }
    
    /**
     * Sync cell to Firebase server
     */
    private fun syncCellToServer(row: Int, col: Int, value: Int) {
        val currentState = _state.value
        val room = currentState.room ?: return
        
        viewModelScope.launch {
            gameRepository.updateCell(
                roomId = room.id,
                playerId = currentState.playerId,
                row = row,
                col = col,
                value = value
            )
            
            // Update player progress
            val progress = SudokuValidator.calculateProgress(currentState.board)
            gameRepository.updatePlayerProgress(
                roomId = room.id,
                playerId = currentState.playerId,
                progress = progress,
                errors = currentState.errors,
                score = calculateScore()
            )
        }
    }
    
    /**
     * Handle game completion
     */
    private fun handleGameComplete() {
        stopTimer()
        
        val currentState = _state.value
        val room = currentState.room
        
        if (room != null) {
            viewModelScope.launch {
                val score = calculateScore()
                
                // Mark player as finished
                gameRepository.finishGame(
                    roomId = room.id,
                    playerId = currentState.playerId,
                    score = score
                )
                
                // If competitive and first to finish, set as winner
                if (currentState.gameMode == GameMode.COMPETITIVE) {
                    gameRepository.endGame(room.id, currentState.playerId)
                }
                
                // Update player stats
                gameRepository.updatePlayerStats(
                    playerId = currentState.playerId,
                    won = true,
                    score = score
                )
            }
        }
    }
    
    /**
     * Calculate score based on time and errors
     */
    private fun calculateScore(): Int {
        val currentState = _state.value
        val baseScore = 10000
        val timeDeduction = currentState.elapsedSeconds * 2
        val errorDeduction = currentState.errors * 100
        val difficultyBonus = when (currentState.difficulty) {
            Difficulty.EASY -> 0
            Difficulty.MEDIUM -> 1000
            Difficulty.HARD -> 2500
            Difficulty.EXPERT -> 5000
        }
        
        return maxOf(0, baseScore - timeDeduction - errorDeduction + difficultyBonus)
    }
    
    /**
     * Toggle notes mode
     */
    fun toggleNotesMode() {
        _state.value = _state.value.copy(isNotesMode = !_state.value.isNotesMode)
    }
    
    /**
     * Clear the selected cell
     */
    fun clearCell() {
        enterNumber(0)
    }
    
    /**
     * Use hint (max 3 per game, or bonus hints from ads) - directly places the correct value
     */
    fun useHint() {
        val currentState = _state.value

        // Check if hints available (free hints or bonus hints)
        if (currentState.hintsUsed >= currentState.maxHints && currentState.bonusHints <= 0) {
            // No hints left - show ad dialog
            _state.value = currentState.copy(showAdForHintDialog = true)
            return
        }

        val (row, col) = currentState.selectedCell ?: return

        val cell = currentState.board.cells[row][col]
        // Only use hint if cell is empty or has wrong value
        if (cell.isFixed) return
        if (cell.value != 0 && cell.value == currentState.board.solution[row][col]) return

        val correctValue = currentState.board.solution[row][col]

        // Use bonus hint first, otherwise use regular hint
        val newState = if (currentState.hintsUsed >= currentState.maxHints && currentState.bonusHints > 0) {
            currentState.copy(bonusHints = currentState.bonusHints - 1)
        } else {
            currentState.copy(hintsUsed = currentState.hintsUsed + 1)
        }
        _state.value = newState

        // Place the correct value directly
        val newCell = cell.copy(
            value = correctValue,
            notes = emptyList(),
            isError = false,
            enteredBy = currentState.playerId.ifEmpty { null }
        )
        updateCellLocally(row, col, newCell)

        // Sync to Firebase for multiplayer
        if (currentState.room != null) {
            syncCellToServer(row, col, correctValue)
        }
    }

    /**
     * Called when ad reward is earned - grants bonus hint and uses it
     */
    fun onAdRewardEarned() {
        _state.value = _state.value.copy(
            bonusHints = _state.value.bonusHints + 1,
            showAdForHintDialog = false
        )
        useHint()
    }

    /**
     * Dismiss the ad dialog without watching
     */
    fun dismissAdDialog() {
        _state.value = _state.value.copy(showAdForHintDialog = false)
    }
    
    /**
     * Undo last move (simplified - just clear current cell)
     */
    fun undo() {
        clearCell()
    }
    
    /**
     * Start game timer
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_state.value.isPaused && !_state.value.isComplete) {
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
     * Pause/resume game
     */
    fun togglePause() {
        _state.value = _state.value.copy(isPaused = !_state.value.isPaused)
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
            gameRepository.leaveRoom(room.id, currentState.playerId)
        }
        
        _state.value = GameState()
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
        // Save game before resetting (only for single player)
        val currentState = _state.value
        if (currentState.gameMode == GameMode.SINGLE_PLAYER && 
            !currentState.isComplete && 
            !currentState.isGameOver) {
            saveCurrentGame()
        }
        
        stopTimer()
        roomObserverJob?.cancel()
        _state.value = GameState()
    }
    
    /**
     * Dismiss game over dialog
     */
    fun dismissGameOver() {
        _state.value = _state.value.copy(showGameOver = false)
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Auto-save game when ViewModel is cleared (e.g., app backgrounded)
        val currentState = _state.value
        if (currentState.gameMode == GameMode.SINGLE_PLAYER && 
            !currentState.isComplete && 
            !currentState.isGameOver) {
            saveCurrentGame()
        }
        
        stopTimer()
        roomObserverJob?.cancel()
    }
    
    /**
     * Pause game and save state
     */
    fun pauseAndSave() {
        _state.value = _state.value.copy(isPaused = true)
        saveCurrentGame()
    }
    
    /**
     * Delete saved game (e.g., when game is completed or game over)
     */
    fun clearSavedGame() {
        savedGameRepository?.deleteSavedGame()
    }
}
