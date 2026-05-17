package de.sudokuonline.app.game

import de.sudokuonline.app.data.model.Difficulty
import kotlin.random.Random

/**
 * Killer Sudoku Generator
 * Generates Killer Sudoku puzzles with cages (groups of cells) that must sum to a specific value
 */
class KillerSudokuGenerator {
    
    /**
     * A cage in Killer Sudoku - a group of cells that must sum to a target value
     */
    data class Cage(
        val cells: List<Pair<Int, Int>>, // List of (row, col) positions
        val targetSum: Int,
        val color: Int // Index for coloring (0-7)
    )
    
    /**
     * A complete Killer Sudoku puzzle
     */
    data class KillerSudokuPuzzle(
        val solution: Array<IntArray>,
        val cages: List<Cage>,
        val givenCells: Set<Pair<Int, Int>> = emptySet() // Some cells may be given in easier difficulties
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as KillerSudokuPuzzle
            if (!solution.contentDeepEquals(other.solution)) return false
            if (cages != other.cages) return false
            return givenCells == other.givenCells
        }

        override fun hashCode(): Int {
            var result = solution.contentDeepHashCode()
            result = 31 * result + cages.hashCode()
            result = 31 * result + givenCells.hashCode()
            return result
        }
    }
    
    private val random = Random(System.currentTimeMillis())
    
    /**
     * Generate a new Killer Sudoku puzzle
     */
    fun generate(difficulty: Difficulty): KillerSudokuPuzzle {
        // First generate a valid Sudoku solution
        val solution = generateSolution()
        
        // Then create cages based on difficulty
        val (minCageSize, maxCageSize, givenCount) = when (difficulty) {
            Difficulty.EASY -> Triple(2, 3, 15)
            Difficulty.MEDIUM -> Triple(2, 4, 8)
            Difficulty.HARD -> Triple(2, 5, 3)
            Difficulty.EXPERT -> Triple(2, 6, 0)
        }
        
        val cages = generateCages(solution, minCageSize, maxCageSize)
        
        // Select some cells to be given (pre-filled) for easier difficulties
        val givenCells = if (givenCount > 0) {
            selectGivenCells(solution, givenCount)
        } else {
            emptySet()
        }
        
        return KillerSudokuPuzzle(solution, cages, givenCells)
    }
    
    private fun generateSolution(): Array<IntArray> {
        val board = Array(9) { IntArray(9) { 0 } }
        fillBoard(board)
        return board
    }
    
    private fun fillBoard(board: Array<IntArray>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) {
                    val numbers = (1..9).shuffled()
                    for (num in numbers) {
                        if (isValidPlacement(board, row, col, num)) {
                            board[row][col] = num
                            if (fillBoard(board)) return true
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }
    
    private fun isValidPlacement(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        if (board[row].contains(num)) return false
        
        // Check column
        if (board.any { it[col] == num }) return false
        
        // Check 3x3 box
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (board[r][c] == num) return false
            }
        }
        
        return true
    }
    
    private fun generateCages(solution: Array<IntArray>, minSize: Int, maxSize: Int): List<Cage> {
        val cages = mutableListOf<Cage>()
        val assigned = Array(9) { BooleanArray(9) { false } }
        var colorIndex = 0
        
        // Process cells in random order
        val allCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                allCells.add(Pair(r, c))
            }
        }
        allCells.shuffle()
        
        for ((row, col) in allCells) {
            if (assigned[row][col]) continue
            
            // Start a new cage from this cell
            val cageCells = mutableListOf<Pair<Int, Int>>()
            cageCells.add(Pair(row, col))
            assigned[row][col] = true
            
            // Try to expand the cage
            val targetSize = random.nextInt(minSize, maxSize + 1)
            
            while (cageCells.size < targetSize) {
                val neighbors = getUnassignedNeighbors(cageCells, assigned)
                if (neighbors.isEmpty()) break
                
                val next = neighbors.random()
                cageCells.add(next)
                assigned[next.first][next.second] = true
            }
            
            // Calculate the sum for this cage
            val sum = cageCells.sumOf { (r, c) -> solution[r][c] }
            
            cages.add(Cage(cageCells, sum, colorIndex % 8))
            colorIndex++
        }
        
        return cages
    }
    
    private fun getUnassignedNeighbors(
        cageCells: List<Pair<Int, Int>>,
        assigned: Array<BooleanArray>
    ): List<Pair<Int, Int>> {
        val neighbors = mutableSetOf<Pair<Int, Int>>()
        val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        
        for ((row, col) in cageCells) {
            for ((dr, dc) in directions) {
                val nr = row + dr
                val nc = col + dc
                if (nr in 0..8 && nc in 0..8 && !assigned[nr][nc]) {
                    neighbors.add(Pair(nr, nc))
                }
            }
        }
        
        return neighbors.toList()
    }
    
    private fun selectGivenCells(solution: Array<IntArray>, count: Int): Set<Pair<Int, Int>> {
        val allCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                allCells.add(Pair(r, c))
            }
        }
        allCells.shuffle()
        return allCells.take(count).toSet()
    }
    
    /**
     * Validate if a number can be placed in a cell considering Killer Sudoku rules
     */
    fun isValidMove(
        board: Array<IntArray>,
        cages: List<Cage>,
        row: Int,
        col: Int,
        num: Int
    ): Boolean {
        // Standard Sudoku rules
        if (!isValidPlacement(board, row, col, num)) return false
        
        // Find the cage containing this cell
        val cage = cages.find { it.cells.contains(Pair(row, col)) } ?: return true
        
        // Check if number already exists in cage (no duplicates in cage)
        for ((r, c) in cage.cells) {
            if ((r != row || c != col) && board[r][c] == num) return false
        }
        
        // Check if placing this number would make it impossible to reach the target sum
        val currentSum = cage.cells.sumOf { (r, c) -> 
            if (r == row && c == col) num else board[r][c] 
        }
        val emptyCells = cage.cells.count { (r, c) -> 
            (r != row || c != col) && board[r][c] == 0 
        }
        
        if (emptyCells == 0) {
            // All cells filled - must equal target
            return currentSum == cage.targetSum
        } else {
            // Partial fill - sum must not exceed target
            // and must be possible to reach target with remaining cells
            val minPossible = currentSum + (1..emptyCells).sum()
            val maxPossible = currentSum + ((9 - emptyCells + 1)..9).sum()
            return cage.targetSum in minPossible..maxPossible
        }
    }
    
    /**
     * Check if a cage is complete and correct
     */
    fun isCageComplete(board: Array<IntArray>, cage: Cage): Boolean {
        val values = cage.cells.map { (r, c) -> board[r][c] }
        
        // All cells must be filled
        if (values.any { it == 0 }) return false
        
        // No duplicates
        if (values.toSet().size != values.size) return false
        
        // Sum must equal target
        return values.sum() == cage.targetSum
    }
    
    companion object {
        private var instance: KillerSudokuGenerator? = null
        
        fun getInstance(): KillerSudokuGenerator {
            return instance ?: KillerSudokuGenerator().also { instance = it }
        }
    }
}
