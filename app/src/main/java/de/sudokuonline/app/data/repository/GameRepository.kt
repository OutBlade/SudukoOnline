package de.sudokuonline.app.data.repository

import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.SudokuGenerator
import de.sudokuonline.app.game.SudokuValidator
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository {
    private val database = FirebaseDatabase.getInstance("https://sudokuonline-f59b9-default-rtdb.europe-west1.firebasedatabase.app")
    private val roomsRef = database.getReference("rooms")
    private val matchmakingRef = database.getReference("matchmaking")
    private val playersRef = database.getReference("players")
    
    /**
     * Generate a unique 6-character room code
     */
    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Excluded confusing chars
        return (1..6).map { chars.random() }.joinToString("")
    }
    
    /**
     * Create a new game room
     */
    suspend fun createRoom(
        hostId: String,
        hostName: String,
        gameMode: GameMode,
        difficulty: Difficulty,
        isPrivate: Boolean
    ): Result<GameRoom> {
        return try {
            val roomId = roomsRef.push().key ?: throw Exception("Failed to generate room ID")
            val roomCode = if (isPrivate) generateRoomCode() else ""
            
            val board = SudokuGenerator().generate(difficulty)
            
            val hostPlayer = RoomPlayer(
                playerId = hostId,
                displayName = hostName,
                status = PlayerStatus.READY.name,
                progress = 0,
                errors = 0,
                score = 0,
                lastActive = System.currentTimeMillis()
            )
            
            val maxPlayers = when (gameMode) {
                GameMode.COMPETITIVE -> 2
                GameMode.COOP -> 4
                else -> 1
            }
            
            val room = GameRoom(
                id = roomId,
                code = roomCode,
                hostId = hostId,
                gameMode = gameMode.name,
                difficulty = difficulty.name,
                status = RoomStatus.WAITING.name,
                isPrivate = isPrivate,
                maxPlayers = maxPlayers,
                players = mapOf(hostId to hostPlayer),
                board = board,
                createdAt = System.currentTimeMillis()
            )
            
            roomsRef.child(roomId).setValue(room.toMap()).await()
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Join a room by code
     */
    suspend fun joinRoomByCode(
        code: String,
        playerId: String,
        playerName: String
    ): Result<GameRoom> {
        return try {
            val snapshot = roomsRef.orderByChild("code").equalTo(code).get().await()
            
            if (!snapshot.exists()) {
                return Result.failure(Exception("Raum nicht gefunden"))
            }
            
            val roomSnapshot = snapshot.children.first()
            val room = parseRoom(roomSnapshot) ?: throw Exception("Invalid room data")
            
            if (room.status != RoomStatus.WAITING.name) {
                return Result.failure(Exception("Spiel bereits gestartet"))
            }
            
            if (room.players.size >= room.maxPlayers) {
                return Result.failure(Exception("Raum ist voll"))
            }
            
            val newPlayer = RoomPlayer(
                playerId = playerId,
                displayName = playerName,
                status = PlayerStatus.READY.name,
                progress = 0,
                errors = 0,
                score = 0,
                lastActive = System.currentTimeMillis()
            )
            
            roomsRef.child(room.id).child("players").child(playerId)
                .setValue(newPlayer.toMap()).await()
            
            val updatedRoom = room.copy(
                players = room.players + (playerId to newPlayer)
            )
            
            Result.success(updatedRoom)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Join a room by ID
     */
    suspend fun joinRoom(
        roomId: String,
        playerId: String,
        playerName: String
    ): Result<GameRoom> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: throw Exception("Room not found")
            
            if (room.status != RoomStatus.WAITING.name) {
                return Result.failure(Exception("Spiel bereits gestartet"))
            }
            
            if (room.players.size >= room.maxPlayers) {
                return Result.failure(Exception("Raum ist voll"))
            }
            
            val newPlayer = RoomPlayer(
                playerId = playerId,
                displayName = playerName,
                status = PlayerStatus.READY.name,
                lastActive = System.currentTimeMillis()
            )
            
            roomsRef.child(roomId).child("players").child(playerId)
                .setValue(newPlayer.toMap()).await()
            
            Result.success(room.copy(players = room.players + (playerId to newPlayer)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe room changes in real-time
     */
    fun observeRoom(roomId: String): Flow<GameRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = parseRoom(snapshot)
                trySend(room)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("GameRepository", "observeRoom cancelled: ${error.message}")
                trySend(null)
            }
        }

        roomsRef.child(roomId).addValueEventListener(listener)
        awaitClose { roomsRef.child(roomId).removeEventListener(listener) }
    }
    
    /**
     * Start the game in a room
     */
    suspend fun startGame(roomId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to RoomStatus.IN_PROGRESS.name,
                "startedAt" to ServerValue.TIMESTAMP
            )
            roomsRef.child(roomId).updateChildren(updates).await()
            
            // Update all players to playing status
            val playersSnapshot = roomsRef.child(roomId).child("players").get().await()
            playersSnapshot.children.forEach { playerSnapshot ->
                val playerId = playerSnapshot.key ?: return@forEach
                roomsRef.child(roomId).child("players").child(playerId)
                    .child("status").setValue(PlayerStatus.PLAYING.name).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update a cell in the game board
     */
    suspend fun updateCell(
        roomId: String,
        playerId: String,
        row: Int,
        col: Int,
        value: Int
    ): Result<Unit> {
        return try {
            val cellPath = "board/cells/$row/$col"
            val updates = mapOf(
                "$cellPath/value" to value,
                "$cellPath/enteredBy" to playerId,
                "$cellPath/isError" to false
            )
            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update player progress
     */
    suspend fun updatePlayerProgress(
        roomId: String,
        playerId: String,
        progress: Int,
        errors: Int,
        score: Int
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "players/$playerId/progress" to progress,
                "players/$playerId/errors" to errors,
                "players/$playerId/score" to score,
                "players/$playerId/lastActive" to ServerValue.TIMESTAMP
            )
            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark player as finished
     */
    suspend fun finishGame(
        roomId: String,
        playerId: String,
        score: Int
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "players/$playerId/status" to PlayerStatus.FINISHED.name,
                "players/$playerId/finishedAt" to ServerValue.TIMESTAMP,
                "players/$playerId/score" to score
            )
            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Set the winner and end the game
     */
    suspend fun endGame(roomId: String, winnerId: String?): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to RoomStatus.FINISHED.name,
                "finishedAt" to ServerValue.TIMESTAMP,
                "winnerId" to winnerId
            )
            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Leave a room
     */
    suspend fun leaveRoom(roomId: String, playerId: String): Result<Unit> {
        return try {
            roomsRef.child(roomId).child("players").child(playerId).removeValue().await()
            
            // Check if room is empty and delete
            val snapshot = roomsRef.child(roomId).child("players").get().await()
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                roomsRef.child(roomId).removeValue().await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get available public rooms for matchmaking
     */
    fun observeAvailableRooms(gameMode: GameMode): Flow<List<GameRoom>> = callbackFlow {
        val query = roomsRef
            .orderByChild("status")
            .equalTo(RoomStatus.WAITING.name)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = snapshot.children.mapNotNull { parseRoom(it) }
                    .filter {
                        it.gameMode == gameMode.name &&
                        !it.isPrivate &&
                        it.players.size < it.maxPlayers
                    }
                    .sortedByDescending { it.createdAt }
                trySend(rooms)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("GameRepository", "observeAvailableRooms cancelled: ${error.message}")
                trySend(emptyList())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    /**
     * Add to matchmaking queue
     */
    suspend fun joinMatchmaking(
        playerId: String,
        playerName: String,
        gameMode: GameMode,
        difficulty: Difficulty
    ): Result<String> {
        return try {
            val entry = MatchmakingEntry(
                playerId = playerId,
                displayName = playerName,
                gameMode = gameMode.name,
                difficulty = difficulty.name,
                timestamp = System.currentTimeMillis()
            )
            
            matchmakingRef.child(playerId).setValue(entry.toMap()).await()
            Result.success(playerId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Leave matchmaking queue
     */
    suspend fun leaveMatchmaking(playerId: String): Result<Unit> {
        return try {
            matchmakingRef.child(playerId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe matchmaking queue for potential matches
     */
    fun observeMatchmaking(
        playerId: String,
        gameMode: GameMode,
        difficulty: Difficulty
    ): Flow<MatchmakingEntry?> = callbackFlow {
        val query = matchmakingRef
            .orderByChild("gameMode")
            .equalTo(gameMode.name)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = snapshot.children.mapNotNull {
                    it.getValue(MatchmakingEntry::class.java)
                }

                // Find a match (different player, same mode and difficulty)
                val match = entries.firstOrNull {
                    it.playerId != playerId &&
                    it.difficulty == difficulty.name
                }

                trySend(match)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("GameRepository", "observeMatchmaking cancelled: ${error.message}")
                trySend(null)
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    /**
     * Update player stats after game
     */
    suspend fun updatePlayerStats(
        playerId: String,
        won: Boolean,
        score: Int
    ): Result<Unit> {
        return try {
            val playerRef = playersRef.child(playerId)
            val snapshot = playerRef.get().await()
            val player = snapshot.getValue(Player::class.java) ?: return Result.failure(Exception("Player not found"))
            
            val updates = mapOf(
                "gamesPlayed" to (player.gamesPlayed + 1),
                "gamesWon" to (player.gamesWon + if (won) 1 else 0),
                "totalScore" to (player.totalScore + score)
            )
            
            playerRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse a DataSnapshot into a GameRoom
     */
    private fun parseRoom(snapshot: DataSnapshot): GameRoom? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java) ?: return null
            val code = snapshot.child("code").getValue(String::class.java) ?: ""
            val hostId = snapshot.child("hostId").getValue(String::class.java) ?: ""
            val gameMode = snapshot.child("gameMode").getValue(String::class.java) ?: GameMode.COMPETITIVE.name
            val difficulty = snapshot.child("difficulty").getValue(String::class.java) ?: Difficulty.MEDIUM.name
            val status = snapshot.child("status").getValue(String::class.java) ?: RoomStatus.WAITING.name
            val isPrivate = snapshot.child("isPrivate").getValue(Boolean::class.java) ?: false
            val maxPlayers = snapshot.child("maxPlayers").getValue(Int::class.java) ?: 2
            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            val startedAt = snapshot.child("startedAt").getValue(Long::class.java)
            val finishedAt = snapshot.child("finishedAt").getValue(Long::class.java)
            val winnerId = snapshot.child("winnerId").getValue(String::class.java)
            
            // Parse players
            val players = mutableMapOf<String, RoomPlayer>()
            snapshot.child("players").children.forEach { playerSnapshot ->
                val playerId = playerSnapshot.key ?: return@forEach
                val player = playerSnapshot.getValue(RoomPlayer::class.java) ?: return@forEach
                players[playerId] = player
            }
            
            // Parse board
            val boardSnapshot = snapshot.child("board")
            val cells = mutableListOf<List<SudokuCell>>()
            val solution = mutableListOf<List<Int>>()
            
            boardSnapshot.child("cells").children.forEachIndexed { _, rowSnapshot ->
                val row = mutableListOf<SudokuCell>()
                rowSnapshot.children.forEach { cellSnapshot ->
                    val value = cellSnapshot.child("value").getValue(Int::class.java) ?: 0
                    val isFixed = cellSnapshot.child("isFixed").getValue(Boolean::class.java) ?: false
                    val isError = cellSnapshot.child("isError").getValue(Boolean::class.java) ?: false
                    val enteredBy = cellSnapshot.child("enteredBy").getValue(String::class.java)
                    row.add(SudokuCell(value, isFixed, emptyList(), isError, enteredBy))
                }
                if (row.size == 9) cells.add(row)
            }
            
            boardSnapshot.child("solution").children.forEach { rowSnapshot ->
                val row = rowSnapshot.children.mapNotNull { it.getValue(Int::class.java) }
                if (row.size == 9) solution.add(row)
            }
            
            val board = if (cells.size == 9 && solution.size == 9) {
                SudokuBoard(cells, solution)
            } else {
                SudokuBoard()
            }
            
            GameRoom(
                id = id,
                code = code,
                hostId = hostId,
                gameMode = gameMode,
                difficulty = difficulty,
                status = status,
                isPrivate = isPrivate,
                maxPlayers = maxPlayers,
                players = players,
                board = board,
                createdAt = createdAt,
                startedAt = startedAt,
                finishedAt = finishedAt,
                winnerId = winnerId
            )
        } catch (e: Exception) {
            null
        }
    }
}
