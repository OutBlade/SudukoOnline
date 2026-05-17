package de.sudokuonline.app.data.repository

import de.sudokuonline.app.data.model.Player
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://sudokuonline-f59b9-default-rtdb.europe-west1.firebasedatabase.app")
    private val playersRef = database.getReference("players")
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    val isLoggedIn: Boolean
        get() = auth.currentUser != null
    
    val currentUserId: String?
        get() = auth.currentUser?.uid
    
    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    /**
     * Sign in anonymously for quick play
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Anonymous sign in failed")
            
            // Create player profile
            val player = Player(
                id = user.uid,
                displayName = "Spieler${(1000..9999).random()}",
                email = "",
                createdAt = System.currentTimeMillis()
            )
            playersRef.child(user.uid).setValue(player.toMap()).await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Register a new user
     */
    suspend fun register(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Registration failed")
            
            // Create player profile
            val player = Player(
                id = user.uid,
                displayName = displayName,
                email = email,
                createdAt = System.currentTimeMillis()
            )
            playersRef.child(user.uid).setValue(player.toMap()).await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Update display name
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: throw Exception("Not logged in")
            playersRef.child(userId).child("displayName").setValue(displayName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current player profile
     */
    suspend fun getCurrentPlayer(): Result<Player> {
        return try {
            val userId = currentUserId ?: throw Exception("Not logged in")
            val snapshot = playersRef.child(userId).get().await()
            val player = snapshot.getValue(Player::class.java)
                ?: throw Exception("Player not found")
            Result.success(player)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
