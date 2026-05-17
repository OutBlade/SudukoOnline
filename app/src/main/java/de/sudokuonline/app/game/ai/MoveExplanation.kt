package de.sudokuonline.app.game.ai

import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.TicTacToeLogic
import de.sudokuonline.app.game.MuhleLogic

/**
 * AI Move Explanation System - Educational feature that explains AI decisions
 *
 * Provides human-readable explanations for why the AI made a particular move,
 * helping players learn strategy and improve their game.
 */
object MoveExplanation {

    /**
     * Explanation for a move with reasoning and strategic value
     */
    data class Explanation(
        val primaryReason: String,           // Main reason for the move
        val secondaryReasons: List<String>,  // Additional benefits
        val strategicValue: StrategicValue,  // How good is this move
        val alternativeMoves: List<String>,  // Other good options mentioned
        val learningTip: String?             // Educational tip for the player
    )

    enum class StrategicValue(val displayName: String, val emoji: String) {
        WINNING("Gewinnend", "🏆"),
        EXCELLENT("Ausgezeichnet", "⭐"),
        GOOD("Gut", "👍"),
        SOLID("Solide", "✓"),
        DEFENSIVE("Defensiv", "🛡️"),
        RISKY("Riskant", "⚠️")
    }

    // ============ TICTACTOE EXPLANATIONS ============

    /**
     * Explain a TicTacToe move
     */
    fun explainTicTacToeMove(
        board: TicTacToeBoard,
        row: Int,
        col: Int,
        aiSymbol: Int,
        gameMode: TicTacToeGameMode
    ): Explanation {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        val position = getPositionName(row, col, board.size)
        val secondaryReasons = mutableListOf<String>()
        var learningTip: String? = null

        // Check various strategic reasons
        val isWinningMove = wouldWin(board, row, col, aiSymbol)
        val isBlockingMove = wouldWin(board, row, col, opponentSymbol)
        val createsFork = createsFork(board, row, col, aiSymbol)
        val blocksFork = createsFork(board, row, col, opponentSymbol)
        val threatsCreated = countThreatsAfterMove(board, row, col, aiSymbol)
        val isCenter = row == board.size / 2 && col == board.size / 2
        val isCorner = (row == 0 || row == board.size - 1) && (col == 0 || col == board.size - 1)

        // Determine primary reason and strategic value
        val (primaryReason, strategicValue) = when {
            isWinningMove -> {
                learningTip = "Immer nach Gewinnzügen suchen, bevor du andere Züge machst!"
                Pair(
                    "Dieser Zug gewinnt das Spiel! Ich vervollständige eine Reihe von ${board.winCondition}.",
                    StrategicValue.WINNING
                )
            }
            isBlockingMove -> {
                learningTip = "Wenn der Gegner kurz vor dem Sieg steht, musst du blocken!"
                secondaryReasons.add("Ohne diesen Block hätte der Gegner im nächsten Zug gewonnen")
                Pair(
                    "Ich blocke deinen Gewinnzug! Du hättest sonst ${board.winCondition} in einer Reihe gehabt.",
                    StrategicValue.DEFENSIVE
                )
            }
            createsFork -> {
                learningTip = "Eine 'Gabel' (Fork) erzeugt zwei Gewinndrohungen gleichzeitig - der Gegner kann nur eine blocken!"
                secondaryReasons.add("Erzeugt $threatsCreated Gewinndrohungen")
                Pair(
                    "Ich spiele eine Gabel! Mit $position erzeuge ich mehrere Gewinndrohungen, die du nicht alle blocken kannst.",
                    StrategicValue.EXCELLENT
                )
            }
            blocksFork -> {
                learningTip = "Erkenne Gabel-Versuche des Gegners und blocke sie frühzeitig!"
                Pair(
                    "Ich verhindere deine Gabel! Du hättest sonst mehrere Drohungen aufbauen können.",
                    StrategicValue.GOOD
                )
            }
            threatsCreated >= 1 -> {
                secondaryReasons.add("Baut Druck auf")
                if (isCenter) secondaryReasons.add("Kontrolliert das Zentrum")
                if (isCorner) secondaryReasons.add("Sichert eine Ecke")
                Pair(
                    "Ich erzeuge eine Gewinndrohung mit $position. Du musst im nächsten Zug reagieren.",
                    StrategicValue.GOOD
                )
            }
            isCenter -> {
                learningTip = "Das Zentrum ist strategisch wertvoll - es ist Teil der meisten Gewinnlinien!"
                secondaryReasons.add("Maximale Kontrolle über das Spielfeld")
                Pair(
                    "Ich nehme das Zentrum! Von hier aus kann ich in alle Richtungen angreifen.",
                    StrategicValue.EXCELLENT
                )
            }
            isCorner -> {
                learningTip = "Ecken sind stark, weil sie Teil von 3 möglichen Gewinnlinien sind."
                secondaryReasons.add("Teil von Diagonal- und Randlinien")
                Pair(
                    "Ich sichere mir die Ecke $position. Ecken sind strategisch wichtige Positionen.",
                    StrategicValue.GOOD
                )
            }
            else -> {
                if (board.cells.flatten().count { !it.isEmpty() } < 3) {
                    secondaryReasons.add("Frühe Spielphase - Positionierung wichtig")
                }
                Pair(
                    "Ich verbessere meine Position mit $position.",
                    StrategicValue.SOLID
                )
            }
        }

        // Add bomb-specific explanations
        if (gameMode == TicTacToeGameMode.BOMB || gameMode == TicTacToeGameMode.L_BOMB) {
            secondaryReasons.add("Im Bomben-Modus: Diese Position ist relativ sicher vor Bombenangriffen")
        }

        return Explanation(
            primaryReason = primaryReason,
            secondaryReasons = secondaryReasons,
            strategicValue = strategicValue,
            alternativeMoves = getAlternativeMoves(board, aiSymbol),
            learningTip = learningTip
        )
    }

    /**
     * Explain a bomb placement in TicTacToe
     */
    fun explainBombMove(
        board: TicTacToeBoard,
        row: Int,
        col: Int,
        aiSymbol: Int,
        gameMode: TicTacToeGameMode
    ): Explanation {
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        val secondaryReasons = mutableListOf<String>()

        // Analyze what the bomb destroys
        val affectedPositions = getAffectedByBomb(row, col, board.size, gameMode)
        var myPiecesDestroyed = 0
        var oppPiecesDestroyed = 0

        for ((r, c) in affectedPositions) {
            if (r in 0 until board.size && c in 0 until board.size) {
                when (board.cells[r][c].value) {
                    aiSymbol -> myPiecesDestroyed++
                    opponentSymbol -> oppPiecesDestroyed++
                }
            }
        }

        val primaryReason = when {
            oppPiecesDestroyed > myPiecesDestroyed + 1 -> {
                secondaryReasons.add("Zerstört $oppPiecesDestroyed gegnerische Steine")
                "Bombenangriff! Ich zerstöre mehr von deinen Steinen als von meinen."
            }
            oppPiecesDestroyed > 0 && myPiecesDestroyed == 0 -> {
                secondaryReasons.add("Keine eigenen Verluste")
                "Perfekter Bombenangriff! Ich zerstöre $oppPiecesDestroyed deiner Steine ohne eigene Verluste."
            }
            else -> {
                secondaryReasons.add("Taktischer Zug")
                "Strategischer Bombeneinsatz - verändert die Spielsituation."
            }
        }

        return Explanation(
            primaryReason = primaryReason,
            secondaryReasons = secondaryReasons,
            strategicValue = if (oppPiecesDestroyed > myPiecesDestroyed) StrategicValue.EXCELLENT else StrategicValue.RISKY,
            alternativeMoves = emptyList(),
            learningTip = "Bomben sind am effektivsten, wenn der Gegner viele Steine nahe beieinander hat!"
        )
    }

    // ============ MÜHLE EXPLANATIONS ============

    /**
     * Explain a Mühle move
     */
    fun explainMuhleMove(
        board: MuhleBoard,
        position: Int,
        playerNumber: Int,
        phase: MuhleGamePhase,
        isPlacement: Boolean
    ): Explanation {
        val opponentNumber = 3 - playerNumber
        val secondaryReasons = mutableListOf<String>()
        var learningTip: String? = null

        val formsMill = MuhleLogic.formsNewMill(board, position, playerNumber)
        val blocksOpponentMill = wouldFormMill(board, position, opponentNumber)
        val isCrosspoint = position in listOf(4, 10, 13, 19)
        val isTPoint = position in listOf(1, 7, 9, 11, 12, 14, 16, 22)
        val potentialMills = countPotentialMills(board, position, playerNumber)

        val positionName = getMuhlePositionName(position)

        val (primaryReason, strategicValue) = when {
            formsMill -> {
                learningTip = "Eine Mühle zu bilden erlaubt dir, einen gegnerischen Stein zu entfernen!"
                Pair(
                    "Ich bilde eine Mühle! Jetzt darf ich einen deiner Steine entfernen.",
                    StrategicValue.EXCELLENT
                )
            }
            blocksOpponentMill -> {
                learningTip = "Beobachte immer, ob der Gegner kurz vor einer Mühle steht!"
                secondaryReasons.add("Verhindert gegnerische Mühle")
                Pair(
                    "Ich blocke deine Mühle! Ohne diesen Zug hättest du eine Mühle gebildet.",
                    StrategicValue.DEFENSIVE
                )
            }
            potentialMills >= 2 -> {
                learningTip = "Eine Zwickmühle entsteht, wenn ein Stein zwischen zwei Mühlen hin und her bewegt werden kann!"
                secondaryReasons.add("Bereitet Zwickmühle vor")
                Pair(
                    "Ich baue eine Zwickmühle auf! Von $positionName aus kann ich mehrere Mühlen bilden.",
                    StrategicValue.EXCELLENT
                )
            }
            isCrosspoint && phase == MuhleGamePhase.PLACING -> {
                learningTip = "Kreuzungspunkte haben 4 Verbindungen - ideal für Mobilität und Mühlen!"
                secondaryReasons.add("Maximale Bewegungsfreiheit")
                secondaryReasons.add("Teil mehrerer möglicher Mühlen")
                Pair(
                    "Ich sichere den Kreuzungspunkt $positionName - eine der stärksten Positionen!",
                    StrategicValue.GOOD
                )
            }
            isTPoint -> {
                secondaryReasons.add("Gute Verbindungen")
                Pair(
                    "Ich nehme den T-Punkt $positionName für gute Mobilität.",
                    StrategicValue.SOLID
                )
            }
            potentialMills == 1 -> {
                secondaryReasons.add("Baut Druck auf")
                Pair(
                    "Ich bereite eine Mühle vor. Du musst aufpassen!",
                    StrategicValue.GOOD
                )
            }
            else -> {
                Pair(
                    "Ich verbessere meine Position mit $positionName.",
                    StrategicValue.SOLID
                )
            }
        }

        // Add phase-specific tips
        when (phase) {
            MuhleGamePhase.PLACING -> {
                if (learningTip == null) {
                    learningTip = "In der Setzphase: Kontrolliere Kreuzungspunkte und bereite Mühlen vor!"
                }
            }
            MuhleGamePhase.MOVING -> {
                if (learningTip == null) {
                    learningTip = "In der Zugphase: Halte deine Steine mobil und bilde Zwickmühlen!"
                }
            }
            MuhleGamePhase.FLYING -> {
                if (learningTip == null) {
                    learningTip = "Mit nur 3 Steinen kannst du 'fliegen' - nutze diese Mobilität!"
                }
                secondaryReasons.add("Flugphase: Kann überall hin ziehen")
            }
        }

        return Explanation(
            primaryReason = primaryReason,
            secondaryReasons = secondaryReasons,
            strategicValue = strategicValue,
            alternativeMoves = emptyList(),
            learningTip = learningTip
        )
    }

    /**
     * Explain a stone removal after forming a mill
     */
    fun explainMuhleRemoval(
        board: MuhleBoard,
        position: Int,
        opponentNumber: Int
    ): Explanation {
        val secondaryReasons = mutableListOf<String>()

        // Analyze why this stone was chosen for removal
        val wasPartOfMill = isPartOfMill(board, position, opponentNumber)
        val wasPartOfPotentialMill = countPotentialMills(board, position, opponentNumber) > 0
        val isCrosspoint = position in listOf(4, 10, 13, 19)

        val primaryReason = when {
            wasPartOfPotentialMill -> {
                secondaryReasons.add("Zerstört gegnerische Mühlen-Vorbereitung")
                "Ich entferne diesen Stein, weil er Teil einer fast fertigen Mühle war!"
            }
            isCrosspoint -> {
                secondaryReasons.add("Reduziert gegnerische Mobilität")
                "Ich entferne deinen Stein vom Kreuzungspunkt - das schränkt deine Bewegung ein."
            }
            wasPartOfMill -> {
                "Ich entferne diesen Stein aus deiner Mühle."
            }
            else -> {
                "Ich entferne diesen Stein strategisch."
            }
        }

        return Explanation(
            primaryReason = primaryReason,
            secondaryReasons = secondaryReasons,
            strategicValue = StrategicValue.EXCELLENT,
            alternativeMoves = emptyList(),
            learningTip = "Beim Entfernen: Wähle Steine, die gegnerische Pläne stören oder ihre Mobilität einschränken!"
        )
    }

    // ============ HELPER METHODS ============

    private fun getPositionName(row: Int, col: Int, size: Int): String {
        val colName = ('A' + col).toString()
        val rowName = (size - row).toString()
        return "$colName$rowName"
    }

    private fun getMuhlePositionName(position: Int): String {
        // Standard Mühle notation
        val names = listOf(
            "a7", "d7", "g7",      // 0-2
            "b6", "d6", "f6",      // 3-5
            "c5", "d5", "e5",      // 6-8
            "a4", "b4", "c4",      // 9-11
            "e4", "f4", "g4",      // 12-14
            "c3", "d3", "e3",      // 15-17
            "b2", "d2", "f2",      // 18-20
            "a1", "d1", "g1"       // 21-23
        )
        return if (position in names.indices) names[position] else "Position $position"
    }

    private fun wouldWin(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val size = board.size
        val win = board.winCondition

        val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))

        for ((dr, dc) in directions) {
            var count = 1
            var r = row + dr
            var c = col + dc
            while (r in 0 until size && c in 0 until size && board.cells[r][c].value == symbol) {
                count++
                r += dr
                c += dc
            }
            r = row - dr
            c = col - dc
            while (r in 0 until size && c in 0 until size && board.cells[r][c].value == symbol) {
                count++
                r -= dr
                c -= dc
            }
            if (count >= win) return true
        }
        return false
    }

    private fun createsFork(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val newBoard = simulateMove(board, row, col, symbol)
        return countThreatsAfterMove(board, row, col, symbol) >= 2
    }

    private fun countThreatsAfterMove(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Int {
        val newBoard = simulateMove(board, row, col, symbol)
        var threats = 0
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (newBoard.cells[r][c].isEmpty() && wouldWin(newBoard, r, c, symbol)) {
                    threats++
                }
            }
        }
        return threats
    }

    private fun simulateMove(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): TicTacToeBoard {
        val newCells = board.cells.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (r == row && c == col) cell.copy(value = symbol) else cell
            }
        }
        return board.copy(cells = newCells)
    }

    private fun getAlternativeMoves(board: TicTacToeBoard, aiSymbol: Int): List<String> {
        val alternatives = mutableListOf<String>()
        val center = board.size / 2

        if (board.cells[center][center].isEmpty()) {
            alternatives.add("Zentrum (${getPositionName(center, center, board.size)})")
        }

        val corners = listOf(
            Pair(0, 0), Pair(0, board.size - 1),
            Pair(board.size - 1, 0), Pair(board.size - 1, board.size - 1)
        )
        for ((r, c) in corners) {
            if (board.cells[r][c].isEmpty() && alternatives.size < 3) {
                alternatives.add("Ecke ${getPositionName(r, c, board.size)}")
            }
        }

        return alternatives.take(2)
    }

    private fun getAffectedByBomb(row: Int, col: Int, size: Int, gameMode: TicTacToeGameMode): List<Pair<Int, Int>> {
        return if (gameMode == TicTacToeGameMode.L_BOMB) {
            listOf(
                Pair(row, col),
                Pair(row - 1, col), Pair(row + 1, col),
                Pair(row, col - 1), Pair(row, col + 1),
                Pair(row - 1, col - 1), Pair(row + 1, col + 1)
            )
        } else {
            (-1..1).flatMap { dr ->
                (-1..1).map { dc -> Pair(row + dr, col + dc) }
            }
        }
    }

    // Mühle helpers
    private fun wouldFormMill(board: MuhleBoard, position: Int, playerNumber: Int): Boolean {
        return MuhleLogic.formsNewMill(board, position, playerNumber)
    }

    private fun countPotentialMills(board: MuhleBoard, position: Int, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        return MuhleLogic.MILLS.count { mill ->
            position in mill &&
            mill.count { board.positions[it].owner == owner } >= 1 &&
            mill.all { p -> board.positions[p].owner == owner || board.positions[p].isEmpty() || p == position }
        }
    }

    private fun isPartOfMill(board: MuhleBoard, position: Int, playerNumber: Int): Boolean {
        val owner = if (playerNumber == 1) MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        return MuhleLogic.MILLS.any { mill ->
            position in mill && mill.all { board.positions[it].owner == owner }
        }
    }
}
