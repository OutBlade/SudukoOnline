package de.sudokuonline.app.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────
// Colour palette — faithful to Flip 7 rainbow
// ─────────────────────────────────────────

private val numberPalette: Map<Int, Triple<Color, Color, Color>> = mapOf(
    0  to Triple(Color(0xFF37474F), Color(0xFF263238), Color(0xFF90A4AE)),
    1  to Triple(Color(0xFFE53935), Color(0xFFC62828), Color(0xFFFF8A80)),
    2  to Triple(Color(0xFFF4511E), Color(0xFFBF360C), Color(0xFFFF9E80)),
    3  to Triple(Color(0xFFFFB300), Color(0xFFFF6F00), Color(0xFFFFE57F)),
    4  to Triple(Color(0xFFFFD600), Color(0xFFF9A825), Color(0xFFFFF59D)),
    5  to Triple(Color(0xFF7CB342), Color(0xFF558B2F), Color(0xFFCCFF90)),
    6  to Triple(Color(0xFF00897B), Color(0xFF00695C), Color(0xFFA7FFEB)),
    7  to Triple(Color(0xFF039BE5), Color(0xFF0277BD), Color(0xFF80D8FF)),
    8  to Triple(Color(0xFF1E88E5), Color(0xFF1565C0), Color(0xFF82B1FF)),
    9  to Triple(Color(0xFF5E35B1), Color(0xFF4527A0), Color(0xFFB388FF)),
    10 to Triple(Color(0xFF8E24AA), Color(0xFF6A1B9A), Color(0xFFEA80FC)),
    11 to Triple(Color(0xFFD81B60), Color(0xFFAD1457), Color(0xFFFF80AB)),
    12 to Triple(Color(0xFFE53935), Color(0xFF880E4F), Color(0xFFFF8A80)),
)
private fun palette(n: Int) = numberPalette[n]!!
private val secondChancePalette = Triple(Color(0xFFFFD600), Color(0xFFF9A825), Color(0xFFFFF59D))
private val freezePalette       = Triple(Color(0xFF00BCD4), Color(0xFF006064), Color(0xFFB2EBF2))
private val flipThreePalette    = Triple(Color(0xFFFF6D00), Color(0xFFE65100), Color(0xFFFFD180))

// ─────────────────────────────────────────
// Model
// ─────────────────────────────────────────

private sealed class F7Card {
    data class Number(val n: Int) : F7Card()
    object SecondChance : F7Card()
    object Freeze       : F7Card()
    object FlipThree    : F7Card()
}

private enum class F7Phase { IDLE, REVEALING, BUSTED, STOOD, FLIP7_CELEBRATE, GAME_OVER }

private data class F7State(
    val deck: List<F7Card>         = emptyList(),
    val hand: List<F7Card>         = emptyList(),
    val collectedNumbers: Set<Int> = emptySet(),
    val hasSecondChance: Boolean   = false,
    val isFrozen: Boolean          = false,
    val totalScore: Int            = 0,
    val round: Int                 = 1,
    val totalRounds: Int           = 5,
    val streak: Int                = 0,
    val phase: F7Phase             = F7Phase.IDLE,
    val lastCard: F7Card?          = null,
    val bustCardN: Int?            = null,
    val message: String?           = null,
    val isNewRecord: Boolean       = false,
    val closeCall: Boolean         = false,     // survived a risky flip
    val lastScorePop: Int          = 0,         // points to show floating
    val scorePopTick: Int          = 0,         // changes to re-trigger pop
    val flashRed: Boolean          = false,
    val flashGreen: Boolean        = false,
    val comboCount: Int            = 0,         // safe flips in a row this round
)

private fun buildDeck(): List<F7Card> = buildList {
    (0..12).forEach { n -> repeat(if (n == 0) 1 else n) { add(F7Card.Number(n)) } }
    repeat(3) { add(F7Card.SecondChance) }
    repeat(3) { add(F7Card.Freeze) }
    repeat(3) { add(F7Card.FlipThree) }
}.shuffled()

private fun MutableList<F7Card>.pop(): F7Card = removeFirst()
private fun roundScore(hand: List<F7Card>) = hand.filterIsInstance<F7Card.Number>().sumOf { it.n }
private fun bustRisk(deck: List<F7Card>, collected: Set<Int>): Float {
    if (deck.isEmpty()) return 0f
    return deck.count { it is F7Card.Number && it.n in collected }.toFloat() / deck.size
}

// ─────────────────────────────────────────
// Screen
// ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Flip7Screen(onBackClick: () -> Unit) {
    val context    = LocalContext.current
    val prefs      = remember { context.getSharedPreferences("flip7_prefs", Context.MODE_PRIVATE) }
    var bestScore  by remember { mutableIntStateOf(prefs.getInt("best_score", 0)) }
    var state      by remember { mutableStateOf(F7State(deck = buildDeck())) }
    val scope      = rememberCoroutineScope()

    // ── Risk for current state ──────────────────
    val risk = bustRisk(state.deck, state.collectedNumbers)

    // ── Phase side-effects ──────────────────────
    LaunchedEffect(state.phase) {
        when (state.phase) {

            F7Phase.REVEALING -> {
                delay(560)   // card flip animation plays (480ms) + 80ms settle
                val card = state.lastCard ?: return@LaunchedEffect
                when (card) {
                    is F7Card.Number -> {
                        if (state.collectedNumbers.contains(card.n)) {
                            if (state.hasSecondChance) {
                                state = state.copy(phase = F7Phase.IDLE,
                                    hasSecondChance = false,
                                    message = "Second Chance verbraucht!",
                                    flashGreen = true, comboCount = state.comboCount + 1)
                                delay(300); state = state.copy(flashGreen = false)
                            } else {
                                state = state.copy(phase = F7Phase.BUSTED,
                                    bustCardN = card.n,
                                    message = "BUST! Doppelte ${card.n}!",
                                    flashRed = true, comboCount = 0)
                                delay(400); state = state.copy(flashRed = false)
                            }
                        } else {
                            val newCol  = state.collectedNumbers + card.n
                            val newHand = state.hand + card
                            val wasRisky = risk > 0.45f
                            val pop = card.n
                            if (newCol.size == 7) {
                                state = state.copy(
                                    phase = F7Phase.FLIP7_CELEBRATE,
                                    hand = newHand, collectedNumbers = newCol,
                                    lastScorePop = pop, scorePopTick = state.scorePopTick + 1,
                                    comboCount = state.comboCount + 1,
                                    closeCall = wasRisky, message = null, flashGreen = true)
                                delay(300); state = state.copy(flashGreen = false)
                            } else {
                                state = state.copy(
                                    phase = F7Phase.IDLE,
                                    hand = newHand, collectedNumbers = newCol,
                                    lastScorePop = pop, scorePopTick = state.scorePopTick + 1,
                                    comboCount = state.comboCount + 1,
                                    closeCall = wasRisky,
                                    message = if (wasRisky) null else null,
                                    flashGreen = true)
                                delay(300); state = state.copy(flashGreen = false)
                                if (wasRisky) { delay(600); state = state.copy(closeCall = false) }
                            }
                        }
                    }
                    F7Card.SecondChance ->
                        state = state.copy(phase = F7Phase.IDLE,
                            hasSecondChance = true, message = "Second Chance!",
                            flashGreen = true, comboCount = state.comboCount + 1)
                    F7Card.Freeze -> {
                        val earned = roundScore(state.hand)
                        state = state.copy(phase = F7Phase.STOOD, isFrozen = true,
                            message = "FREEZE! +$earned Punkte",
                            totalScore = state.totalScore + earned)
                    }
                    F7Card.FlipThree ->
                        state = state.copy(phase = F7Phase.IDLE,
                            message = "Flip Three!", flashGreen = true,
                            comboCount = state.comboCount + 1)
                }
            }

            F7Phase.FLIP7_CELEBRATE -> {
                delay(2000)
                state = state.copy(phase = F7Phase.IDLE,
                    totalScore = state.totalScore + 25, message = null, closeCall = false)
            }

            F7Phase.BUSTED -> {
                delay(2400)
                val nextRound = state.round + 1
                state = if (nextRound > state.totalRounds) {
                    val newBest = state.totalScore > bestScore
                    if (newBest) { prefs.edit().putInt("best_score", state.totalScore).apply(); bestScore = state.totalScore }
                    state.copy(phase = F7Phase.GAME_OVER, streak = 0, isNewRecord = newBest)
                } else state.copy(
                    deck = if (state.deck.size < 8) buildDeck() else state.deck,
                    hand = emptyList(), collectedNumbers = emptySet(),
                    hasSecondChance = false, isFrozen = false, bustCardN = null,
                    round = nextRound, phase = F7Phase.IDLE, lastCard = null,
                    message = null, streak = 0, closeCall = false, comboCount = 0)
            }

            F7Phase.STOOD -> {
                delay(1400)
                val nextRound = state.round + 1
                state = if (nextRound > state.totalRounds) {
                    val newBest = state.totalScore > bestScore
                    if (newBest) { prefs.edit().putInt("best_score", state.totalScore).apply(); bestScore = state.totalScore }
                    state.copy(phase = F7Phase.GAME_OVER, isNewRecord = newBest)
                } else state.copy(
                    deck = if (state.deck.size < 8) buildDeck() else state.deck,
                    hand = emptyList(), collectedNumbers = emptySet(),
                    hasSecondChance = false, isFrozen = false,
                    round = nextRound, phase = F7Phase.IDLE, lastCard = null,
                    message = null, streak = state.streak + 1, comboCount = 0)
            }
            else -> {}
        }
    }

    // ── Heartbeat border when risky ─────────────
    val heartbeatInf = rememberInfiniteTransition(label = "hb")
    val heartbeatAlpha by heartbeatInf.animateFloat(
        initialValue = 0f,
        targetValue  = when {
            risk > 0.65f -> 0.55f
            risk > 0.45f -> 0.28f
            else         -> 0f
        },
        animationSpec = infiniteRepeatable(
            tween(if (risk > 0.65f) 280 else 500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse),
        label = "hba"
    )

    // ── Screen flash overlay alpha ───────────────
    val flashAlpha by animateFloatAsState(
        targetValue = if (state.flashRed || state.flashGreen) 0.22f else 0f,
        animationSpec = tween(120), label = "flash"
    )
    val flashColor = if (state.flashRed) Color(0xFFE53935) else Color(0xFF4CAF50)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row {
                            listOf("F" to Color(0xFFE53935), "L" to Color(0xFFF4511E),
                                "I" to Color(0xFFFFB300), "P" to Color(0xFF7CB342),
                                " " to Color.Transparent, "7" to Color(0xFF5E35B1)
                            ).forEach { (c, col) ->
                                Text(c, color = col, fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp, letterSpacing = 2.sp)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
                )
            },
            containerColor = Color(0xFF121212)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                F7Header(state, bestScore)

                // badges
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (state.streak >= 2) F7Badge("🔥 ${state.streak}er Serie", Color(0xFFFF6D00))
                    if (state.comboCount >= 3) F7Badge("⚡ ${state.comboCount}× Combo", Color(0xFF039BE5))
                    if (state.hasSecondChance) F7Badge("⭐ Second Chance", Color(0xFFFFD600))
                    if (state.isFrozen) F7Badge("❄ Freeze", Color(0xFF00BCD4))
                }

                // ── Main card area + overlays ────────────
                Box(
                    modifier = Modifier.fillMaxWidth().height(186.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (state.phase) {
                        F7Phase.BUSTED ->
                            BustedOverlay(bustN = state.bustCardN, hand = state.hand)
                        F7Phase.GAME_OVER ->
                            GameOverOverlay(state.totalScore, state.isNewRecord, bestScore)
                        F7Phase.FLIP7_CELEBRATE ->
                            Flip7CelebrationOverlay()
                        F7Phase.REVEALING ->
                            RevealingCardFlip(card = state.lastCard)
                        else ->
                            if (state.lastCard != null) {
                                // card bounces in with spring
                                val cardScale by animateFloatAsState(
                                    1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                                    label = "cs")
                                F7CardFace(state.lastCard!!, large = true,
                                    modifier = Modifier.scale(cardScale))
                            } else {
                                F7DeckPile(remaining = state.deck.size)
                            }
                    }

                    // CLOSE CALL overlay
                    if (state.closeCall && state.phase == F7Phase.IDLE) {
                        CloseCallBanner()
                    }

                    // Floating score popup
                    key(state.scorePopTick) {
                        if (state.lastScorePop > 0 && state.phase == F7Phase.IDLE) {
                            FloatingScorePop(amount = state.lastScorePop)
                        }
                    }
                }

                // ── Risk meter ─────────────────────────
                if (state.phase == F7Phase.IDLE && state.collectedNumbers.isNotEmpty()) {
                    RiskMeter(risk = risk)
                }

                // ── Number tracker ─────────────────────
                F7NumberTracker(collected = state.collectedNumbers)

                // ── Hand row ───────────────────────────
                if (state.hand.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${state.hand.size} Karten  ·  ${roundScore(state.hand)} Punkte",
                            color = Color.White.copy(0.45f), fontSize = 11.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy((-18).dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            items(state.hand.size) { i ->
                                val rot = (i - state.hand.size / 2f) * 3.5f
                                val isBustMatch = state.bustCardN != null &&
                                        state.hand[i].let { it is F7Card.Number && it.n == state.bustCardN }
                                Box {
                                    F7CardFace(state.hand[i], large = false,
                                        modifier = Modifier.rotate(rot))
                                    if (isBustMatch) {
                                        Box(Modifier
                                            .size(70.dp, 98.dp).rotate(rot)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFE53935).copy(0.55f)),
                                            contentAlignment = Alignment.Center) {
                                            Text("✕", color = Color.White,
                                                fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                F7Actions(
                    state = state, risk = risk,
                    onFlip = {
                        if (state.phase != F7Phase.IDLE || state.deck.isEmpty()) return@F7Actions
                        val deck = state.deck.toMutableList()
                        val drawn = deck.pop()
                        state = state.copy(deck = deck, lastCard = drawn,
                            phase = F7Phase.REVEALING, message = null, closeCall = false)
                    },
                    onStand = {
                        if (state.phase != F7Phase.IDLE || state.hand.isEmpty()) return@F7Actions
                        val earned = roundScore(state.hand)
                        state = state.copy(phase = F7Phase.STOOD,
                            totalScore = state.totalScore + earned,
                            message = "+$earned Punkte gesichert!")
                    },
                    onNewGame = { state = F7State(deck = buildDeck()) }
                )
            }
        }

        // ── Heartbeat border overlay ──────────────
        if (heartbeatAlpha > 0.01f) {
            Box(Modifier.fillMaxSize()
                .border(6.dp, Color(0xFFE53935).copy(heartbeatAlpha),
                    RoundedCornerShape(0.dp)))
        }

        // ── Screen flash overlay ──────────────────
        if (flashAlpha > 0.01f) {
            Box(Modifier.fillMaxSize().background(flashColor.copy(flashAlpha)))
        }
    }
}

// ─────────────────────────────────────────
// Header
// ─────────────────────────────────────────

@Composable
private fun F7Header(state: F7State, bestScore: Int) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("RUNDE", color = Color.White.copy(0.35f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("${state.round}/${state.totalRounds}", color = Color.White,
                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BEST", color = Color.White.copy(0.35f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("$bestScore", color = Color(0xFFFFD600).copy(0.65f),
                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("PUNKTE", color = Color.White.copy(0.35f), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            // score pulses when changed
            val scoreScale by animateFloatAsState(
                1f, spring(Spring.DampingRatioMediumBouncy), label = "sc")
            Text("${state.totalScore}", color = Color(0xFFFFD600),
                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                modifier = Modifier.scale(scoreScale))
        }
    }
}

// ─────────────────────────────────────────
// Risk meter
// ─────────────────────────────────────────

@Composable
private fun RiskMeter(risk: Float) {
    val animRisk by animateFloatAsState(risk, tween(350), label = "rm")
    val color = when {
        animRisk < 0.20f -> Color(0xFF7CB342)
        animRisk < 0.40f -> Color(0xFFFFD600)
        animRisk < 0.60f -> Color(0xFFFF6D00)
        else             -> Color(0xFFE53935)
    }
    val inf = rememberInfiniteTransition(label = "ri")
    val glow by inf.animateFloat(0.7f, 1f,
        infiniteRepeatable(tween(if (animRisk > 0.5f) 300 else 600), RepeatMode.Reverse),
        label = "rg")
    val label = when {
        animRisk < 0.20f -> "✓ Sicher"
        animRisk < 0.40f -> "~ Risiko ${(animRisk * 100).toInt()}%"
        animRisk < 0.60f -> "⚠ Gefährlich ${(animRisk * 100).toInt()}%"
        else             -> "💀 ${(animRisk * 100).toInt()}% – Wage es!"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = color.copy(glow), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(0.07f))) {
            Box(Modifier.fillMaxWidth(animRisk.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(
                    listOf(Color(0xFF7CB342), Color(0xFFFFD600), color))))
        }
    }
}

// ─────────────────────────────────────────
// Floating score "+N" popup
// ─────────────────────────────────────────

@Composable
private fun FloatingScorePop(amount: Int) {
    var triggered by remember { mutableStateOf(false) }
    val offsetY by animateFloatAsState(
        if (triggered) -80f else 0f, tween(900, easing = FastOutSlowInEasing), label = "fy")
    val alpha by animateFloatAsState(
        if (triggered) 0f else 1f, tween(900), label = "fa")

    LaunchedEffect(Unit) { delay(30); triggered = true }

    Box(Modifier.fillMaxSize()) {
        Text("+$amount",
            color = Color(0xFFFFD600),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (60 + offsetY).dp)
                .alpha(alpha)
                .graphicsLayer { shadowElevation = 8f })
    }
}

// ─────────────────────────────────────────
// CLOSE CALL banner
// ─────────────────────────────────────────

@Composable
private fun CloseCallBanner() {
    val inf = rememberInfiniteTransition(label = "cc")
    val s by inf.animateFloat(1f, 1.06f,
        infiniteRepeatable(tween(250), RepeatMode.Reverse), label = "cs")
    Surface(shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFF6D00), shadowElevation = 18.dp,
        modifier = Modifier.scale(s)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text("😅", fontSize = 24.sp)
            Column {
                Text("CLOSE CALL!", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 2.sp)
                Text("Knapp überlebt!", color = Color.White.copy(0.8f), fontSize = 11.sp)
            }
        }
    }
}

// ─────────────────────────────────────────
// Card face — Flip 7 faithful design
// ─────────────────────────────────────────

@Composable
private fun F7CardFace(card: F7Card, large: Boolean, modifier: Modifier = Modifier) {
    val w: Dp = if (large) 112.dp else 70.dp
    val h: Dp = if (large) 157.dp else 98.dp
    val r     = if (large) 16.dp  else 10.dp
    val (bgTop, bgBot, accent) = when (card) {
        is F7Card.Number    -> palette(card.n)
        F7Card.SecondChance -> secondChancePalette
        F7Card.Freeze       -> freezePalette
        F7Card.FlipThree    -> flipThreePalette
    }
    Box(modifier = modifier
        .size(w, h)
        .shadow(if (large) 14.dp else 5.dp, RoundedCornerShape(r))
        .clip(RoundedCornerShape(r))
        .background(Brush.verticalGradient(listOf(bgTop, bgBot)))
        .border(if (large) 2.5.dp else 1.5.dp, accent.copy(0.6f), RoundedCornerShape(r)),
        contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(if (large) 5.dp else 3.dp)
            .border(if (large) 1.5.dp else 1.dp, Color.White.copy(0.22f),
                RoundedCornerShape(if (large) 12.dp else 7.dp)))
        when (card) {
            is F7Card.Number    -> NumberCardContent(card.n, large, accent)
            F7Card.SecondChance -> ActionCardContent("⭐","SECOND","CHANCE","SC", large, accent)
            F7Card.Freeze       -> ActionCardContent("❄","FREEZE","",     "FR", large, accent)
            F7Card.FlipThree    -> ActionCardContent("×3","FLIP","THREE", "F3", large, accent)
        }
    }
}

@Composable
private fun BoxScope.NumberCardContent(n: Int, large: Boolean, accent: Color) {
    val cf = if (large) 14.sp else 9.sp
    val mf = if (large) 72.sp else 44.sp
    val pd = if (large) 6.dp  else 4.dp
    Column(Modifier.align(Alignment.TopStart).padding(pd),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", color = Color.White, fontSize = cf,
            fontWeight = FontWeight.ExtraBold, lineHeight = cf)
        Text("●", color = accent, fontSize = (cf.value * 0.6f).sp, lineHeight = (cf.value * 0.7f).sp)
    }
    Text("$n", modifier = Modifier.align(Alignment.Center),
        color = Color.White, fontSize = mf, fontWeight = FontWeight.ExtraBold,
        style = TextStyle(fontSize = mf, fontWeight = FontWeight.ExtraBold,
            shadow = androidx.compose.ui.graphics.Shadow(
                Color.Black.copy(0.35f), androidx.compose.ui.geometry.Offset(2f, 3f), 4f)))
    Column(Modifier.align(Alignment.BottomEnd).padding(pd).rotate(180f),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", color = Color.White, fontSize = cf,
            fontWeight = FontWeight.ExtraBold, lineHeight = cf)
        Text("●", color = accent, fontSize = (cf.value * 0.6f).sp, lineHeight = (cf.value * 0.7f).sp)
    }
}

@Composable
private fun BoxScope.ActionCardContent(
    emoji: String, line1: String, line2: String,
    tag: String, large: Boolean, accent: Color
) {
    val cf = if (large) 11.sp else 7.sp
    val ef = if (large) 32.sp else 20.sp
    val lf = if (large) 11.sp else 7.5.sp
    val pd = if (large) 6.dp  else 4.dp
    Text(tag, color = Color.White, fontSize = cf, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.align(Alignment.TopStart).padding(pd))
    Text(tag, color = Color.White, fontSize = cf, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.align(Alignment.BottomEnd).padding(pd).rotate(180f))
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji, fontSize = ef, textAlign = TextAlign.Center)
        Text(line1, color = Color.White, fontWeight = FontWeight.ExtraBold,
            fontSize = lf, letterSpacing = 1.sp)
        if (line2.isNotEmpty())
            Text(line2, color = Color.White.copy(0.85f), fontWeight = FontWeight.Bold,
                fontSize = lf, letterSpacing = 1.sp)
    }
}

// ─────────────────────────────────────────
// 180° card flip reveal (THE tension moment)
// ─────────────────────────────────────────

@Composable
private fun RevealingCardFlip(card: F7Card?) {
    val density = LocalDensity.current
    var flipTrigger by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipTrigger) 180f else 0f,
        animationSpec = tween(480, easing = FastOutSlowInEasing),
        label = "flip"
    )
    LaunchedEffect(Unit) { delay(60); flipTrigger = true }

    Box(contentAlignment = Alignment.Center) {
        if (rotation <= 90f) {
            // Showing back
            F7CardBack(
                modifier = Modifier.graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density.density
                }
            )
        } else {
            // Showing face (counter-rotated so it reads correctly)
            if (card != null) {
                F7CardFace(
                    card = card, large = true,
                    modifier = Modifier.graphicsLayer {
                        rotationY = rotation - 180f
                        cameraDistance = 14f * density.density
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// Deck pile + card back
// ─────────────────────────────────────────

@Composable
private fun F7DeckPile(remaining: Int) {
    Box(contentAlignment = Alignment.Center) {
        listOf(8.dp to 6.dp, 4.dp to 3.dp, 0.dp to 0.dp).forEach { (x, y) ->
            Box(Modifier.offset(x, y).size(112.dp, 157.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A))
                .border(2.dp, Color.White.copy(0.09f), RoundedCornerShape(16.dp)))
        }
        F7CardBack(remaining = remaining)
    }
}

@Composable
private fun F7CardBack(modifier: Modifier = Modifier, remaining: Int? = null) {
    Box(modifier.size(112.dp, 157.dp)
        .shadow(14.dp, RoundedCornerShape(16.dp))
        .clip(RoundedCornerShape(16.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF1F1F2E), Color(0xFF0A0A14))))
        .border(2.5.dp,
            Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFFFB300),
                Color(0xFF039BE5), Color(0xFF5E35B1))),
            RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(5.dp)
            .border(1.dp, Color.White.copy(0.13f), RoundedCornerShape(12.dp)))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                listOf("F" to Color(0xFFE53935), "L" to Color(0xFFFFB300),
                    "I" to Color(0xFF7CB342), "P" to Color(0xFF039BE5)).forEach { (c, col) ->
                    Text(c, color = col, fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp, letterSpacing = 2.sp)
                }
            }
            Text("7", color = Color.White, fontWeight = FontWeight.ExtraBold,
                fontSize = 52.sp, lineHeight = 52.sp)
            if (remaining != null)
                Text("$remaining cards", color = Color.White.copy(0.3f),
                    fontSize = 9.sp, fontStyle = FontStyle.Italic)
        }
    }
}

// ─────────────────────────────────────────
// Number tracker 0–12
// ─────────────────────────────────────────

@Composable
private fun F7NumberTracker(collected: Set<Int>) {
    val isFlip7 = collected.size >= 7
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        val inf = rememberInfiniteTransition(label = "nt")
        val glow by inf.animateFloat(0.7f, 1f,
            infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "ng")
        Text(
            if (isFlip7) "✦ FLIP 7 erreicht! ✦"
            else "${collected.size}/13 Zahlen · noch ${13 - collected.size} offen",
            color = if (isFlip7) Color(0xFFFFD600).copy(glow) else Color.White.copy(0.38f),
            fontSize = 10.sp, fontWeight = if (isFlip7) FontWeight.Bold else FontWeight.Normal
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..12).forEach { n ->
                val (bg, _, accent) = palette(n)
                val hit = n in collected
                val bubbleScale by animateFloatAsState(
                    if (hit) 1.18f else 1f,
                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                    label = "b$n")
                Box(Modifier.scale(bubbleScale).size(24.dp).clip(CircleShape)
                    .background(if (hit) bg else Color.White.copy(0.06f))
                    .border(1.5.dp, if (hit) accent else Color.White.copy(0.09f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("$n",
                        color = if (hit) Color.White else Color.White.copy(0.18f),
                        fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// Overlays
// ─────────────────────────────────────────

@Composable
private fun BustedOverlay(bustN: Int?, hand: List<F7Card>) {
    val shakeInf = rememberInfiniteTransition(label = "shk")
    val shakeX by shakeInf.animateFloat(-7f, 7f,
        infiniteRepeatable(tween(70, easing = LinearEasing), RepeatMode.Reverse), label = "sx")

    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.graphicsLayer { translationX = shakeX }) {

        if (bustN != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                // Card already in hand
                Box(contentAlignment = Alignment.Center) {
                    F7CardFace(F7Card.Number(bustN), large = false)
                    Box(Modifier.size(70.dp, 98.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE53935).copy(0.4f)),
                        contentAlignment = Alignment.Center) {
                        Text("IN\nHAND", color = Color.White, fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("=", color = Color(0xFFE53935), fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("BUST", color = Color(0xFFE53935), fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
                // New duplicate
                Box(contentAlignment = Alignment.Center) {
                    F7CardFace(F7Card.Number(bustN), large = false)
                    Box(Modifier.size(70.dp, 98.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE53935).copy(0.4f)),
                        contentAlignment = Alignment.Center) {
                        Text("NEUE\nKARTE", color = Color.White, fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFB71C1C),
            shadowElevation = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
                Text("💥", fontSize = 28.sp)
                Column {
                    Text("BUST!", color = Color.White, fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp, letterSpacing = 3.sp)
                    if (bustN != null)
                        Text("Doppelte $bustN eliminiert dich!", color = Color.White.copy(0.7f),
                            fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Flip7CelebrationOverlay() {
    val inf = rememberInfiniteTransition(label = "f7c")
    val scale by inf.animateFloat(1f, 1.06f,
        infiniteRepeatable(tween(280), RepeatMode.Reverse), label = "f7s")
    val rot by inf.animateFloat(-3f, 3f,
        infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "f7r")
    Surface(shape = RoundedCornerShape(18.dp), shadowElevation = 22.dp,
        modifier = Modifier.scale(scale).rotate(rot).fillMaxWidth(0.9f)) {
        Box(Modifier.background(Brush.horizontalGradient(listOf(
            Color(0xFFE53935), Color(0xFFFFB300), Color(0xFF7CB342), Color(0xFF039BE5), Color(0xFF5E35B1)
        ))).padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("🎆", fontSize = 34.sp)
                Text("FLIP  7!", color = Color.White, fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp, letterSpacing = 4.sp)
                Text("+25 Punkte Bonus!", color = Color.White.copy(0.9f),
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, isNewRecord: Boolean, bestScore: Int) {
    val inf = rememberInfiniteTransition(label = "goc")
    val alpha by inf.animateFloat(0.85f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ga")
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF1E1E1E),
        shadowElevation = 18.dp, modifier = Modifier.fillMaxWidth(0.92f).alpha(alpha)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (isNewRecord) "🏆" else "🎉", fontSize = 38.sp)
            Text("SPIEL VORBEI", color = Color(0xFFFFD600),
                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 2.sp)
            Text("$score", color = Color.White,
                fontWeight = FontWeight.ExtraBold, fontSize = 54.sp)
            if (isNewRecord) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFD600).copy(0.15f)) {
                    Text("🏆 NEUER REKORD!", color = Color(0xFFFFD600),
                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                }
            } else {
                Text("Rekord: $bestScore", color = Color.White.copy(0.38f), fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────
// Badge + Actions
// ─────────────────────────────────────────

@Composable
private fun F7Badge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(0.18f)) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}

@Composable
private fun F7Actions(
    state: F7State, risk: Float,
    onFlip: () -> Unit, onStand: () -> Unit, onNewGame: () -> Unit
) {
    when (state.phase) {
        F7Phase.GAME_OVER -> Button(
            onClick = onNewGame,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Nochmal spielen", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        F7Phase.BUSTED, F7Phase.STOOD, F7Phase.REVEALING, F7Phase.FLIP7_CELEBRATE ->
            Box(Modifier.fillMaxWidth().height(56.dp), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE53935), modifier = Modifier.size(30.dp))
            }

        else -> {
            val canAct = state.phase == F7Phase.IDLE && state.deck.isNotEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // FLIP button — pulses faster the higher the risk
                    val inf = rememberInfiniteTransition(label = "fb")
                    val pulse by inf.animateFloat(1f,
                        when { risk > 0.6f -> 1.06f; risk > 0.35f -> 1.03f; else -> 1.015f },
                        infiniteRepeatable(
                            tween(when { risk > 0.6f -> 300; risk > 0.35f -> 500; else -> 800 }),
                            RepeatMode.Reverse),
                        label = "fp")
                    // button color shifts toward danger red
                    val btnColor = when {
                        risk > 0.6f  -> Color(0xFFB71C1C)
                        risk > 0.35f -> Color(0xFFE53935)
                        else         -> Color(0xFFE53935)
                    }
                    Button(
                        onClick = onFlip, enabled = canAct,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = btnColor,
                            disabledContainerColor = btnColor.copy(0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.6f).height(56.dp).scale(if (canAct) pulse else 1f)
                    ) {
                        Text("🃏  FLIP!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = onStand, enabled = canAct && state.hand.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E1E1E),
                            disabledContainerColor = Color(0xFF1E1E1E).copy(0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("STOP", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White)
                    }
                }
                // info row
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    if (state.hand.isNotEmpty())
                        Text("+${roundScore(state.hand)} Punkte wenn STOP",
                            color = Color.White.copy(0.35f), fontSize = 10.sp)
                    if (state.deck.size <= 5 && state.deck.isNotEmpty())
                        Text("⚠ Nur noch ${state.deck.size} Karten!",
                            color = Color(0xFFFFB300), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
