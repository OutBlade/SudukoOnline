package de.sudokuonline.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Game phases in Muhle (Nine Men's Morris)
 */
enum class MuhleGamePhase(val displayName: String) {
    PLACING("Setzphase"),     // Players place stones (9 each)
    MOVING("Zugphase"),       // Players move stones to adjacent positions
    FLYING("Sprungphase")     // Player with 3 stones can "fly" to any empty position
}

/**
 * Stone owner on the board
 */
enum class MuhleStoneOwner {
    EMPTY,
    PLAYER_1,  // White stones
    PLAYER_2   // Black stones
}

/**
 * Action type for the current turn
 */
enum class MuhleActionType {
    PLACE,      // Place a new stone
    SELECT,     // Select a stone to move
    MOVE,       // Move selected stone to new position
    REMOVE      // Remove opponent's stone after forming a mill
}

/**
 * Position on the Muhle board (24 positions total)
 * Board layout:
 * 0---------1---------2
 * |         |         |
 * |   3-----4-----5   |
 * |   |     |     |   |
 * |   |  6--7--8  |   |
 * |   |  |     |  |   |
 * 9--10-11    12-13--14
 * |   |  |     |  |   |
 * |   | 15-16-17  |   |
 * |   |     |     |   |
 * |  18----19----20   |
 * |         |         |
 * 21-------22--------23
 */
@IgnoreExtraProperties
data class MuhlePosition(
    val index: Int = -1,
    val owner: String = MuhleStoneOwner.EMPTY.name
) {
    constructor() : this(-1, MuhleStoneOwner.EMPTY.name)

    fun toMap(): Map<String, Any> = mapOf(
        "index" to index,
        "owner" to owner
    )

    fun isEmpty(): Boolean = owner == MuhleStoneOwner.EMPTY.name
    fun isPlayer1(): Boolean = owner == MuhleStoneOwner.PLAYER_1.name
    fun isPlayer2(): Boolean = owner == MuhleStoneOwner.PLAYER_2.name
}

/**
 * Complete Muhle board state
 */
@IgnoreExtraProperties
data class MuhleBoard(
    val positions: List<MuhlePosition> = List(24) { MuhlePosition(it, MuhleStoneOwner.EMPTY.name) },
    val lastMillPositions: List<Int> = emptyList()  // Highlight recently formed mill
) {
    constructor() : this(List(24) { MuhlePosition(it, MuhleStoneOwner.EMPTY.name) }, emptyList())

    fun toMap(): Map<String, Any> = mapOf(
        "positions" to positions.map { it.toMap() },
        "lastMillPositions" to lastMillPositions
    )

    companion object {
        fun create(): MuhleBoard = MuhleBoard()
    }
}

/**
 * Player state for Muhle
 */
@IgnoreExtraProperties
data class MuhleRoomPlayer(
    val playerId: String = "",
    val displayName: String = "",
    val status: String = PlayerStatus.WAITING.name,
    val playerNumber: Int = 0,       // 1 or 2
    val stonesToPlace: Int = 9,      // Stones remaining to place
    val stonesOnBoard: Int = 0,      // Stones currently on board
    val stonesLost: Int = 0,         // Stones removed by opponent
    val score: Int = 0,
    val lastActive: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", PlayerStatus.WAITING.name, 0, 9, 0, 0, 0, System.currentTimeMillis())

    fun toMap(): Map<String, Any?> = mapOf(
        "playerId" to playerId,
        "displayName" to displayName,
        "status" to status,
        "playerNumber" to playerNumber,
        "stonesToPlace" to stonesToPlace,
        "stonesOnBoard" to stonesOnBoard,
        "stonesLost" to stonesLost,
        "score" to score,
        "lastActive" to lastActive
    )

    fun getTotalStones(): Int = stonesToPlace + stonesOnBoard

    fun getGamePhase(): MuhleGamePhase = when {
        stonesToPlace > 0 -> MuhleGamePhase.PLACING
        stonesOnBoard <= 3 && stonesToPlace == 0 -> MuhleGamePhase.FLYING
        else -> MuhleGamePhase.MOVING
    }
}

/**
 * Muhle Game Room
 */
@IgnoreExtraProperties
data class MuhleRoom(
    val id: String = "",
    val code: String = "",
    val hostId: String = "",
    val status: String = RoomStatus.WAITING.name,
    val isPrivate: Boolean = false,
    val maxPlayers: Int = 2,
    val players: Map<String, MuhleRoomPlayer> = emptyMap(),
    val board: MuhleBoard = MuhleBoard(),
    val currentTurnPlayerId: String = "",
    val currentPlayerNumber: Int = 1,   // 1 or 2
    val currentAction: String = MuhleActionType.PLACE.name,
    val selectedPosition: Int = -1,     // For move selection
    val mustRemoveStone: Boolean = false,  // After forming a mill
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val winnerId: String? = null
) {
    constructor() : this(
        "", "", "", RoomStatus.WAITING.name, false, 2, emptyMap(),
        MuhleBoard(), "", 1, MuhleActionType.PLACE.name, -1, false,
        System.currentTimeMillis(), null, null, null
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
        "currentPlayerNumber" to currentPlayerNumber,
        "currentAction" to currentAction,
        "selectedPosition" to selectedPosition,
        "mustRemoveStone" to mustRemoveStone,
        "createdAt" to createdAt,
        "startedAt" to startedAt,
        "finishedAt" to finishedAt,
        "winnerId" to winnerId
    )

    fun isMyTurn(playerId: String): Boolean = currentTurnPlayerId == playerId

    fun getMyPlayerNumber(playerId: String): Int = players[playerId]?.playerNumber ?: 0

    fun getOpponent(myPlayerId: String): MuhleRoomPlayer? =
        players.values.firstOrNull { it.playerId != myPlayerId }

    fun getCurrentPlayerPhase(): MuhleGamePhase {
        val currentPlayer = players.values.firstOrNull { it.playerNumber == currentPlayerNumber }
        return currentPlayer?.getGamePhase() ?: MuhleGamePhase.PLACING
    }
}

/**
 * Muhle Matchmaking Entry
 */
@IgnoreExtraProperties
data class MuhleMatchmakingEntry(
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
