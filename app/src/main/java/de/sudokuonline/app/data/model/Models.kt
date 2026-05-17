package de.sudokuonline.app.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

// Sudoku difficulty levels
enum class Difficulty(val cellsToRemove: Int, val displayName: String) {
    EASY(35, "Einfach"),
    MEDIUM(45, "Mittel"),
    HARD(52, "Schwer"),
    EXPERT(58, "Experte")
}

// Game modes
enum class GameMode(val displayName: String) {
    SINGLE_PLAYER("Einzelspieler"),
    COMPETITIVE("1v1 Wettkampf"),
    COOP("Kooperativ"),
    PRACTICE("Übung")
}

// Room status
enum class RoomStatus {
    WAITING,      // Waiting for players
    STARTING,     // Game is starting
    IN_PROGRESS,  // Game in progress
    FINISHED,     // Game completed
    ABANDONED     // Room abandoned
}

// Player status in a room
enum class PlayerStatus {
    WAITING,
    READY,
    PLAYING,
    FINISHED,
    DISCONNECTED
}

// Single cell in the Sudoku grid
@IgnoreExtraProperties
data class SudokuCell(
    val value: Int = 0,           // Current value (0 = empty)
    val isFixed: Boolean = false, // Part of the original puzzle
    val notes: List<Int> = emptyList(), // Pencil marks
    val isError: Boolean = false, // Has conflict
    val enteredBy: String? = null // Player ID who entered (for coop/competitive)
) {
    // Firebase requires no-arg constructor
    constructor() : this(0, false, emptyList(), false, null)
    
    fun toMap(): Map<String, Any?> = mapOf(
        "value" to value,
        "isFixed" to isFixed,
        "notes" to notes,
        "isError" to isError,
        "enteredBy" to enteredBy
    )
}

// Complete Sudoku board state
@IgnoreExtraProperties
data class SudokuBoard(
    val cells: List<List<SudokuCell>> = List(9) { List(9) { SudokuCell() } },
    val solution: List<List<Int>> = List(9) { List(9) { 0 } }
) {
    constructor() : this(List(9) { List(9) { SudokuCell() } }, List(9) { List(9) { 0 } })
    
    fun toMap(): Map<String, Any> = mapOf(
        "cells" to cells.map { row -> row.map { it.toMap() } },
        "solution" to solution
    )
}

// Player info
@IgnoreExtraProperties
data class Player(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val totalScore: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", 0, 0, 0, System.currentTimeMillis())
    
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "email" to email,
        "avatarUrl" to avatarUrl,
        "gamesPlayed" to gamesPlayed,
        "gamesWon" to gamesWon,
        "totalScore" to totalScore,
        "createdAt" to createdAt
    )
}

// Player state within a game room
@IgnoreExtraProperties
data class RoomPlayer(
    val playerId: String = "",
    val displayName: String = "",
    val status: String = PlayerStatus.WAITING.name,
    val progress: Int = 0,           // Cells filled correctly (0-81)
    val errors: Int = 0,             // Number of errors made
    val score: Int = 0,              // Current score
    val finishedAt: Long? = null,    // Timestamp when finished
    val lastActive: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", PlayerStatus.WAITING.name, 0, 0, 0, null, System.currentTimeMillis())
    
    fun toMap(): Map<String, Any?> = mapOf(
        "playerId" to playerId,
        "displayName" to displayName,
        "status" to status,
        "progress" to progress,
        "errors" to errors,
        "score" to score,
        "finishedAt" to finishedAt,
        "lastActive" to lastActive
    )
}

// Game room for multiplayer
@IgnoreExtraProperties
data class GameRoom(
    val id: String = "",
    val code: String = "",           // 6-digit room code for private rooms
    val hostId: String = "",
    val gameMode: String = GameMode.COMPETITIVE.name,
    val difficulty: String = Difficulty.MEDIUM.name,
    val status: String = RoomStatus.WAITING.name,
    val isPrivate: Boolean = false,
    val maxPlayers: Int = 2,
    val players: Map<String, RoomPlayer> = emptyMap(),
    val board: SudokuBoard = SudokuBoard(),
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val winnerId: String? = null
) {
    constructor() : this("", "", "", GameMode.COMPETITIVE.name, Difficulty.MEDIUM.name, 
        RoomStatus.WAITING.name, false, 2, emptyMap(), SudokuBoard(), 
        System.currentTimeMillis(), null, null, null)
    
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "code" to code,
        "hostId" to hostId,
        "gameMode" to gameMode,
        "difficulty" to difficulty,
        "status" to status,
        "isPrivate" to isPrivate,
        "maxPlayers" to maxPlayers,
        "players" to players.mapValues { it.value.toMap() },
        "board" to board.toMap(),
        "createdAt" to createdAt,
        "startedAt" to startedAt,
        "finishedAt" to finishedAt,
        "winnerId" to winnerId
    )
}

// Matchmaking queue entry
@IgnoreExtraProperties
data class MatchmakingEntry(
    val playerId: String = "",
    val displayName: String = "",
    val gameMode: String = GameMode.COMPETITIVE.name,
    val difficulty: String = Difficulty.MEDIUM.name,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", GameMode.COMPETITIVE.name, Difficulty.MEDIUM.name, System.currentTimeMillis())
    
    fun toMap(): Map<String, Any> = mapOf(
        "playerId" to playerId,
        "displayName" to displayName,
        "gameMode" to gameMode,
        "difficulty" to difficulty,
        "timestamp" to timestamp
    )
}

// Game result for history
@IgnoreExtraProperties
data class GameResult(
    val gameId: String = "",
    val playerId: String = "",
    val opponentId: String? = null,
    val opponentName: String? = null,
    val gameMode: String = "",
    val difficulty: String = "",
    val won: Boolean = false,
    val score: Int = 0,
    val errors: Int = 0,
    val timeSeconds: Int = 0,
    val completedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", null, null, "", "", false, 0, 0, 0, System.currentTimeMillis())
}
