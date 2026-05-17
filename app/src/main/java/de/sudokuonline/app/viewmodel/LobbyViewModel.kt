package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LobbyState(
    val availableRooms: List<GameRoom> = emptyList(),
    val isSearching: Boolean = false,
    val matchFound: MatchmakingEntry? = null,
    val createdRoom: GameRoom? = null,
    val joinedRoom: GameRoom? = null,
    val selectedGameMode: GameMode = GameMode.COMPETITIVE,
    val selectedDifficulty: Difficulty = Difficulty.MEDIUM,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LobbyViewModel : ViewModel() {
    private val gameRepository = GameRepository()
    
    private val _state = MutableStateFlow(LobbyState())
    val state: StateFlow<LobbyState> = _state.asStateFlow()
    
    private var roomsObserverJob: Job? = null
    private var matchmakingJob: Job? = null
    
    /**
     * Start observing available rooms
     */
    fun loadAvailableRooms(gameMode: GameMode) {
        roomsObserverJob?.cancel()
        roomsObserverJob = viewModelScope.launch {
            try {
                gameRepository.observeAvailableRooms(gameMode).collect { rooms ->
                    _state.value = _state.value.copy(availableRooms = rooms)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Räume konnten nicht geladen werden. Bitte erneut versuchen."
                )
            }
        }
    }
    
    /**
     * Set selected game mode
     */
    fun setGameMode(gameMode: GameMode) {
        _state.value = _state.value.copy(selectedGameMode = gameMode)
        loadAvailableRooms(gameMode)
    }
    
    /**
     * Set selected difficulty
     */
    fun setDifficulty(difficulty: Difficulty) {
        _state.value = _state.value.copy(selectedDifficulty = difficulty)
    }
    
    /**
     * Create a new room
     */
    fun createRoom(
        playerId: String,
        playerName: String,
        isPrivate: Boolean
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val currentState = _state.value
            val result = gameRepository.createRoom(
                hostId = playerId,
                hostName = playerName,
                gameMode = currentState.selectedGameMode,
                difficulty = currentState.selectedDifficulty,
                isPrivate = isPrivate
            )
            
            result.onSuccess { room ->
                _state.value = _state.value.copy(
                    createdRoom = room,
                    isLoading = false
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Fehler beim Erstellen"
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
                code = code.uppercase().trim(),
                playerId = playerId,
                playerName = playerName
            )
            
            result.onSuccess { room ->
                _state.value = _state.value.copy(
                    joinedRoom = room,
                    isLoading = false
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Raum nicht gefunden"
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
                    joinedRoom = room,
                    isLoading = false
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Fehler beim Beitreten"
                )
            }
        }
    }
    
    /**
     * Start matchmaking
     */
    fun startMatchmaking(playerId: String, playerName: String) {
        val currentState = _state.value
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, error = null)
            
            // Add to matchmaking queue
            val result = gameRepository.joinMatchmaking(
                playerId = playerId,
                playerName = playerName,
                gameMode = currentState.selectedGameMode,
                difficulty = currentState.selectedDifficulty
            )
            
            result.onFailure { e ->
                _state.value = _state.value.copy(
                    isSearching = false,
                    error = e.message ?: "Matchmaking fehlgeschlagen"
                )
                return@launch
            }
            
            // Start observing for matches
            matchmakingJob = viewModelScope.launch {
                try {
                    gameRepository.observeMatchmaking(
                        playerId = playerId,
                        gameMode = currentState.selectedGameMode,
                        difficulty = currentState.selectedDifficulty
                    ).collect { match ->
                        if (match != null) {
                            handleMatchFound(playerId, playerName, match)
                        }
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        isSearching = false,
                        error = "Matchmaking fehlgeschlagen. Bitte erneut versuchen."
                    )
                }
            }
        }
    }
    
    /**
     * Handle when a match is found
     */
    private suspend fun handleMatchFound(
        playerId: String,
        playerName: String,
        match: MatchmakingEntry
    ) {
        val currentState = _state.value
        
        // Create room (the player with earlier timestamp creates)
        val shouldCreateRoom = playerId < match.playerId
        
        if (shouldCreateRoom) {
            val result = gameRepository.createRoom(
                hostId = playerId,
                hostName = playerName,
                gameMode = currentState.selectedGameMode,
                difficulty = currentState.selectedDifficulty,
                isPrivate = false
            )
            
            result.onSuccess { room ->
                // Remove both players from matchmaking
                gameRepository.leaveMatchmaking(playerId)
                gameRepository.leaveMatchmaking(match.playerId)
                
                _state.value = _state.value.copy(
                    matchFound = match,
                    createdRoom = room,
                    isSearching = false
                )
            }
        } else {
            // Wait a bit for the other player to create the room
            kotlinx.coroutines.delay(500)
            
            // Look for the room created by the match
            // The other player should have created it
            _state.value = _state.value.copy(
                matchFound = match,
                isSearching = false
            )
        }
    }
    
    /**
     * Cancel matchmaking
     */
    fun cancelMatchmaking(playerId: String) {
        matchmakingJob?.cancel()
        
        viewModelScope.launch {
            gameRepository.leaveMatchmaking(playerId)
            _state.value = _state.value.copy(
                isSearching = false,
                matchFound = null
            )
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    /**
     * Reset state
     */
    fun resetState() {
        roomsObserverJob?.cancel()
        matchmakingJob?.cancel()
        _state.value = LobbyState()
    }
    
    /**
     * Clear navigation states
     */
    fun clearNavigation() {
        _state.value = _state.value.copy(
            createdRoom = null,
            joinedRoom = null,
            matchFound = null
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        roomsObserverJob?.cancel()
        matchmakingJob?.cancel()
    }
}
