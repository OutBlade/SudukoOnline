package de.sudokuonline.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.sudokuonline.app.data.model.Player
import de.sudokuonline.app.ui.screens.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeaderboardState(
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LeaderboardViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val playersRef = database.getReference("players")

    private val _state = MutableStateFlow(LeaderboardState())
    val state: StateFlow<LeaderboardState> = _state.asStateFlow()

    private var playersListener: ValueEventListener? = null

    fun loadLeaderboard(currentPlayerId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        playersListener?.let { playersRef.removeEventListener(it) }

        playersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    try {
                        val players = mutableListOf<Player>()

                        for (childSnapshot in snapshot.children) {
                            val player = childSnapshot.getValue(Player::class.java)
                            if (player != null && player.id.isNotEmpty()) {
                                players.add(player)
                            }
                        }

                        // Sort by totalScore descending
                        val sortedPlayers = players.sortedByDescending { it.totalScore }

                        // Convert to LeaderboardEntry with ranks
                        val leaderboardEntries = sortedPlayers.mapIndexed { index, player ->
                            LeaderboardEntry(
                                rank = index + 1,
                                playerId = player.id,
                                displayName = player.displayName.ifEmpty { "Unbekannt" },
                                totalScore = player.totalScore,
                                gamesPlayed = player.gamesPlayed,
                                gamesWon = player.gamesWon,
                                isCurrentPlayer = player.id == currentPlayerId
                            )
                        }

                        _state.value = _state.value.copy(
                            leaderboard = leaderboardEntries,
                            isLoading = false
                        )
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Fehler beim Laden: ${e.message}"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Fehler: ${error.message}"
                )
            }
        }

        // Query players ordered by totalScore (limited to top 100)
        playersRef.orderByChild("totalScore")
            .limitToLast(100)
            .addValueEventListener(playersListener!!)
    }

    fun refresh(currentPlayerId: String) {
        loadLeaderboard(currentPlayerId)
    }

    override fun onCleared() {
        super.onCleared()
        playersListener?.let { playersRef.removeEventListener(it) }
    }
}
