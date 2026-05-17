package de.sudokuonline.app.data.repository

import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.TicTacToeLogic
import de.sudokuonline.app.game.UltimateTicTacToeLogic
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
// RematchStatus is imported via de.sudokuonline.app.data.model.*

class TicTacToeRepository {
    private val database = FirebaseDatabase.getInstance("https://sudokuonline-f59b9-default-rtdb.europe-west1.firebasedatabase.app")
    private val roomsRef = database.getReference("tictactoe_rooms")
    private val matchmakingRef = database.getReference("tictactoe_matchmaking")
    private val playersRef = database.getReference("players")

    /**
     * Generate a unique 6-character room code
     */
    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    /**
     * Create a new TicTacToe room
     */
    suspend fun createRoom(
        hostId: String,
        hostName: String,
        gameMode: TicTacToeGameMode,
        boardSize: TicTacToeBoardSize,
        isPrivate: Boolean
    ): Result<TicTacToeRoom> {
        return try {
            val roomId = roomsRef.push().key ?: throw Exception("Failed to generate room ID")
            val roomCode = if (isPrivate) generateRoomCode() else ""

            val board = TicTacToeBoard.create(boardSize)
            val bombsRemaining = if (gameMode != TicTacToeGameMode.CLASSIC) 3 else 0

            val hostPlayer = TicTacToeRoomPlayer(
                playerId = hostId,
                displayName = hostName,
                status = PlayerStatus.READY.name,
                symbol = 1, // Host is X
                bombsRemaining = bombsRemaining,
                score = 0,
                wins = 0,
                lastActive = System.currentTimeMillis()
            )

            val room = TicTacToeRoom(
                id = roomId,
                code = roomCode,
                hostId = hostId,
                gameMode = gameMode.name,
                boardSize = boardSize.name,
                status = RoomStatus.WAITING.name,
                isPrivate = isPrivate,
                maxPlayers = 2,
                players = mapOf(hostId to hostPlayer),
                board = board,
                currentTurnPlayerId = hostId, // X (host) starts
                currentTurnSymbol = 1,
                roundNumber = 1,
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
    ): Result<TicTacToeRoom> {
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

            val gameMode = room.getGameModeEnum()
            val bombsRemaining = if (gameMode != TicTacToeGameMode.CLASSIC) 3 else 0

            val newPlayer = TicTacToeRoomPlayer(
                playerId = playerId,
                displayName = playerName,
                status = PlayerStatus.READY.name,
                symbol = 2, // Joiner is O
                bombsRemaining = bombsRemaining,
                score = 0,
                wins = 0,
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
    ): Result<TicTacToeRoom> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: throw Exception("Room not found")

            if (room.status != RoomStatus.WAITING.name) {
                return Result.failure(Exception("Spiel bereits gestartet"))
            }

            if (room.players.size >= room.maxPlayers) {
                return Result.failure(Exception("Raum ist voll"))
            }

            val gameMode = room.getGameModeEnum()
            val bombsRemaining = if (gameMode != TicTacToeGameMode.CLASSIC) 3 else 0

            val newPlayer = TicTacToeRoomPlayer(
                playerId = playerId,
                displayName = playerName,
                status = PlayerStatus.READY.name,
                symbol = 2, // Joiner is O
                bombsRemaining = bombsRemaining,
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
    fun observeRoom(roomId: String): Flow<TicTacToeRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = parseRoom(snapshot)
                trySend(room)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        roomsRef.child(roomId).addValueEventListener(listener)
        awaitClose { roomsRef.child(roomId).removeEventListener(listener) }
    }

    /**
     * Start the game
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
     * Make a move (place X or O)
     */
    suspend fun makeMove(
        roomId: String,
        playerId: String,
        row: Int,
        col: Int,
        symbol: Int
    ): Result<Unit> {
        return try {
            val cellPath = "board/cells/$row/$col"
            val updates = mutableMapOf<String, Any?>(
                "$cellPath/value" to symbol,
                "$cellPath/playerId" to playerId,
                "$cellPath/isBomb" to false,
                "$cellPath/bombOwnerId" to null
            )

            // Switch turn
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot)
            if (room != null) {
                val nextPlayerId = room.players.keys.firstOrNull { it != playerId } ?: playerId
                val nextSymbol = if (symbol == 1) 2 else 1
                updates["currentTurnPlayerId"] = nextPlayerId
                updates["currentTurnSymbol"] = nextSymbol
            }

            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Place and detonate a bomb
     */
    suspend fun placeBomb(
        roomId: String,
        playerId: String,
        row: Int,
        col: Int,
        gameMode: TicTacToeGameMode,
        newBoard: TicTacToeBoard
    ): Result<Unit> {
        return try {
            // Update the entire board after bomb detonation
            val boardMap = newBoard.toMap()

            val updates = mutableMapOf<String, Any?>(
                "board" to boardMap,
                "players/$playerId/bombsRemaining" to ServerValue.increment(-1)
            )

            // Switch turn
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot)
            if (room != null) {
                val nextPlayerId = room.players.keys.firstOrNull { it != playerId } ?: playerId
                val currentSymbol = room.players[playerId]?.symbol ?: 1
                val nextSymbol = if (currentSymbol == 1) 2 else 1
                updates["currentTurnPlayerId"] = nextPlayerId
                updates["currentTurnSymbol"] = nextSymbol
            }

            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Make a move in Ultimate TicTacToe
     */
    suspend fun makeUltimateMove(
        roomId: String,
        playerId: String,
        boardRow: Int,
        boardCol: Int,
        cellRow: Int,
        cellCol: Int,
        symbol: Int,
        newBoard: UltimateTicTacToeBoard
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>(
                "ultimateBoard" to newBoard.toMap()
            )

            // Switch turn
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot)
            if (room != null) {
                val nextPlayerId = room.players.keys.firstOrNull { it != playerId } ?: playerId
                val nextSymbol = if (symbol == 1) 2 else 1
                updates["currentTurnPlayerId"] = nextPlayerId
                updates["currentTurnSymbol"] = nextSymbol
            }

            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * End the game
     */
    suspend fun endGame(roomId: String, winnerId: String?, isDraw: Boolean): Result<Unit> {
        return try {
            // Get current series score
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot)
            val seriesScore = room?.seriesScore?.toMutableMap() ?: mutableMapOf()
            
            // Update series score
            if (winnerId != null) {
                seriesScore[winnerId] = (seriesScore[winnerId] ?: 0) + 1
            }
            
            val updates = mapOf(
                "status" to RoomStatus.FINISHED.name,
                "finishedAt" to ServerValue.TIMESTAMP,
                "winnerId" to winnerId,
                "isDraw" to isDraw,
                "seriesScore" to seriesScore,
                "rematchStatus" to RematchStatus.NONE.name,
                "rematchRequestedBy" to null
            )
            roomsRef.child(roomId).updateChildren(updates).await()

            // Update player stats
            if (winnerId != null) {
                updatePlayerStats(winnerId, won = true, score = 100)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Request a rematch
     */
    suspend fun requestRematch(roomId: String, playerId: String): Result<Unit> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: return Result.failure(Exception("Room not found"))
            
            // Check if other player already requested rematch
            if (room.rematchStatus == RematchStatus.PENDING.name && 
                room.rematchRequestedBy != null && 
                room.rematchRequestedBy != playerId) {
                // Both players want rematch - start new game
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
    
    /**
     * Accept a rematch request
     */
    suspend fun acceptRematch(roomId: String, playerId: String): Result<Unit> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: return Result.failure(Exception("Room not found"))
            
            // Verify that the other player requested the rematch
            if (room.rematchRequestedBy == playerId) {
                return Result.failure(Exception("You requested the rematch"))
            }
            
            // Start the rematch
            return startRematch(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Decline a rematch request
     */
    suspend fun declineRematch(roomId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "rematchStatus" to RematchStatus.DECLINED.name
            )
            roomsRef.child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start a rematch - reset board and swap starting player
     */
    private suspend fun startRematch(roomId: String): Result<Unit> {
        return try {
            val snapshot = roomsRef.child(roomId).get().await()
            val room = parseRoom(snapshot) ?: return Result.failure(Exception("Room not found"))
            
            val boardSize = room.getBoardSizeEnum()
            val gameMode = room.getGameModeEnum()
            val newBoard = TicTacToeBoard.create(boardSize)
            
            // Swap starting player (loser starts, or if draw, previous non-starter)
            val previousStarter = room.players.values.find { it.symbol == 1 }?.playerId
            val newStarter = room.players.keys.find { it != previousStarter } ?: previousStarter ?: ""
            
            // Reset bombs for bomb modes
            val bombsRemaining = if (gameMode != TicTacToeGameMode.CLASSIC) 3 else 0
            val updatedPlayers = room.players.mapValues { (playerId, player) ->
                player.copy(
                    bombsRemaining = bombsRemaining,
                    symbol = if (playerId == newStarter) 1 else 2  // Swap symbols
                ).toMap()
            }
            
            val updates = mapOf(
                "status" to RoomStatus.IN_PROGRESS.name,
                "board" to newBoard.toMap(),
                "ultimateBoard" to UltimateTicTacToeBoard().toMap(),
                "currentTurnPlayerId" to newStarter,
                "currentTurnSymbol" to 1,
                "roundNumber" to (room.roundNumber + 1),
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
     * Get available public rooms
     */
    fun observeAvailableRooms(): Flow<List<TicTacToeRoom>> = callbackFlow {
        val query = roomsRef
            .orderByChild("status")
            .equalTo(RoomStatus.WAITING.name)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = snapshot.children.mapNotNull { parseRoom(it) }
                    .filter { !it.isPrivate && it.players.size < it.maxPlayers }
                    .sortedByDescending { it.createdAt }
                trySend(rooms)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * Update player stats after game
     */
    private suspend fun updatePlayerStats(
        playerId: String,
        won: Boolean,
        score: Int
    ): Result<Unit> {
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

    /**
     * Parse a DataSnapshot into a TicTacToeRoom
     */
    private fun parseRoom(snapshot: DataSnapshot): TicTacToeRoom? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java) ?: return null
            val code = snapshot.child("code").getValue(String::class.java) ?: ""
            val hostId = snapshot.child("hostId").getValue(String::class.java) ?: ""
            val gameMode = snapshot.child("gameMode").getValue(String::class.java)
                ?: TicTacToeGameMode.CLASSIC.name
            val boardSizeStr = snapshot.child("boardSize").getValue(String::class.java)
                ?: TicTacToeBoardSize.SMALL.name
            val status = snapshot.child("status").getValue(String::class.java)
                ?: RoomStatus.WAITING.name
            val isPrivate = snapshot.child("isPrivate").getValue(Boolean::class.java) ?: false
            val maxPlayers = snapshot.child("maxPlayers").getValue(Int::class.java) ?: 2
            val currentTurnPlayerId = snapshot.child("currentTurnPlayerId").getValue(String::class.java) ?: ""
            val currentTurnSymbol = snapshot.child("currentTurnSymbol").getValue(Int::class.java) ?: 1
            val roundNumber = snapshot.child("roundNumber").getValue(Int::class.java) ?: 1
            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            val startedAt = snapshot.child("startedAt").getValue(Long::class.java)
            val finishedAt = snapshot.child("finishedAt").getValue(Long::class.java)
            val winnerId = snapshot.child("winnerId").getValue(String::class.java)
            val isDraw = snapshot.child("isDraw").getValue(Boolean::class.java) ?: false

            // Parse players
            val players = mutableMapOf<String, TicTacToeRoomPlayer>()
            snapshot.child("players").children.forEach { playerSnapshot ->
                val playerId = playerSnapshot.key ?: return@forEach
                val player = TicTacToeRoomPlayer(
                    playerId = playerSnapshot.child("playerId").getValue(String::class.java) ?: "",
                    displayName = playerSnapshot.child("displayName").getValue(String::class.java) ?: "",
                    status = playerSnapshot.child("status").getValue(String::class.java)
                        ?: PlayerStatus.WAITING.name,
                    symbol = playerSnapshot.child("symbol").getValue(Int::class.java) ?: 0,
                    bombsRemaining = playerSnapshot.child("bombsRemaining").getValue(Int::class.java) ?: 3,
                    score = playerSnapshot.child("score").getValue(Int::class.java) ?: 0,
                    wins = playerSnapshot.child("wins").getValue(Int::class.java) ?: 0,
                    lastActive = playerSnapshot.child("lastActive").getValue(Long::class.java)
                        ?: System.currentTimeMillis()
                )
                players[playerId] = player
            }

            // Parse board
            val boardSnapshot = snapshot.child("board")
            val boardSize = try {
                TicTacToeBoardSize.valueOf(boardSizeStr)
            } catch (e: Exception) {
                TicTacToeBoardSize.SMALL
            }

            val size = boardSnapshot.child("size").getValue(Int::class.java) ?: boardSize.size
            val winCondition = boardSnapshot.child("winCondition").getValue(Int::class.java)
                ?: boardSize.winCondition

            val cells = mutableListOf<List<TicTacToeCell>>()
            boardSnapshot.child("cells").children.forEachIndexed { _, rowSnapshot ->
                val row = mutableListOf<TicTacToeCell>()
                rowSnapshot.children.forEach { cellSnapshot ->
                    val value = cellSnapshot.child("value").getValue(Int::class.java) ?: 0
                    val cellPlayerId = cellSnapshot.child("playerId").getValue(String::class.java)
                    val isBomb = cellSnapshot.child("isBomb").getValue(Boolean::class.java) ?: false
                    val bombOwnerId = cellSnapshot.child("bombOwnerId").getValue(String::class.java)
                    row.add(TicTacToeCell(value, cellPlayerId, isBomb, bombOwnerId))
                }
                if (row.size == size) cells.add(row)
            }

            val board = if (cells.size == size) {
                TicTacToeBoard(cells, size, winCondition)
            } else {
                TicTacToeBoard.create(boardSize)
            }

            // Parse rematch fields
            val rematchStatus = snapshot.child("rematchStatus").getValue(String::class.java)
                ?: RematchStatus.NONE.name
            val rematchRequestedBy = snapshot.child("rematchRequestedBy").getValue(String::class.java)

            // Parse series score
            val seriesScore = mutableMapOf<String, Int>()
            snapshot.child("seriesScore").children.forEach { scoreSnapshot ->
                val key = scoreSnapshot.key ?: return@forEach
                val value = scoreSnapshot.getValue(Int::class.java) ?: 0
                seriesScore[key] = value
            }

            // Parse ultimate board
            val ultimateBoardSnapshot = snapshot.child("ultimateBoard")
            val ultimateBoard = if (ultimateBoardSnapshot.exists()) {
                val miniBoards = mutableListOf<List<UltimateMiniBoard>>()
                ultimateBoardSnapshot.child("miniBoards").children.forEachIndexed { _, rowSnapshot ->
                    val row = mutableListOf<UltimateMiniBoard>()
                    rowSnapshot.children.forEach { miniBoardSnapshot ->
                        val miniCells = mutableListOf<List<TicTacToeCell>>()
                        miniBoardSnapshot.child("cells").children.forEach { cellRowSnapshot ->
                            val cellRow = mutableListOf<TicTacToeCell>()
                            cellRowSnapshot.children.forEach { cellSnapshot ->
                                val cellValue = cellSnapshot.child("value").getValue(Int::class.java) ?: 0
                                val cellPId = cellSnapshot.child("playerId").getValue(String::class.java)
                                val cellIsBomb = cellSnapshot.child("isBomb").getValue(Boolean::class.java) ?: false
                                val cellBombOwner = cellSnapshot.child("bombOwnerId").getValue(String::class.java)
                                cellRow.add(TicTacToeCell(cellValue, cellPId, cellIsBomb, cellBombOwner))
                            }
                            if (cellRow.size == 3) miniCells.add(cellRow)
                        }
                        val miniWinner = miniBoardSnapshot.child("winner").getValue(Int::class.java) ?: 0
                        row.add(UltimateMiniBoard(
                            cells = if (miniCells.size == 3) miniCells else List(3) { List(3) { TicTacToeCell() } },
                            winner = miniWinner
                        ))
                    }
                    if (row.size == 3) miniBoards.add(row)
                }
                val activeBoardRow = ultimateBoardSnapshot.child("activeBoardRow").getValue(Int::class.java)
                val activeBoardCol = ultimateBoardSnapshot.child("activeBoardCol").getValue(Int::class.java)
                val activeBoard = if (activeBoardRow != null && activeBoardCol != null) {
                    Pair(activeBoardRow, activeBoardCol)
                } else null
                if (miniBoards.size == 3) {
                    UltimateTicTacToeBoard(miniBoards, activeBoard)
                } else {
                    UltimateTicTacToeBoard()
                }
            } else {
                UltimateTicTacToeBoard()
            }

            TicTacToeRoom(
                id = id,
                code = code,
                hostId = hostId,
                gameMode = gameMode,
                boardSize = boardSizeStr,
                status = status,
                isPrivate = isPrivate,
                maxPlayers = maxPlayers,
                players = players,
                board = board,
                ultimateBoard = ultimateBoard,
                currentTurnPlayerId = currentTurnPlayerId,
                currentTurnSymbol = currentTurnSymbol,
                roundNumber = roundNumber,
                createdAt = createdAt,
                startedAt = startedAt,
                finishedAt = finishedAt,
                winnerId = winnerId,
                isDraw = isDraw,
                rematchStatus = rematchStatus,
                rematchRequestedBy = rematchRequestedBy,
                seriesScore = seriesScore
            )
        } catch (e: Exception) {
            null
        }
    }
}
