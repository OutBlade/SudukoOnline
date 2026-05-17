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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.game.DameAI
import de.sudokuonline.app.game.DameLogic
import de.sudokuonline.app.ui.components.DameBoardView
import de.sudokuonline.app.ui.theme.*
import de.sudokuonline.app.util.rememberHapticFeedback
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.CoinReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class DameSetupPhase {
    SETUP, PLAYING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DameAIScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }
    val lootboxRepository = remember { de.sudokuonline.app.data.repository.LootboxRepository.getInstance(context) }

    var gamePhase by remember { mutableStateOf(DameSetupPhase.SETUP) }
    var playerColor by remember { mutableStateOf(DamePlayerColor.WHITE) }
    var aiStrength by remember { mutableIntStateOf(50) }

    // Game state
    var board by remember { mutableStateOf(DameBoard.create()) }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var selectedPiece by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var validMoves by remember { mutableStateOf<List<DameMove>>(emptyList()) }
    var allPlayerMoves by remember { mutableStateOf<List<DameMove>>(emptyList()) }
    var winner by remember { mutableStateOf<DamePlayerColor?>(null) }
    var showGameOver by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isAIThinking by remember { mutableStateOf(false) }
    var coinsAwarded by remember { mutableStateOf(false) }
    var lootboxAwarded by remember { mutableStateOf(false) }
    var mustContinueJump by remember { mutableStateOf(false) }
    var continuingPiece by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var moveHistory by remember { mutableStateOf<List<DameMove>>(emptyList()) }
    var gameStartCounter by remember { mutableIntStateOf(0) }

    val aiColor = if (playerColor == DamePlayerColor.WHITE) DamePlayerColor.BLACK else DamePlayerColor.WHITE

    // Timer
    LaunchedEffect(gamePhase) {
        if (gamePhase == DameSetupPhase.PLAYING) {
            while (gamePhase == DameSetupPhase.PLAYING && winner == null) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    // Calculate valid moves
    LaunchedEffect(isPlayerTurn, board, mustContinueJump, continuingPiece, gameStartCounter) {
        if (isPlayerTurn && winner == null && gamePhase == DameSetupPhase.PLAYING) {
            if (mustContinueJump && continuingPiece != null) {
                val (r, c) = continuingPiece!!
                val captures = DameLogic.getCapturesForPiece(
                    board, r, c,
                    DamePieceType.valueOf(board.cells[r][c].pieceType),
                    playerColor
                )
                validMoves = captures
                allPlayerMoves = captures
                selectedPiece = continuingPiece
            } else {
                allPlayerMoves = DameLogic.getValidMoves(board, playerColor)
                validMoves = emptyList()
                selectedPiece = null
            }
        }
    }

    // AI turn
    LaunchedEffect(isPlayerTurn, gamePhase, winner) {
        if (gamePhase == DameSetupPhase.PLAYING && !isPlayerTurn && winner == null) {
            isAIThinking = true
            delay(500)

            withContext(Dispatchers.Default) {
                var currentBoard = board
                var keepJumping = true
                while (keepJumping) {
                    val aiMove = DameAI.getBestMove(currentBoard, aiColor, aiStrength)
                    if (aiMove != null) {
                        currentBoard = DameLogic.executeMove(currentBoard, aiMove)
                        moveHistory = moveHistory + aiMove

                        // Check multi-jump
                        if (aiMove.captures.isNotEmpty()) {
                            val landRow = aiMove.toRow
                            val landCol = aiMove.toCol
                            val continuingCaptures = DameLogic.getCapturesForPiece(
                                currentBoard, landRow, landCol,
                                DamePieceType.valueOf(currentBoard.cells[landRow][landCol].pieceType),
                                aiColor
                            )
                            if (continuingCaptures.isNotEmpty()) {
                                delay(400)
                                continue
                            }
                        }
                    }
                    keepJumping = false
                }
                board = currentBoard
            }

            // Check winner
            val gameWinner = DameLogic.checkWinner(board, playerColor)
            if (gameWinner != null) {
                winner = gameWinner
                showGameOver = true
                if (gameWinner == playerColor && !coinsAwarded) {
                    currencyRepository.addCoins(CoinReason.DAME_WIN.baseAmount, CoinReason.DAME_WIN)
                    coinsAwarded = true
                    if (!lootboxAwarded) {
                        lootboxRepository.addLootbox(de.sudokuonline.app.data.model.LootboxRarity.BRONZE, "dame_win")
                        lootboxAwarded = true
                    }
                }
            }

            isAIThinking = false
            isPlayerTurn = true
            mustContinueJump = false
            continuingPiece = null
        }
    }

    fun startGame() {
        board = DameBoard.create()
        winner = null
        showGameOver = false
        elapsedSeconds = 0
        selectedPiece = null
        validMoves = emptyList()
        allPlayerMoves = emptyList()
        mustContinueJump = false
        continuingPiece = null
        moveHistory = emptyList()
        isPlayerTurn = playerColor == DamePlayerColor.WHITE
        gamePhase = DameSetupPhase.PLAYING
        coinsAwarded = false
        lootboxAwarded = false
        gameStartCounter++
    }

    fun onCellClick(row: Int, col: Int) {
        if (!isPlayerTurn || winner != null || isAIThinking) return

        val cell = board.cells[row][col]
        val isOwnPiece = (playerColor == DamePlayerColor.WHITE && cell.owner == DamePlayerColor.WHITE.name) ||
                (playerColor == DamePlayerColor.BLACK && cell.owner == DamePlayerColor.BLACK.name)

        if (mustContinueJump) {
            // Can only move the continuing piece
            val move = validMoves.firstOrNull { it.toRow == row && it.toCol == col }
            if (move != null) {
                haptic.tap()
                board = DameLogic.executeMove(board, move)
                moveHistory = moveHistory + move

                // Check further jumps
                if (move.captures.isNotEmpty()) {
                    val newPieceType = DamePieceType.valueOf(board.cells[move.toRow][move.toCol].pieceType)
                    val furtherCaptures = DameLogic.getCapturesForPiece(
                        board, move.toRow, move.toCol, newPieceType, playerColor
                    )
                    if (furtherCaptures.isNotEmpty()) {
                        mustContinueJump = true
                        continuingPiece = Pair(move.toRow, move.toCol)
                        return
                    }
                }

                // Turn done
                mustContinueJump = false
                continuingPiece = null
                selectedPiece = null
                validMoves = emptyList()

                val gameWinner = DameLogic.checkWinner(board, aiColor)
                if (gameWinner != null) {
                    winner = gameWinner
                    showGameOver = true
                    if (gameWinner == playerColor && !coinsAwarded) {
                        haptic.win()
                        currencyRepository.addCoins(CoinReason.DAME_WIN.baseAmount, CoinReason.DAME_WIN)
                        coinsAwarded = true
                    } else {
                        haptic.lose()
                    }
                } else {
                    isPlayerTurn = false
                }
            }
            return
        }

        if (selectedPiece != null) {
            if (row == selectedPiece!!.first && col == selectedPiece!!.second) {
                // Deselect
                selectedPiece = null
                validMoves = emptyList()
                return
            }

            if (isOwnPiece) {
                // Select different piece
                val pieceMoves = allPlayerMoves.filter { it.fromRow == row && it.fromCol == col }
                if (pieceMoves.isNotEmpty()) {
                    haptic.tap()
                    selectedPiece = Pair(row, col)
                    validMoves = pieceMoves
                }
                return
            }

            // Try to execute move
            val move = validMoves.firstOrNull { it.toRow == row && it.toCol == col }
            if (move != null) {
                haptic.tap()
                board = DameLogic.executeMove(board, move)
                moveHistory = moveHistory + move

                // Check multi-jump
                if (move.captures.isNotEmpty()) {
                    val newPieceType = DamePieceType.valueOf(board.cells[move.toRow][move.toCol].pieceType)
                    val furtherCaptures = DameLogic.getCapturesForPiece(
                        board, move.toRow, move.toCol, newPieceType, playerColor
                    )
                    if (furtherCaptures.isNotEmpty()) {
                        haptic.success()
                        mustContinueJump = true
                        continuingPiece = Pair(move.toRow, move.toCol)
                        return
                    }
                }

                selectedPiece = null
                validMoves = emptyList()

                val gameWinner = DameLogic.checkWinner(board, aiColor)
                if (gameWinner != null) {
                    winner = gameWinner
                    showGameOver = true
                    if (gameWinner == playerColor && !coinsAwarded) {
                        haptic.win()
                        currencyRepository.addCoins(CoinReason.DAME_WIN.baseAmount, CoinReason.DAME_WIN)
                        coinsAwarded = true
                    } else {
                        haptic.lose()
                    }
                } else {
                    isPlayerTurn = false
                }
            }
        } else {
            // Select a piece
            if (isOwnPiece) {
                val pieceMoves = allPlayerMoves.filter { it.fromRow == row && it.fromCol == col }
                if (pieceMoves.isNotEmpty()) {
                    haptic.tap()
                    selectedPiece = Pair(row, col)
                    validMoves = pieceMoves
                }
            }
        }
    }

    // Count pieces
    val playerPieces = board.cells.flatten().count { it.owner == playerColor.name }
    val aiPieces = board.cells.flatten().count { it.owner == aiColor.name }

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
            DameSetupPhase.SETUP -> {
                DameSetupScreen(
                    playerColor = playerColor,
                    aiStrength = aiStrength,
                    onPlayerColorChange = { playerColor = it },
                    onAiStrengthChange = { aiStrength = it },
                    onStartGame = { startGame() },
                    onBackClick = onBackClick
                )
            }
            DameSetupPhase.PLAYING -> {
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
                        IconButton(onClick = { gamePhase = DameSetupPhase.SETUP }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zuruck")
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(text = timeString, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player info cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DamePlayerCard(
                            name = "Du",
                            isWhite = playerColor == DamePlayerColor.WHITE,
                            piecesCount = playerPieces,
                            isCurrentPlayer = isPlayerTurn && !isAIThinking,
                            modifier = Modifier.weight(1f)
                        )
                        DamePlayerCard(
                            name = "KI",
                            isWhite = aiColor == DamePlayerColor.WHITE,
                            piecesCount = aiPieces,
                            isCurrentPlayer = !isPlayerTurn || isAIThinking,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Turn indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            mustContinueJump -> WarningColor.copy(alpha = 0.15f)
                            isPlayerTurn && !isAIThinking -> SuccessColor.copy(alpha = 0.15f)
                            else -> ErrorColor.copy(alpha = 0.15f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAIThinking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("KI denkt nach...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            } else {
                                val text = when {
                                    mustContinueJump -> "Weiter springen!"
                                    isPlayerTurn -> "Dein Zug"
                                    else -> "KI ist dran"
                                }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        mustContinueJump -> WarningColor
                                        isPlayerTurn -> SuccessColor
                                        else -> ErrorColor
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    DameBoardView(
                        board = board,
                        selectedPiece = selectedPiece,
                        validMoves = validMoves,
                        isMyTurn = isPlayerTurn && !isAIThinking && winner == null,
                        playerColor = playerColor,
                        onCellClick = { r, c -> onCellClick(r, c) },
                        boardSize = 320.dp
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Game over dialog
    if (showGameOver) {
        val playerWon = winner == playerColor
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

                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = iconColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (playerWon) "Du hast gewonnen!" else "Die KI hat gewonnen!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showGameOver = false; gamePhase = DameSetupPhase.SETUP }, modifier = Modifier.weight(1f)) {
                            Text("Menu")
                        }
                        Button(onClick = { showGameOver = false; startGame() }, modifier = Modifier.weight(1f)) {
                            Text("Nochmal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DameSetupScreen(
    playerColor: DamePlayerColor,
    aiStrength: Int,
    onPlayerColorChange: (DamePlayerColor) -> Unit,
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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zuruck")
            }
            Text("Dame vs KI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dame (Checkers)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ziehe deine Steine diagonal vorwarts. Springe uber gegnerische Steine, um sie zu schlagen. " +
                            "Erreiche die gegnerische Grundlinie, um eine Dame zu bekommen. " +
                            "Gewinne, indem du alle gegnerischen Steine schlägst!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Deine Farbe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DameColorOption(
                isWhite = true,
                description = "Erster Zug",
                isSelected = playerColor == DamePlayerColor.WHITE,
                onClick = { onPlayerColorChange(DamePlayerColor.WHITE) },
                modifier = Modifier.weight(1f)
            )
            DameColorOption(
                isWhite = false,
                description = "Zweiter Zug",
                isSelected = playerColor == DamePlayerColor.BLACK,
                onClick = { onPlayerColorChange(DamePlayerColor.BLACK) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("KI Starke", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val strengthLabel = when {
                        aiStrength <= 20 -> "Anfanger"
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
                    Text(text = "${aiStrength}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

        Button(
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.SmartToy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gegen KI spielen")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DameColorOption(
    isWhite: Boolean,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pieceColor = if (isWhite) Color(0xFFFFF8E1) else Color(0xFF3E2723)
    val borderColor = if (isWhite) Color(0xFF8D6E63) else Color(0xFF1B0000)
    val selectColor = if (isWhite) Color(0xFF8D6E63) else Color(0xFF5D4037)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(selectColor)
        ) else null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(pieceColor)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = borderColor, style = Stroke(width = 2.dp.toPx()))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isWhite) "Weiss" else "Schwarz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) selectColor else MaterialTheme.colorScheme.onSurface
            )
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DamePlayerCard(
    name: String,
    isWhite: Boolean,
    piecesCount: Int,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val pieceColor = if (isWhite) Color(0xFFFFF8E1) else Color(0xFF3E2723)
    val borderColor = if (isWhite) Color(0xFF8D6E63) else Color(0xFF1B0000)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentPlayer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isCurrentPlayer) ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(pieceColor)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = borderColor, style = Stroke(width = 1.5.dp.toPx()))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Steine: $piecesCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
