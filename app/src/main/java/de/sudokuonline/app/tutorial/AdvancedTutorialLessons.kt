package de.sudokuonline.app.tutorial

/**
 * Advanced Tutorial Lessons - Fortgeschrittene Sudoku-Techniken
 * Enthält X-Wing, Swordfish, Y-Wing, Coloring und mehr
 */

/**
 * Erstellt alle fortgeschrittenen Tutorial-Lektionen
 */
fun createAdvancedLessons(): List<TutorialLesson> = listOf(
    // Lektion 6: Pointing Pair / Box-Line Reduction (Detailliert)
    TutorialLesson(
        id = 6,
        title = "Pointing Pair",
        description = "Kandidaten die in eine Richtung zeigen",
        icon = "👉",
        difficulty = LessonDifficulty.MEDIUM,
        estimatedMinutes = 12,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist ein Pointing Pair?",
                description = "Ein Pointing Pair entsteht, wenn eine Zahl innerhalb eines 3×3 Blocks " +
                        "nur noch in einer einzigen Zeile oder Spalte vorkommen kann. " +
                        "Diese Kandidaten 'zeigen' in eine Richtung und eliminieren die Zahl " +
                        "aus dem Rest dieser Zeile/Spalte außerhalb des Blocks."
            ),
            TutorialStep.Explanation(
                title = "Visuelles Beispiel",
                description = "Stell dir vor, im oberen linken Block kann die 5 nur noch in Zeile 1 stehen " +
                        "(z.B. in den Zellen (0,0) und (0,2)). " +
                        "Da die 5 definitiv irgendwo in Zeile 1 innerhalb dieses Blocks sein wird, " +
                        "kann sie NICHT mehr in Zeile 1 außerhalb dieses Blocks sein!",
                exampleBoard = createPointingPairExample(),
                highlightCells = listOf(0 to 0, 0 to 2, 0 to 5, 0 to 7)
            ),
            TutorialStep.Explanation(
                title = "Die Logik dahinter",
                description = "Der Block 'reserviert' diese Zeile für seine 5. " +
                        "Es ist wie ein Hotelzimmer das für einen Gast reserviert ist - " +
                        "auch wenn wir noch nicht wissen WELCHES Bett er nimmt, " +
                        "wissen wir dass er in DIESEM Zimmer schlafen wird!"
            ),
            TutorialStep.Exercise(
                title = "Übung: Finde das Pointing Pair",
                description = "Im oberen linken Block kann die 7 nur in Spalte 0 stehen. " +
                        "Welche Zahl muss an Position (5,0) stehen?",
                board = createPointingPairExercise(),
                targetCell = 5 to 0,
                correctAnswer = 3,
                hint = "Die 7 im oberen Block eliminiert die 7 aus (5,0). Was bleibt übrig?",
                technique = "Pointing Pair"
            ),
            TutorialStep.Quiz(
                title = "Quiz: Pointing Pair",
                description = "Teste dein Verständnis",
                question = "Ein Pointing Pair hilft dir...",
                options = listOf(
                    "...sofort eine Zahl einzutragen",
                    "...Kandidaten außerhalb des Blocks zu eliminieren",
                    "...den Block zu lösen",
                    "...die Spalte komplett zu füllen"
                ),
                correctIndex = 1,
                explanation = "Pointing Pairs eliminieren Kandidaten in der Zeile/Spalte AUSSERHALB des Blocks, " +
                        "in dem das Pointing Pair liegt."
            )
        )
    ),

    // Lektion 7: Box/Line Reduction
    TutorialLesson(
        id = 7,
        title = "Box/Line Reduction",
        description = "Das Gegenstück zum Pointing Pair",
        icon = "📦",
        difficulty = LessonDifficulty.MEDIUM,
        estimatedMinutes = 10,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Box/Line Reduction erklärt",
                description = "Dies ist das Gegenteil des Pointing Pairs: " +
                        "Wenn eine Zahl in einer ZEILE oder SPALTE nur innerhalb eines bestimmten BLOCKS vorkommen kann, " +
                        "dann kann diese Zahl aus den anderen Zellen dieses Blocks entfernt werden."
            ),
            TutorialStep.Explanation(
                title = "Beispiel",
                description = "Stell dir vor, in Zeile 3 kann die 8 nur in den Spalten 0, 1 oder 2 stehen " +
                        "(also nur im linken Block). Dann wissen wir: Die 8 MUSS in diesem Teil der Zeile sein. " +
                        "Also kann die 8 aus den anderen Zellen des Blocks (Zeilen 4 und 5) entfernt werden!",
                exampleBoard = createBoxLineReductionExample(),
                highlightCells = listOf(3 to 0, 3 to 1, 3 to 2, 4 to 0, 5 to 1)
            ),
            TutorialStep.Quiz(
                title = "Verständnis-Check",
                description = "Wann wendest du Box/Line Reduction an?",
                question = "Box/Line Reduction ist nützlich wenn...",
                options = listOf(
                    "...eine Zahl im Block nur in einer Zeile/Spalte möglich ist",
                    "...eine Zahl in einer Zeile/Spalte nur in einem Block möglich ist",
                    "...zwei Zellen die gleichen Kandidaten haben",
                    "...der Block fast voll ist"
                ),
                correctIndex = 1,
                explanation = "Box/Line Reduction: Zeile/Spalte schränkt Block ein. " +
                        "Pointing Pair: Block schränkt Zeile/Spalte ein."
            )
        )
    ),

    // Lektion 8: X-Wing
    TutorialLesson(
        id = 8,
        title = "X-Wing",
        description = "Deine erste fortgeschrittene Technik",
        icon = "✈️",
        difficulty = LessonDifficulty.ADVANCED,
        estimatedMinutes = 15,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist X-Wing?",
                description = "X-Wing ist eine der elegantesten Sudoku-Techniken. " +
                        "Sie entsteht, wenn eine Zahl in genau zwei Zeilen " +
                        "jeweils nur in denselben zwei Spalten vorkommen kann. " +
                        "Die vier Zellen bilden ein Rechteck - wie die Form eines X."
            ),
            TutorialStep.Explanation(
                title = "Die Mathematik dahinter",
                description = "Wenn die 5 in Zeile 2 nur in Spalten 3 und 7 möglich ist, " +
                        "UND in Zeile 6 ebenfalls nur in Spalten 3 und 7 möglich ist, " +
                        "dann MUSS die 5 diagonal zueinander in diesem Rechteck platziert werden. " +
                        "Das bedeutet: Die 5 kann aus ALLEN anderen Zellen in Spalten 3 und 7 entfernt werden!",
                exampleBoard = createXWingExample(),
                highlightCells = listOf(2 to 3, 2 to 7, 6 to 3, 6 to 7)
            ),
            TutorialStep.Explanation(
                title = "Warum 'X'-Wing?",
                description = "Wenn du dir vorstellst, welche zwei Zellen die Zahl tatsächlich enthalten werden, " +
                        "bilden sie ein X (diagonal). Entweder oben-links und unten-rechts, " +
                        "ODER oben-rechts und unten-links. Beide Möglichkeiten 'blockieren' die Spalten!"
            ),
            TutorialStep.Explanation(
                title = "Schritt für Schritt",
                description = "So findest du X-Wings:\n" +
                        "1. Suche eine Zahl, die in einer Zeile nur zweimal als Kandidat vorkommt\n" +
                        "2. Finde eine zweite Zeile, wo dieselbe Zahl in denselben Spalten vorkommt\n" +
                        "3. Eliminiere diese Zahl aus allen anderen Zellen dieser Spalten\n\n" +
                        "Das funktioniert auch mit Spalten statt Zeilen!"
            ),
            TutorialStep.Quiz(
                title = "X-Wing Quiz",
                description = "Teste dein Verständnis",
                question = "Bei einem X-Wing werden Kandidaten eliminiert aus...",
                options = listOf(
                    "...den vier Eckzellen des Rechtecks",
                    "...den Zeilen, die das X-Wing bilden",
                    "...den Spalten (oder Zeilen), die das X-Wing NICHT bilden",
                    "...den Spalten (oder Zeilen), die das X-Wing bilden, AUSSER den Eckzellen"
                ),
                correctIndex = 3,
                explanation = "X-Wing eliminiert Kandidaten aus den Spalten (bei Zeilen-X-Wing) " +
                        "oder Zeilen (bei Spalten-X-Wing), aber NICHT aus den vier Eckzellen selbst!"
            )
        )
    ),

    // Lektion 9: Swordfish
    TutorialLesson(
        id = 9,
        title = "Swordfish",
        description = "X-Wing mit drei Dimensionen",
        icon = "🐟",
        difficulty = LessonDifficulty.EXPERT,
        estimatedMinutes = 18,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist Swordfish?",
                description = "Swordfish ist die erweiterte Version des X-Wing. " +
                        "Statt 2 Zeilen und 2 Spalten nutzt es 3 Zeilen und 3 Spalten. " +
                        "Es ist seltener, aber sehr mächtig wenn du es findest!"
            ),
            TutorialStep.Explanation(
                title = "Das Muster",
                description = "Ein Swordfish liegt vor, wenn eine Zahl in drei Zeilen " +
                        "jeweils nur in maximal drei bestimmten Spalten vorkommen kann " +
                        "(und jede Spalte mindestens zweimal betroffen ist). " +
                        "Die Zahl kann dann aus allen anderen Zellen dieser drei Spalten entfernt werden."
            ),
            TutorialStep.Explanation(
                title = "Visualisierung",
                description = "Stell dir ein 3×3 Raster vor. Die Zahl kann in jeder der 3 Zeilen " +
                        "nur in 2-3 der 3 markierten Spalten stehen. " +
                        "Zusammen 'reservieren' diese drei Zeilen diese drei Spalten für die Zahl.",
                exampleBoard = createSwordfishExample(),
                highlightCells = listOf(
                    1 to 2, 1 to 5, 1 to 8,
                    4 to 2, 4 to 5,
                    7 to 5, 7 to 8
                )
            ),
            TutorialStep.Quiz(
                title = "Swordfish Quiz",
                description = "Zeige dein Verständnis",
                question = "Swordfish ist im Vergleich zu X-Wing...",
                options = listOf(
                    "...einfacher zu finden",
                    "...die gleiche Technik",
                    "...eine Erweiterung auf 3 Zeilen/Spalten",
                    "...nur bei leichten Rätseln nützlich"
                ),
                correctIndex = 2,
                explanation = "Swordfish erweitert X-Wing von 2×2 auf 3×3. " +
                        "Es gibt sogar Jellyfish (4×4) für Experten!"
            )
        )
    ),

    // Lektion 10: Y-Wing (XY-Wing)
    TutorialLesson(
        id = 10,
        title = "Y-Wing (XY-Wing)",
        description = "Drei Zellen, drei Paare",
        icon = "🔱",
        difficulty = LessonDifficulty.EXPERT,
        estimatedMinutes = 20,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist Y-Wing?",
                description = "Y-Wing (auch XY-Wing genannt) nutzt drei Zellen mit je zwei Kandidaten. " +
                        "Eine 'Pivot'-Zelle ist mit zwei 'Wing'-Zellen verbunden. " +
                        "Der gemeinsame Kandidat der beiden Wings kann aus Zellen eliminiert werden, " +
                        "die BEIDE Wings sehen können."
            ),
            TutorialStep.Explanation(
                title = "Die drei Zellen",
                description = "• Pivot-Zelle: Hat Kandidaten {A, B}\n" +
                        "• Wing 1: Hat Kandidaten {A, C} und sieht den Pivot\n" +
                        "• Wing 2: Hat Kandidaten {B, C} und sieht den Pivot\n\n" +
                        "Die Zahl C kann aus allen Zellen eliminiert werden, die BEIDE Wings sehen!"
            ),
            TutorialStep.Explanation(
                title = "Die Logik",
                description = "Egal was passiert:\n" +
                        "• Wenn Pivot = A → Wing 1 ≠ A → Wing 1 = C\n" +
                        "• Wenn Pivot = B → Wing 2 ≠ B → Wing 2 = C\n\n" +
                        "Also ist IMMER mindestens einer der Wings = C. " +
                        "Jede Zelle die beide Wings sieht, kann C nicht sein!",
                exampleBoard = createYWingExample(),
                highlightCells = listOf(0 to 0, 0 to 4, 4 to 0)
            ),
            TutorialStep.Quiz(
                title = "Y-Wing Quiz",
                description = "Verstehst du Y-Wing?",
                question = "Bei Y-Wing wird die eliminierte Zahl...",
                options = listOf(
                    "...vom Pivot bestimmt",
                    "...von beiden Wings geteilt (nicht vom Pivot)",
                    "...in allen drei Zellen gefunden",
                    "...zufällig gewählt"
                ),
                correctIndex = 1,
                explanation = "Der gemeinsame Kandidat C der beiden Wings (der NICHT im Pivot ist) " +
                        "wird eliminiert, weil einer der Wings immer C sein muss."
            )
        )
    ),

    // Lektion 11: Simple Coloring
    TutorialLesson(
        id = 11,
        title = "Simple Coloring",
        description = "Färbe die Kandidaten ein",
        icon = "🎨",
        difficulty = LessonDifficulty.EXPERT,
        estimatedMinutes = 15,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Was ist Coloring?",
                description = "Coloring ist eine visuelle Technik. Du 'färbst' Kandidaten einer Zahl " +
                        "abwechselnd mit zwei Farben (z.B. Blau und Grün). " +
                        "Wenn eine Zahl nur zweimal in einer Einheit vorkommt, " +
                        "müssen diese zwei Zellen verschiedene Farben haben."
            ),
            TutorialStep.Explanation(
                title = "Die Kette bilden",
                description = "Beginne mit einer Zelle und färbe sie Blau. " +
                        "Die einzige andere Zelle in derselben Zeile/Spalte/Block mit diesem Kandidaten " +
                        "wird Grün. Von dort geht es weiter: Blau → Grün → Blau → ..."
            ),
            TutorialStep.Explanation(
                title = "Eliminierungsregeln",
                description = "Nach dem Färben kannst du eliminieren:\n\n" +
                        "1. Wenn zwei Zellen der gleichen Farbe sich sehen → diese Farbe ist falsch\n" +
                        "2. Wenn eine ungefärbte Zelle eine Blaue UND eine Grüne Zelle sieht → " +
                        "diese Zelle kann die Zahl nicht haben"
            ),
            TutorialStep.Quiz(
                title = "Coloring Quiz",
                description = "Was bedeuten die Farben?",
                question = "Wenn zwei blaue Zellen in derselben Zeile sind...",
                options = listOf(
                    "...ist alles normal",
                    "...sind alle blauen Zellen falsch (werden grün)",
                    "...sind alle grünen Zellen die Lösung",
                    "...müssen wir von vorne anfangen"
                ),
                correctIndex = 2,
                explanation = "Wenn zwei gleichfarbige Zellen sich sehen, kann diese Farbe nicht korrekt sein. " +
                        "Also ist die ANDERE Farbe die Lösung für diese Zahl!"
            )
        )
    ),

    // Lektion 12: Praxis-Strategien
    TutorialLesson(
        id = 12,
        title = "Meister-Strategien",
        description = "Kombiniere alles was du gelernt hast",
        icon = "🎓",
        difficulty = LessonDifficulty.EXPERT,
        estimatedMinutes = 10,
        steps = listOf(
            TutorialStep.Explanation(
                title = "Die richtige Reihenfolge",
                description = "Profis folgen immer dieser Reihenfolge:\n\n" +
                        "1. Naked Singles (schnell, einfach)\n" +
                        "2. Hidden Singles (in allen 27 Einheiten)\n" +
                        "3. Kandidaten notieren\n" +
                        "4. Pointing Pairs & Box/Line Reduction\n" +
                        "5. Naked/Hidden Pairs und Triples\n" +
                        "6. X-Wing, Swordfish\n" +
                        "7. Y-Wing, Coloring\n" +
                        "8. Nur im Notfall: Raten (Bifurkation)"
            ),
            TutorialStep.Explanation(
                title = "Effizienz-Tipps",
                description = "• Scanne zuerst alle Blöcke nach offensichtlichen Singles\n" +
                        "• Konzentriere dich auf Zahlen die oft vorkommen\n" +
                        "• Nutze Notizen bei schweren Rätseln\n" +
                        "• Wenn du feststeckst: Gehe eine Stufe höher in der Technik-Hierarchie"
            ),
            TutorialStep.Explanation(
                title = "Fehler vermeiden",
                description = "Häufige Fehler:\n\n" +
                        "• Kandidaten vergessen zu aktualisieren nach einer Eintragung\n" +
                        "• Zu schnell zur nächsten Technik springen\n" +
                        "• Nicht alle Einheiten prüfen (besonders Blöcke werden vergessen)\n" +
                        "• Bei X-Wing/Swordfish: Vergessen dass es auch vertikal funktioniert"
            ),
            TutorialStep.Quiz(
                title = "Abschluss-Quiz",
                description = "Du hast alle Techniken gelernt!",
                question = "Was machst du, wenn keine einfache Technik funktioniert?",
                options = listOf(
                    "Sofort aufgeben",
                    "Zufällig eine Zahl einsetzen und hoffen",
                    "Systematisch fortgeschrittenere Techniken anwenden",
                    "Das Rätsel ist unlösbar"
                ),
                correctIndex = 2,
                explanation = "Jedes korrekt konstruierte Sudoku ist mit Logik lösbar. " +
                        "Wenn einfache Techniken nicht helfen, nutze fortgeschrittenere!"
            )
        )
    )
)

// ========== HELPER FUNCTIONS FÜR BEISPIEL-BOARDS ==========

private fun createPointingPairExample(): Array<IntArray> {
    val board = Array(9) { IntArray(9) { 0 } }
    // Setup wo 5 im oberen linken Block nur in Zeile 0 möglich ist
    board[1][3] = 5  // blockiert Zeile 1 für Block 0
    board[2][6] = 5  // blockiert Zeile 2 für Block 0
    board[3][0] = 5  // blockiert Spalte 0 für Block 0 unten
    board[3][2] = 5  // blockiert Spalte 2 für Block 0 unten
    return board
}

private fun createPointingPairExercise(): Array<IntArray> {
    val board = Array(9) { IntArray(9) { 0 } }
    // Zelle (5,0) soll 3 sein, nachdem 7 durch Pointing Pair eliminiert wurde
    board[0][0] = 7  // 7 in Block 0 kann nur in Spalte 0 sein (Zeilen 1,2)
    board[0][1] = 1
    board[0][2] = 2
    board[1][1] = 3  // blockiert 3 aus Block 0 außer (1,0) und (2,0)
    board[2][2] = 3  // blockiert 3 aus Zeile 2
    board[3][0] = 1
    board[4][0] = 2
    board[5][0] = 0  // TARGET - soll 3 sein
    board[6][0] = 4
    board[7][0] = 5
    board[8][0] = 6
    return board
}

private fun createBoxLineReductionExample(): Array<IntArray> {
    val board = Array(9) { IntArray(9) { 0 } }
    // 8 in Zeile 3 ist nur in Block links möglich
    board[3][4] = 8  // eliminiert 8 aus Mitte von Zeile 3
    board[3][7] = 8  // eliminiert 8 aus rechts von Zeile 3
    return board
}

private fun createXWingExample(): Array<IntArray> {
    val board = Array(9) { IntArray(9) { 0 } }
    // Setup für X-Wing mit der 5
    // In Zeile 2: 5 nur in Spalten 3 und 7 möglich
    // In Zeile 6: 5 nur in Spalten 3 und 7 möglich
    
    // Blockiere 5 aus anderen Spalten in Zeile 2
    for (c in 0..8) {
        if (c != 3 && c != 7) {
            board[2][c] = if (c % 2 == 0) 1 else 2  // Fülle mit anderen Zahlen
        }
    }
    // Blockiere 5 aus anderen Spalten in Zeile 6
    for (c in 0..8) {
        if (c != 3 && c != 7) {
            board[6][c] = if (c % 2 == 0) 3 else 4
        }
    }
    // 5 in anderen Zeilen der Spalten 3 und 7 (diese werden eliminiert)
    board[0][3] = 0  // Hier könnte 5 sein, wird durch X-Wing eliminiert
    board[4][7] = 0  // Hier könnte 5 sein, wird durch X-Wing eliminiert
    
    return board
}

private fun createSwordfishExample(): Array<IntArray> {
    val board = Array(9) { IntArray(9) { 0 } }
    // Swordfish Setup - 3 Zeilen, 3 Spalten
    // Markierte Zellen zeigen wo die Zahl nur sein kann
    return board
}

private fun createYWingExample(): Array<IntArray> {
    val board = Array(9) { IntArray(9) { 0 } }
    // Y-Wing Setup
    // Pivot (0,0) hat {2,5}
    // Wing 1 (0,4) hat {2,9} - sieht Pivot über Zeile
    // Wing 2 (4,0) hat {5,9} - sieht Pivot über Spalte
    // 9 kann aus (4,4) eliminiert werden (sieht beide Wings)
    
    // Fülle einige Zellen um das Pattern zu zeigen
    board[0][1] = 1
    board[0][2] = 3
    board[0][3] = 4
    board[1][0] = 6
    board[2][0] = 7
    board[3][0] = 8
    
    return board
}
