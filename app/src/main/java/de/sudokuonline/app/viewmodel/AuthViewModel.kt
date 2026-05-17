package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sudokuonline.app.data.model.Player
import de.sudokuonline.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: FirebaseUser? = null,
    val player: Player? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _state.value = _state.value.copy(
                    isLoggedIn = user != null,
                    user = user
                )
                if (user != null) {
                    loadPlayer()
                }
            }
        }
    }
    
    private suspend fun loadPlayer() {
        val result = authRepository.getCurrentPlayer()
        result.onSuccess { player ->
            _state.value = _state.value.copy(player = player)
        }
    }
    
    fun signInAnonymously() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = authRepository.signInAnonymously()
            result.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Anmeldung fehlgeschlagen"
                )
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = authRepository.signIn(email, password)
            result.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Anmeldung fehlgeschlagen"
                )
            }
        }
    }
    
    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = authRepository.register(email, password, displayName)
            result.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registrierung fehlgeschlagen"
                )
            }
        }
    }
    
    fun signOut() {
        authRepository.signOut()
        _state.value = AuthState()
    }
    
    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(name)
            result.onSuccess {
                loadPlayer()
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = authRepository.resetPassword(email)
            result.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Fehler beim Passwort-Reset"
                )
            }
        }
    }
}
