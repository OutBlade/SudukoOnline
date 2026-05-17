package de.sudokuonline.app.tutorial

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tutorial Manager - Verwaltet das interaktive Tutorial-System
 */
class TutorialManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)

    private val _completedLessons = MutableStateFlow<Set<Int>>(loadCompletedLessons())
    val completedLessons: StateFlow<Set<Int>> = _completedLessons.asStateFlow()

    private val _currentLesson = MutableStateFlow<TutorialLesson?>(null)
    val currentLesson: StateFlow<TutorialLesson?> = _currentLesson.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    val lessons: List<TutorialLesson> = createLessons() + createAdvancedLessons()

    fun startLesson(lessonIndex: Int) {
        if (lessonIndex in lessons.indices) {
            _currentLesson.value = lessons[lessonIndex]
            _currentStepIndex.value = 0
        }
    }

    fun nextStep(): Boolean {
        val lesson = _currentLesson.value ?: return false
        return if (_currentStepIndex.value < lesson.steps.size - 1) {
            _currentStepIndex.value++
            true
        } else {
            false
        }
    }

    fun previousStep(): Boolean {
        return if (_currentStepIndex.value > 0) {
            _currentStepIndex.value--
            true
        } else {
            false
        }
    }

    fun completeLesson(lessonIndex: Int) {
        val updated = _completedLessons.value.toMutableSet()
        updated.add(lessonIndex)
        _completedLessons.value = updated
        saveCompletedLessons(updated)
    }

    fun exitLesson() {
        _currentLesson.value = null
        _currentStepIndex.value = 0
    }

    fun isLessonCompleted(lessonIndex: Int): Boolean {
        return lessonIndex in _completedLessons.value
    }

    fun isLessonUnlocked(lessonIndex: Int): Boolean {
        // First lesson is always unlocked, others require previous completion
        return lessonIndex == 0 || (lessonIndex - 1) in _completedLessons.value
    }

    fun getProgress(): Float {
        return if (lessons.isEmpty()) 0f else _completedLessons.value.size.toFloat() / lessons.size
    }

    fun resetProgress() {
        _completedLessons.value = emptySet()
        prefs.edit().remove("completed_lessons").apply()
    }

    private fun loadCompletedLessons(): Set<Int> {
        val stored = prefs.getStringSet("completed_lessons", emptySet()) ?: emptySet()
        return stored.mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun saveCompletedLessons(completed: Set<Int>) {
        prefs.edit().putStringSet("completed_lessons", completed.map { it.toString() }.toSet()).apply()
    }

    companion object {
        @Volatile
        private var instance: TutorialManager? = null

        fun getInstance(context: Context): TutorialManager {
            return instance ?: synchronized(this) {
                instance ?: TutorialManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Eine Tutorial-Lektion
 */
data class TutorialLesson(
    val id: Int,
    val title: String,
    val description: String,
    val icon: String, // Emoji
    val difficulty: LessonDifficulty,
    val steps: List<TutorialStep>,
    val estimatedMinutes: Int
)

enum class LessonDifficulty(val displayName: String, val color: Long) {
    BEGINNER("Anfänger", 0xFF4CAF50),
    EASY("Leicht", 0xFF8BC34A),
    MEDIUM("Mittel", 0xFFFF9800),
    ADVANCED("Fortgeschritten", 0xFFFF5722),
    EXPERT("Experte", 0xFFF44336)
}

/**
 * Ein einzelner Schritt im Tutorial
 */
sealed class TutorialStep {
    abstract val title: String
    abstract val description: String

    /**
     * Erklärender Text mit optionalem Bild
     */
    data class Explanation(
        override val title: String,
        override val description: String,
        val highlightCells: List<Pair<Int, Int>> = emptyList(), // row, col
        val highlightRegion: HighlightRegion? = null,
        val exampleBoard: Array<IntArray>? = null
    ) : TutorialStep() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Explanation) return false
            return title == other.title && description == other.description
        }
        override fun hashCode(): Int = title.hashCode() * 31 + description.hashCode()
    }

    /**
     * Interaktive Übung
     */
    data class Exercise(
        override val title: String,
        override val description: String,
        val board: Array<IntArray>,
        val targetCell: Pair<Int, Int>, // row, col
        val correctAnswer: Int,
        val hint: String,
        val technique: String
    ) : TutorialStep() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Exercise) return false
            return title == other.title && targetCell == other.targetCell
        }
        override fun hashCode(): Int = title.hashCode() * 31 + targetCell.hashCode()
    }

    /**
     * Quiz-Frage
     */
    data class Quiz(
        override val title: String,
        override val description: String,
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String
    ) : TutorialStep()
}

enum class HighlightRegion {
    ROW, COLUMN, BOX, CELL
}

/**
 * Erstellt alle Tutorial-Lektionen
 */
private fun createLessons(): List<TutorialLesson> = listOf(
    // Lektion 1: Grundlagen
    TutorialLesson(
        id = 0,
        title = "Was ist Sudoku?",
        description = "Lerne die Grundregeln von Sudoku kennen",
        icon = "📚",
        difficulty = LessonDifficulty.BEGINNER,
        estimatedMinutes = 5,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Willkommen!",
                description = "Sudoku ist ein Logikrätsel, das auf einem 9×9 Gitter gespielt wird. " +
                        "Das Ziel ist es, jede Zeile, Spalte und jeden 3×3 Block mit den Zahlen 1-9 zu füllen, " +
                        "wobei keine Zahl doppelt vorkommen darf.",
                exampleBoard = createEmptyBoard()
            ),
            TutorialStep.Explanation(
                title = "Das Spielfeld",
                description = "Das Sudoku-Gitter besteht aus 81 Zellen (9×9). " +
                        "Es ist in 9 Blöcke zu je 3×3 Zellen unterteilt. " +
                        "Einige Zahlen sind bereits vorgegeben - diese sind deine Hinweise!",
                highlightRegion = HighlightRegion.BOX
            ),
            TutorialStep.Explanation(
                title = "Regel 1: Zeilen",
                description = "Jede horizontale Zeile muss die Zahlen 1 bis 9 genau einmal enthalten. " +
                        "Keine Zahl darf sich in einer Zeile wiederholen!",
                highlightRegion = HighlightRegion.ROW,
                highlightCells = (0..8).map { 0 to it }
            ),
            TutorialStep.Explanation(
                title = "Regel 2: Spalten",
                description = "Jede vertikale Spalte muss ebenfalls die Zahlen 1 bis 9 genau einmal enthalten. " +
                        "Achte darauf, keine Zahl in einer Spalte zu wiederholen!",
                highlightRegion = HighlightRegion.COLUMN,
                highlightCells = (0..8).map { it to 0 }
            ),
            TutorialStep.Explanation(
                title = "Regel 3: Blöcke",
                description = "Jeder 3×3 Block muss alle Zahlen von 1 bis 9 enthalten. " +
                        "Die Blöcke sind durch dickere Linien getrennt.",
                highlightRegion = HighlightRegion.BOX,
                highlightCells = listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2)
            ),
            TutorialStep.Quiz(
                title = "Quiz",
                description = "Teste dein Verständnis!",
                question = "Wie viele Zahlen müssen in jeder Zeile, Spalte und jedem Block stehen?",
                options = listOf("6", "8", "9", "12"),
                correctIndex = 2,
                explanation = "Richtig! Jede Zeile, Spalte und jeder Block enthält genau die Zahlen 1 bis 9."
            )
        )
    ),

    // Lektion 2: Erste Technik - Naked Single
    TutorialLesson(
        id = 1,
        title = "Naked Single",
        description = "Die einfachste Lösungstechnik",
        icon = "🎯",
        difficulty = LessonDifficulty.BEGINNER,
        estimatedMinutes = 7,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist ein Naked Single?",
                description = "Ein 'Naked Single' (Nackter Einzelner) ist die einfachste Technik. " +
                        "Wenn für eine Zelle nur noch eine einzige Zahl möglich ist, " +
                        "weil alle anderen bereits in der Zeile, Spalte oder im Block vorkommen, " +
                        "dann MUSS diese Zahl dort hin!"
            ),
            TutorialStep.Explanation(
                title = "Beispiel",
                description = "Schaue dir diese Zeile an. Die Zahlen 1-8 sind bereits vorhanden. " +
                        "Welche Zahl fehlt? Genau - die 9!",
                exampleBoard = createNakedSingleExample(),
                highlightCells = listOf(0 to 8)
            ),
            TutorialStep.Exercise(
                title = "Übung 1",
                description = "Finde die fehlende Zahl in dieser Zeile!",
                board = createNakedSingleExercise1(),
                targetCell = 0 to 4,
                correctAnswer = 5,
                hint = "Welche Zahl von 1-9 fehlt in dieser Zeile?",
                technique = "Naked Single"
            ),
            TutorialStep.Exercise(
                title = "Übung 2",
                description = "Jetzt mit einer Spalte! Finde die fehlende Zahl.",
                board = createNakedSingleExercise2(),
                targetCell = 4 to 0,
                correctAnswer = 7,
                hint = "Schaue dir die erste Spalte genau an.",
                technique = "Naked Single"
            ),
            TutorialStep.Quiz(
                title = "Verständnis-Check",
                description = "Wann kannst du die Naked Single Technik anwenden?",
                question = "Ein Naked Single liegt vor, wenn...",
                options = listOf(
                    "...eine Zelle leer ist",
                    "...nur noch eine Zahl für eine Zelle möglich ist",
                    "...zwei Zahlen möglich sind",
                    "...die Zelle in einer Ecke liegt"
                ),
                correctIndex = 1,
                explanation = "Ein Naked Single bedeutet, dass durch Ausschluss nur noch eine einzige Zahl in die Zelle passt."
            )
        )
    ),

    // Lektion 3: Hidden Single
    TutorialLesson(
        id = 2,
        title = "Hidden Single",
        description = "Finde versteckte Einzelzahlen",
        icon = "🔍",
        difficulty = LessonDifficulty.EASY,
        estimatedMinutes = 8,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist ein Hidden Single?",
                description = "Ein 'Hidden Single' (Versteckter Einzelner) liegt vor, wenn eine Zahl " +
                        "in einer Zeile, Spalte oder einem Block nur noch an einer einzigen Stelle möglich ist. " +
                        "Die Zahl 'versteckt' sich, weil in der Zelle auch andere Zahlen möglich wären."
            ),
            TutorialStep.Explanation(
                title = "Der Unterschied",
                description = "Beim Naked Single schauen wir auf EINE Zelle und fragen: 'Welche Zahl passt hier?' " +
                        "Beim Hidden Single schauen wir auf EINE Zahl und fragen: 'Wo kann diese Zahl noch hin?'"
            ),
            TutorialStep.Explanation(
                title = "Beispiel",
                description = "Schaue auf diesen Block. Die 5 kann nur noch an einer Stelle stehen! " +
                        "In den anderen Zellen wird sie durch vorhandene 5en in den Zeilen/Spalten blockiert.",
                exampleBoard = createHiddenSingleExample(),
                highlightCells = listOf(1 to 1)
            ),
            TutorialStep.Exercise(
                title = "Übung",
                description = "Wo muss die 3 in diesem Block hin?",
                board = createHiddenSingleExercise(),
                targetCell = 2 to 2,
                correctAnswer = 3,
                hint = "Prüfe, wo die 3 im oberen linken Block noch platziert werden kann.",
                technique = "Hidden Single"
            ),
            TutorialStep.Quiz(
                title = "Quiz",
                description = "Teste dein Wissen!",
                question = "Bei Hidden Single konzentrierst du dich auf...",
                options = listOf(
                    "...eine leere Zelle",
                    "...eine bestimmte Zahl",
                    "...die größte Zahl",
                    "...die kleinste Zahl"
                ),
                correctIndex = 1,
                explanation = "Bei Hidden Single fragst du: 'Wo kann diese bestimmte Zahl noch platziert werden?'"
            )
        )
    ),

    // Lektion 4: Kandidaten-Notation
    TutorialLesson(
        id = 3,
        title = "Notizen & Kandidaten",
        description = "Nutze Notizen effektiv",
        icon = "✏️",
        difficulty = LessonDifficulty.EASY,
        estimatedMinutes = 6,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was sind Kandidaten?",
                description = "Kandidaten (auch Pencil Marks genannt) sind kleine Notizen in einer Zelle. " +
                        "Sie zeigen dir, welche Zahlen dort noch möglich sind. " +
                        "Dies ist besonders bei schwierigeren Rätseln hilfreich!"
            ),
            TutorialStep.Explanation(
                title = "Notizen machen",
                description = "In unserer App kannst du den Notiz-Modus aktivieren (Stift-Symbol). " +
                        "Dann werden deine Eingaben als kleine Zahlen gespeichert, nicht als endgültige Lösung."
            ),
            TutorialStep.Explanation(
                title = "Systematisch vorgehen",
                description = "Profis füllen oft zuerst alle Kandidaten aus. Dann suchen sie nach Mustern: " +
                        "• Wenn nur ein Kandidat übrig ist → Naked Single\n" +
                        "• Wenn eine Zahl nur einmal vorkommt → Hidden Single\n" +
                        "• Paare und Tripel helfen beim Eliminieren"
            ),
            TutorialStep.Quiz(
                title = "Quiz",
                description = "Wann sind Notizen besonders nützlich?",
                question = "Notizen helfen vor allem bei...",
                options = listOf(
                    "...sehr einfachen Rätseln",
                    "...mittleren bis schweren Rätseln",
                    "...nur bei Experten-Rätseln",
                    "...gar nicht, sie verwirren nur"
                ),
                correctIndex = 1,
                explanation = "Notizen sind besonders bei mittleren bis schweren Rätseln hilfreich, " +
                        "wenn einfache Techniken nicht mehr ausreichen."
            )
        )
    ),

    // Lektion 5: Naked Pair
    TutorialLesson(
        id = 4,
        title = "Naked Pair",
        description = "Zwei Zellen, zwei Zahlen",
        icon = "👯",
        difficulty = LessonDifficulty.MEDIUM,
        estimatedMinutes = 10,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist ein Naked Pair?",
                description = "Ein 'Naked Pair' liegt vor, wenn zwei Zellen in derselben Zeile, Spalte oder Block " +
                        "nur noch dieselben zwei Kandidaten haben. " +
                        "Diese zwei Zahlen MÜSSEN in diese zwei Zellen - also können sie aus anderen Zellen entfernt werden!"
            ),
            TutorialStep.Explanation(
                title = "Beispiel",
                description = "Stell dir vor, Zelle A und B in einer Zeile haben beide nur die Kandidaten {3, 7}. " +
                        "Eine der Zellen wird die 3, die andere die 7 haben. " +
                        "Also können 3 und 7 aus allen anderen Zellen dieser Zeile entfernt werden!"
            ),
            TutorialStep.Explanation(
                title = "Warum funktioniert das?",
                description = "Die beiden Zahlen sind 'reserviert' für diese zwei Zellen. " +
                        "Es ist wie zwei Parkplätze für zwei bestimmte Autos - " +
                        "kein anderes Auto (keine andere Zelle) kann diese Plätze nutzen!"
            ),
            TutorialStep.Quiz(
                title = "Verständnis-Check",
                description = "Was ist die Folge eines Naked Pairs?",
                question = "Wenn du ein Naked Pair findest, kannst du...",
                options = listOf(
                    "...sofort beide Zahlen eintragen",
                    "...die Zahlen aus anderen Zellen der gleichen Einheit entfernen",
                    "...das Rätsel als gelöst betrachten",
                    "...nichts tun, es ist nur informativ"
                ),
                correctIndex = 1,
                explanation = "Ein Naked Pair erlaubt es dir, diese Zahlen als Kandidaten aus anderen Zellen " +
                        "derselben Zeile, Spalte oder desselben Blocks zu entfernen."
            )
        )
    ),

    // Lektion 6: Strategien für Fortgeschrittene
    TutorialLesson(
        id = 5,
        title = "Fortgeschrittene Tipps",
        description = "Werde ein Sudoku-Profi",
        icon = "🏆",
        difficulty = LessonDifficulty.ADVANCED,
        estimatedMinutes = 8,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Systematisch arbeiten",
                description = "Profis gehen systematisch vor:\n" +
                        "1. Zuerst alle offensichtlichen Singles finden\n" +
                        "2. Dann Kandidaten notieren\n" +
                        "3. Nach Paaren und Tripeln suchen\n" +
                        "4. Fortgeschrittene Techniken nur bei Bedarf"
            ),
            TutorialStep.Explanation(
                title = "Pointing Pair",
                description = "Wenn eine Zahl in einem Block nur in einer Zeile/Spalte vorkommen kann, " +
                        "kann sie aus dem Rest dieser Zeile/Spalte entfernt werden. " +
                        "Die Kandidaten 'zeigen' auf eine Richtung!"
            ),
            TutorialStep.Explanation(
                title = "Box/Line Reduction",
                description = "Das Gegenteil des Pointing Pairs: " +
                        "Wenn eine Zahl in einer Zeile/Spalte nur in einem Block vorkommen kann, " +
                        "kann sie aus dem Rest des Blocks entfernt werden."
            ),
            TutorialStep.Explanation(
                title = "X-Wing",
                description = "Eine fortgeschrittene Technik für Experten: " +
                        "Wenn eine Zahl in zwei Zeilen jeweils nur in denselben zwei Spalten vorkommen kann, " +
                        "bildet sie ein 'X'-Muster und kann aus anderen Zellen dieser Spalten entfernt werden."
            ),
            TutorialStep.Quiz(
                title = "Abschluss-Quiz",
                description = "Was hast du gelernt?",
                question = "Was ist der beste Ansatz für schwere Sudokus?",
                options = listOf(
                    "Sofort mit X-Wing beginnen",
                    "Zufällig Zahlen ausprobieren",
                    "Systematisch von einfach zu komplex",
                    "Nur die leeren Zellen zählen"
                ),
                correctIndex = 2,
                explanation = "Beginne immer mit den einfachsten Techniken und steigere die Komplexität nur bei Bedarf!"
            )
        )
    )
)

// Helper functions for creating example boards
private fun createEmptyBoard(): Array<IntArray> = Array(9) { IntArray(9) { 0 } }

private fun createNakedSingleExample(): Array<IntArray> {
    val board = createEmptyBoard()
    board[0] = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 0)
    return board
}

private fun createNakedSingleExercise1(): Array<IntArray> {
    val board = createEmptyBoard()
    board[0] = intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9)
    return board
}

private fun createNakedSingleExercise2(): Array<IntArray> {
    val board = createEmptyBoard()
    for (i in 0..8) {
        if (i != 4) {
            board[i][0] = when (i) {
                0 -> 1; 1 -> 2; 2 -> 3; 3 -> 4; 5 -> 8; 6 -> 9; 7 -> 5; 8 -> 6
                else -> 0
            }
        }
    }
    return board
}

private fun createHiddenSingleExample(): Array<IntArray> {
    val board = createEmptyBoard()
    // Block oben links hat eine 5, die nur in (1,1) sein kann
    board[0][3] = 5 // blockiert Zeile 0
    board[2][4] = 5 // blockiert Zeile 2
    board[1][6] = 5 // blockiert Spalte 1 außer Block
    return board
}

private fun createHiddenSingleExercise(): Array<IntArray> {
    val board = createEmptyBoard()
    board[0][3] = 3 // blockiert Zeile 0
    board[1][4] = 3 // blockiert Zeile 1
    board[2][0] = 1 // belegt (2,0)
    board[2][1] = 2 // belegt (2,1)
    return board
}
