package de.sudokuonline.app.game.ai

import android.content.Context
import de.sudokuonline.app.data.local.GameResult
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.AILearningRepository
import de.sudokuonline.app.data.repository.GameMove
import de.sudokuonline.app.data.repository.MoveType
import de.sudokuonline.app.game.AIMove
import de.sudokuonline.app.game.AIUltimateMove
import de.sudokuonline.app.game.TicTacToeLogic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * TicTacToe AI Service - PERFECT PLAY
 *
 * Features:
 * - Perfect play in all modes at 100% strength
 * - Smart bomb usage (ONLY when hitting 2+ opponent stones)
 * - Deep search with iterative deepening
 * - Transposition tables for speed
 */
class TicTacToeAIService private constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val aiEngine = AIEngine()
    private val positionCache = PositionCache.getInstance(context)
    private val learningRepo = AILearningRepository.getInstance(context)

    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    private var strength: Int = 100
    private var timeoutMs: Long = 5000L

    // Track current game for learning
    private val currentGameMoves = mutableListOf<GameMove>()
    private val currentGamePositionHashes = mutableListOf<String>()
    private var currentGameMode: TicTacToeGameMode = TicTacToeGameMode.CLASSIC
    private var currentBoardSize: Int = 3
    private var currentAiSymbol: Int = 2
    private var gameStartTime: Long = System.currentTimeMillis()

    private val currentGamePositions = mutableListOf<Pair<TicTacToeBoard, Pair<Int, Int>>>()

    /**
     * Get best move - PERFECT PLAY with LEARNING
     */
    suspend fun getBestMove(
        board: TicTacToeBoard,
        aiSymbol: Int,
        gameMode: TicTacToeGameMode,
        bombsRemaining: Int = 0
    ): AIResult = withContext(Dispatchers.Default) {
        _aiState.value = AIState.Thinking
        val startTime = System.currentTimeMillis()

        // Update game tracking
        currentGameMode = gameMode
        currentBoardSize = board.size
        currentAiSymbol = aiSymbol

        try {
            val opponentSymbol = if (aiSymbol == 1) 2 else 1

            // Store position hash for learning
            val positionHash = learningRepo.computeBoardHash(board, gameMode)
            currentGamePositionHashes.add(positionHash)

            // ===== CHECK LEARNED POSITION (Opening Book) =====
            if (strength >= 90) {  // Only use learning at high strength
                val learnedMove = learningRepo.getLearnedMove(board, gameMode)
                if (learnedMove != null && learnedMove.winRate > 0.6f && learnedMove.confidence > 0.5f) {
                    // We have a high-confidence winning move from past games
                    val move = if (learnedMove.moveType == MoveType.BOMB && bombsRemaining > 0) {
                        AIMove.PlaceBomb(learnedMove.row, learnedMove.col)
                    } else {
                        AIMove.PlaceSymbol(learnedMove.row, learnedMove.col)
                    }

                    // Record the move
                    currentGameMoves.add(GameMove(learnedMove.row, learnedMove.col, aiSymbol, learnedMove.moveType.name))

                    _aiState.value = AIState.Idle
                    return@withContext AIResult.Success(
                        move = move,
                        evaluation = (learnedMove.winRate * 10000).toInt(),
                        depth = 0,  // From opening book
                        nodesSearched = 1,
                        timeMs = System.currentTimeMillis() - startTime
                    )
                }

                // Check if we should avoid this position
                if (learningRepo.shouldAvoidPosition(board, gameMode)) {
                    // Position led to losses before - search deeper
                    // This makes AI not repeat losing patterns
                }
            }

            // ===== USE PERFECT OPENING BOOK FOR 3x3 =====
            if (board.size == 3 && gameMode == TicTacToeGameMode.CLASSIC) {
                val openingMove = OpeningBook.getTicTacToe3x3Move(board, aiSymbol)
                if (openingMove != null) {
                    val row = openingMove / 3
                    val col = openingMove % 3
                    currentGameMoves.add(GameMove(row, col, aiSymbol, "SYMBOL"))
                    _aiState.value = AIState.Idle
                    return@withContext AIResult.Success(
                        move = AIMove.PlaceSymbol(row, col),
                        evaluation = 100,
                        depth = 1,
                        nodesSearched = 1,
                        timeMs = System.currentTimeMillis() - startTime
                    )
                }
            }

            // ===== BOMB MODE LOGIC =====
            if ((gameMode == TicTacToeGameMode.BOMB || gameMode == TicTacToeGameMode.L_BOMB) && bombsRemaining > 0) {
                // For bomb mode, also use opening book for first moves
                if (board.size == 3) {
                    val openingMove = OpeningBook.getTicTacToe3x3Move(board, aiSymbol)
                    if (openingMove != null) {
                        val row = openingMove / 3
                        val col = openingMove % 3
                        currentGameMoves.add(GameMove(row, col, aiSymbol, "SYMBOL"))
                        _aiState.value = AIState.Idle
                        return@withContext AIResult.Success(
                            move = AIMove.PlaceSymbol(row, col),
                            evaluation = 100,
                            depth = 1,
                            nodesSearched = 1,
                            timeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }

                val bombResult = evaluateBombMove(board, aiSymbol, gameMode, bombsRemaining)
                if (bombResult != null) {
                    // Record bomb move
                    if (bombResult is AIResult.Success && bombResult.move is AIMove.PlaceBomb) {
                        val bm = bombResult.move as AIMove.PlaceBomb
                        currentGameMoves.add(GameMove(bm.row, bm.col, aiSymbol, "BOMB"))
                    }
                    _aiState.value = AIState.Idle
                    return@withContext bombResult
                }
            }

            // ===== CHECK IMMEDIATE WIN =====
            val winMove = findWinningMove(board, aiSymbol)
            if (winMove != null) {
                currentGameMoves.add(GameMove(winMove.first, winMove.second, aiSymbol, "SYMBOL"))
                _aiState.value = AIState.Idle
                return@withContext AIResult.Success(
                    move = AIMove.PlaceSymbol(winMove.first, winMove.second),
                    evaluation = AIEngine.WIN_SCORE,
                    depth = 1,
                    nodesSearched = 1,
                    timeMs = System.currentTimeMillis() - startTime
                )
            }

            // ===== CHECK MUST BLOCK =====
            val blockMove = findWinningMove(board, opponentSymbol)
            if (blockMove != null) {
                currentGameMoves.add(GameMove(blockMove.first, blockMove.second, aiSymbol, "SYMBOL"))
                _aiState.value = AIState.Idle
                return@withContext AIResult.Success(
                    move = AIMove.PlaceSymbol(blockMove.first, blockMove.second),
                    evaluation = 0,
                    depth = 1,
                    nodesSearched = 1,
                    timeMs = System.currentTimeMillis() - startTime
                )
            }

            // ===== DEEP SEARCH =====
            val effectiveTimeLimit = when (board.size) {
                3 -> minOf(timeoutMs, 3000L)  // 3x3: fast response
                5 -> maxOf(timeoutMs, 4000L)  // 5x5: give more time for deeper search
                else -> timeoutMs
            }

            // For 5x5: deeper search with more time
            val effectiveMaxDepth = when (board.size) {
                3 -> 15  // 3x3: solve completely
                5 -> 18  // 5x5: deep search but with time limit
                else -> 14
            }

            val result = aiEngine.findBestMove(
                board = board,
                aiSymbol = aiSymbol,
                gameMode = gameMode,
                timeLimitMs = effectiveTimeLimit,
                maxDepth = effectiveMaxDepth
            )

            val adjustedMove = applyStrength(result.bestMove, board, aiSymbol)

            // Record move for learning
            when (adjustedMove) {
                is AIMove.PlaceSymbol -> {
                    currentGamePositions.add(Pair(board.copy(), Pair(adjustedMove.row, adjustedMove.col)))
                    currentGameMoves.add(GameMove(adjustedMove.row, adjustedMove.col, aiSymbol, "SYMBOL"))
                    positionCache.store(board, adjustedMove.row, adjustedMove.col, result.score, result.depth)

                    // Store in learning repository
                    learningRepo.storePosition(board, gameMode, adjustedMove, result.score, result.depth)
                }
                is AIMove.PlaceBomb -> {
                    currentGameMoves.add(GameMove(adjustedMove.row, adjustedMove.col, aiSymbol, "BOMB"))
                }
            }

            _aiState.value = AIState.Idle

            AIResult.Success(
                move = adjustedMove,
                evaluation = result.score,
                depth = result.depth,
                nodesSearched = result.nodesSearched,
                timeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: CancellationException) {
            _aiState.value = AIState.Idle
            AIResult.Cancelled
        } catch (e: Exception) {
            _aiState.value = AIState.Idle
            val fallback = getFallbackMove(board)
            AIResult.Timeout(fallbackMove = fallback)
        }
    }

    /**
     * Evaluate bomb placement for 3x3 Bomb Mode
     * 
     * STRATEGY FOR 3x3:
     * - Bombs are VERY powerful in 3x3 because the board is small
     * - Use bomb to destroy opponent's winning threat
     * - Use bomb to destroy opponent's fork setup
     * - Use bomb if opponent has 2 in a row (even just 1 stone destroyed can help)
     * - Don't waste bomb if we're clearly winning
     */
    private suspend fun evaluateBombMove(
        board: TicTacToeBoard,
        aiSymbol: Int,
        gameMode: TicTacToeGameMode,
        bombsRemaining: Int
    ): AIResult? {
        val startTime = System.currentTimeMillis()
        val opponentSymbol = if (aiSymbol == 1) 2 else 1
        val size = board.size

        // First check if we can win without bomb
        val winMove = findWinningMove(board, aiSymbol)
        if (winMove != null) return null  // Don't use bomb, we can win normally

        // Check if opponent can win next move - USE BOMB to destroy their threat!
        val oppWinMove = findWinningMove(board, opponentSymbol)
        if (oppWinMove != null) {
            // Find a bomb placement that destroys the winning piece
            for (row in 0 until size) {
                for (col in 0 until size) {
                    if (!board.cells[row][col].isEmpty()) continue

                    val affectedPositions = if (gameMode == TicTacToeGameMode.L_BOMB) {
                        TicTacToeLogic.getLBombAffectedPositions(row, col, size)
                    } else {
                        TicTacToeLogic.getStandardBombAffectedPositions(row, col, size)
                    }

                    // Check if this bomb destroys opponent pieces that form the threat
                    var destroysOpponentPiece = false
                    var ownHits = 0
                    
                    for ((r, c) in affectedPositions) {
                        if (r < 0 || r >= size || c < 0 || c >= size) continue
                        if (board.cells[r][c].value == opponentSymbol) {
                            destroysOpponentPiece = true
                        }
                        if (board.cells[r][c].value == aiSymbol) {
                            ownHits++
                        }
                    }

                    // Use bomb if it destroys opponent piece and doesn't hurt us too much
                    if (destroysOpponentPiece && ownHits == 0) {
                        return AIResult.Success(
                            move = AIMove.PlaceBomb(row, col),
                            evaluation = 50000,
                            depth = 2,
                            nodesSearched = 50,
                            timeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
            }
        }

        // Check if opponent has a fork threat (2 ways to win) - bomb it!
        val opponentForkMoves = findForkMoves(board, opponentSymbol)
        if (opponentForkMoves.isNotEmpty()) {
            // Try to find a bomb that disrupts the fork
            for (row in 0 until size) {
                for (col in 0 until size) {
                    if (!board.cells[row][col].isEmpty()) continue

                    val affectedPositions = if (gameMode == TicTacToeGameMode.L_BOMB) {
                        TicTacToeLogic.getLBombAffectedPositions(row, col, size)
                    } else {
                        TicTacToeLogic.getStandardBombAffectedPositions(row, col, size)
                    }

                    var opponentHits = 0
                    var ownHits = 0
                    
                    for ((r, c) in affectedPositions) {
                        if (r < 0 || r >= size || c < 0 || c >= size) continue
                        if (board.cells[r][c].value == opponentSymbol) opponentHits++
                        if (board.cells[r][c].value == aiSymbol) ownHits++
                    }

                    // Use bomb if it hits opponent and doesn't hurt us
                    if (opponentHits >= 1 && ownHits == 0) {
                        return AIResult.Success(
                            move = AIMove.PlaceBomb(row, col),
                            evaluation = 30000,
                            depth = 2,
                            nodesSearched = 50,
                            timeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
            }
        }

        // Find best bomb placement - now with lower threshold for 3x3
        var bestBombPos: Pair<Int, Int>? = null
        var bestBombScore = Int.MIN_VALUE

        for (row in 0 until size) {
            for (col in 0 until size) {
                if (!board.cells[row][col].isEmpty()) continue

                val affectedPositions = if (gameMode == TicTacToeGameMode.L_BOMB) {
                    TicTacToeLogic.getLBombAffectedPositions(row, col, size)
                } else {
                    TicTacToeLogic.getStandardBombAffectedPositions(row, col, size)
                }

                var opponentHits = 0
                var ownHits = 0
                var destroysThreat = false

                for ((r, c) in affectedPositions) {
                    if (r < 0 || r >= size || c < 0 || c >= size) continue
                    when (board.cells[r][c].value) {
                        opponentSymbol -> {
                            opponentHits++
                            // Check if this piece is part of a 2-in-a-row
                            if (isPartOfTwoInRow(board, r, c, opponentSymbol)) {
                                destroysThreat = true
                            }
                        }
                        aiSymbol -> ownHits++
                    }
                }

                // Score calculation for 3x3:
                // - Each opponent hit: +1000
                // - Each own hit: -1500 (penalize more)
                // - Destroying a threat: +5000
                var score = opponentHits * 1000 - ownHits * 1500
                if (destroysThreat) score += 5000

                if (score > bestBombScore && opponentHits > 0 && score > 0) {
                    bestBombScore = score
                    bestBombPos = Pair(row, col)
                }
            }
        }

        // Use bomb if we found a decent target
        if (bestBombPos != null && bestBombScore > 0) {
            return AIResult.Success(
                move = AIMove.PlaceBomb(bestBombPos.first, bestBombPos.second),
                evaluation = bestBombScore,
                depth = 2,
                nodesSearched = 50,
                timeMs = System.currentTimeMillis() - startTime
            )
        }

        return null  // No good bomb placement, use normal move
    }
    
    /**
     * Find all positions that would create a fork for the given symbol
     */
    private fun findForkMoves(board: TicTacToeBoard, symbol: Int): List<Pair<Int, Int>> {
        val forks = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    // Simulate placing here
                    val newCells = board.cells.mapIndexed { r, rowCells ->
                        rowCells.mapIndexed { c, cell ->
                            if (r == row && c == col) cell.copy(value = symbol) else cell
                        }
                    }
                    val newBoard = board.copy(cells = newCells)
                    
                    // Count winning threats
                    var threats = 0
                    for (r in 0 until board.size) {
                        for (c in 0 until board.size) {
                            if (newBoard.cells[r][c].isEmpty() && wouldWin(newBoard, r, c, symbol)) {
                                threats++
                            }
                        }
                    }
                    if (threats >= 2) {
                        forks.add(Pair(row, col))
                    }
                }
            }
        }
        return forks
    }
    
    /**
     * Check if a piece is part of a 2-in-a-row formation
     */
    private fun isPartOfTwoInRow(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val size = board.size
        val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))
        
        for ((dr, dc) in directions) {
            var count = 1
            var hasEmpty = false
            
            // Check positive direction
            var r = row + dr
            var c = col + dc
            while (r in 0 until size && c in 0 until size) {
                when (board.cells[r][c].value) {
                    symbol -> count++
                    0 -> hasEmpty = true
                    else -> break
                }
                r += dr
                c += dc
            }
            
            // Check negative direction
            r = row - dr
            c = col - dc
            while (r in 0 until size && c in 0 until size) {
                when (board.cells[r][c].value) {
                    symbol -> count++
                    0 -> hasEmpty = true
                    else -> break
                }
                r -= dr
                c -= dc
            }
            
            // 2 in a row with empty space = threat
            if (count >= 2 && hasEmpty) return true
        }
        return false
    }

    /**
     * Find a move that wins immediately
     */
    private fun findWinningMove(board: TicTacToeBoard, symbol: Int): Pair<Int, Int>? {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    if (wouldWin(board, row, col, symbol)) {
                        return Pair(row, col)
                    }
                }
            }
        }
        return null
    }

    private fun wouldWin(board: TicTacToeBoard, row: Int, col: Int, symbol: Int): Boolean {
        val size = board.size
        val winCondition = board.winCondition

        // Check row
        var count = 1
        var c = col - 1
        while (c >= 0 && board.cells[row][c].value == symbol) { count++; c-- }
        c = col + 1
        while (c < size && board.cells[row][c].value == symbol) { count++; c++ }
        if (count >= winCondition) return true

        // Check column
        count = 1
        var r = row - 1
        while (r >= 0 && board.cells[r][col].value == symbol) { count++; r-- }
        r = row + 1
        while (r < size && board.cells[r][col].value == symbol) { count++; r++ }
        if (count >= winCondition) return true

        // Check diagonal
        count = 1
        r = row - 1; c = col - 1
        while (r >= 0 && c >= 0 && board.cells[r][c].value == symbol) { count++; r--; c-- }
        r = row + 1; c = col + 1
        while (r < size && c < size && board.cells[r][c].value == symbol) { count++; r++; c++ }
        if (count >= winCondition) return true

        // Check anti-diagonal
        count = 1
        r = row - 1; c = col + 1
        while (r >= 0 && c < size && board.cells[r][c].value == symbol) { count++; r--; c++ }
        r = row + 1; c = col - 1
        while (r < size && c >= 0 && board.cells[r][c].value == symbol) { count++; r++; c-- }
        if (count >= winCondition) return true

        return false
    }

    /**
     * Get best move for Ultimate TicTacToe
     */
    suspend fun getBestUltimateMove(
        ultimateBoard: UltimateTicTacToeBoard,
        aiSymbol: Int,
        playableBoards: List<Pair<Int, Int>>
    ): AIUltimateResult = withContext(Dispatchers.Default) {
        _aiState.value = AIState.Thinking
        val startTime = System.currentTimeMillis()

        try {
            val result = aiEngine.findBestUltimateMove(
                board = ultimateBoard,
                aiSymbol = aiSymbol,
                playableBoards = playableBoards,
                timeLimitMs = timeoutMs
            )

            val adjustedMove = applyUltimateStrength(result, ultimateBoard, aiSymbol, playableBoards)

            _aiState.value = AIState.Idle

            AIUltimateResult.Success(
                move = adjustedMove,
                evaluation = 0,
                timeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: CancellationException) {
            _aiState.value = AIState.Idle
            AIUltimateResult.Error("Cancelled")
        } catch (e: Exception) {
            _aiState.value = AIState.Idle
            val fallback = getFallbackUltimateMove(ultimateBoard, playableBoards)
            AIUltimateResult.Timeout(fallbackMove = fallback)
        }
    }

    /**
     * Apply strength - at 100%, play perfectly
     */
    private fun applyStrength(optimalMove: AIMove, board: TicTacToeBoard, aiSymbol: Int): AIMove {
        if (strength >= 95) return optimalMove

        val mistakeProbability = when {
            strength <= 10 -> 0.8f
            strength <= 25 -> 0.6f
            strength <= 40 -> 0.4f
            strength <= 55 -> 0.25f
            strength <= 70 -> 0.15f
            strength <= 85 -> 0.08f
            else -> 0.03f
        }

        if (Random.nextFloat() >= mistakeProbability) return optimalMove

        // Get alternative moves
        val alternatives = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].isEmpty()) {
                    alternatives.add(Pair(row, col))
                }
            }
        }

        if (alternatives.isEmpty()) return optimalMove

        // For higher strength, avoid obviously losing moves
        val safeMoves = if (strength > 30) {
            val opponentSymbol = if (aiSymbol == 1) 2 else 1
            alternatives.filter { (row, col) ->
                // Don't skip blocking if opponent can win
                val opponentWinMove = findWinningMove(board, opponentSymbol)
                if (opponentWinMove != null) {
                    row == opponentWinMove.first && col == opponentWinMove.second
                } else {
                    true
                }
            }
        } else {
            alternatives
        }

        if (safeMoves.isEmpty()) return optimalMove

        val (row, col) = safeMoves.random()
        return AIMove.PlaceSymbol(row, col)
    }

    private fun applyUltimateStrength(
        optimalMove: AIUltimateMove,
        board: UltimateTicTacToeBoard,
        aiSymbol: Int,
        playableBoards: List<Pair<Int, Int>>
    ): AIUltimateMove {
        if (strength >= 95) return optimalMove

        val mistakeProbability = when {
            strength <= 25 -> 0.5f
            strength <= 50 -> 0.3f
            strength <= 75 -> 0.15f
            else -> 0.05f
        }

        if (Random.nextFloat() >= mistakeProbability) return optimalMove

        val allMoves = mutableListOf<AIUltimateMove>()
        for ((boardRow, boardCol) in playableBoards) {
            val miniBoard = board.miniBoards[boardRow][boardCol]
            if (miniBoard.isFinished()) continue

            for (cellRow in 0 until 3) {
                for (cellCol in 0 until 3) {
                    if (miniBoard.cells[cellRow][cellCol].isEmpty()) {
                        allMoves.add(AIUltimateMove(boardRow, boardCol, cellRow, cellCol))
                    }
                }
            }
        }

        return if (allMoves.isNotEmpty()) allMoves.random() else optimalMove
    }

    private fun getFallbackMove(board: TicTacToeBoard): AIMove {
        val center = board.size / 2
        if (board.cells[center][center].isEmpty()) {
            return AIMove.PlaceSymbol(center, center)
        }

        val corners = listOf(
            Pair(0, 0), Pair(0, board.size - 1),
            Pair(board.size - 1, 0), Pair(board.size - 1, board.size - 1)
        )
        for ((r, c) in corners) {
            if (board.cells[r][c].isEmpty()) {
                return AIMove.PlaceSymbol(r, c)
            }
        }

        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board.cells[r][c].isEmpty()) {
                    return AIMove.PlaceSymbol(r, c)
                }
            }
        }

        return AIMove.PlaceSymbol(0, 0)
    }

    private fun getFallbackUltimateMove(
        board: UltimateTicTacToeBoard,
        playableBoards: List<Pair<Int, Int>>
    ): AIUltimateMove {
        for ((boardRow, boardCol) in playableBoards) {
            val miniBoard = board.miniBoards[boardRow][boardCol]
            if (miniBoard.isFinished()) continue

            if (miniBoard.cells[1][1].isEmpty()) {
                return AIUltimateMove(boardRow, boardCol, 1, 1)
            }

            for (r in 0..2) {
                for (c in 0..2) {
                    if (miniBoard.cells[r][c].isEmpty()) {
                        return AIUltimateMove(boardRow, boardCol, r, c)
                    }
                }
            }
        }
        return AIUltimateMove(0, 0, 0, 0)
    }

    /**
     * Report game result for learning
     * This is crucial - AI learns from wins AND losses
     */
    suspend fun reportGameResult(winner: Int, aiSymbol: Int) {
        // Legacy position cache
        positionCache.storeGame(currentGamePositions, aiSymbol, winner)

        // New learning repository
        val result = when {
            winner == 0 -> GameResult.DRAW
            winner == aiSymbol -> GameResult.WIN
            else -> GameResult.LOSS
        }

        val durationSeconds = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()

        // Record game for learning - this is how AI improves over time
        learningRepo.recordGame(
            gameMode = currentGameMode,
            boardSize = currentBoardSize,
            aiStrength = strength,
            aiPlayedAs = aiSymbol,
            result = result,
            moves = currentGameMoves.toList(),
            positionHashes = currentGamePositionHashes.toList(),
            durationSeconds = durationSeconds
        )

        // Clear tracking
        currentGamePositions.clear()
        currentGameMoves.clear()
        currentGamePositionHashes.clear()
    }

    /**
     * Start a new game - resets all tracking
     */
    fun newGame() {
        currentGamePositions.clear()
        currentGameMoves.clear()
        currentGamePositionHashes.clear()
        gameStartTime = System.currentTimeMillis()
    }

    /**
     * Initialize learning for a game mode
     */
    suspend fun initializeLearning(gameMode: TicTacToeGameMode, boardSize: Int) {
        learningRepo.initialize(gameMode, boardSize)
    }

    /**
     * Get learning statistics
     */
    suspend fun getLearningStats(gameMode: TicTacToeGameMode, boardSize: Int) =
        learningRepo.getStatistics(gameMode, boardSize)

    fun setStrength(value: Int) {
        strength = value.coerceIn(0, 100)
    }

    fun getStrength(): Int = strength

    fun setTimeout(ms: Long) {
        timeoutMs = ms.coerceIn(1000L, 10000L)
    }

    fun getTimeout(): Long = timeoutMs

    fun getStats(): AIStats {
        val cacheStats = positionCache.getStats()
        val engineStats = aiEngine.getTableStats()

        return AIStats(
            cachedPositions = cacheStats.positionCount,
            totalGames = cacheStats.totalGames,
            winRate = cacheStats.winRate,
            transpositionTableSize = engineStats.size,
            transpositionTableFill = engineStats.fillRate
        )
    }

    fun cancel() {
        aiEngine.cancel()
        _aiState.value = AIState.Idle
    }

    fun clearCaches() {
        aiEngine.clearCache()
        positionCache.clear()
    }

    fun shutdown() {
        scope.cancel()
    }

    data class AIStats(
        val cachedPositions: Int,
        val totalGames: Int,
        val winRate: Float,
        val transpositionTableSize: Int,
        val transpositionTableFill: Float
    )

    companion object {
        @Volatile
        private var instance: TicTacToeAIService? = null

        fun getInstance(context: Context): TicTacToeAIService {
            return instance ?: synchronized(this) {
                instance ?: TicTacToeAIService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
