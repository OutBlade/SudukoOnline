package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Repository for managing friends, friend requests, and challenges
 */
class FriendsRepository private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("friends_prefs", Context.MODE_PRIVATE)
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()
    
    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests.asStateFlow()
    
    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentRequests: StateFlow<List<FriendRequest>> = _sentRequests.asStateFlow()
    
    private val _activeChallenges = MutableStateFlow<List<Challenge>>(emptyList())
    val activeChallenges: StateFlow<List<Challenge>> = _activeChallenges.asStateFlow()
    
    private val _activityFeed = MutableStateFlow<List<FriendActivity>>(emptyList())
    val activityFeed: StateFlow<List<FriendActivity>> = _activityFeed.asStateFlow()
    
    private val _friendCode = MutableStateFlow(getOrCreateFriendCode())
    val friendCode: StateFlow<String> = _friendCode.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        setupListeners()
    }
    
    private fun getOrCreateFriendCode(): String {
        val existing = prefs.getString("friend_code", null)
        if (existing != null) return existing
        
        // Generate a 6-character alphanumeric code
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        prefs.edit().putString("friend_code", code).apply()
        
        // Save to Firebase
        auth.currentUser?.uid?.let { playerId ->
            database.child("friend_codes").child(code).setValue(playerId)
            database.child("players").child(playerId).child("friendCode").setValue(code)
        }
        
        return code
    }
    
    private fun setupListeners() {
        val playerId = auth.currentUser?.uid ?: return
        
        // Listen for friends
        database.child("friends").child(playerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val friendsList = mutableListOf<Friend>()
                    snapshot.children.forEach { child ->
                        child.getValue(Friend::class.java)?.let { friend ->
                            friendsList.add(friend)
                        }
                    }
                    _friends.value = friendsList.sortedByDescending { it.lastOnline }
                }
                override fun onCancelled(error: DatabaseError) {
                    _error.value = error.message
                }
            })
        
        // Listen for pending friend requests (received)
        database.child("friend_requests").child(playerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = mutableListOf<FriendRequest>()
                    snapshot.children.forEach { child ->
                        child.getValue(FriendRequest::class.java)?.let { request ->
                            if (request.status == RequestStatus.PENDING.name) {
                                requests.add(request)
                            }
                        }
                    }
                    _pendingRequests.value = requests.sortedByDescending { it.timestamp }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        
        // Listen for sent requests
        database.child("sent_requests").child(playerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = mutableListOf<FriendRequest>()
                    snapshot.children.forEach { child ->
                        child.getValue(FriendRequest::class.java)?.let { request ->
                            requests.add(request)
                        }
                    }
                    _sentRequests.value = requests
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        
        // Listen for challenges
        database.child("challenges").child(playerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val challenges = mutableListOf<Challenge>()
                    snapshot.children.forEach { child ->
                        child.getValue(Challenge::class.java)?.let { challenge ->
                            challenges.add(challenge)
                        }
                    }
                    _activeChallenges.value = challenges.sortedByDescending { it.createdAt }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        
        // Listen for activity feed
        database.child("activity_feed").child(playerId)
            .orderByChild("timestamp")
            .limitToLast(50)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val activities = mutableListOf<FriendActivity>()
                    snapshot.children.forEach { child ->
                        child.getValue(FriendActivity::class.java)?.let { activity ->
                            activities.add(activity)
                        }
                    }
                    _activityFeed.value = activities.sortedByDescending { it.timestamp }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        
        // Update online status
        updateOnlineStatus(true)
    }
    
    /**
     * Send a friend request by friend code
     */
    fun sendFriendRequest(code: String, onResult: (Boolean, String) -> Unit) {
        val playerId = auth.currentUser?.uid ?: run {
            onResult(false, "Nicht angemeldet")
            return
        }
        
        if (code.uppercase() == _friendCode.value) {
            onResult(false, "Du kannst dich nicht selbst hinzufügen")
            return
        }
        
        _isLoading.value = true
        
        // Find player by friend code
        database.child("friend_codes").child(code.uppercase())
            .get()
            .addOnSuccessListener { snapshot ->
                val targetPlayerId = snapshot.getValue(String::class.java)
                if (targetPlayerId == null) {
                    _isLoading.value = false
                    onResult(false, "Freundescode nicht gefunden")
                    return@addOnSuccessListener
                }
                
                // Check if already friends
                if (_friends.value.any { it.oderId == targetPlayerId }) {
                    _isLoading.value = false
                    onResult(false, "Ihr seid bereits Freunde")
                    return@addOnSuccessListener
                }
                
                // Check if request already sent
                if (_sentRequests.value.any { it.toPlayerId == targetPlayerId }) {
                    _isLoading.value = false
                    onResult(false, "Anfrage bereits gesendet")
                    return@addOnSuccessListener
                }
                
                // Get current player info
                database.child("players").child(playerId).get()
                    .addOnSuccessListener { playerSnapshot ->
                        val playerName = playerSnapshot.child("displayName").getValue(String::class.java) ?: "Spieler"
                        
                        // Get target player info
                        database.child("players").child(targetPlayerId).get()
                            .addOnSuccessListener { targetSnapshot ->
                                val targetName = targetSnapshot.child("displayName").getValue(String::class.java) ?: "Spieler"
                                
                                val requestId = UUID.randomUUID().toString()
                                val request = FriendRequest(
                                    id = requestId,
                                    fromPlayerId = playerId,
                                    fromPlayerName = playerName,
                                    toPlayerId = targetPlayerId,
                                    toPlayerName = targetName,
                                    timestamp = System.currentTimeMillis(),
                                    status = RequestStatus.PENDING.name
                                )
                                
                                // Save request for target player
                                database.child("friend_requests").child(targetPlayerId).child(requestId)
                                    .setValue(request)
                                
                                // Save sent request for current player
                                database.child("sent_requests").child(playerId).child(requestId)
                                    .setValue(request)
                                
                                _isLoading.value = false
                                onResult(true, "Freundschaftsanfrage gesendet an $targetName")
                            }
                    }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onResult(false, "Fehler: ${e.message}")
            }
    }
    
    /**
     * Accept a friend request
     */
    fun acceptFriendRequest(request: FriendRequest) {
        val playerId = auth.currentUser?.uid ?: return
        val playerName = prefs.getString("player_name", "Spieler") ?: "Spieler"
        
        // Create friend entries for both players
        val myFriend = Friend(
            oderId = request.fromPlayerId,
            displayName = request.fromPlayerName,
            friendSince = System.currentTimeMillis(),
            lastOnline = System.currentTimeMillis()
        )
        
        val theirFriend = Friend(
            oderId = playerId,
            displayName = playerName,
            friendSince = System.currentTimeMillis(),
            lastOnline = System.currentTimeMillis()
        )
        
        // Add to both friend lists
        database.child("friends").child(playerId).child(request.fromPlayerId).setValue(myFriend)
        database.child("friends").child(request.fromPlayerId).child(playerId).setValue(theirFriend)
        
        // Remove requests
        database.child("friend_requests").child(playerId).child(request.id).removeValue()
        database.child("sent_requests").child(request.fromPlayerId).child(request.id).removeValue()
        
        // Add activity for both
        addActivity(
            playerId = request.fromPlayerId,
            type = ActivityType.FRIEND_ACCEPTED,
            message = "$playerName hat deine Freundschaftsanfrage angenommen!",
            relatedPlayerId = playerId,
            relatedPlayerName = playerName
        )
    }
    
    /**
     * Decline a friend request
     */
    fun declineFriendRequest(request: FriendRequest) {
        val playerId = auth.currentUser?.uid ?: return
        
        // Remove requests
        database.child("friend_requests").child(playerId).child(request.id).removeValue()
        database.child("sent_requests").child(request.fromPlayerId).child(request.id).removeValue()
    }
    
    /**
     * Remove a friend
     */
    fun removeFriend(friend: Friend) {
        val playerId = auth.currentUser?.uid ?: return
        
        database.child("friends").child(playerId).child(friend.oderId).removeValue()
        database.child("friends").child(friend.oderId).child(playerId).removeValue()
    }
    
    /**
     * Send a challenge to a friend
     */
    fun sendChallenge(
        friend: Friend,
        gameType: GameType,
        difficulty: String,
        message: String = ""
    ) {
        val playerId = auth.currentUser?.uid ?: return
        val playerName = prefs.getString("player_name", "Spieler") ?: "Spieler"
        
        val challengeId = UUID.randomUUID().toString()
        val challenge = Challenge(
            id = challengeId,
            fromPlayerId = playerId,
            fromPlayerName = playerName,
            toPlayerId = friend.oderId,
            toPlayerName = friend.displayName,
            gameType = gameType.name,
            difficulty = difficulty,
            message = message,
            status = ChallengeStatus.PENDING.name,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 hours
        )
        
        // Save challenge for recipient
        database.child("challenges").child(friend.oderId).child(challengeId).setValue(challenge)
        
        // Save challenge for sender (to track)
        database.child("sent_challenges").child(playerId).child(challengeId).setValue(challenge)
        
        // Add activity
        addActivity(
            playerId = friend.oderId,
            type = ActivityType.CHALLENGE_RECEIVED,
            message = "$playerName fordert dich heraus! ${gameType.displayName} - $difficulty",
            relatedPlayerId = playerId,
            relatedPlayerName = playerName,
            challengeId = challengeId
        )
    }
    
    /**
     * Get a challenge by ID
     */
    fun getChallengeById(challengeId: String, onResult: (Challenge?) -> Unit) {
        val playerId = auth.currentUser?.uid ?: run {
            onResult(null)
            return
        }

        // First check in received challenges
        database.child("challenges").child(playerId).child(challengeId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val challenge = snapshot.getValue(Challenge::class.java)
                    onResult(challenge)
                } else {
                    // Check in sent challenges
                    database.child("sent_challenges").child(playerId).child(challengeId)
                        .get()
                        .addOnSuccessListener { sentSnapshot ->
                            val challenge = sentSnapshot.getValue(Challenge::class.java)
                            onResult(challenge)
                        }
                        .addOnFailureListener {
                            onResult(null)
                        }
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Accept a challenge
     */
    fun acceptChallenge(challenge: Challenge, onAccepted: (Challenge) -> Unit) {
        val playerId = auth.currentUser?.uid ?: return
        val playerName = prefs.getString("player_name", "Spieler") ?: "Spieler"
        
        val updatedChallenge = challenge.copy(
            status = ChallengeStatus.ACCEPTED.name,
            acceptedAt = System.currentTimeMillis()
        )
        
        // Update challenge status
        database.child("challenges").child(playerId).child(challenge.id).setValue(updatedChallenge)
        database.child("sent_challenges").child(challenge.fromPlayerId).child(challenge.id).setValue(updatedChallenge)
        
        // Notify challenger
        addActivity(
            playerId = challenge.fromPlayerId,
            type = ActivityType.CHALLENGE_ACCEPTED,
            message = "$playerName hat deine Herausforderung angenommen!",
            relatedPlayerId = playerId,
            relatedPlayerName = playerName,
            challengeId = challenge.id
        )
        
        onAccepted(updatedChallenge)
    }
    
    /**
     * Decline a challenge
     */
    fun declineChallenge(challenge: Challenge) {
        val playerId = auth.currentUser?.uid ?: return
        val playerName = prefs.getString("player_name", "Spieler") ?: "Spieler"
        
        // Remove challenge
        database.child("challenges").child(playerId).child(challenge.id).removeValue()
        database.child("sent_challenges").child(challenge.fromPlayerId).child(challenge.id).removeValue()
        
        // Notify challenger
        addActivity(
            playerId = challenge.fromPlayerId,
            type = ActivityType.CHALLENGE_DECLINED,
            message = "$playerName hat deine Herausforderung abgelehnt",
            relatedPlayerId = playerId,
            relatedPlayerName = playerName
        )
    }
    
    /**
     * Complete a challenge with result
     */
    fun completeChallenge(
        challenge: Challenge,
        won: Boolean,
        timeSeconds: Int,
        score: Int
    ) {
        val playerId = auth.currentUser?.uid ?: return
        val playerName = prefs.getString("player_name", "Spieler") ?: "Spieler"
        
        val result = ChallengeResult(
            oderId = playerId,
            playerName = playerName,
            timeSeconds = timeSeconds,
            score = score,
            completedAt = System.currentTimeMillis()
        )
        
        // Update challenge with result
        database.child("challenges").child(playerId).child(challenge.id).child("myResult").setValue(result)
        database.child("sent_challenges").child(challenge.fromPlayerId).child(challenge.id).child("opponentResult").setValue(result)
        
        // Notify the other player
        val otherPlayerId = if (challenge.fromPlayerId == playerId) challenge.toPlayerId else challenge.fromPlayerId
        addActivity(
            playerId = otherPlayerId,
            type = ActivityType.CHALLENGE_COMPLETED,
            message = "$playerName hat die Herausforderung in ${formatTime(timeSeconds)} abgeschlossen!",
            relatedPlayerId = playerId,
            relatedPlayerName = playerName,
            challengeId = challenge.id
        )
    }
    
    /**
     * Share an achievement/score with friends
     */
    fun shareWithFriends(
        gameType: String,
        difficulty: String,
        timeSeconds: Int,
        score: Int
    ) {
        val playerId = auth.currentUser?.uid ?: return
        val playerName = prefs.getString("player_name", "Spieler") ?: "Spieler"
        
        val message = "$playerName hat $difficulty $gameType in ${formatTime(timeSeconds)} gelöst!"
        
        // Add activity to all friends' feeds
        _friends.value.forEach { friend ->
            addActivity(
                playerId = friend.oderId,
                type = ActivityType.GAME_COMPLETED,
                message = message,
                relatedPlayerId = playerId,
                relatedPlayerName = playerName
            )
        }
    }
    
    private fun addActivity(
        playerId: String,
        type: ActivityType,
        message: String,
        relatedPlayerId: String,
        relatedPlayerName: String,
        challengeId: String? = null
    ) {
        val activityId = UUID.randomUUID().toString()
        val activity = FriendActivity(
            id = activityId,
            type = type.name,
            message = message,
            relatedPlayerId = relatedPlayerId,
            relatedPlayerName = relatedPlayerName,
            challengeId = challengeId,
            timestamp = System.currentTimeMillis()
        )
        
        database.child("activity_feed").child(playerId).child(activityId).setValue(activity)
    }
    
    private fun updateOnlineStatus(online: Boolean) {
        val playerId = auth.currentUser?.uid ?: return
        val timestamp = if (online) System.currentTimeMillis() else System.currentTimeMillis()
        
        database.child("players").child(playerId).child("lastOnline").setValue(timestamp)
        
        // Update for all friends
        _friends.value.forEach { friend ->
            database.child("friends").child(friend.oderId).child(playerId).child("lastOnline").setValue(timestamp)
        }
    }
    
    fun setPlayerName(name: String) {
        prefs.edit().putString("player_name", name).apply()
    }
    
    fun clearError() {
        _error.value = null
    }
    
    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "${mins}:${secs.toString().padStart(2, '0')}"
    }
    
    companion object {
        @Volatile
        private var instance: FriendsRepository? = null
        
        fun getInstance(context: Context): FriendsRepository {
            return instance ?: synchronized(this) {
                instance ?: FriendsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

// Data classes

@IgnoreExtraProperties
data class Friend(
    val oderId: String = "",
    val displayName: String = "",
    val friendSince: Long = 0,
    val lastOnline: Long = 0,
    val avatarUrl: String? = null
) {
    constructor() : this("", "", 0, 0, null)

    fun isOnline(): Boolean {
        // Consider online if active in last 5 minutes
        return System.currentTimeMillis() - lastOnline < 5 * 60 * 1000
    }
    
    fun getLastSeenText(): String {
        val diff = System.currentTimeMillis() - lastOnline
        return when {
            diff < 5 * 60 * 1000 -> "Online"
            diff < 60 * 60 * 1000 -> "Vor ${diff / (60 * 1000)} Min."
            diff < 24 * 60 * 60 * 1000 -> "Vor ${diff / (60 * 60 * 1000)} Std."
            else -> "Vor ${diff / (24 * 60 * 60 * 1000)} Tagen"
        }
    }
}

@IgnoreExtraProperties
data class FriendRequest(
    val id: String = "",
    val fromPlayerId: String = "",
    val fromPlayerName: String = "",
    val toPlayerId: String = "",
    val toPlayerName: String = "",
    val timestamp: Long = 0,
    val status: String = RequestStatus.PENDING.name
) {
    constructor() : this("", "", "", "", "", 0, RequestStatus.PENDING.name)
}

enum class RequestStatus {
    PENDING, ACCEPTED, DECLINED
}

@IgnoreExtraProperties
data class Challenge(
    val id: String = "",
    val fromPlayerId: String = "",
    val fromPlayerName: String = "",
    val toPlayerId: String = "",
    val toPlayerName: String = "",
    val gameType: String = "",
    val difficulty: String = "",
    val message: String = "",
    val status: String = ChallengeStatus.PENDING.name,
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
    val acceptedAt: Long? = null,
    val myResult: ChallengeResult? = null,
    val opponentResult: ChallengeResult? = null
) {
    constructor() : this("", "", "", "", "", "", "", "", ChallengeStatus.PENDING.name, 0, 0, null, null, null)

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    
    fun getTimeRemainingText(): String {
        val remaining = expiresAt - System.currentTimeMillis()
        return when {
            remaining <= 0 -> "Abgelaufen"
            remaining < 60 * 60 * 1000 -> "${remaining / (60 * 1000)} Min. übrig"
            else -> "${remaining / (60 * 60 * 1000)} Std. übrig"
        }
    }
}

@IgnoreExtraProperties
data class ChallengeResult(
    val oderId: String = "",
    val playerName: String = "",
    val timeSeconds: Int = 0,
    val score: Int = 0,
    val completedAt: Long = 0
) {
    constructor() : this("", "", 0, 0, 0)
}

enum class ChallengeStatus {
    PENDING, ACCEPTED, COMPLETED, EXPIRED, DECLINED
}

enum class GameType(val displayName: String) {
    SUDOKU("Sudoku"),
    KILLER_SUDOKU("Killer Sudoku"),
    TICTACTOE("TicTacToe"),
    MUHLE("Mühle")
}

@IgnoreExtraProperties
data class FriendActivity(
    val id: String = "",
    val type: String = "",
    val message: String = "",
    val relatedPlayerId: String = "",
    val relatedPlayerName: String = "",
    val challengeId: String? = null,
    val timestamp: Long = 0
) {
    constructor() : this("", "", "", "", "", null, 0)
}

enum class ActivityType {
    FRIEND_ACCEPTED,
    CHALLENGE_RECEIVED,
    CHALLENGE_ACCEPTED,
    CHALLENGE_DECLINED,
    CHALLENGE_COMPLETED,
    GAME_COMPLETED,
    ACHIEVEMENT_UNLOCKED
}
