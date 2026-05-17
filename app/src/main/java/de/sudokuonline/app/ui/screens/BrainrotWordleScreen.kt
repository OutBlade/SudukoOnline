package de.sudokuonline.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WordleGreen = Color(0xFF6AAA64)
private val WordleYellow = Color(0xFFC9B458)
private val WordleDarkGray = Color(0xFF787C7E)
private val WordleLightGray = Color(0xFFD3D6DA)
private val WordleEmptyBorder = Color(0xFF878A8C)
private val WordleFilledBorder = Color(0xFF565758)

private val BRAINROT_WORDS = listOf(
    "SIGMA", "RIZZY", "ALPHA", "BRAIN", "GYATT", "BASED", "RATIO",
    "GRIND", "VIBES", "FANUM", "COOKE", "BODEN", "DIGGA", "BRUHO",
    "SHILL", "SALTY", "SWIPE", "STALK", "GHOST", "CLOUT", "TOXIC",
    "VIBEN", "GONNT", "EHREN", "GAMED", "HYPED", "DRIPP", "GOATE",
    "SKIBI", "DELUL", "AURAZ", "OOMPH", "NOOBS", "SIMPS", "FLEXS",
    "GRINS", "YEETS", "MEMES", "LOWKE", "HIGHK", "CHILL", "CRASH",
    "SHOOK", "VALID", "QUEEN", "SLAYS", "STEAL", "SNACK", "GOALS",
    "EXTRA", "BASIC", "VEGAN", "KAREN", "BETAS", "OMEGA", "SCHAU",
    "ALTER", "KRASS", "DUMME", "FRESH", "GRILL", "STARK", "LAUCH",
    "OFFEN", "BREIT", "TIGHT", "SWAGS", "DRAUF", "ALMAN", "BUBAT",
    "AMOGN", "CRING", "EDITS", "FLAME", "GRIMD", "HUMID", "MAXED",
    "NICHE", "OWNED", "PRIME", "MAINS", "RATEY", "SPAWN",
    "TROLL", "ULTRA", "VIDAL", "WHACK", "WOKEN", "ZESTY", "BEAST",
    "CLOWN", "DEMON", "ELFIN", "FEAST", "GLAZE", "SAUCE", "SERVE",
    "TROPH", "LURKN", "TIKTK", "CAPPP", "NOCAP", "RISKY", "GRWTH"
)

private enum class LetterState {
    EMPTY, FILLED, CORRECT, PRESENT, ABSENT
}

private data class TileState(
    val letter: Char? = null,
    val state: LetterState = LetterState.EMPTY,
    val revealed: Boolean = false
)

private data class WordleGameState(
    val targetWord: String = BRAINROT_WORDS.random(),
    val guesses: List<List<TileState>> = List(6) { List(5) { TileState() } },
    val currentRow: Int = 0,
    val currentCol: Int = 0,
    val keyboardStates: Map<Char, LetterState> = emptyMap(),
    val gameOver: Boolean = false,
    val won: Boolean = false,
    val isRevealing: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrainrotWordleScreen(
    onBackClick: () -> Unit
) {
    var gameState by remember { mutableStateOf(WordleGameState()) }
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Reveal animation triggers per tile
    val revealingRow = remember { mutableStateOf(-1) }
    val revealedCols = remember { mutableStateListOf<Int>() }

    fun resetGame() {
        gameState = WordleGameState()
        showDialog = false
        revealingRow.value = -1
        revealedCols.clear()
    }

    fun submitGuess() {
        if (gameState.currentCol < 5 || gameState.isRevealing || gameState.gameOver) return

        val guess = gameState.guesses[gameState.currentRow]
            .mapNotNull { it.letter }
            .joinToString("")

        if (guess.length < 5) return

        val target = gameState.targetWord
        val newRow = mutableListOf<TileState>()
        val targetChars = target.toCharArray().toMutableList()
        val states = Array(5) { LetterState.ABSENT }

        // First pass: correct positions
        for (i in 0 until 5) {
            if (guess[i] == target[i]) {
                states[i] = LetterState.CORRECT
                targetChars[i] = ' '
            }
        }
        // Second pass: present but wrong position
        for (i in 0 until 5) {
            if (states[i] != LetterState.CORRECT) {
                val idx = targetChars.indexOf(guess[i])
                if (idx != -1) {
                    states[i] = LetterState.PRESENT
                    targetChars[idx] = ' '
                }
            }
        }

        for (i in 0 until 5) {
            newRow.add(TileState(letter = guess[i], state = states[i], revealed = false))
        }

        val newGuesses = gameState.guesses.toMutableList()
        newGuesses[gameState.currentRow] = newRow

        // Update keyboard states
        val newKeyStates = gameState.keyboardStates.toMutableMap()
        for (i in 0 until 5) {
            val c = guess[i]
            val newState = states[i]
            val current = newKeyStates[c]
            if (current == null || newState.ordinal > current.ordinal) {
                newKeyStates[c] = newState
            }
        }

        val won = guess == target
        val isLastRow = gameState.currentRow == 5

        gameState = gameState.copy(
            guesses = newGuesses,
            keyboardStates = newKeyStates,
            isRevealing = true
        )

        // Animate reveal
        revealingRow.value = gameState.currentRow
        revealedCols.clear()

        coroutineScope.launch {
            for (col in 0 until 5) {
                delay(300)
                revealedCols.add(col)
            }
            delay(200)

            // Mark tiles as revealed
            val revealed = gameState.guesses.toMutableList()
            revealed[gameState.currentRow] = revealed[gameState.currentRow].map {
                it.copy(revealed = true)
            }

            gameState = gameState.copy(
                guesses = revealed,
                currentRow = gameState.currentRow + 1,
                currentCol = 0,
                gameOver = won || isLastRow,
                won = won,
                isRevealing = false
            )
            revealingRow.value = -1
            revealedCols.clear()

            if (won || isLastRow) {
                delay(400)
                showDialog = true
            }
        }
    }

    fun onKeyPress(key: Char) {
        if (gameState.gameOver || gameState.isRevealing) return
        if (gameState.currentCol >= 5) return

        val newGuesses = gameState.guesses.toMutableList()
        val newRow = newGuesses[gameState.currentRow].toMutableList()
        newRow[gameState.currentCol] = TileState(letter = key, state = LetterState.FILLED)
        newGuesses[gameState.currentRow] = newRow

        gameState = gameState.copy(
            guesses = newGuesses,
            currentCol = gameState.currentCol + 1
        )
    }

    fun onBackspace() {
        if (gameState.gameOver || gameState.isRevealing) return
        if (gameState.currentCol <= 0) return

        val newGuesses = gameState.guesses.toMutableList()
        val newRow = newGuesses[gameState.currentRow].toMutableList()
        newRow[gameState.currentCol - 1] = TileState()
        newGuesses[gameState.currentRow] = newRow

        gameState = gameState.copy(
            guesses = newGuesses,
            currentCol = gameState.currentCol - 1
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Brainrot Wordle",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurueck"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Game board
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until 6) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 5) {
                            val tile = gameState.guesses[row][col]
                            val isRevealing = revealingRow.value == row &&
                                    col in revealedCols &&
                                    !tile.revealed

                            WordleTile(
                                tile = tile,
                                isRevealing = isRevealing,
                                revealDelay = col * 300
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Keyboard
            WordleKeyboard(
                keyboardStates = gameState.keyboardStates,
                onKeyPress = ::onKeyPress,
                onEnter = ::submitGuess,
                onBackspace = ::onBackspace
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Game over dialog
    if (showDialog) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (gameState.won) "Geschafft!" else "Verloren!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gameState.won) WordleGreen else MaterialTheme.colorScheme.error
                    )

                    if (gameState.won) {
                        Text(
                            text = "Du hast es in ${gameState.currentRow} ${if (gameState.currentRow == 1) "Versuch" else "Versuchen"} erraten!",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Das Wort war:",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = gameState.targetWord,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        color = WordleGreen
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { resetGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WordleGreen
                        )
                    ) {
                        Text(
                            text = "Nochmal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordleTile(
    tile: TileState,
    isRevealing: Boolean,
    revealDelay: Int
) {
    val flipAnim = remember { Animatable(0f) }

    LaunchedEffect(isRevealing) {
        if (isRevealing) {
            flipAnim.snapTo(0f)
            flipAnim.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    val rotation = flipAnim.value
    val showBack = rotation > 90f

    val backgroundColor = when {
        showBack || tile.revealed -> when (tile.state) {
            LetterState.CORRECT -> WordleGreen
            LetterState.PRESENT -> WordleYellow
            LetterState.ABSENT -> WordleDarkGray
            else -> Color.Transparent
        }
        else -> Color.Transparent
    }

    val borderColor = when {
        showBack || tile.revealed -> Color.Transparent
        tile.letter != null -> WordleFilledBorder
        else -> WordleLightGray
    }

    val textColor = when {
        showBack || tile.revealed -> when (tile.state) {
            LetterState.CORRECT, LetterState.PRESENT, LetterState.ABSENT -> Color.White
            else -> MaterialTheme.colorScheme.onSurface
        }
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(58.dp)
            .graphicsLayer {
                rotationX = if (rotation <= 90f) rotation else 180f - rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        tile.letter?.let { letter ->
            Text(
                text = letter.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.graphicsLayer {
                    // Flip text so it reads correctly on back
                    if (showBack) rotationX = 180f
                }
            )
        }
    }
}

@Composable
private fun WordleKeyboard(
    keyboardStates: Map<Char, LetterState>,
    onKeyPress: (Char) -> Unit,
    onEnter: () -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        "QWERTZUIOP",
        "ASDFGHJKL",
        "YXCVBNM"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index == 2) {
                    // Enter key
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .width(56.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(WordleLightGray)
                            .clickable { onEnter() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                row.forEach { char ->
                    val state = keyboardStates[char]
                    val bgColor = when (state) {
                        LetterState.CORRECT -> WordleGreen
                        LetterState.PRESENT -> WordleYellow
                        LetterState.ABSENT -> WordleDarkGray
                        else -> WordleLightGray
                    }
                    val fgColor = when (state) {
                        LetterState.CORRECT, LetterState.PRESENT, LetterState.ABSENT -> Color.White
                        else -> Color.Black
                    }

                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .width(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .clickable { onKeyPress(char) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = fgColor
                        )
                    }
                }

                if (index == 2) {
                    // Backspace key
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .width(56.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(WordleLightGray)
                            .clickable { onBackspace() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Backspace,
                            contentDescription = "Loeschen",
                            modifier = Modifier.size(22.dp),
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
