package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.DameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DameLobbyState(
    val availableRooms: List<DameRoom> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdRoom: DameRoom? = null,
    val joinedRoom: DameRoom? = null
)

class DameLobbyViewModel : ViewModel() {
    private val repository = DameRepository()
    private val _state = MutableStateFlow(DameLobbyState())
    val state: StateFlow<DameLobbyState> = _state.asStateFlow()

    private var observeJob: Job? = null

    fun loadAvailableRooms() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeAvailableRooms().collect { rooms ->
                _state.value = _state.value.copy(availableRooms = rooms)
            }
        }
    }

    fun createRoom(playerId: String, playerName: String, isPrivate: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.createRoom(playerId, playerName, isPrivate)
                .onSuccess { room ->
                    _state.value = _state.value.copy(isLoading = false, createdRoom = room)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun joinRoom(roomId: String, playerId: String, playerName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.joinRoom(roomId, playerId, playerName)
                .onSuccess { room ->
                    _state.value = _state.value.copy(isLoading = false, joinedRoom = room)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun joinRoomByCode(code: String, playerId: String, playerName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.joinRoomByCode(code, playerId, playerName)
                .onSuccess { room ->
                    _state.value = _state.value.copy(isLoading = false, joinedRoom = room)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun clearNavigation() {
        _state.value = _state.value.copy(createdRoom = null, joinedRoom = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetState() {
        _state.value = DameLobbyState()
        observeJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
