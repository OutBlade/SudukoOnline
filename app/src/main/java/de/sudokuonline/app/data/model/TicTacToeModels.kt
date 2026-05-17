package de.sudokuonline.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * TicTacToe Game Modes
 */
enum class TicTacToeGameMode(val displayName: String, val description: String) {
    CLASSIC("Klassisch", "Normales TicTacToe ohne Spezialregeln"),
    BOMB("Bomben Modus", "3 Bomben pro Spieler - löschen angrenzende Felder"),
    L_BOMB("L-Bomben Modus", "Bomben löschen in L-Form (Spalte oben + rechts)"),
    ULTIMATE("Ultimate", "9 Bretter in einem - gewinne 3 Bretter in einer Reihe!")
}

/**
 * TicTacToe Board Size
 */
enum class TicTacToeBoardSize(val size: Int, val winCondition: Int, val displayName: String) {
    SMALL(3, 3, "3x3 (3 in einer Reihe)"),
    LARGE(5, 4, "5x5 (4 in einer Reihe)")
}

/**
 * Cell value in TicTacToe
 */
enum class TicTacToeCellValue(val symbol: String) {
    EMPTY(""),
    X("X"),
    O("O"),
    BOMB("💣")
}

/**
 * Single cell in the TicTacToe grid
 */
@IgnoreExtraProperties
data class TicTacToeCell(
    val value: Int = 0,           // 0=empty, 1=X, 2=O, 3=Bomb
    val playerId: String? = null, // Player who placed this
    val isBomb: Boolean = false,  // Is this a bomb placement
    val bombOwnerId: String? = null // Who placed the bomb (for bomb modes)
) {
    constructor() : this(0, null, false, null)

    fun toMap(): Map<String, Any?> = mapOf(
        "value" to value,
        "playerId" to playerId,
        "isBomb" to isBomb,
        "bombOwnerId" to bombOwnerId
    )

    fun isEmpty(): Boolean = value == 0 && !isBomb
    fun isX(): Boolean = value == 1
    fun isO(): Boolean = value == 2

    fun getSymbol(): String = when {
        isBomb -> "💣"
        value == 1 -> "X"
        value == 2 -> "O"
        else -> ""
    }
}

/**
 * Complete TicTacToe board state
 */
@IgnoreExtraProperties
data class TicTacToeBoard(
    val cells: List<List<TicTacToeCell>> = List(3) { List(3) { TicTacToeCell() } },
    val size: Int = 3,
    val winCondition: Int = 3  // How many in a row to win
) {
    constructor() : this(List(3) { List(3) { TicTacToeCell() } }, 3, 3)

    fun toMap(): Map<String, Any> = mapOf(
        "cells" to cells.map { row -> row.map { it.toMap() } },
        "size" to size,
        "winCondition" to winCondition
    )

    companion object {
        fun create(boardSize: TicTacToeBoardSize): TicTacToeBoard {
            return TicTacToeBoard(
                cells = List(boardSize.size) { List(boardSize.size) { TicTacToeCell() } },
                size = boardSize.size,
                winCondition = boardSize.winCondition
            )
        }
    }
}

/**
 * Player state for TicTacToe
 */
@IgnoreExtraProperties
data class TicTacToeRoomPlayer(
    val playerId: String = "",
    val displayName: String = "",
    val status: String = PlayerStatus.WAITING.name,
    val symbol: Int = 0,          // 1=X, 2=O
    val bombsRemaining: Int = 3,  // For bomb modes
    val score: Int = 0,
    val wins: Int = 0,
    val lastActive: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", PlayerStatus.WAITING.name, 0, 3, 0, 0, System.currentTimeMillis())

    fun toMap(): Map<String, Any?> = mapOf(
        "playerId" to playerId,
        "displayName" to displayName,
        "status" to status,
        "symbol" to symbol,
        "bombsRemaining" to bombsRemaining,
        "score" to score,
        "wins" to wins,
        "lastActive" to lastActive
    )
}

/**
 * Rematch request status
 */
enum class RematchStatus {
    NONE,           // No rematch requested
    PENDING,        // Rematch requested, waiting for response
    ACCEPTED,       // Both players accepted
    DECLINED        // Rematch declined
}

/**
 * TicTacToe Game Room
 */
@IgnoreExtraProperties
data class TicTacToeRoom(
    val id: String = "",
    val code: String = "",
    val hostId: String = "",
    val gameMode: String = TicTacToeGameMode.CLASSIC.name,
    val boardSize: String = TicTacToeBoardSize.SMALL.name,
    val status: String = RoomStatus.WAITING.name,
    val isPrivate: Boolean = false,
    val maxPlayers: Int = 2,
    val players: Map<String, TicTacToeRoomPlayer> = emptyMap(),
    val board: TicTacToeBoard = TicTacToeBoard(),
    val ultimateBoard: UltimateTicTacToeBoard = UltimateTicTacToeBoard(),  // For Ultimate mode
    val currentTurnPlayerId: String = "",  // Whose turn it is
    val currentTurnSymbol: Int = 1,        // 1=X always starts
    val roundNumber: Int = 1,              // For best-of series
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val winnerId: String? = null,
    val isDraw: Boolean = false,
    // Rematch fields
    val rematchStatus: String = RematchStatus.NONE.name,
    val rematchRequestedBy: String? = null,
    val seriesScore: Map<String, Int> = emptyMap()  // playerId -> wins in series
) {
    constructor() : this(
        "", "", "", TicTacToeGameMode.CLASSIC.name, TicTacToeBoardSize.SMALL.name,
        RoomStatus.WAITING.name, false, 2, emptyMap(), TicTacToeBoard(),
        UltimateTicTacToeBoard(), "", 1, 1, System.currentTimeMillis(), null, null, null, false,
        RematchStatus.NONE.name, null, emptyMap()
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "code" to code,
        "hostId" to hostId,
        "gameMode" to gameMode,
        "boardSize" to boardSize,
        "status" to status,
        "isPrivate" to isPrivate,
        "maxPlayers" to maxPlayers,
        "players" to players.mapValues { it.value.toMap() },
        "board" to board.toMap(),
        "ultimateBoard" to ultimateBoard.toMap(),
        "currentTurnPlayerId" to currentTurnPlayerId,
        "currentTurnSymbol" to currentTurnSymbol,
        "roundNumber" to roundNumber,
        "createdAt" to createdAt,
        "startedAt" to startedAt,
        "finishedAt" to finishedAt,
        "winnerId" to winnerId,
        "isDraw" to isDraw,
        "rematchStatus" to rematchStatus,
        "rematchRequestedBy" to rematchRequestedBy,
        "seriesScore" to seriesScore
    )

    fun getGameModeEnum(): TicTacToeGameMode =
        try { TicTacToeGameMode.valueOf(gameMode) } catch (e: Exception) { TicTacToeGameMode.CLASSIC }

    fun getBoardSizeEnum(): TicTacToeBoardSize =
        try { TicTacToeBoardSize.valueOf(boardSize) } catch (e: Exception) { TicTacToeBoardSize.SMALL }

    fun isMyTurn(playerId: String): Boolean =
        currentTurnPlayerId == playerId

    fun getMySymbol(playerId: String): Int =
        players[playerId]?.symbol ?: 0

    fun getOpponent(myPlayerId: String): TicTacToeRoomPlayer? =
        players.values.firstOrNull { it.playerId != myPlayerId }
}

/**
 * TicTacToe Matchmaking Entry
 */
@IgnoreExtraProperties
data class TicTacToeMatchmakingEntry(
    val playerId: String = "",
    val displayName: String = "",
    val gameMode: String = TicTacToeGameMode.CLASSIC.name,
    val boardSize: String = TicTacToeBoardSize.SMALL.name,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", TicTacToeGameMode.CLASSIC.name, TicTacToeBoardSize.SMALL.name, System.currentTimeMillis())

    fun toMap(): Map<String, Any> = mapOf(
        "playerId" to playerId,
        "displayName" to displayName,
        "gameMode" to gameMode,
        "boardSize" to boardSize,
        "timestamp" to timestamp
    )
}

/**
 * Ultimate TicTacToe - A single mini board within the 3x3 meta grid
 */
@IgnoreExtraProperties
data class UltimateMiniBoard(
    val cells: List<List<TicTacToeCell>> = List(3) { List(3) { TicTacToeCell() } },
    val winner: Int = 0  // 0=none, 1=X, 2=O, 3=draw
) {
    constructor() : this(List(3) { List(3) { TicTacToeCell() } }, 0)

    fun toMap(): Map<String, Any> = mapOf(
        "cells" to cells.map { row -> row.map { it.toMap() } },
        "winner" to winner
    )

    fun isFinished(): Boolean = winner != 0
}

/**
 * Ultimate TicTacToe Board - 3x3 grid of mini boards
 */
@IgnoreExtraProperties
data class UltimateTicTacToeBoard(
    val miniBoards: List<List<UltimateMiniBoard>> = List(3) { List(3) { UltimateMiniBoard() } },
    val activeBoard: Pair<Int, Int>? = null  // Which mini board must be played next (null = any)
) {
    constructor() : this(List(3) { List(3) { UltimateMiniBoard() } }, null)

    fun toMap(): Map<String, Any?> = mapOf(
        "miniBoards" to miniBoards.map { row -> row.map { it.toMap() } },
        "activeBoardRow" to activeBoard?.first,
        "activeBoardCol" to activeBoard?.second
    )
}

/**
 * Ultimate TicTacToe Room
 */
@IgnoreExtraProperties
data class UltimateTicTacToeRoom(
    val id: String = "",
    val code: String = "",
    val hostId: String = "",
    val status: String = RoomStatus.WAITING.name,
    val isPrivate: Boolean = false,
    val maxPlayers: Int = 2,
    val players: Map<String, TicTacToeRoomPlayer> = emptyMap(),
    val board: UltimateTicTacToeBoard = UltimateTicTacToeBoard(),
    val currentTurnPlayerId: String = "",
    val currentTurnSymbol: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val winnerId: String? = null,
    val isDraw: Boolean = false
) {
    constructor() : this(
        "", "", "", RoomStatus.WAITING.name, false, 2, emptyMap(),
        UltimateTicTacToeBoard(), "", 1, System.currentTimeMillis(),
        null, null, null, false
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "code" to code,
        "hostId" to hostId,
        "status" to status,
        "isPrivate" to isPrivate,
        "maxPlayers" to maxPlayers,
        "players" to players.mapValues { it.value.toMap() },
        "board" to board.toMap(),
        "currentTurnPlayerId" to currentTurnPlayerId,
        "currentTurnSymbol" to currentTurnSymbol,
        "createdAt" to createdAt,
        "startedAt" to startedAt,
        "finishedAt" to finishedAt,
        "winnerId" to winnerId,
        "isDraw" to isDraw
    )

    fun isMyTurn(playerId: String): Boolean = currentTurnPlayerId == playerId

    fun getMySymbol(playerId: String): Int = players[playerId]?.symbol ?: 0

    fun getOpponent(myPlayerId: String): TicTacToeRoomPlayer? =
        players.values.firstOrNull { it.playerId != myPlayerId }
}
