package de.sudokuonline.app.game

import android.content.Context
import de.sudokuonline.app.data.model.SudokuBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intelligent Hint Service - Provides detailed, educational hints for Sudoku
 * Uses the SudokuSolver to analyze the board and explain the best next move
 */
class IntelligentHintService private constructor(context: Context) {

    private val solver = SudokuSolver()

    /**
     * Result of an intelligent hint analysis
     */
    data class IntelligentHint(
        val type: HintType,
        val title: String,
        val explanation: String,
        val detailedSteps: List<String>,
        val targetCell: Pair<Int, Int>?,
        val value: Int?,
        val highlightCells: List<Pair<Int, Int>>,
        val eliminatedCandidates: Map<Pair<Int, Int>, List<Int>>,
        val difficulty: HintDifficulty,
        val learningTip: String?
    )

    enum class HintType {
        NAKED_SINGLE,
        HIDDEN_SINGLE,
        NAKED_PAIR,
        HIDDEN_PAIR,
        NAKED_TRIPLE,
        POINTING_PAIR,
        BOX_LINE_REDUCTION,
        X_WING,
        SWORDFISH,
        Y_WING,
        NO_HINT_NEEDED,
        STUCK
    }

    enum class HintDifficulty(val displayName: String, val color: Long) {
        BEGINNER("Anfänger", 0xFF4CAF50),
        EASY("Leicht", 0xFF8BC34A),
        MEDIUM("Mittel", 0xFFFF9800),
        ADVANCED("Fortgeschritten", 0xFFFF5722),
        EXPERT("Experte", 0xFFF44336)
    }

    /**
     * Get an intelligent hint for the current board state
     */
    suspend fun getHint(board: SudokuBoard): IntelligentHint = withContext(Dispatchers.Default) {
        // Initialize solver with current board
        solver.initialize(board)

        // Check if board is already solved
        if (solver.isSolved()) {
            return@withContext IntelligentHint(
                type = HintType.NO_HINT_NEEDED,
                title = "Geschafft!",
                explanation = "Das Sudoku ist bereits vollständig gelöst!",
                detailedSteps = emptyList(),
                targetCell = null,
                value = null,
                highlightCells = emptyList(),
                eliminatedCandidates = emptyMap(),
                difficulty = HintDifficulty.BEGINNER,
                learningTip = null
            )
        }

        // Get the next logical step
        val step = solver.getNextStep()

        if (step == null) {
            return@withContext IntelligentHint(
                type = HintType.STUCK,
                title = "Keine logische Lösung gefunden",
                explanation = "Das Rätsel scheint sehr schwierig zu sein oder enthält möglicherweise einen Fehler. " +
                        "Überprüfe deine bisherigen Eingaben.",
                detailedSteps = listOf(
                    "Überprüfe alle eingetragenen Zahlen auf Fehler",
                    "Nutze den Notizmodus für Kandidaten",
                    "Manchmal hilft es, das Rätsel kurz zu pausieren"
                ),
                targetCell = null,
                value = null,
                highlightCells = emptyList(),
                eliminatedCandidates = emptyMap(),
                difficulty = HintDifficulty.EXPERT,
                learningTip = "Tipp: Bei sehr schweren Rätseln kann systematisches Ausschließen mit Notizen helfen."
            )
        }

        // Convert SolveStep to IntelligentHint with detailed explanations
        convertToIntelligentHint(step)
    }

    /**
     * Get a hint for a specific cell
     */
    suspend fun getHintForCell(board: SudokuBoard, row: Int, col: Int): IntelligentHint = withContext(Dispatchers.Default) {
        solver.initialize(board)

        val cell = board.cells[row][col]
        if (cell.value != 0 && !cell.isError) {
            return@withContext IntelligentHint(
                type = HintType.NO_HINT_NEEDED,
                title = "Zelle bereits gefüllt",
                explanation = "Diese Zelle enthält bereits die Zahl ${cell.value}.",
                detailedSteps = emptyList(),
                targetCell = row to col,
                value = cell.value,
                highlightCells = listOf(row to col),
                eliminatedCandidates = emptyMap(),
                difficulty = HintDifficulty.BEGINNER,
                learningTip = null
            )
        }

        val step = solver.getHintForCell(row, col)
        if (step != null) {
            convertToIntelligentHint(step)
        } else {
            IntelligentHint(
                type = HintType.STUCK,
                title = "Komplexe Situation",
                explanation = "Diese Zelle erfordert fortgeschrittene Techniken.",
                detailedSteps = listOf("Prüfe Kandidaten mit Notizmodus"),
                targetCell = row to col,
                value = null,
                highlightCells = listOf(row to col),
                eliminatedCandidates = emptyMap(),
                difficulty = HintDifficulty.EXPERT,
                learningTip = "Nutze den Notizmodus um mögliche Kandidaten zu markieren."
            )
        }
    }

    /**
     * Get all candidates for a cell
     */
    fun getCandidatesForCell(board: SudokuBoard, row: Int, col: Int): Set<Int> {
        solver.initialize(board)
        return solver.getCandidates(row, col)
    }

    private fun convertToIntelligentHint(step: SudokuSolver.SolveStep): IntelligentHint {
        val (type, difficulty, learningTip) = when (step.technique) {
            SudokuSolver.SolvingTechnique.NAKED_SINGLE -> Triple(
                HintType.NAKED_SINGLE,
                HintDifficulty.BEGINNER,
                "Tipp: Naked Singles sind die einfachste Technik. Wenn nur eine Zahl übrig bleibt, muss sie dort hin!"
            )
            SudokuSolver.SolvingTechnique.HIDDEN_SINGLE -> Triple(
                HintType.HIDDEN_SINGLE,
                HintDifficulty.EASY,
                "Tipp: Bei Hidden Singles fragst du: 'Wo kann diese Zahl noch hin?' statt 'Welche Zahl passt hier?'"
            )
            SudokuSolver.SolvingTechnique.NAKED_PAIR -> Triple(
                HintType.NAKED_PAIR,
                HintDifficulty.MEDIUM,
                "Tipp: Wenn zwei Zellen nur dieselben zwei Kandidaten haben, 'reservieren' sie diese Zahlen."
            )
            SudokuSolver.SolvingTechnique.HIDDEN_PAIR -> Triple(
                HintType.HIDDEN_PAIR,
                HintDifficulty.MEDIUM,
                "Tipp: Hidden Pairs verstecken sich unter anderen Kandidaten. Suche nach Zahlen die nur an zwei Stellen vorkommen."
            )
            SudokuSolver.SolvingTechnique.NAKED_TRIPLE -> Triple(
                HintType.NAKED_TRIPLE,
                HintDifficulty.ADVANCED,
                "Tipp: Triples funktionieren wie Pairs, aber mit drei Zellen und maximal drei Kandidaten."
            )
            SudokuSolver.SolvingTechnique.POINTING_PAIR -> Triple(
                HintType.POINTING_PAIR,
                HintDifficulty.MEDIUM,
                "Tipp: Wenn eine Zahl in einem Block nur in einer Zeile/Spalte möglich ist, zeigt sie in diese Richtung!"
            )
            SudokuSolver.SolvingTechnique.BOX_LINE_REDUCTION -> Triple(
                HintType.BOX_LINE_REDUCTION,
                HintDifficulty.MEDIUM,
                "Tipp: Das Gegenteil von Pointing Pair - die Zeile/Spalte schränkt den Block ein."
            )
            SudokuSolver.SolvingTechnique.X_WING -> Triple(
                HintType.X_WING,
                HintDifficulty.ADVANCED,
                "Tipp: X-Wing bildet ein Rechteck. Die Zahl muss diagonal zueinander stehen, also können die Spalten/Zeilen bereinigt werden."
            )
            SudokuSolver.SolvingTechnique.SWORDFISH -> Triple(
                HintType.SWORDFISH,
                HintDifficulty.EXPERT,
                "Tipp: Swordfish ist wie X-Wing, aber mit 3 Zeilen und 3 Spalten statt 2×2."
            )
            SudokuSolver.SolvingTechnique.BRUTE_FORCE -> Triple(
                HintType.STUCK,
                HintDifficulty.EXPERT,
                null
            )
        }

        val detailedSteps = createDetailedSteps(step)

        return IntelligentHint(
            type = type,
            title = step.technique.displayName,
            explanation = step.description,
            detailedSteps = detailedSteps,
            targetCell = step.affectedCells.firstOrNull(),
            value = step.value,
            highlightCells = step.highlightCells,
            eliminatedCandidates = step.eliminatedCandidates,
            difficulty = difficulty,
            learningTip = learningTip
        )
    }

    private fun createDetailedSteps(step: SudokuSolver.SolveStep): List<String> {
        return when (step.technique) {
            SudokuSolver.SolvingTechnique.NAKED_SINGLE -> listOf(
                "Schaue dir die markierte Zelle an",
                "Prüfe welche Zahlen in der Zeile bereits vorkommen",
                "Prüfe welche Zahlen in der Spalte bereits vorkommen",
                "Prüfe welche Zahlen im 3×3 Block bereits vorkommen",
                "Die einzige übrige Zahl ist ${step.value}"
            )
            SudokuSolver.SolvingTechnique.HIDDEN_SINGLE -> listOf(
                "Schaue dir die markierte Region an (Zeile, Spalte oder Block)",
                "Finde heraus, wo die Zahl ${step.value} noch platziert werden kann",
                "Es gibt nur eine mögliche Position für ${step.value}",
                "Trage ${step.value} in die markierte Zelle ein"
            )
            SudokuSolver.SolvingTechnique.NAKED_PAIR -> listOf(
                "Finde zwei Zellen mit identischen Kandidaten-Paaren",
                "Diese Zahlen sind für diese zwei Zellen 'reserviert'",
                "Entferne diese Kandidaten aus anderen Zellen der Einheit",
                "Dies kann weitere Singles freigeben"
            )
            SudokuSolver.SolvingTechnique.X_WING -> listOf(
                "Die markierte Zahl kommt in zwei Zeilen jeweils nur in denselben zwei Spalten vor",
                "Die vier Zellen bilden ein Rechteck",
                "Die Zahl muss diagonal zueinander platziert werden",
                "Entferne die Zahl aus allen anderen Zellen dieser Spalten"
            )
            else -> listOf(step.description)
        }
    }

    companion object {
        @Volatile
        private var instance: IntelligentHintService? = null

        fun getInstance(context: Context): IntelligentHintService {
            return instance ?: synchronized(this) {
                instance ?: IntelligentHintService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
