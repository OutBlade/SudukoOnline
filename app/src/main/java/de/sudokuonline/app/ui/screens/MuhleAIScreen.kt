package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.MuhleAI
import de.sudokuonline.app.game.MuhleLogic
import de.sudokuonline.app.ui.components.*
import de.sudokuonline.app.ui.theme.*
import de.sudokuonline.app.util.rememberHapticFeedback
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.CoinReason
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuhleAIScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }

    var gamePhase by remember { mutableStateOf(MuhleSetupPhase.SETUP) }
    var playerNumber by remember { mutableIntStateOf(1) }  // 1=White (first), 2=Black (second)
    var aiStrength by remember { mutableIntStateOf(70) }   // 0-100% AI strength

    // Game state
    var board by remember { mutableStateOf(MuhleBoard.create()) }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var playerStonesToPlace by remember { mutableIntStateOf(9) }
    var playerStonesOnBoard by remember { mutableIntStateOf(0) }
    var aiStonesToPlace by remember { mutableIntStateOf(9) }
    var aiStonesOnBoard by remember { mutableIntStateOf(0) }
    var currentAction by remember { mutableStateOf(MuhleActionType.PLACE) }
    var selectedPosition by remember { mutableIntStateOf(-1) }
    var mustRemoveStone by remember { mutableStateOf(false) }
    var validMoves by remember { mutableStateOf<List<Int>>(emptyList()) }
    var removableStones by remember { mutableStateOf<List<Int>>(emptyList()) }
    var highlightedMill by remember { mutableStateOf<List<Int>>(emptyList()) }
    var winner by remember { mutableStateOf<Int?>(null) }
    var showGameOver by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isAIThinking by remember { mutableStateOf(false) }
    var coinsAwarded by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(gamePhase) {
        if (gamePhase == MuhleSetupPhase.PLAYING) {
            while (gamePhase == MuhleSetupPhase.PLAYING && winner == null) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    // Calculate valid moves when turn changes
    LaunchedEffect(isPlayerTurn, currentAction, selectedPosition, mustRemoveStone, board) {
        if (isPlayerTurn && winner == null) {
            val playerPhase = when {
                playerStonesToPlace > 0 -> MuhleGamePhase.PLACING
                playerStonesOnBoard <= 3 -> MuhleGamePhase.FLYING
                else -> MuhleGamePhase.MOVING
            }

            when {
                mustRemoveStone -> {
                    val aiNumber = if (playerNumber == 1) 2 else 1
                    removableStones = MuhleLogic.getRemovableStones(board, aiNumber)
                    validMoves = emptyList()
                }
                currentAction == MuhleActionType.PLACE -> {
                    validMoves = MuhleLogic.getEmptyPositions(board)
                    removableStones = emptyList()
                }
                currentAction == MuhleActionType.SELECT -> {
                    validMoves = emptyList()
                    removableStones = emptyList()
                }
                currentAction == MuhleActionType.MOVE && selectedPosition >= 0 -> {
                    val canFly = playerPhase == MuhleGamePhase.FLYING
                    validMoves = if (canFly) {
                        MuhleLogic.getEmptyPositions(board)
                    } else {
                        MuhleLogic.getAdjacentPositions(selectedPosition)
                            .filter { board.positions[it].isEmpty() }
                    }
                    removableStones = emptyList()
                }
                else -> {
                    validMoves = emptyList()
                    removableStones = emptyList()
                }
            }
        }
    }

    // AI turn
    LaunchedEffect(isPlayerTurn, gamePhase, winner, mustRemoveStone) {
        if (gamePhase == MuhleSetupPhase.PLAYING && !isPlayerTurn && winner == null) {
            isAIThinking = true
            delay(500)  // Thinking delay

            val aiNumber = if (playerNumber == 1) 2 else 1
            val aiPhase = when {
                aiStonesToPlace > 0 -> MuhleGamePhase.PLACING
                aiStonesOnBoard <= 3 -> MuhleGamePhase.FLYING
                else -> MuhleGamePhase.MOVING
            }

            // Run AI on background thread
            withContext(Dispatchers.Default) {
                if (mustRemoveStone) {
                    // AI needs to remove opponent's stone
                    val removal = MuhleAI.getBestRemoval(board, playerNumber, aiStrength)
                    board = MuhleLogic.removeStone(board, removal.position)
                    if (playerNumber == 1) {
                        playerStonesOnBoard--
                    }
                    mustRemoveStone = false
                    highlightedMill = emptyList()
                } else {
                    val aiMove = MuhleAI.getBestMove(board, aiNumber, aiPhase, aiStrength)

                    when (aiMove) {
                        is MuhleAI.MuhleAIMove.Place -> {
                            val newBoard = MuhleLogic.placeStone(board, aiMove.position, aiNumber)
                            board = newBoard
                            aiStonesToPlace--
                            aiStonesOnBoard++

                            // Check if mill formed
                            if (MuhleLogic.formsNewMill(board, aiMove.position, aiNumber)) {
                                highlightedMill = MuhleLogic.getMillsContainingPosition(board, aiMove.position)
                                    .flatten().distinct()
                                mustRemoveStone = true
                            }
                        }
                        is MuhleAI.MuhleAIMove.Move -> {
                            val newBoard = MuhleLogic.moveStone(board, aiMove.from, aiMove.to, aiNumber)
                            board = newBoard

                            // Check if mill formed
                            if (MuhleLogic.formsNewMill(board, aiMove.to, aiNumber)) {
                                highlightedMill = MuhleLogic.getMillsContainingPosition(board, aiMove.to)
                                    .flatten().distinct()
                                mustRemoveStone = true
                            }
                        }
                        is MuhleAI.MuhleAIMove.Remove -> {
                            // This shouldn't happen during AI's main move
                        }
                    }
                }
            }

            // Check for winner
            val players = mapOf(
                "player" to MuhleRoomPlayer(
                    playerNumber = playerNumber,
                    stonesToPlace = playerStonesToPlace,
                    stonesOnBoard = playerStonesOnBoard
                ),
                "ai" to MuhleRoomPlayer(
                    playerNumber = if (playerNumber == 1) 2 else 1,
                    stonesToPlace = aiStonesToPlace,
                    stonesOnBoard = aiStonesOnBoard
                )
            )
            val gameWinner = MuhleLogic.checkWinner(board, players)
            if (gameWinner != null) {
                winner = gameWinner
                showGameOver = true
                // Award coins if player won (only once)
                if (gameWinner == playerNumber && !coinsAwarded) {
                    currencyRepository.addCoins(
                        CoinReason.MUHLE_WIN.baseAmount,
                        CoinReason.MUHLE_WIN
                    )
                    coinsAwarded = true
                }
            }

            if (!mustRemoveStone) {
                isAIThinking = false
                isPlayerTurn = true
                currentAction = if (playerStonesToPlace > 0) {
                    MuhleActionType.PLACE
                } else {
                    MuhleActionType.SELECT
                }
            } else {
                // AI still needs to remove a stone
                isAIThinking = false
                isPlayerTurn = false
            }
        }
    }

    fun startGame() {
        board = MuhleBoard.create()
        playerStonesToPlace = 9
        playerStonesOnBoard = 0
        aiStonesToPlace = 9
        aiStonesOnBoard = 0
        winner = null
        showGameOver = false
        elapsedSeconds = 0
        mustRemoveStone = false
        selectedPosition = -1
        highlightedMill = emptyList()
        currentAction = MuhleActionType.PLACE
        isPlayerTurn = playerNumber == 1  // Player 1 (White) always starts
        gamePhase = MuhleSetupPhase.PLAYING
        coinsAwarded = false
    }

    fun onPositionClick(position: Int) {
        if (!isPlayerTurn || winner != null || isAIThinking) return

        val playerOwner = if (playerNumber == 1)
            MuhleStoneOwner.PLAYER_1.name else MuhleStoneOwner.PLAYER_2.name
        val aiNumber = if (playerNumber == 1) 2 else 1

        when {
            mustRemoveStone -> {
                // Remove opponent's stone
                if (position in removableStones) {
                    haptic.success()
                    board = MuhleLogic.removeStone(board, position)
                    aiStonesOnBoard--
                    mustRemoveStone = false
                    highlightedMill = emptyList()

                    // Check for winner
                    val players = mapOf(
                        "player" to MuhleRoomPlayer(
                            playerNumber = playerNumber,
                            stonesToPlace = playerStonesToPlace,
                            stonesOnBoard = playerStonesOnBoard
                        ),
                        "ai" to MuhleRoomPlayer(
                            playerNumber = aiNumber,
                            stonesToPlace = aiStonesToPlace,
                            stonesOnBoard = aiStonesOnBoard
                        )
                    )
                    val gameWinner = MuhleLogic.checkWinner(board, players)
                    if (gameWinner != null) {
                        winner = gameWinner
                        showGameOver = true
                        if (gameWinner == playerNumber) {
                            haptic.win()
                            // Award coins (only once)
                            if (!coinsAwarded) {
                                currencyRepository.addCoins(
                                    CoinReason.MUHLE_WIN.baseAmount,
                                    CoinReason.MUHLE_WIN
                                )
                                coinsAwarded = true
                            }
                        } else {
                            haptic.lose()
                        }
                    } else {
                        isPlayerTurn = false
                    }
                }
            }
            currentAction == MuhleActionType.PLACE -> {
                // Place a new stone
                if (position in validMoves && playerStonesToPlace > 0) {
                    haptic.tap()
                    val newBoard = MuhleLogic.placeStone(board, position, playerNumber)
                    board = newBoard
                    playerStonesToPlace--
                    playerStonesOnBoard++

                    // Check if mill formed
                    if (MuhleLogic.formsNewMill(board, position, playerNumber)) {
                        haptic.success()
                        highlightedMill = MuhleLogic.getMillsContainingPosition(board, position)
                            .flatten().distinct()
                        mustRemoveStone = true
                        removableStones = MuhleLogic.getRemovableStones(board, aiNumber)
                    } else {
                        isPlayerTurn = false
                    }
                }
            }
            currentAction == MuhleActionType.SELECT -> {
                // Select a stone to move
                if (board.positions[position].owner == playerOwner) {
                    haptic.tap()
                    selectedPosition = position
                    currentAction = MuhleActionType.MOVE
                }
            }
            currentAction == MuhleActionType.MOVE -> {
                if (position == selectedPosition) {
                    // Deselect
                    selectedPosition = -1
                    currentAction = MuhleActionType.SELECT
                } else if (board.positions[position].owner == playerOwner) {
                    // Select different stone
                    haptic.tap()
                    selectedPosition = position
                } else if (position in validMoves) {
                    // Move stone
                    haptic.tap()
                    val newBoard = MuhleLogic.moveStone(board, selectedPosition, position, playerNumber)
                    board = newBoard
                    selectedPosition = -1

                    // Check if mill formed
                    if (MuhleLogic.formsNewMill(board, position, playerNumber)) {
                        haptic.success()
                        highlightedMill = MuhleLogic.getMillsContainingPosition(board, position)
                            .flatten().distinct()
                        mustRemoveStone = true
                        removableStones = MuhleLogic.getRemovableStones(board, aiNumber)
                        currentAction = MuhleActionType.REMOVE
                    } else {
                        currentAction = MuhleActionType.SELECT
                        isPlayerTurn = false
                    }
                }
            }
        }
    }

    val playerPhase = when {
        playerStonesToPlace > 0 -> MuhleGamePhase.PLACING
        playerStonesOnBoard <= 3 -> MuhleGamePhase.FLYING
        else -> MuhleGamePhase.MOVING
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        when (gamePhase) {
            MuhleSetupPhase.SETUP -> {
                MuhleSetupScreen(
                    playerNumber = playerNumber,
                    aiStrength = aiStrength,
                    onPlayerNumberChange = { playerNumber = it },
                    onAiStrengthChange = { aiStrength = it },
                    onStartGame = { startGame() },
                    onBackClick = onBackClick
                )
            }
            MuhleSetupPhase.PLAYING -> {
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
                        IconButton(onClick = { gamePhase = MuhleSetupPhase.SETUP }) {
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

                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phase indicator
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when (playerPhase) {
                            MuhleGamePhase.PLACING -> SuccessColor.copy(alpha = 0.2f)
                            MuhleGamePhase.MOVING -> InfoColor.copy(alpha = 0.2f)
                            MuhleGamePhase.FLYING -> WarningColor.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = playerPhase.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (playerPhase) {
                                MuhleGamePhase.PLACING -> SuccessColor
                                MuhleGamePhase.MOVING -> InfoColor
                                MuhleGamePhase.FLYING -> WarningColor
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player info cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MuhlePlayerCard(
                            name = "Du",
                            isWhite = playerNumber == 1,
                            stonesToPlace = playerStonesToPlace,
                            stonesOnBoard = playerStonesOnBoard,
                            isCurrentPlayer = isPlayerTurn && !isAIThinking,
                            modifier = Modifier.weight(1f)
                        )
                        MuhlePlayerCard(
                            name = "KI",
                            isWhite = playerNumber != 1,
                            stonesToPlace = aiStonesToPlace,
                            stonesOnBoard = aiStonesOnBoard,
                            isCurrentPlayer = !isPlayerTurn || isAIThinking,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Turn/Action indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            mustRemoveStone -> WarningColor.copy(alpha = 0.15f)
                            isPlayerTurn && !isAIThinking -> SuccessColor.copy(alpha = 0.15f)
                            else -> ErrorColor.copy(alpha = 0.15f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                                val text = when {
                                    mustRemoveStone -> "Entferne einen gegnerischen Stein!"
                                    !isPlayerTurn -> "KI ist dran"
                                    currentAction == MuhleActionType.PLACE -> "Setze einen Stein"
                                    currentAction == MuhleActionType.SELECT -> "Wähle einen Stein"
                                    currentAction == MuhleActionType.MOVE -> "Bewege den Stein"
                                    else -> "Dein Zug!"
                                }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        mustRemoveStone -> WarningColor
                                        isPlayerTurn -> SuccessColor
                                        else -> ErrorColor
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Game board
                    MuhleBoardView(
                        board = board,
                        selectedPosition = selectedPosition,
                        validMoves = validMoves,
                        removableStones = removableStones,
                        highlightedMill = highlightedMill,
                        isMyTurn = isPlayerTurn && !isAIThinking && winner == null,
                        onPositionClick = { onPositionClick(it) },
                        boardSize = 320.dp
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Game over dialog
    if (showGameOver) {
        val playerWon = winner == playerNumber
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = if (playerWon) Icons.Default.EmojiEvents else Icons.Default.SentimentDissatisfied
                    val iconColor = if (playerWon) SuccessColor else ErrorColor

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = iconColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (playerWon) "Du hast gewonnen!" else "Die KI hat gewonnen!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                showGameOver = false
                                gamePhase = MuhleSetupPhase.SETUP
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

private enum class MuhleSetupPhase {
    SETUP, PLAYING
}

@Composable
private fun MuhleSetupScreen(
    playerNumber: Int,
    aiStrength: Int,
    onPlayerNumberChange: (Int) -> Unit,
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
                text = "Muhle vs KI",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Game info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Muhle (Nine Men's Morris)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Platziere deine 9 Steine und bilde Muhlen (3 in einer Reihe). " +
                            "Bei jeder Muhle entfernst du einen gegnerischen Stein. " +
                            "Gewinne, indem du den Gegner auf weniger als 3 Steine reduzierst!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Color selection (who goes first)
        Text(
            text = "Deine Farbe",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MuhleColorOption(
                isWhite = true,
                description = "Erster Zug",
                isSelected = playerNumber == 1,
                onClick = { onPlayerNumberChange(1) },
                modifier = Modifier.weight(1f)
            )
            MuhleColorOption(
                isWhite = false,
                description = "Zweiter Zug",
                isSelected = playerNumber == 2,
                onClick = { onPlayerNumberChange(2) },
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
                    steps = 9,
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
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Start button
        Button(
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(Icons.Default.SmartToy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gegen KI spielen")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MuhleColorOption(
    isWhite: Boolean,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stoneColor = if (isWhite) WhiteStoneColor else BlackStoneColor
    val borderColor = if (isWhite) WhiteStoneBorder else BlackStoneBorder
    val selectColor = if (isWhite) Color(0xFF8D6E63) else Color(0xFF5D4037)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectColor.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) {
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(selectColor)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stone preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(stoneColor)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = borderColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isWhite) "Weiss" else "Schwarz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) selectColor else MaterialTheme.colorScheme.onSurface
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
private fun MuhlePlayerCard(
    name: String,
    isWhite: Boolean,
    stonesToPlace: Int,
    stonesOnBoard: Int,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val stoneColor = if (isWhite) WhiteStoneColor else BlackStoneColor
    val borderColor = if (isWhite) WhiteStoneBorder else BlackStoneBorder

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentPlayer) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isCurrentPlayer) {
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary
                )
            )
        } else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stone indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(stoneColor)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = borderColor,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Setzen: $stonesToPlace | Brett: $stonesOnBoard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
