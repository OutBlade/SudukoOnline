package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.SudokuBoard

/**
 * Intelligent Sudoku Solver with step-by-step explanations
 * Implements various solving techniques from basic to advanced
 */
class SudokuSolver {

    /**
     * Represents a solving step with explanation
     */
    data class SolveStep(
        val technique: SolvingTechnique,
        val description: String,
        val affectedCells: List<Pair<Int, Int>>,
        val highlightCells: List<Pair<Int, Int>>,
        val value: Int?,
        val eliminatedCandidates: Map<Pair<Int, Int>, List<Int>> = emptyMap()
    )

    /**
     * Available solving techniques in order of complexity
     */
    enum class SolvingTechnique(val displayName: String, val difficulty: Int) {
        NAKED_SINGLE("Nackter Single", 1),
        HIDDEN_SINGLE("Versteckter Single", 1),
        NAKED_PAIR("Nacktes Paar", 2),
        HIDDEN_PAIR("Verstecktes Paar", 2),
        NAKED_TRIPLE("Nacktes Tripel", 3),
        POINTING_PAIR("Zeigendes Paar", 3),
        BOX_LINE_REDUCTION("Box/Linien-Reduktion", 3),
        X_WING("X-Wing", 4),
        SWORDFISH("Schwertfisch", 5),
        BRUTE_FORCE("Brute Force", 10)
    }

    // Internal board representation with candidates
    private var board = Array(9) { IntArray(9) }
    private var candidates = Array(9) { Array(9) { mutableSetOf<Int>() } }

    /**
     * Initialize solver with a board
     */
    fun initialize(puzzle: List<List<Int>>) {
        // Copy board
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                board[row][col] = puzzle[row][col]
            }
        }
        // Calculate initial candidates
        calculateAllCandidates()
    }

    /**
     * Initialize from SudokuBoard
     */
    fun initialize(sudokuBoard: SudokuBoard) {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                board[row][col] = sudokuBoard.cells[row][col].value
            }
        }
        calculateAllCandidates()
    }

    /**
     * Get the next solving step with explanation
     */
    fun getNextStep(): SolveStep? {
        // Try techniques in order of difficulty
        findNakedSingle()?.let { return it }
        findHiddenSingle()?.let { return it }
        findNakedPair()?.let { return it }
        findPointingPair()?.let { return it }
        findBoxLineReduction()?.let { return it }
        findHiddenPair()?.let { return it }
        findNakedTriple()?.let { return it }
        findXWing()?.let { return it }

        // If no logical technique works, use brute force for one cell
        return findBruteForceStep()
    }

    /**
     * Get hint for a specific cell
     */
    fun getHintForCell(row: Int, col: Int): SolveStep? {
        if (board[row][col] != 0) return null

        val cellCandidates = candidates[row][col]
        if (cellCandidates.size == 1) {
            val value = cellCandidates.first()
            return SolveStep(
                technique = SolvingTechnique.NAKED_SINGLE,
                description = "Diese Zelle kann nur $value sein, da alle anderen Zahlen bereits in der Zeile, Spalte oder Box vorkommen.",
                affectedCells = listOf(Pair(row, col)),
                highlightCells = getRelatedCells(row, col),
                value = value
            )
        }

        // Check for hidden single in row, col, or box
        for (candidate in cellCandidates) {
            // Check row
            var uniqueInRow = true
            for (c in 0 until 9) {
                if (c != col && candidate in candidates[row][c]) {
                    uniqueInRow = false
                    break
                }
            }
            if (uniqueInRow) {
                return SolveStep(
                    technique = SolvingTechnique.HIDDEN_SINGLE,
                    description = "$candidate kann in dieser Zeile nur hier stehen.",
                    affectedCells = listOf(Pair(row, col)),
                    highlightCells = (0 until 9).map { Pair(row, it) },
                    value = candidate
                )
            }

            // Check column
            var uniqueInCol = true
            for (r in 0 until 9) {
                if (r != row && candidate in candidates[r][col]) {
                    uniqueInCol = false
                    break
                }
            }
            if (uniqueInCol) {
                return SolveStep(
                    technique = SolvingTechnique.HIDDEN_SINGLE,
                    description = "$candidate kann in dieser Spalte nur hier stehen.",
                    affectedCells = listOf(Pair(row, col)),
                    highlightCells = (0 until 9).map { Pair(it, col) },
                    value = candidate
                )
            }

            // Check box
            val boxRow = (row / 3) * 3
            val boxCol = (col / 3) * 3
            var uniqueInBox = true
            for (r in boxRow until boxRow + 3) {
                for (c in boxCol until boxCol + 3) {
                    if ((r != row || c != col) && candidate in candidates[r][c]) {
                        uniqueInBox = false
                        break
                    }
                }
            }
            if (uniqueInBox) {
                val boxCells = mutableListOf<Pair<Int, Int>>()
                for (r in boxRow until boxRow + 3) {
                    for (c in boxCol until boxCol + 3) {
                        boxCells.add(Pair(r, c))
                    }
                }
                return SolveStep(
                    technique = SolvingTechnique.HIDDEN_SINGLE,
                    description = "$candidate kann in dieser Box nur hier stehen.",
                    affectedCells = listOf(Pair(row, col)),
                    highlightCells = boxCells,
                    value = candidate
                )
            }
        }

        // No simple hint found
        return SolveStep(
            technique = SolvingTechnique.BRUTE_FORCE,
            description = "Mögliche Zahlen: ${cellCandidates.sorted().joinToString(", ")}. Versuche logisches Ausschließen oder probiere eine Zahl.",
            affectedCells = listOf(Pair(row, col)),
            highlightCells = listOf(Pair(row, col)),
            value = null
        )
    }

    /**
     * Solve the entire puzzle and return all steps
     */
    fun solveWithSteps(): List<SolveStep> {
        val steps = mutableListOf<SolveStep>()
        var maxIterations = 500

        while (!isSolved() && maxIterations > 0) {
            val step = getNextStep() ?: break
            steps.add(step)

            // Apply the step
            if (step.value != null && step.affectedCells.isNotEmpty()) {
                val (row, col) = step.affectedCells.first()
                board[row][col] = step.value
                calculateAllCandidates()
            } else if (step.eliminatedCandidates.isNotEmpty()) {
                for ((cell, eliminated) in step.eliminatedCandidates) {
                    candidates[cell.first][cell.second].removeAll(eliminated.toSet())
                }
            }

            maxIterations--
        }

        return steps
    }

    /**
     * Check if puzzle is solved
     */
    fun isSolved(): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) return false
            }
        }
        return true
    }

    /**
     * Get candidates for a cell
     */
    fun getCandidates(row: Int, col: Int): Set<Int> = candidates[row][col].toSet()

    // ============ SOLVING TECHNIQUES ============

    private fun findNakedSingle(): SolveStep? {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0 && candidates[row][col].size == 1) {
                    val value = candidates[row][col].first()
                    return SolveStep(
                        technique = SolvingTechnique.NAKED_SINGLE,
                        description = "Zelle [${row + 1},${col + 1}]: Nur $value ist möglich, da alle anderen Zahlen bereits in der Zeile, Spalte oder Box vorkommen.",
                        affectedCells = listOf(Pair(row, col)),
                        highlightCells = getRelatedCells(row, col),
                        value = value
                    )
                }
            }
        }
        return null
    }

    private fun findHiddenSingle(): SolveStep? {
        // Check rows
        for (row in 0 until 9) {
            for (num in 1..9) {
                val positions = mutableListOf<Int>()
                for (col in 0 until 9) {
                    if (board[row][col] == 0 && num in candidates[row][col]) {
                        positions.add(col)
                    }
                }
                if (positions.size == 1) {
                    val col = positions[0]
                    return SolveStep(
                        technique = SolvingTechnique.HIDDEN_SINGLE,
                        description = "Zeile ${row + 1}: $num kann nur in Spalte ${col + 1} stehen, da es die einzige freie Zelle in dieser Zeile ist, die $num enthalten kann.",
                        affectedCells = listOf(Pair(row, col)),
                        highlightCells = (0 until 9).map { Pair(row, it) },
                        value = num
                    )
                }
            }
        }

        // Check columns
        for (col in 0 until 9) {
            for (num in 1..9) {
                val positions = mutableListOf<Int>()
                for (row in 0 until 9) {
                    if (board[row][col] == 0 && num in candidates[row][col]) {
                        positions.add(row)
                    }
                }
                if (positions.size == 1) {
                    val row = positions[0]
                    return SolveStep(
                        technique = SolvingTechnique.HIDDEN_SINGLE,
                        description = "Spalte ${col + 1}: $num kann nur in Zeile ${row + 1} stehen, da es die einzige freie Zelle in dieser Spalte ist, die $num enthalten kann.",
                        affectedCells = listOf(Pair(row, col)),
                        highlightCells = (0 until 9).map { Pair(it, col) },
                        value = num
                    )
                }
            }
        }

        // Check boxes
        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                for (num in 1..9) {
                    val positions = mutableListOf<Pair<Int, Int>>()
                    for (r in boxRow * 3 until boxRow * 3 + 3) {
                        for (c in boxCol * 3 until boxCol * 3 + 3) {
                            if (board[r][c] == 0 && num in candidates[r][c]) {
                                positions.add(Pair(r, c))
                            }
                        }
                    }
                    if (positions.size == 1) {
                        val (row, col) = positions[0]
                        val boxCells = mutableListOf<Pair<Int, Int>>()
                        for (r in boxRow * 3 until boxRow * 3 + 3) {
                            for (c in boxCol * 3 until boxCol * 3 + 3) {
                                boxCells.add(Pair(r, c))
                            }
                        }
                        return SolveStep(
                            technique = SolvingTechnique.HIDDEN_SINGLE,
                            description = "Box ${boxRow * 3 + boxCol + 1}: $num kann nur in Zelle [${row + 1},${col + 1}] stehen.",
                            affectedCells = listOf(Pair(row, col)),
                            highlightCells = boxCells,
                            value = num
                        )
                    }
                }
            }
        }

        return null
    }

    private fun findNakedPair(): SolveStep? {
        // Check rows for naked pairs
        for (row in 0 until 9) {
            val pairs = mutableListOf<Pair<Int, Set<Int>>>()
            for (col in 0 until 9) {
                if (board[row][col] == 0 && candidates[row][col].size == 2) {
                    pairs.add(Pair(col, candidates[row][col].toSet()))
                }
            }

            for (i in 0 until pairs.size) {
                for (j in i + 1 until pairs.size) {
                    if (pairs[i].second == pairs[j].second) {
                        val pairCandidates = pairs[i].second
                        val col1 = pairs[i].first
                        val col2 = pairs[j].first
                        val eliminations = mutableMapOf<Pair<Int, Int>, List<Int>>()

                        for (col in 0 until 9) {
                            if (col != col1 && col != col2 && board[row][col] == 0) {
                                val toRemove = candidates[row][col].intersect(pairCandidates)
                                if (toRemove.isNotEmpty()) {
                                    eliminations[Pair(row, col)] = toRemove.toList()
                                }
                            }
                        }

                        if (eliminations.isNotEmpty()) {
                            return SolveStep(
                                technique = SolvingTechnique.NAKED_PAIR,
                                description = "Nacktes Paar {${pairCandidates.sorted().joinToString(",")}} in Zeile ${row + 1} (Spalten ${col1 + 1} und ${col2 + 1}). Diese Zahlen können aus den anderen Zellen der Zeile entfernt werden.",
                                affectedCells = listOf(Pair(row, col1), Pair(row, col2)),
                                highlightCells = (0 until 9).map { Pair(row, it) },
                                value = null,
                                eliminatedCandidates = eliminations
                            )
                        }
                    }
                }
            }
        }

        // Similar for columns and boxes...
        return null
    }

    private fun findHiddenPair(): SolveStep? {
        // Check rows for hidden pairs
        for (row in 0 until 9) {
            for (n1 in 1..8) {
                for (n2 in n1 + 1..9) {
                    val positions1 = (0 until 9).filter { board[row][it] == 0 && n1 in candidates[row][it] }
                    val positions2 = (0 until 9).filter { board[row][it] == 0 && n2 in candidates[row][it] }

                    if (positions1.size == 2 && positions1 == positions2) {
                        val col1 = positions1[0]
                        val col2 = positions1[1]

                        // Check if there are other candidates to eliminate
                        val other1 = candidates[row][col1] - setOf(n1, n2)
                        val other2 = candidates[row][col2] - setOf(n1, n2)

                        if (other1.isNotEmpty() || other2.isNotEmpty()) {
                            val eliminations = mutableMapOf<Pair<Int, Int>, List<Int>>()
                            if (other1.isNotEmpty()) eliminations[Pair(row, col1)] = other1.toList()
                            if (other2.isNotEmpty()) eliminations[Pair(row, col2)] = other2.toList()

                            return SolveStep(
                                technique = SolvingTechnique.HIDDEN_PAIR,
                                description = "Verstecktes Paar {$n1,$n2} in Zeile ${row + 1}. Diese Zahlen kommen nur in Spalten ${col1 + 1} und ${col2 + 1} vor, daher können andere Kandidaten dort entfernt werden.",
                                affectedCells = listOf(Pair(row, col1), Pair(row, col2)),
                                highlightCells = (0 until 9).map { Pair(row, it) },
                                value = null,
                                eliminatedCandidates = eliminations
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findNakedTriple(): SolveStep? {
        // Simplified - check rows
        for (row in 0 until 9) {
            val cells = (0 until 9).filter { board[row][it] == 0 && candidates[row][it].size in 2..3 }

            if (cells.size >= 3) {
                for (i in 0 until cells.size - 2) {
                    for (j in i + 1 until cells.size - 1) {
                        for (k in j + 1 until cells.size) {
                            val combined = candidates[row][cells[i]] +
                                    candidates[row][cells[j]] +
                                    candidates[row][cells[k]]
                            if (combined.size == 3) {
                                val eliminations = mutableMapOf<Pair<Int, Int>, List<Int>>()
                                for (col in 0 until 9) {
                                    if (col !in listOf(cells[i], cells[j], cells[k]) && board[row][col] == 0) {
                                        val toRemove = candidates[row][col].intersect(combined)
                                        if (toRemove.isNotEmpty()) {
                                            eliminations[Pair(row, col)] = toRemove.toList()
                                        }
                                    }
                                }
                                if (eliminations.isNotEmpty()) {
                                    return SolveStep(
                                        technique = SolvingTechnique.NAKED_TRIPLE,
                                        description = "Nacktes Tripel {${combined.sorted().joinToString(",")}} in Zeile ${row + 1}.",
                                        affectedCells = listOf(Pair(row, cells[i]), Pair(row, cells[j]), Pair(row, cells[k])),
                                        highlightCells = (0 until 9).map { Pair(row, it) },
                                        value = null,
                                        eliminatedCandidates = eliminations
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findPointingPair(): SolveStep? {
        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                for (num in 1..9) {
                    val positions = mutableListOf<Pair<Int, Int>>()
                    for (r in boxRow * 3 until boxRow * 3 + 3) {
                        for (c in boxCol * 3 until boxCol * 3 + 3) {
                            if (board[r][c] == 0 && num in candidates[r][c]) {
                                positions.add(Pair(r, c))
                            }
                        }
                    }

                    if (positions.size in 2..3) {
                        // Check if all in same row
                        if (positions.map { it.first }.distinct().size == 1) {
                            val row = positions[0].first
                            val eliminations = mutableMapOf<Pair<Int, Int>, List<Int>>()
                            for (col in 0 until 9) {
                                if (col / 3 != boxCol && board[row][col] == 0 && num in candidates[row][col]) {
                                    eliminations[Pair(row, col)] = listOf(num)
                                }
                            }
                            if (eliminations.isNotEmpty()) {
                                return SolveStep(
                                    technique = SolvingTechnique.POINTING_PAIR,
                                    description = "Zeigendes Paar: $num in Box ${boxRow * 3 + boxCol + 1} kommt nur in Zeile ${row + 1} vor. $num kann aus dem Rest der Zeile entfernt werden.",
                                    affectedCells = positions,
                                    highlightCells = (0 until 9).map { Pair(row, it) },
                                    value = null,
                                    eliminatedCandidates = eliminations
                                )
                            }
                        }

                        // Check if all in same column
                        if (positions.map { it.second }.distinct().size == 1) {
                            val col = positions[0].second
                            val eliminations = mutableMapOf<Pair<Int, Int>, List<Int>>()
                            for (row in 0 until 9) {
                                if (row / 3 != boxRow && board[row][col] == 0 && num in candidates[row][col]) {
                                    eliminations[Pair(row, col)] = listOf(num)
                                }
                            }
                            if (eliminations.isNotEmpty()) {
                                return SolveStep(
                                    technique = SolvingTechnique.POINTING_PAIR,
                                    description = "Zeigendes Paar: $num in Box ${boxRow * 3 + boxCol + 1} kommt nur in Spalte ${col + 1} vor. $num kann aus dem Rest der Spalte entfernt werden.",
                                    affectedCells = positions,
                                    highlightCells = (0 until 9).map { Pair(it, col) },
                                    value = null,
                                    eliminatedCandidates = eliminations
                                )
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findBoxLineReduction(): SolveStep? {
        // Check rows
        for (row in 0 until 9) {
            for (num in 1..9) {
                val positions = (0 until 9).filter { board[row][it] == 0 && num in candidates[row][it] }
                if (positions.size in 2..3 && positions.map { it / 3 }.distinct().size == 1) {
                    val boxCol = positions[0] / 3
                    val boxRow = row / 3
                    val eliminations = mutableMapOf<Pair<Int, Int>, List<Int>>()

                    for (r in boxRow * 3 until boxRow * 3 + 3) {
                        for (c in boxCol * 3 until boxCol * 3 + 3) {
                            if (r != row && board[r][c] == 0 && num in candidates[r][c]) {
                                eliminations[Pair(r, c)] = listOf(num)
                            }
                        }
                    }

                    if (eliminations.isNotEmpty()) {
                        return SolveStep(
                            technique = SolvingTechnique.BOX_LINE_REDUCTION,
                            description = "Box/Linien-Reduktion: $num in Zeile ${row + 1} ist auf Box ${boxRow * 3 + boxCol + 1} beschränkt. $num kann aus anderen Zellen der Box entfernt werden.",
                            affectedCells = positions.map { Pair(row, it) },
                            highlightCells = buildList {
                                for (r in boxRow * 3 until boxRow * 3 + 3) {
                                    for (c in boxCol * 3 until boxCol * 3 + 3) {
                                        add(Pair(r, c))
                                    }
                                }
                            },
                            value = null,
                            eliminatedCandidates = eliminations
                        )
                    }
                }
            }
        }
        return null
    }

    private fun findXWing(): SolveStep? {
        // Check rows for X-Wing
        for (num in 1..9) {
            val rowsWithTwoPositions = mutableListOf<Pair<Int, List<Int>>>()

            for (row in 0 until 9) {
                val positions = (0 until 9).filter { board[row][it] == 0 && num in candidates[row][it] }
                if (positions.size == 2) {
                    rowsWithTwoPositions.add(Pair(row, positions))
                }
            }

            for (i in 0 until rowsWithTwoPositions.size - 1) {
                for (j in i + 1 until rowsWithTwoPositions.size) {
                    if (rowsWithTwoPositions[i].second == rowsWithTwoPositions[j].second) {
                        val row1 = rowsWithTwoPositions[i].first
                        val row2 = rowsWithTwoPositions[j].first
                        val col1 = rowsWithTwoPositions[i].second[0]
                        val col2 = rowsWithTwoPositions[i].second[1]

                        val eliminations = mutableMapOf<Pair<Int, Int>, List<Int>>()
                        for (row in 0 until 9) {
                            if (row != row1 && row != row2) {
                                if (board[row][col1] == 0 && num in candidates[row][col1]) {
                                    eliminations[Pair(row, col1)] = listOf(num)
                                }
                                if (board[row][col2] == 0 && num in candidates[row][col2]) {
                                    eliminations[Pair(row, col2)] = listOf(num)
                                }
                            }
                        }

                        if (eliminations.isNotEmpty()) {
                            return SolveStep(
                                technique = SolvingTechnique.X_WING,
                                description = "X-Wing: $num bildet ein Rechteck in den Zeilen ${row1 + 1} und ${row2 + 1}, Spalten ${col1 + 1} und ${col2 + 1}. $num kann aus den restlichen Zellen dieser Spalten entfernt werden.",
                                affectedCells = listOf(Pair(row1, col1), Pair(row1, col2), Pair(row2, col1), Pair(row2, col2)),
                                highlightCells = listOf(Pair(row1, col1), Pair(row1, col2), Pair(row2, col1), Pair(row2, col2)),
                                value = null,
                                eliminatedCandidates = eliminations
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findBruteForceStep(): SolveStep? {
        // Find cell with minimum candidates
        var minCandidates = 10
        var bestCell: Pair<Int, Int>? = null

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0 && candidates[row][col].size < minCandidates) {
                    minCandidates = candidates[row][col].size
                    bestCell = Pair(row, col)
                }
            }
        }

        if (bestCell != null && minCandidates > 0) {
            val (row, col) = bestCell
            val value = candidates[row][col].first()
            return SolveStep(
                technique = SolvingTechnique.BRUTE_FORCE,
                description = "Keine logische Technik gefunden. Versuche $value in Zelle [${row + 1},${col + 1}].",
                affectedCells = listOf(bestCell),
                highlightCells = listOf(bestCell),
                value = value
            )
        }

        return null
    }

    // ============ HELPER METHODS ============

    private fun calculateAllCandidates() {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) {
                    candidates[row][col] = calculateCandidatesForCell(row, col)
                } else {
                    candidates[row][col] = mutableSetOf()
                }
            }
        }
    }

    private fun calculateCandidatesForCell(row: Int, col: Int): MutableSet<Int> {
        val result = (1..9).toMutableSet()

        // Remove numbers in same row
        for (c in 0 until 9) {
            result.remove(board[row][c])
        }

        // Remove numbers in same column
        for (r in 0 until 9) {
            result.remove(board[r][col])
        }

        // Remove numbers in same box
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                result.remove(board[r][c])
            }
        }

        return result
    }

    private fun getRelatedCells(row: Int, col: Int): List<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()

        // Row
        for (c in 0 until 9) result.add(Pair(row, c))

        // Column
        for (r in 0 until 9) result.add(Pair(r, col))

        // Box
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                result.add(Pair(r, c))
            }
        }

        return result.toList()
    }
}
