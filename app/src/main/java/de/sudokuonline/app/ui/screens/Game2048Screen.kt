package de.sudokuonline.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.CoinReason
import kotlin.math.abs

private val TileColors = mapOf(
    0 to Color(0xFFCDC1B4),
    2 to Color(0xFFEEE4DA),
    4 to Color(0xFFEDE0C8),
    8 to Color(0xFFF2B179),
    16 to Color(0xFFF59563),
    32 to Color(0xFFF67C5F),
    64 to Color(0xFFF65E3B),
    128 to Color(0xFFEDCF72),
    256 to Color(0xFFEDCC61),
    512 to Color(0xFFEDC850),
    1024 to Color(0xFFEDC53F),
    2048 to Color(0xFFEDC22E),
    4096 to Color(0xFF3C3A32),
    8192 to Color(0xFF3C3A32)
)

private fun tileTextColor(value: Int): Color =
    if (value <= 4) Color(0xFF776E65) else Color.White

private fun tileFontSize(value: Int): Int = when {
    value < 100 -> 36
    value < 1000 -> 28
    value < 10000 -> 22
    else -> 18
}

private enum class Direction { UP, DOWN, LEFT, RIGHT }

private data class Game2048State(
    val grid: List<List<Int>> = List(4) { List(4) { 0 } },
    val score: Int = 0,
    val bestScore: Int = 0,
    val gameOver: Boolean = false,
    val won: Boolean = false,
    val showWinDialog: Boolean = false
)

private fun newGame(): Game2048State {
    val grid = MutableList(4) { MutableList(4) { 0 } }
    addRandomTile(grid)
    addRandomTile(grid)
    return Game2048State(grid = grid.map { it.toList() })
}

private fun addRandomTile(grid: MutableList<MutableList<Int>>) {
    val empty = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until 4) for (c in 0 until 4) {
        if (grid[r][c] == 0) empty.add(r to c)
    }
    if (empty.isEmpty()) return
    val (r, c) = empty.random()
    grid[r][c] = if (Math.random() < 0.9) 2 else 4
}

private fun move(state: Game2048State, dir: Direction): Game2048State {
    if (state.gameOver) return state
    val grid = state.grid.map { it.toMutableList() }.toMutableList()
    var score = state.score
    var moved = false

    fun slideLine(line: MutableList<Int>): Int {
        var pts = 0
        // Remove zeros
        val nonZero = line.filter { it != 0 }.toMutableList()
        // Merge
        var i = 0
        while (i < nonZero.size - 1) {
            if (nonZero[i] == nonZero[i + 1]) {
                nonZero[i] *= 2
                pts += nonZero[i]
                nonZero.removeAt(i + 1)
            }
            i++
        }
        // Pad
        while (nonZero.size < 4) nonZero.add(0)
        for (j in 0 until 4) {
            if (line[j] != nonZero[j]) moved = true
            line[j] = nonZero[j]
        }
        return pts
    }

    when (dir) {
        Direction.LEFT -> {
            for (r in 0 until 4) score += slideLine(grid[r])
        }
        Direction.RIGHT -> {
            for (r in 0 until 4) {
                grid[r].reverse()
                score += slideLine(grid[r])
                grid[r].reverse()
            }
        }
        Direction.UP -> {
            for (c in 0 until 4) {
                val col = MutableList(4) { grid[it][c] }
                score += slideLine(col)
                for (r in 0 until 4) grid[r][c] = col[r]
            }
        }
        Direction.DOWN -> {
            for (c in 0 until 4) {
                val col = MutableList(4) { grid[3 - it][c] }
                score += slideLine(col)
                for (r in 0 until 4) grid[3 - r][c] = col[r]
            }
        }
    }

    if (!moved) return state

    addRandomTile(grid)

    val won = grid.any { row -> row.any { it >= 2048 } }
    val gameOver = isGameOver(grid)

    return state.copy(
        grid = grid.map { it.toList() },
        score = score,
        bestScore = maxOf(state.bestScore, score),
        gameOver = gameOver,
        won = won,
        showWinDialog = won && !state.won
    )
}

private fun isGameOver(grid: List<List<Int>>): Boolean {
    for (r in 0 until 4) for (c in 0 until 4) {
        if (grid[r][c] == 0) return false
        if (r < 3 && grid[r][c] == grid[r + 1][c]) return false
        if (c < 3 && grid[r][c] == grid[r][c + 1]) return false
    }
    return true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Game2048Screen(
    onBackClick: () -> Unit
) {
    var gameState by remember { mutableStateOf(newGame()) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }

    // Track if coins were awarded this game
    var coinsAwarded by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.gameOver) {
        if (gameState.gameOver) showGameOverDialog = true
    }

    LaunchedEffect(gameState.showWinDialog) {
        if (gameState.showWinDialog && !coinsAwarded) {
            currencyRepository.addCoins(100, CoinReason.ACHIEVEMENT)
            coinsAwarded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("2048", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        gameState = newGame()
                        showGameOverDialog = false
                        coinsAwarded = false
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Neues Spiel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreBox(label = "PUNKTE", value = gameState.score)
                ScoreBox(label = "REKORD", value = gameState.bestScore)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Game grid with swipe detection
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFBBADA0))
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val (dx, dy) = dragAmount
                            if (abs(dx) < 10 && abs(dy) < 10) return@detectDragGestures
                            val dir = if (abs(dx) > abs(dy)) {
                                if (dx > 0) Direction.RIGHT else Direction.LEFT
                            } else {
                                if (dy > 0) Direction.DOWN else Direction.UP
                            }
                            gameState = move(gameState, dir)
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 4) {
                                val value = gameState.grid[row][col]
                                Tile2048(
                                    value = value,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Wische um die Kacheln zu bewegen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Win dialog
    if (gameState.showWinDialog) {
        Dialog(onDismissRequest = {
            gameState = gameState.copy(showWinDialog = false)
        }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("2048 erreicht!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEDC22E))
                    Text("+100 Coins!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8860B))
                    Text("Du kannst weiterspielen!", style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = { gameState = gameState.copy(showWinDialog = false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDC22E))
                    ) {
                        Text("Weiterspielen", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Game over dialog
    if (showGameOverDialog) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Game Over!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Punkte: ${gameState.score}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            gameState = newGame()
                            showGameOverDialog = false
                            coinsAwarded = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Nochmal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBox(label: String, value: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFBBADA0)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEEE4DA))
            Text(text = "$value", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun Tile2048(value: Int, modifier: Modifier = Modifier) {
    val bgColor = TileColors[value] ?: TileColors[8192]!!

    val appear by animateFloatAsState(
        targetValue = if (value != 0) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "appear"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .scale(if (value != 0) appear else 1f),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = "$value",
                fontSize = tileFontSize(value).sp,
                fontWeight = FontWeight.Bold,
                color = tileTextColor(value),
                textAlign = TextAlign.Center
            )
        }
    }
}
