package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.model.SudokuBoard
import de.sudokuonline.app.data.model.SudokuCell
import kotlin.random.Random

/**
 * Sudoku puzzle generator with various difficulty levels
 */
class SudokuGenerator {

    private var random: Random = Random

    /**
     * Generate a new Sudoku puzzle with the specified difficulty
     */
    fun generate(difficulty: Difficulty): SudokuBoard {
        random = Random
        val solution = generateSolution()
        val puzzle = createPuzzle(solution, difficulty.cellsToRemove)

        val cells = puzzle.mapIndexed { row, rowValues ->
            rowValues.mapIndexed { col, value ->
                SudokuCell(
                    value = value,
                    isFixed = value != 0,
                    notes = emptyList(),
                    isError = false,
                    enteredBy = null
                )
            }
        }

        return SudokuBoard(cells = cells, solution = solution)
    }

    /**
     * Generate a puzzle with a specific seed for deterministic generation.
     * Used for Daily Challenge - same seed produces same puzzle.
     */
    fun generateWithSeed(difficulty: Difficulty, seed: Long): Pair<List<List<Int>>, List<List<Int>>> {
        random = Random(seed)
        val solution = generateSolution()
        val puzzle = createPuzzle(solution, difficulty.cellsToRemove)
        return Pair(puzzle, solution)
    }

    /**
     * Generate a complete valid Sudoku solution using backtracking
     */
    private fun generateSolution(): List<List<Int>> {
        val grid = Array(9) { IntArray(9) { 0 } }
        fillGrid(grid)
        return grid.map { it.toList() }
    }

    /**
     * Fill the grid with a valid Sudoku solution
     */
    private fun fillGrid(grid: Array<IntArray>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (grid[row][col] == 0) {
                    val numbers = (1..9).shuffled(random)
                    for (num in numbers) {
                        if (isValidPlacement(grid, row, col, num)) {
                            grid[row][col] = num
                            if (fillGrid(grid)) {
                                return true
                            }
                            grid[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }
    
    /**
     * Check if placing a number at the given position is valid
     */
    private fun isValidPlacement(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        if (num in grid[row]) return false
        
        // Check column
        for (r in 0 until 9) {
            if (grid[r][col] == num) return false
        }
        
        // Check 3x3 box
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (grid[r][c] == num) return false
            }
        }
        
        return true
    }
    
    /**
     * Create a puzzle by removing cells from the solution
     */
    private fun createPuzzle(solution: List<List<Int>>, cellsToRemove: Int): List<List<Int>> {
        val puzzle = solution.map { it.toMutableList() }.toMutableList()
        val positions = mutableListOf<Pair<Int, Int>>()

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                positions.add(Pair(row, col))
            }
        }
        positions.shuffle(random)
        
        var removed = 0
        for ((row, col) in positions) {
            if (removed >= cellsToRemove) break
            
            val backup = puzzle[row][col]
            puzzle[row][col] = 0
            
            // Check if puzzle still has unique solution
            val copy = puzzle.map { it.toIntArray() }.toTypedArray()
            if (countSolutions(copy) == 1) {
                removed++
            } else {
                puzzle[row][col] = backup
            }
        }
        
        return puzzle.map { it.toList() }
    }
    
    /**
     * Count the number of solutions for a puzzle (up to 2)
     */
    private fun countSolutions(grid: Array<IntArray>, limit: Int = 2): Int {
        var count = 0
        
        fun solve(): Boolean {
            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    if (grid[row][col] == 0) {
                        for (num in 1..9) {
                            if (isValidPlacement(grid, row, col, num)) {
                                grid[row][col] = num
                                if (solve()) {
                                    if (count >= limit) return true
                                }
                                grid[row][col] = 0
                            }
                        }
                        return false
                    }
                }
            }
            count++
            return count >= limit
        }
        
        solve()
        return count
    }
}

/**
 * Simple Sudoku solver for validation and hints (internal use)
 */
private object SimpleSudokuSolver {
    
    /**
     * Solve a Sudoku puzzle
     */
    fun solve(puzzle: List<List<Int>>): List<List<Int>>? {
        val grid = puzzle.map { it.toMutableList() }.toMutableList()
        return if (solveGrid(grid)) grid.map { it.toList() } else null
    }
    
    private fun solveGrid(grid: MutableList<MutableList<Int>>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (grid[row][col] == 0) {
                    for (num in 1..9) {
                        if (isValid(grid, row, col, num)) {
                            grid[row][col] = num
                            if (solveGrid(grid)) {
                                return true
                            }
                            grid[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }
    
    private fun isValid(grid: List<List<Int>>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        if (num in grid[row]) return false
        
        // Check column
        for (r in 0 until 9) {
            if (grid[r][col] == num) return false
        }
        
        // Check 3x3 box
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (grid[r][c] == num) return false
            }
        }
        
        return true
    }
    
    /**
     * Get a hint for the next cell to fill
     */
    fun getHint(board: SudokuBoard): Triple<Int, Int, Int>? {
        val cells = board.cells
        val solution = board.solution
        
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (cells[row][col].value == 0) {
                    return Triple(row, col, solution[row][col])
                }
            }
        }
        return null
    }
}

/**
 * Sudoku validator for checking moves and board state
 */
object SudokuValidator {
    
    /**
     * Check if a value is valid at the given position
     */
    fun isValidMove(board: SudokuBoard, row: Int, col: Int, value: Int): Boolean {
        if (value == 0) return true
        return board.solution[row][col] == value
    }
    
    /**
     * Check if a move creates any conflicts (for real-time feedback)
     */
    fun hasConflict(cells: List<List<SudokuCell>>, row: Int, col: Int, value: Int): Boolean {
        if (value == 0) return false
        
        // Check row
        for (c in 0 until 9) {
            if (c != col && cells[row][c].value == value) return true
        }
        
        // Check column
        for (r in 0 until 9) {
            if (r != row && cells[r][col].value == value) return true
        }
        
        // Check 3x3 box
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (r != row && c != col && cells[r][c].value == value) return true
            }
        }
        
        return false
    }
    
    /**
     * Check if the puzzle is complete and correct
     */
    fun isComplete(board: SudokuBoard): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board.cells[row][col].value != board.solution[row][col]) {
                    return false
                }
            }
        }
        return true
    }
    
    /**
     * Calculate progress (number of correctly filled cells)
     */
    fun calculateProgress(board: SudokuBoard): Int {
        var correct = 0
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cell = board.cells[row][col]
                if (cell.value != 0 && cell.value == board.solution[row][col]) {
                    correct++
                }
            }
        }
        return correct
    }
    
    /**
     * Count empty cells
     */
    fun countEmptyCells(board: SudokuBoard): Int {
        var empty = 0
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board.cells[row][col].value == 0) {
                    empty++
                }
            }
        }
        return empty
    }
    
    /**
     * Find all cells with conflicts
     */
    fun findConflicts(cells: List<List<SudokuCell>>): Set<Pair<Int, Int>> {
        val conflicts = mutableSetOf<Pair<Int, Int>>()
        
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val value = cells[row][col].value
                if (value != 0 && hasConflict(cells, row, col, value)) {
                    conflicts.add(Pair(row, col))
                }
            }
        }
        
        return conflicts
    }
}
