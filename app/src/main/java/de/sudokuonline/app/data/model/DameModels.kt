package de.sudokuonline.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Piece types in Dame (Checkers)
 */
enum class DamePieceType {
    NONE,
    MAN,
    KING
}

/**
 * Player colors in Dame
 */
enum class DamePlayerColor {
    WHITE,
    BLACK
}

/**
 * A single cell on the Dame board
 */
@IgnoreExtraProperties
data class DameCell(
    val row: Int = 0,
    val col: Int = 0,
    val pieceType: String = DamePieceType.NONE.name,
    val owner: String = ""
) {
    constructor() : this(0, 0, DamePieceType.NONE.name, "")

    fun toMap(): Map<String, Any> = mapOf(
        "row" to row,
        "col" to col,
        "pieceType" to pieceType,
        "owner" to owner
    )

    fun isEmpty(): Boolean = pieceType == DamePieceType.NONE.name
    fun isWhite(): Boolean = owner == DamePlayerColor.WHITE.name
    fun isBlack(): Boolean = owner == DamePlayerColor.BLACK.name
}

/**
 * Complete Dame board state (8x8)
 */
@IgnoreExtraProperties
data class DameBoard(
    val cells: List<List<DameCell>> = createInitialBoard()
) {
    constructor() : this(createInitialBoard())

    fun toMap(): Map<String, Any> = mapOf(
        "cells" to cells.map { row -> row.map { it.toMap() } }
    )

    companion object {
        fun createInitialBoard(): List<List<DameCell>> {
            return List(8) { row ->
                List(8) { col ->
                    val isDarkSquare = (row + col) % 2 == 1
                    when {
                        isDarkSquare && row in 0..2 -> DameCell(
                            row = row,
                            col = col,
                            pieceType = DamePieceType.MAN.name,
                            owner = DamePlayerColor.BLACK.name
                        )
                        isDarkSquare && row in 5..7 -> DameCell(
                            row = row,
                            col = col,
                            pieceType = DamePieceType.MAN.name,
                            owner = DamePlayerColor.WHITE.name
                        )
                        else -> DameCell(row = row, col = col)
                    }
                }
            }
        }

        fun create(): DameBoard = DameBoard(createInitialBoard())
    }
}

/**
 * A move in Dame
 */
@IgnoreExtraProperties
data class DameMove(
    val fromRow: Int = -1,
    val fromCol: Int = -1,
    val toRow: Int = -1,
    val toCol: Int = -1,
    val captures: List<Pair<Int, Int>> = emptyList(),
    val isKingPromotion: Boolean = false
) {
    constructor() : this(-1, -1, -1, -1, emptyList(), false)

    fun toMap(): Map<String, Any> = mapOf(
        "fromRow" to fromRow,
        "fromCol" to fromCol,
        "toRow" to toRow,
        "toCol" to toCol,
        "captures" to captures.map { mapOf("first" to it.first, "second" to it.second) },
        "isKingPromotion" to isKingPromotion
    )
}

/**
 * Player state for Dame
 */
@IgnoreExtraProperties
data class DameRoomPlayer(
    val playerId: String = "",
    val displayName: String = "",
    val status: String = PlayerStatus.WAITING.name,
    val playerColor: String = DamePlayerColor.WHITE.name,
    val piecesRemaining: Int = 12,
    val score: Int = 0,
    val lastActive: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", PlayerStatus.WAITING.name, DamePlayerColor.WHITE.name, 12, 0, System.currentTimeMillis())

    fun toMap(): Map<String, Any?> = mapOf(
        "playerId" to playerId,
        "displayName" to displayName,
        "status" to status,
        "playerColor" to playerColor,
        "piecesRemaining" to piecesRemaining,
        "score" to score,
        "lastActive" to lastActive
    )
}

/**
 * Dame Game Room
 */
@IgnoreExtraProperties
data class DameRoom(
    val id: String = "",
    val code: String = "",
    val hostId: String = "",
    val status: String = RoomStatus.WAITING.name,
    val isPrivate: Boolean = false,
    val maxPlayers: Int = 2,
    val players: Map<String, DameRoomPlayer> = emptyMap(),
    val board: DameBoard = DameBoard(),
    val currentTurnPlayerId: String = "",
    val currentPlayerColor: String = DamePlayerColor.WHITE.name,
    val mustContinueJump: Boolean = false,
    val continuingPieceRow: Int = -1,
    val continuingPieceCol: Int = -1,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val winnerId: String? = null,
    val isDraw: Boolean = false,
    val rematchStatus: String = RematchStatus.NONE.name,
    val rematchRequestedBy: String? = null
) {
    constructor() : this(
        "", "", "", RoomStatus.WAITING.name, false, 2, emptyMap(),
        DameBoard(), "", DamePlayerColor.WHITE.name, false, -1, -1,
        System.currentTimeMillis(), null, null, null, false,
        RematchStatus.NONE.name, null
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
        "currentPlayerColor" to currentPlayerColor,
        "mustContinueJump" to mustContinueJump,
        "continuingPieceRow" to continuingPieceRow,
        "continuingPieceCol" to continuingPieceCol,
        "createdAt" to createdAt,
        "startedAt" to startedAt,
        "finishedAt" to finishedAt,
        "winnerId" to winnerId,
        "isDraw" to isDraw,
        "rematchStatus" to rematchStatus,
        "rematchRequestedBy" to rematchRequestedBy
    )

    fun isMyTurn(playerId: String): Boolean = currentTurnPlayerId == playerId

    fun getMyColor(playerId: String): DamePlayerColor {
        val player = players[playerId] ?: return DamePlayerColor.WHITE
        return DamePlayerColor.valueOf(player.playerColor)
    }

    fun getOpponent(myPlayerId: String): DameRoomPlayer? =
        players.values.firstOrNull { it.playerId != myPlayerId }
}

/**
 * Dame Matchmaking Entry
 */
@IgnoreExtraProperties
data class DameMatchmakingEntry(
    val playerId: String = "",
    val displayName: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", System.currentTimeMillis())

    fun toMap(): Map<String, Any> = mapOf(
        "playerId" to playerId,
        "displayName" to displayName,
        "timestamp" to timestamp
    )
}
