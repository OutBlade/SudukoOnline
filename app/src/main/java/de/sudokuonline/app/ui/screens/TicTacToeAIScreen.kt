package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.TicTacToeAI
import de.sudokuonline.app.game.AIMove
import de.sudokuonline.app.game.TicTacToeLogic
import de.sudokuonline.app.game.UltimateTicTacToeLogic
import de.sudokuonline.app.game.ai.AIResult
import de.sudokuonline.app.game.ai.AIUltimateResult
import de.sudokuonline.app.game.ai.TicTacToeAIService
import de.sudokuonline.app.game.ai.MoveExplanation
import de.sudokuonline.app.game.ai.AIPersonality
import de.sudokuonline.app.ui.components.*
import de.sudokuonline.app.ui.theme.*
import de.sudokuonline.app.util.rememberHapticFeedback
import de.sudokuonline.app.util.rememberSoundManager
import de.sudokuonline.app.data.repository.AchievementsRepository
import de.sudokuonline.app.data.repository.AchievementEvent
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.CoinReason
import de.sudokuonline.app.viewmodel.TicTacToeActionMode
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeAIScreen(
    onBackClick: () -> Unit
) {
    // Get AI Service, Haptic Feedback, Sound Manager, Achievements, and Currency
    val context = LocalContext.current
    val aiService = remember { TicTacToeAIService.getInstance(context) }
    val haptic = rememberHapticFeedback()
    val soundManager = rememberSoundManager()
    val achievementsRepository = remember { AchievementsRepository.getInstance(context) }
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }

    var gamePhase by remember { mutableStateOf(GamePhase.SETUP) }
    var selectedGameMode by remember { mutableStateOf(TicTacToeGameMode.CLASSIC) }
    var selectedBoardSize by remember { mutableStateOf(TicTacToeBoardSize.SMALL) }
    var playerSymbol by remember { mutableIntStateOf(1) }  // 1=X (first), 2=O (second)
    var aiStrength by remember { mutableIntStateOf(100) }  // 0-100% AI strength

    // Game state
    var board by remember { mutableStateOf(TicTacToeBoard.create(TicTacToeBoardSize.SMALL)) }
    var ultimateBoard by remember { mutableStateOf(UltimateTicTacToeBoard()) }
    var playableBoards by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var playerBombs by remember { mutableIntStateOf(3) }
    var aiBombs by remember { mutableIntStateOf(3) }
    var actionMode by remember { mutableStateOf(TicTacToeActionMode.SYMBOL) }
    var winningLine by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var winner by remember { mutableStateOf<Int?>(null) }
    var isDraw by remember { mutableStateOf(false) }
    var showGameOver by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isAIThinking by remember { mutableStateOf(false) }

    // Statistics (persisted via SharedPreferences)
    val prefs = remember { context.getSharedPreferences("game_stats", Context.MODE_PRIVATE) }
    var totalGames by remember { mutableIntStateOf(prefs.getInt("total_games", 0)) }
    var playerWins by remember { mutableIntStateOf(prefs.getInt("player_wins", 0)) }
    var aiWins by remember { mutableIntStateOf(prefs.getInt("ai_wins", 0)) }

    // Undo history
    var moveHistory by remember { mutableStateOf<List<TicTacToeBoard>>(emptyList()) }
    var canUndo by remember { mutableStateOf(false) }

    // AI Explanation (educational feature)
    var lastAIExplanation by remember { mutableStateOf<MoveExplanation.Explanation?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var aiPersonality by remember { mutableStateOf(AIPersonality.BALANCED) }

    // Timer
    LaunchedEffect(gamePhase) {
        if (gamePhase == GamePhase.PLAYING) {
            while (gamePhase == GamePhase.PLAYING && winner == null && !isDraw) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    // AI turn - using new AIService with timeout and background execution
    LaunchedEffect(isPlayerTurn, gamePhase, winner, isDraw) {
        if (gamePhase == GamePhase.PLAYING && !isPlayerTurn && winner == null && !isDraw) {
            isAIThinking = true
            delay(300)  // Small delay for natural feel

            val aiSymbol = if (playerSymbol == 1) 2 else 1

            if (selectedGameMode == TicTacToeGameMode.ULTIMATE) {
                // Ultimate mode AI
                if (playableBoards.isNotEmpty()) {
                    val result = aiService.getBestUltimateMove(ultimateBoard, aiSymbol, playableBoards)

                    val aiMove = when (result) {
                        is AIUltimateResult.Success -> result.move
                        is AIUltimateResult.Timeout -> result.fallbackMove
                        is AIUltimateResult.Error -> {
                            // Fallback: pick first valid move
                            val (br, bc) = playableBoards.first()
                            val miniBoard = ultimateBoard.miniBoards[br][bc]
                            var move = de.sudokuonline.app.game.AIUltimateMove(br, bc, 0, 0)
                            for (r in 0..2) {
                                for (c in 0..2) {
                                    if (miniBoard.cells[r][c].isEmpty()) {
                                        move = de.sudokuonline.app.game.AIUltimateMove(br, bc, r, c)
                                        break
                                    }
                                }
                            }
                            move
                        }
                    }

                    val newBoard = UltimateTicTacToeLogic.makeMove(
                        ultimateBoard, aiMove.boardRow, aiMove.boardCol,
                        aiMove.cellRow, aiMove.cellCol, aiSymbol, "AI"
                    )
                    ultimateBoard = newBoard
                    playableBoards = UltimateTicTacToeLogic.getPlayableBoards(newBoard)

                    // Check for winner
                    val ultimateWinner = UltimateTicTacToeLogic.checkWinner(newBoard)
                    if (ultimateWinner != null) {
                        winner = ultimateWinner
                        showGameOver = true
                    } else if (UltimateTicTacToeLogic.isDraw(newBoard)) {
                        isDraw = true
                        showGameOver = true
                    }
                }
            } else {
                // Regular mode AI - using AIService with timeout
                val result = aiService.getBestMove(board, aiSymbol, selectedGameMode, aiBombs)

                val aiMove = when (result) {
                    is AIResult.Success -> result.move
                    is AIResult.Timeout -> result.fallbackMove
                    is AIResult.Cancelled -> AIMove.PlaceSymbol(0, 0)  // Shouldn't happen
                    is AIResult.Error -> {
                        // Fallback: find any empty cell
                        var fallback: AIMove = AIMove.PlaceSymbol(0, 0)
                        for (r in 0 until board.size) {
                            for (c in 0 until board.size) {
                                if (board.cells[r][c].isEmpty()) {
                                    fallback = AIMove.PlaceSymbol(r, c)
                                    break
                                }
                            }
                        }
                        fallback
                    }
                }

                    when (aiMove) {
                        is AIMove.PlaceSymbol -> {
                            // Generate explanation BEFORE making the move (to analyze correctly)
                            lastAIExplanation = MoveExplanation.explainTicTacToeMove(
                                board, aiMove.row, aiMove.col, aiSymbol, selectedGameMode
                            )
                            showExplanation = true

                            val newCells = board.cells.mapIndexed { r, row ->
                                row.mapIndexed { c, cell ->
                                    if (r == aiMove.row && c == aiMove.col) {
                                        cell.copy(value = aiSymbol)
                                    } else cell
                                }
                            }
                            board = board.copy(cells = newCells)
                            soundManager.playPlace()
                        }
                        is AIMove.PlaceBomb -> {
                            // Generate bomb explanation
                            lastAIExplanation = MoveExplanation.explainBombMove(
                                board, aiMove.row, aiMove.col, aiSymbol, selectedGameMode
                            )
                            showExplanation = true

                            board = if (selectedGameMode == TicTacToeGameMode.L_BOMB) {
                                TicTacToeLogic.detonateLBomb(board, aiMove.row, aiMove.col)
                            } else {
                                TicTacToeLogic.detonateStandardBomb(board, aiMove.row, aiMove.col)
                            }
                            aiBombs--
                            soundManager.playBomb()
                        }
                    }

                // Check for winner
                val gameWinner = TicTacToeLogic.checkWinner(board)
                if (gameWinner != null) {
                    winner = gameWinner
                    winningLine = TicTacToeLogic.getWinningLine(board)
                    showGameOver = true
                } else if (TicTacToeLogic.isBoardFull(board)) {
                    isDraw = true
                    showGameOver = true
                }
            }

            isAIThinking = false
            isPlayerTurn = true
        }
    }

    fun startGame() {
        // Set AI strength before starting
        aiService.setStrength(aiStrength)

        // Initialize new game in AI service (for learning)
        aiService.newGame()

        // Clear undo history
        moveHistory = emptyList()
        canUndo = false

        board = TicTacToeBoard.create(selectedBoardSize)
        ultimateBoard = UltimateTicTacToeBoard()
        playableBoards = if (selectedGameMode == TicTacToeGameMode.ULTIMATE) {
            // All boards playable initially
            (0..2).flatMap { r -> (0..2).map { c -> Pair(r, c) } }
        } else emptyList()
        playerBombs = 3
        aiBombs = 3
        winner = null
        isDraw = false
        winningLine = null
        showGameOver = false
        elapsedSeconds = 0
        actionMode = TicTacToeActionMode.SYMBOL
        isPlayerTurn = playerSymbol == 1  // X always starts
        gamePhase = GamePhase.PLAYING
        
        // Play game start sound
        soundManager.playGameStart()
        
        // Track games played achievement
        achievementsRepository.checkAchievements(AchievementEvent.GamesPlayed)
    }

    // Report game result for AI learning when game ends & update statistics
    val coroutineScope = rememberCoroutineScope()
    var coinsAwarded by remember { mutableStateOf(false) }
    var lootboxAwarded by remember { mutableStateOf(false) }
    val lootboxRepository = remember { de.sudokuonline.app.data.repository.LootboxRepository.getInstance(context) }

    LaunchedEffect(showGameOver) {
        if (showGameOver && gamePhase == GamePhase.PLAYING) {
            val aiSymbol = if (playerSymbol == 1) 2 else 1
            val resultWinner = when {
                isDraw -> 0
                winner != null -> winner!!
                else -> 0
            }
            aiService.reportGameResult(resultWinner, aiSymbol)

            // Update statistics
            totalGames++
            if (resultWinner == playerSymbol) {
                playerWins++
                // Award coins for winning (only once)
                if (!coinsAwarded) {
                    currencyRepository.addCoins(
                        CoinReason.TICTACTOE_WIN.baseAmount,
                        CoinReason.TICTACTOE_WIN
                    )
                    coinsAwarded = true
                    if (!lootboxAwarded) {
                        lootboxRepository.addLootbox(de.sudokuonline.app.data.model.LootboxRarity.BRONZE, "tictactoe_win")
                        lootboxAwarded = true
                    }
                }
            } else if (resultWinner == aiSymbol) {
                aiWins++
            }

            // Save to SharedPreferences
            prefs.edit()
                .putInt("total_games", totalGames)
                .putInt("player_wins", playerWins)
                .putInt("ai_wins", aiWins)
                .apply()

            // Auto-difficulty adjustment
            // If player wins too often, increase AI strength
            // If player loses too often, decrease AI strength
            if (totalGames >= 3) {
                val winRate = playerWins.toFloat() / totalGames
                val newStrength = when {
                    winRate > 0.7f && aiStrength < 100 -> minOf(100, aiStrength + 10)
                    winRate < 0.3f && aiStrength > 20 -> maxOf(20, aiStrength - 10)
                    else -> aiStrength
                }
                if (newStrength != aiStrength) {
                    aiStrength = newStrength
                    aiService.setStrength(newStrength)
                }
            }
        }
    }

    // Undo function
    fun undoLastMove() {
        if (moveHistory.isNotEmpty() && isPlayerTurn && !isAIThinking && winner == null && !isDraw) {
            // Undo both player's move and AI's response
            val historySize = moveHistory.size
            if (historySize >= 2) {
                board = moveHistory[historySize - 2]
                moveHistory = moveHistory.dropLast(2)
            } else if (historySize == 1) {
                board = moveHistory[0]
                moveHistory = emptyList()
            }
            canUndo = moveHistory.isNotEmpty()
            haptic.tap()
        }
    }

    fun onCellClick(row: Int, col: Int) {
        if (!isPlayerTurn || winner != null || isDraw || isAIThinking) return
        if (!board.cells[row][col].isEmpty()) return

        // Save current board for undo
        moveHistory = moveHistory + board.copy()
        canUndo = true

        if (actionMode == TicTacToeActionMode.BOMB && playerBombs > 0) {
            // Place bomb
            haptic.explosion()
            soundManager.playBomb()
            board = if (selectedGameMode == TicTacToeGameMode.L_BOMB) {
                TicTacToeLogic.detonateLBomb(board, row, col)
            } else {
                TicTacToeLogic.detonateStandardBomb(board, row, col)
            }
            playerBombs--
            actionMode = TicTacToeActionMode.SYMBOL
        } else {
            // Place symbol
            haptic.tap()
            soundManager.playPlace()
            val newCells = board.cells.mapIndexed { r, rowCells ->
                rowCells.mapIndexed { c, cell ->
                    if (r == row && c == col) {
                        cell.copy(value = playerSymbol)
                    } else cell
                }
            }
            board = board.copy(cells = newCells)
        }

        // Check for winner
        val gameWinner = TicTacToeLogic.checkWinner(board)
        if (gameWinner != null) {
            winner = gameWinner
            winningLine = TicTacToeLogic.getWinningLine(board)
            showGameOver = true
            if (gameWinner == playerSymbol) {
                haptic.win()
                soundManager.playWin()
                // Track achievement
                achievementsRepository.checkAchievements(
                    AchievementEvent.TicTacToeWon(againstAI = true, aiStrength = aiStrength)
                )
            } else {
                haptic.lose()
                soundManager.playLose()
            }
        } else if (TicTacToeLogic.isBoardFull(board)) {
            isDraw = true
            showGameOver = true
        } else {
            isPlayerTurn = false
        }
    }

    fun onUltimateCellClick(boardRow: Int, boardCol: Int, cellRow: Int, cellCol: Int) {
        if (!isPlayerTurn || winner != null || isDraw || isAIThinking) return

        // Check if this board is playable
        if (!playableBoards.contains(Pair(boardRow, boardCol))) return
        if (!UltimateTicTacToeLogic.isValidMove(ultimateBoard, boardRow, boardCol, cellRow, cellCol)) return

        haptic.tap()
        soundManager.playPlace()
        val newBoard = UltimateTicTacToeLogic.makeMove(
            ultimateBoard, boardRow, boardCol, cellRow, cellCol, playerSymbol, "player"
        )
        ultimateBoard = newBoard
        playableBoards = UltimateTicTacToeLogic.getPlayableBoards(newBoard)

        // Check for winner
        val ultimateWinner = UltimateTicTacToeLogic.checkWinner(newBoard)
        if (ultimateWinner != null) {
            winner = ultimateWinner
            showGameOver = true
            if (ultimateWinner == playerSymbol) {
                haptic.win()
                soundManager.playWin()
                achievementsRepository.checkAchievements(
                    AchievementEvent.TicTacToeWon(againstAI = true, aiStrength = aiStrength)
                )
            } else {
                haptic.lose()
                soundManager.playLose()
            }
        } else if (UltimateTicTacToeLogic.isDraw(newBoard)) {
            isDraw = true
            showGameOver = true
        } else {
            isPlayerTurn = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        when (gamePhase) {
            GamePhase.SETUP -> {
                SetupScreen(
                    selectedGameMode = selectedGameMode,
                    selectedBoardSize = selectedBoardSize,
                    playerSymbol = playerSymbol,
                    aiStrength = aiStrength,
                    totalGames = totalGames,
                    playerWins = playerWins,
                    aiWins = aiWins,
                    onGameModeChange = { selectedGameMode = it },
                    onBoardSizeChange = { selectedBoardSize = it },
                    onPlayerSymbolChange = { playerSymbol = it },
                    onAiStrengthChange = { aiStrength = it },
                    onStartGame = { startGame() },
                    onBackClick = onBackClick
                )
            }
            GamePhase.PLAYING -> {
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { gamePhase = GamePhase.SETUP }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }

                        // Timer
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = timeString,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Undo Button
                        IconButton(
                            onClick = { undoLastMove() },
                            enabled = canUndo && isPlayerTurn && !isAIThinking && winner == null && !isDraw && selectedGameMode != TicTacToeGameMode.ULTIMATE
                        ) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo && isPlayerTurn && !isAIThinking && winner == null && !isDraw)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Game mode info
                    GameModeInfoCard(
                        gameMode = selectedGameMode,
                        boardSize = if (selectedGameMode != TicTacToeGameMode.ULTIMATE) selectedBoardSize.displayName else "Ultimate"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player info cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayerInfoCard(
                            name = "Du",
                            symbol = playerSymbol,
                            bombsRemaining = playerBombs,
                            isCurrentPlayer = isPlayerTurn && !isAIThinking,
                            gameMode = selectedGameMode,
                            modifier = Modifier.weight(1f)
                        )
                        PlayerInfoCard(
                            name = "KI",
                            symbol = if (playerSymbol == 1) 2 else 1,
                            bombsRemaining = aiBombs,
                            isCurrentPlayer = !isPlayerTurn || isAIThinking,
                            gameMode = selectedGameMode,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Turn indicator
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isPlayerTurn && !isAIThinking) {
                            SuccessColor.copy(alpha = 0.2f)
                        } else {
                            ErrorColor.copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAIThinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "KI denkt nach...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = if (isPlayerTurn) "Dein Zug!" else "KI ist dran",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Game board
                    if (selectedGameMode == TicTacToeGameMode.ULTIMATE) {
                        UltimateTicTacToeBoard(
                            board = ultimateBoard,
                            playableBoards = playableBoards,
                            isMyTurn = isPlayerTurn && !isAIThinking && winner == null && !isDraw,
                            mySymbol = playerSymbol,
                            onCellClick = { br, bc, cr, cc -> onUltimateCellClick(br, bc, cr, cc) }
                        )
                    } else {
                        TicTacToeBoard(
                            board = board,
                            winningLine = winningLine,
                            isMyTurn = isPlayerTurn && !isAIThinking && winner == null && !isDraw,
                            mySymbol = playerSymbol,
                            onCellClick = { r, c -> onCellClick(r, c) }
                        )
                    }

                    // AI Move Explanation
                    AnimatedVisibility(
                        visible = showExplanation && lastAIExplanation != null && isPlayerTurn,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut()
                    ) {
                        lastAIExplanation?.let { explanation ->
                            AIExplanationCard(
                                explanation = explanation,
                                onDismiss = { showExplanation = false }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action selector for bomb modes
                    if (selectedGameMode == TicTacToeGameMode.BOMB || selectedGameMode == TicTacToeGameMode.L_BOMB) {
                        TicTacToeActionSelector(
                            currentMode = actionMode,
                            gameMode = selectedGameMode,
                            mySymbol = playerSymbol,
                            bombsRemaining = playerBombs,
                            isMyTurn = isPlayerTurn && !isAIThinking && winner == null,
                            onModeChange = { actionMode = it }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Game over dialog
    if (showGameOver) {
        val playerWon = winner == playerSymbol
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = when {
                        isDraw -> Icons.Default.Balance
                        playerWon -> Icons.Default.EmojiEvents
                        else -> Icons.Default.SentimentDissatisfied
                    }
                    val iconColor = when {
                        isDraw -> InfoColor
                        playerWon -> SuccessColor
                        else -> ErrorColor
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = iconColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val resultText = when {
                        isDraw -> "Unentschieden!"
                        playerWon -> "Du hast gewonnen!"
                        else -> "Die KI hat gewonnen!"
                    }

                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                showGameOver = false
                                gamePhase = GamePhase.SETUP
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Menu")
                        }
                        Button(
                            onClick = {
                                showGameOver = false
                                startGame()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Nochmal")
                        }
                    }
                }
            }
        }
    }
}

private enum class GamePhase {
    SETUP, PLAYING
}

@Composable
private fun SetupScreen(
    selectedGameMode: TicTacToeGameMode,
    selectedBoardSize: TicTacToeBoardSize,
    playerSymbol: Int,
    aiStrength: Int,
    totalGames: Int,
    playerWins: Int,
    aiWins: Int,
    onGameModeChange: (TicTacToeGameMode) -> Unit,
    onBoardSizeChange: (TicTacToeBoardSize) -> Unit,
    onPlayerSymbolChange: (Int) -> Unit,
    onAiStrengthChange: (Int) -> Unit,
    onStartGame: () -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
            Text(
                text = "TicTacToe vs KI",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Statistics Card
        if (totalGames > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deine Statistik",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            value = totalGames.toString(),
                            label = "Spiele",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StatItem(
                            value = playerWins.toString(),
                            label = "Siege",
                            color = SuccessColor
                        )
                        StatItem(
                            value = aiWins.toString(),
                            label = "Niederlagen",
                            color = ErrorColor
                        )
                        StatItem(
                            value = "${if (totalGames > 0) (playerWins * 100 / totalGames) else 0}%",
                            label = "Winrate",
                            color = if (playerWins > aiWins) SuccessColor else ErrorColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Game mode selection
        Text(
            text = "Spielmodus",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TicTacToeGameMode.entries.forEach { mode ->
                GameModeOptionAI(
                    mode = mode,
                    isSelected = mode == selectedGameMode,
                    onClick = { onGameModeChange(mode) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Board size selection (not for Ultimate)
        if (selectedGameMode != TicTacToeGameMode.ULTIMATE) {
            Text(
                text = "Spielfeldgröße",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TicTacToeBoardSize.entries.forEach { size ->
                    BoardSizeOptionAI(
                        size = size,
                        isSelected = size == selectedBoardSize,
                        onClick = { onBoardSizeChange(size) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Symbol selection (who goes first)
        Text(
            text = "Dein Symbol",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SymbolOption(
                symbol = "X",
                description = "Erster Zug",
                isSelected = playerSymbol == 1,
                onClick = { onPlayerSymbolChange(1) },
                modifier = Modifier.weight(1f)
            )
            SymbolOption(
                symbol = "O",
                description = "Zweiter Zug",
                isSelected = playerSymbol == 2,
                onClick = { onPlayerSymbolChange(2) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Strength slider
        Text(
            text = "KI Stärke",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val strengthLabel = when {
                        aiStrength <= 20 -> "Anfänger"
                        aiStrength <= 40 -> "Einfach"
                        aiStrength <= 60 -> "Mittel"
                        aiStrength <= 80 -> "Schwer"
                        else -> "Experte"
                    }
                    Text(
                        text = strengthLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            aiStrength <= 40 -> SuccessColor
                            aiStrength <= 70 -> WarningColor
                            else -> ErrorColor
                        }
                    )
                    Text(
                        text = "${aiStrength}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = aiStrength.toFloat(),
                    onValueChange = { onAiStrengthChange(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 9,  // 10 levels: 0, 10, 20, ..., 100
                    colors = SliderDefaults.colors(
                        thumbColor = when {
                            aiStrength <= 40 -> SuccessColor
                            aiStrength <= 70 -> WarningColor
                            else -> ErrorColor
                        },
                        activeTrackColor = when {
                            aiStrength <= 40 -> SuccessColor
                            aiStrength <= 70 -> WarningColor
                            else -> ErrorColor
                        }
                    )
                )

                Text(
                    text = when {
                        aiStrength <= 20 -> "Die KI macht häufig Fehler - perfekt zum Lernen!"
                        aiStrength <= 40 -> "Die KI spielt entspannt mit gelegentlichen Fehlern."
                        aiStrength <= 60 -> "Die KI spielt gut, aber nicht perfekt."
                        aiStrength <= 80 -> "Die KI ist ein starker Gegner mit seltenen Fehlern."
                        else -> "Die KI spielt nahezu perfekt - eine echte Herausforderung!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start button
        Button(
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.SmartToy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gegen KI spielen")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // AI info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = "Die KI verwendet den Minimax-Algorithmus. Stelle die Stärke ein für dein Level!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GameModeOptionAI(
    mode: TicTacToeGameMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (mode) {
        TicTacToeGameMode.CLASSIC -> InfoColor
        TicTacToeGameMode.BOMB -> WarningColor
        TicTacToeGameMode.L_BOMB -> BombColor
        TicTacToeGameMode.ULTIMATE -> SuccessColor
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) {
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(color)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (mode) {
                TicTacToeGameMode.CLASSIC -> "X"
                TicTacToeGameMode.BOMB -> "B"
                TicTacToeGameMode.L_BOMB -> "L"
                TicTacToeGameMode.ULTIMATE -> "U"
            }
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color
                )
            }
        }
    }
}

@Composable
private fun BoardSizeOptionAI(
    size: TicTacToeBoardSize,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${size.size}x${size.size}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "${size.winCondition} in Reihe",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SymbolOption(
    symbol: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (symbol == "X") InfoColor else ErrorColor

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) {
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(color)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = symbol,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * AI Move Explanation Card - Educational feature
 */
@Composable
private fun AIExplanationCard(
    explanation: MoveExplanation.Explanation,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with emoji and strategic value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = explanation.strategicValue.emoji,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KI-Zug: ${explanation.strategicValue.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Schließen",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary reason
            Text(
                text = explanation.primaryReason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // Secondary reasons
            if (explanation.secondaryReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                explanation.secondaryReasons.forEach { reason ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("•", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Learning tip
            explanation.learningTip?.let { tip ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
