package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.TicTacToeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TicTacToeLobbyState(
    val availableRooms: List<TicTacToeRoom> = emptyList(),
    val selectedGameMode: TicTacToeGameMode = TicTacToeGameMode.CLASSIC,
    val selectedBoardSize: TicTacToeBoardSize = TicTacToeBoardSize.SMALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdRoom: TicTacToeRoom? = null,
    val joinedRoom: TicTacToeRoom? = null
)

class TicTacToeLobbyViewModel : ViewModel() {
    private val repository = TicTacToeRepository()

    private val _state = MutableStateFlow(TicTacToeLobbyState())
    val state: StateFlow<TicTacToeLobbyState> = _state.asStateFlow()

    private var roomsObserverJob: Job? = null

    init {
        loadAvailableRooms()
    }

    /**
     * Load available public rooms
     */
    fun loadAvailableRooms() {
        roomsObserverJob?.cancel()
        roomsObserverJob = viewModelScope.launch {
            try {
                repository.observeAvailableRooms().collect { rooms ->
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
    fun setGameMode(mode: TicTacToeGameMode) {
        _state.value = _state.value.copy(selectedGameMode = mode)
    }

    /**
     * Set selected board size
     */
    fun setBoardSize(size: TicTacToeBoardSize) {
        _state.value = _state.value.copy(selectedBoardSize = size)
    }

    /**
     * Create a new room
     */
    fun createRoom(playerId: String, playerName: String, isPrivate: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = repository.createRoom(
                hostId = playerId,
                hostName = playerName,
                gameMode = _state.value.selectedGameMode,
                boardSize = _state.value.selectedBoardSize,
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
                    error = e.message ?: "Fehler beim Erstellen des Raums"
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
     * Join a room by code
     */
    fun joinRoomByCode(code: String, playerId: String, playerName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = repository.joinRoomByCode(
                code = code,
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
     * Clear navigation state after navigating
     */
    fun clearNavigation() {
        _state.value = _state.value.copy(
            createdRoom = null,
            joinedRoom = null
        )
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
        _state.value = TicTacToeLobbyState()
    }

    override fun onCleared() {
        super.onCleared()
        roomsObserverJob?.cancel()
    }
}
