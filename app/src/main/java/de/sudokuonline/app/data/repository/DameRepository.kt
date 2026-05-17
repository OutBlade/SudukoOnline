package de.sudokuonline.app.data.repository

import de.sudokuonline.app.data.model.*
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DameRepository {
    private val database = FirebaseDatabase.getInstance("https://sudokuonline-f59b9-default-rtdb.europe-west1.firebasedatabase.app")
    private val roomsRef = database.getReference("dame_rooms")
    private val playersRef = database.getReference("players")

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    suspend fun createRoom(
        hostId: String,
        hostName: String,
        isPrivate: Boolean
    ): Result<DameRoom> {
        return try {
            val roomId = roomsRef.push().key ?: throw Exception("Failed to generate room ID")
            val roomCode = if (isPrivate) generateRoomCode() else ""
            val board = DameBoard.create()

            val hostPlayer = DameRoomPlayer(
                playerId = hostId,
                displayName = hostName,
                status = PlayerStatus.READY.name,
                playerColor = DamePlayerColor.WHITE.name,
                piecesRemaining = 12,
                score = 0,
                lastActive = System.currentTimeMillis()
            )

            val room = DameRoom(
                id = roomId,
                code = roomCode,
                hostId = hostId,
                status = RoomStatus.WAITING.name,
                isPrivate = isPrivate,
                maxPlayers = 2,
                players = mapOf(hostId to hostPlayer),
                board = board,
                currentTurnPlayerId = hostId,
                currentPlayerColor = DamePlayerColor.WHITE.name,
                createdAt = System.currentTimeMillis()
            )

            roomsRef.child(roomId).setValue(room.toMap()).await()
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(
        code: String,
        playerId: String,
        playerName: String
    ): Result<DameRoom> {
        return try {
            val snapshot = roomsRef.orderByChild("code").equalTo(code).get().await()
            if (!snapshot.exists()) return Result.failure(Exception("Raum nicht gefunden"))

            val roomSnapshot = snapshot.children.first()
            val room = parseRoom(roomSnapshot) ?: throw Exception("Invalid room data")

            if (room.status != RoomStatus.WAITING.name) return Result.failure(Exception("Spiel bereits gestartet"))
            if (room.players.size >= room.maxPlayers) return Result.failure(Exception("Raum ist voll"))

            val newPlayer = DameRoomPlayer(
                playerId = playerId,
                displayName = playerName,
                status = PlayerStatus.READY.name,
                playerColor = DamePlayerColor.BLACK.name,
                piecesRemaining = 12,
                score = 0,
                lastActive = System.currentTimeMillis()
            )

            roomsRef.child(room.id).child("players").child(playerId)
                .setValue(newPlayer.toMap()).await()

            Result.success(room.copy(players = room.players + (playerId to newPlayer)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoom(
        roomId: String,
        playerId: String,
        playerName: String
    ): Result<DameRoom> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: throw Exception("Room not found")

            if (room.status != RoomStatus.WAITING.name) return Result.failure(Exception("Spiel bereits gestartet"))
            if (room.players.size >= room.maxPlayers) return Result.failure(Exception("Raum ist voll"))

            val newPlayer = DameRoomPlayer(
                playerId = playerId,
                displayName = playerName,
                status = PlayerStatus.READY.name,
                playerColor = DamePlayerColor.BLACK.name,
                piecesRemaining = 12,
                score = 0,
                lastActive = System.currentTimeMillis()
            )

            roomsRef.child(roomId).child("players").child(playerId)
                .setValue(newPlayer.toMap()).await()

            Result.success(room.copy(players = room.players + (playerId to newPlayer)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeRoom(roomId: String): Flow<DameRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseRoom(snapshot))
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("DameRepository", "observeRoom cancelled: ${error.message}")
                trySend(null)
            }
        }
        roomsRef.child(roomId).addValueEventListener(listener)
        awaitClose { roomsRef.child(roomId).removeEventListener(listener) }
    }

    suspend fun startGame(roomId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to RoomStatus.IN_PROGRESS.name,
                "startedAt" to ServerValue.TIMESTAMP
            )
            roomsRef.child(roomId).updateChildren(updates).await()

            val playersSnapshot = roomsRef.child(roomId).child("players").get().await()
            playersSnapshot.children.forEach { playerSnapshot ->
                val pid = playerSnapshot.key ?: return@forEach
                roomsRef.child(roomId).child("players").child(pid)
                    .child("status").setValue(PlayerStatus.PLAYING.name).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun makeMove(
        roomId: String,
        newBoard: DameBoard,
        nextPlayerId: String,
        nextColor: DamePlayerColor,
        mustContinueJump: Boolean,
        continuingRow: Int,
        continuingCol: Int,
        whitePieces: Int,
        blackPieces: Int
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>(
                "board" to newBoard.toMap(),
                "currentTurnPlayerId" to nextPlayerId,
                "currentPlayerColor" to nextColor.name,
                "mustContinueJump" to mustContinueJump,
                "continuingPieceRow" to continuingRow,
                "continuingPieceCol" to continuingCol
            )

            // Update piece counts for each player
            val playersSnapshot = roomsRef.child(roomId).child("players").get().await()
            playersSnapshot.children.forEach { ps ->
                val pid = ps.key ?: return@forEach
                val color = ps.child("playerColor").getValue(String::class.java) ?: return@forEach
                val count = if (color == DamePlayerColor.WHITE.name) whitePieces else blackPieces
                updates["players/$pid/piecesRemaining"] = count
                updates["players/$pid/lastActive"] = ServerValue.TIMESTAMP
            }

            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endGame(roomId: String, winnerId: String?, isDraw: Boolean): Result<Unit> {
        return try {
            val updates = mapOf<String, Any?>(
                "status" to RoomStatus.FINISHED.name,
                "finishedAt" to ServerValue.TIMESTAMP,
                "winnerId" to winnerId,
                "isDraw" to isDraw,
                "rematchStatus" to RematchStatus.NONE.name,
                "rematchRequestedBy" to null
            )
            roomsRef.child(roomId).updateChildren(updates).await()

            if (winnerId != null) {
                updatePlayerStats(winnerId, won = true, score = 100)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestRematch(roomId: String, playerId: String): Result<Unit> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: return Result.failure(Exception("Room not found"))

            if (room.rematchStatus == RematchStatus.PENDING.name &&
                room.rematchRequestedBy != null &&
                room.rematchRequestedBy != playerId
            ) {
                return startRematch(roomId)
            }

            val updates = mapOf(
                "rematchStatus" to RematchStatus.PENDING.name,
                "rematchRequestedBy" to playerId
            )
            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptRematch(roomId: String, playerId: String): Result<Unit> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: return Result.failure(Exception("Room not found"))
            if (room.rematchRequestedBy == playerId) return Result.failure(Exception("You requested the rematch"))
            return startRematch(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineRematch(roomId: String): Result<Unit> {
        return try {
            roomsRef.child(roomId).updateChildren(mapOf("rematchStatus" to RematchStatus.DECLINED.name)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun startRematch(roomId: String): Result<Unit> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: return Result.failure(Exception("Room not found"))

            val newBoard = DameBoard.create()

            // Swap colors
            val updatedPlayers = room.players.mapValues { (_, player) ->
                val newColor = if (player.playerColor == DamePlayerColor.WHITE.name)
                    DamePlayerColor.BLACK.name else DamePlayerColor.WHITE.name
                player.copy(
                    playerColor = newColor,
                    piecesRemaining = 12
                ).toMap()
            }

            // New white player starts
            val newWhitePlayerId = room.players.entries
                .firstOrNull { it.value.playerColor == DamePlayerColor.BLACK.name }?.key ?: ""

            val updates = mapOf<String, Any?>(
                "status" to RoomStatus.IN_PROGRESS.name,
                "board" to newBoard.toMap(),
                "currentTurnPlayerId" to newWhitePlayerId,
                "currentPlayerColor" to DamePlayerColor.WHITE.name,
                "mustContinueJump" to false,
                "continuingPieceRow" to -1,
                "continuingPieceCol" to -1,
                "startedAt" to ServerValue.TIMESTAMP,
                "finishedAt" to null,
                "winnerId" to null,
                "isDraw" to false,
                "rematchStatus" to RematchStatus.ACCEPTED.name,
                "rematchRequestedBy" to null,
                "players" to updatedPlayers
            )

            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomId: String, playerId: String): Result<Unit> {
        return try {
            roomsRef.child(roomId).child("players").child(playerId).removeValue().await()
            val snapshot = roomsRef.child(roomId).child("players").get().await()
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                roomsRef.child(roomId).removeValue().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAvailableRooms(): Flow<List<DameRoom>> = callbackFlow {
        val query = roomsRef.orderByChild("status").equalTo(RoomStatus.WAITING.name)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = snapshot.children.mapNotNull { parseRoom(it) }
                    .filter { !it.isPrivate && it.players.size < it.maxPlayers }
                    .sortedByDescending { it.createdAt }
                trySend(rooms)
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("DameRepository", "observeAvailableRooms cancelled: ${error.message}")
                trySend(emptyList())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    private suspend fun updatePlayerStats(playerId: String, won: Boolean, score: Int): Result<Unit> {
        return try {
            val playerRef = playersRef.child(playerId)
            val snapshot = playerRef.get().await()
            val player = snapshot.getValue(Player::class.java)
                ?: return Result.failure(Exception("Player not found"))
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

    private fun parseRoom(snapshot: DataSnapshot): DameRoom? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java) ?: return null
            val code = snapshot.child("code").getValue(String::class.java) ?: ""
            val hostId = snapshot.child("hostId").getValue(String::class.java) ?: ""
            val status = snapshot.child("status").getValue(String::class.java) ?: RoomStatus.WAITING.name
            val isPrivate = snapshot.child("isPrivate").getValue(Boolean::class.java) ?: false
            val maxPlayers = snapshot.child("maxPlayers").getValue(Int::class.java) ?: 2
            val currentTurnPlayerId = snapshot.child("currentTurnPlayerId").getValue(String::class.java) ?: ""
            val currentPlayerColor = snapshot.child("currentPlayerColor").getValue(String::class.java) ?: DamePlayerColor.WHITE.name
            val mustContinueJump = snapshot.child("mustContinueJump").getValue(Boolean::class.java) ?: false
            val continuingPieceRow = snapshot.child("continuingPieceRow").getValue(Int::class.java) ?: -1
            val continuingPieceCol = snapshot.child("continuingPieceCol").getValue(Int::class.java) ?: -1
            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            val startedAt = snapshot.child("startedAt").getValue(Long::class.java)
            val finishedAt = snapshot.child("finishedAt").getValue(Long::class.java)
            val winnerId = snapshot.child("winnerId").getValue(String::class.java)
            val isDraw = snapshot.child("isDraw").getValue(Boolean::class.java) ?: false
            val rematchStatus = snapshot.child("rematchStatus").getValue(String::class.java) ?: RematchStatus.NONE.name
            val rematchRequestedBy = snapshot.child("rematchRequestedBy").getValue(String::class.java)

            // Parse players
            val players = mutableMapOf<String, DameRoomPlayer>()
            snapshot.child("players").children.forEach { ps ->
                val pid = ps.key ?: return@forEach
                players[pid] = DameRoomPlayer(
                    playerId = ps.child("playerId").getValue(String::class.java) ?: "",
                    displayName = ps.child("displayName").getValue(String::class.java) ?: "",
                    status = ps.child("status").getValue(String::class.java) ?: PlayerStatus.WAITING.name,
                    playerColor = ps.child("playerColor").getValue(String::class.java) ?: DamePlayerColor.WHITE.name,
                    piecesRemaining = ps.child("piecesRemaining").getValue(Int::class.java) ?: 12,
                    score = ps.child("score").getValue(Int::class.java) ?: 0,
                    lastActive = ps.child("lastActive").getValue(Long::class.java) ?: System.currentTimeMillis()
                )
            }

            // Parse board
            val cells = mutableListOf<List<DameCell>>()
            snapshot.child("board").child("cells").children.forEachIndexed { _, rowSnapshot ->
                val row = mutableListOf<DameCell>()
                rowSnapshot.children.forEach { cellSnapshot ->
                    row.add(
                        DameCell(
                            row = cellSnapshot.child("row").getValue(Int::class.java) ?: 0,
                            col = cellSnapshot.child("col").getValue(Int::class.java) ?: 0,
                            pieceType = cellSnapshot.child("pieceType").getValue(String::class.java) ?: DamePieceType.NONE.name,
                            owner = cellSnapshot.child("owner").getValue(String::class.java) ?: ""
                        )
                    )
                }
                if (row.size == 8) cells.add(row)
            }

            val board = if (cells.size == 8) DameBoard(cells) else DameBoard.create()

            DameRoom(
                id = id, code = code, hostId = hostId, status = status, isPrivate = isPrivate,
                maxPlayers = maxPlayers, players = players, board = board,
                currentTurnPlayerId = currentTurnPlayerId, currentPlayerColor = currentPlayerColor,
                mustContinueJump = mustContinueJump, continuingPieceRow = continuingPieceRow,
                continuingPieceCol = continuingPieceCol, createdAt = createdAt, startedAt = startedAt,
                finishedAt = finishedAt, winnerId = winnerId, isDraw = isDraw,
                rematchStatus = rematchStatus, rematchRequestedBy = rematchRequestedBy
            )
        } catch (e: Exception) {
            null
        }
    }
}
