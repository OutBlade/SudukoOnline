package de.sudokuonline.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────

private enum class Suit(val symbol: String, val color: Color) {
    HEARTS("♥", Color(0xFFD32F2F)),
    DIAMONDS("♦", Color(0xFFD32F2F)),
    CLUBS("♣", Color(0xFF212121)),
    SPADES("♠", Color(0xFF212121))
}

private data class Card(val suit: Suit, val rank: Int) {
    val display: String get() = when (rank) {
        1  -> "A"
        11 -> "J"
        12 -> "Q"
        13 -> "K"
        else -> rank.toString()
    }
    val value: Int get() = when {
        rank == 1  -> 11   // Ace starts as 11; adjusted later
        rank >= 10 -> 10
        else       -> rank
    }
    // Unicode playing card character (U+1F0A1 … U+1F0DE)
    val unicodeChar: String get() {
        val base = when (suit) {
            Suit.SPADES   -> 0x1F0A0
            Suit.HEARTS   -> 0x1F0B0
            Suit.DIAMONDS -> 0x1F0C0
            Suit.CLUBS    -> 0x1F0D0
        }
        val offset = when (rank) {
            1        -> 1
            in 2..9  -> rank
            10       -> 0xA
            11       -> 0xB  // Jack
            12       -> 0xD  // Queen (0xC = Knight, skip)
            13       -> 0xE  // King
            else     -> 1
        }
        return String(Character.toChars(base + offset))
    }
}

private enum class BlackjackPhase {
    BETTING, DEALING, PLAYER_TURN, DEALER_TURN, RESULT
}

private enum class GameResult {
    WIN, LOSE, PUSH, BLACKJACK, BUST
}

private data class BlackjackState(
    val deck: List<Card> = emptyList(),
    val playerHand: List<Card> = emptyList(),
    val dealerHand: List<Card> = emptyList(),
    val phase: BlackjackPhase = BlackjackPhase.BETTING,
    val chips: Int = 1000,
    val bet: Int = 0,
    val result: GameResult? = null,
    val dealerRevealed: Boolean = false,
    val dealerThinking: Boolean = false
)

// ─────────────────────────────────────────
// Hand-value calculation
// ─────────────────────────────────────────

private fun handValue(cards: List<Card>): Int {
    var total = cards.sumOf { it.value }
    var aces = cards.count { it.rank == 1 }
    while (total > 21 && aces > 0) {
        total -= 10
        aces--
    }
    return total
}

private fun isBlackjack(cards: List<Card>): Boolean =
    cards.size == 2 && handValue(cards) == 21

// ─────────────────────────────────────────
// Deck
// ─────────────────────────────────────────

private fun freshDeck(): List<Card> =
    Suit.entries.flatMap { suit -> (1..13).map { rank -> Card(suit, rank) } }.shuffled()

private fun MutableList<Card>.deal(): Card = removeFirst()

// ─────────────────────────────────────────
// Screen
// ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackjackScreen(onBackClick: () -> Unit) {
    var state by remember { mutableStateOf(BlackjackState(deck = freshDeck())) }

    // Dealer auto-play effect
    LaunchedEffect(state.phase) {
        if (state.phase == BlackjackPhase.DEALER_TURN) {
            delay(600)
            var current = state
            current = current.copy(dealerRevealed = true, dealerThinking = false)
            state = current
            delay(700)

            // Dealer draws until 17+
            while (handValue(current.dealerHand) < 17) {
                current = current.copy(dealerThinking = true)
                state = current
                delay(900)
                val newCard = current.deck.toMutableList().also { }
                val mDeck = current.deck.toMutableList()
                val drawn = mDeck.deal()
                current = current.copy(
                    deck = mDeck,
                    dealerHand = current.dealerHand + drawn,
                    dealerThinking = false
                )
                state = current
                delay(500)
            }

            // Determine result
            val playerScore = handValue(current.playerHand)
            val dealerScore = handValue(current.dealerHand)
            val result = when {
                dealerScore > 21                    -> GameResult.WIN
                dealerScore == playerScore          -> GameResult.PUSH
                playerScore > dealerScore           -> GameResult.WIN
                else                                -> GameResult.LOSE
            }
            val winnings = when (result) {
                GameResult.WIN  -> current.bet
                GameResult.PUSH -> 0
                else            -> -current.bet
            }
            state = current.copy(
                phase = BlackjackPhase.RESULT,
                result = result,
                chips = current.chips + winnings
            )
        }
    }

    // ── UI ──────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blackjack", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        state = BlackjackState(deck = freshDeck())
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Neu starten")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF2E7D32)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chips & Bet info
            ChipsBar(chips = state.chips, bet = state.bet)

            // Dealer section
            HandSection(
                label = "Dealer",
                cards = state.dealerHand,
                revealed = state.dealerRevealed || state.phase == BlackjackPhase.RESULT,
                thinking = state.dealerThinking,
                showScore = state.dealerRevealed || state.phase == BlackjackPhase.RESULT
            )

            Spacer(modifier = Modifier.weight(1f))

            // Result banner
            if (state.phase == BlackjackPhase.RESULT) {
                ResultBanner(result = state.result, bet = state.bet)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Player section
            HandSection(
                label = "Du",
                cards = state.playerHand,
                revealed = true,
                thinking = false,
                showScore = state.playerHand.isNotEmpty()
            )

            // Action buttons
            ActionArea(
                state = state,
                onBetChange = { amount ->
                    val maxBet = state.chips
                    val newBet = (state.bet + amount).coerceIn(0, maxBet)
                    state = state.copy(bet = newBet)
                },
                onDeal = {
                    if (state.bet > 0) {
                        val deck = (if (state.deck.size < 15) freshDeck() else state.deck).toMutableList()
                        val playerHand = listOf(deck.deal(), deck.deal())
                        val dealerHand = listOf(deck.deal(), deck.deal())
                        val chips = state.chips - state.bet

                        if (isBlackjack(playerHand)) {
                            val blackjackWin = (state.bet * 1.5).toInt()
                            state = state.copy(
                                deck = deck,
                                playerHand = playerHand,
                                dealerHand = dealerHand,
                                chips = chips + state.bet + blackjackWin,
                                phase = BlackjackPhase.RESULT,
                                result = GameResult.BLACKJACK,
                                dealerRevealed = true
                            )
                        } else {
                            state = state.copy(
                                deck = deck,
                                playerHand = playerHand,
                                dealerHand = dealerHand,
                                chips = chips,
                                phase = BlackjackPhase.PLAYER_TURN,
                                result = null,
                                dealerRevealed = false
                            )
                        }
                    }
                },
                onHit = {
                    val deck = state.deck.toMutableList()
                    val newHand = state.playerHand + deck.deal()
                    val score = handValue(newHand)
                    if (score > 21) {
                        state = state.copy(
                            deck = deck,
                            playerHand = newHand,
                            phase = BlackjackPhase.RESULT,
                            result = GameResult.BUST,
                            dealerRevealed = true
                        )
                    } else {
                        state = state.copy(deck = deck, playerHand = newHand)
                    }
                },
                onStand = {
                    state = state.copy(phase = BlackjackPhase.DEALER_TURN)
                },
                onDouble = {
                    val extraBet = minOf(state.bet, state.chips)
                    val deck = state.deck.toMutableList()
                    val newHand = state.playerHand + deck.deal()
                    val score = handValue(newHand)
                    val newChips = state.chips - extraBet
                    val newBet = state.bet + extraBet
                    if (score > 21) {
                        state = state.copy(
                            deck = deck,
                            playerHand = newHand,
                            chips = newChips,
                            bet = newBet,
                            phase = BlackjackPhase.RESULT,
                            result = GameResult.BUST,
                            dealerRevealed = true
                        )
                    } else {
                        state = state.copy(
                            deck = deck,
                            playerHand = newHand,
                            chips = newChips,
                            bet = newBet,
                            phase = BlackjackPhase.DEALER_TURN
                        )
                    }
                },
                onNextRound = {
                    val nextBet = if (state.chips > 0) minOf(state.bet, state.chips) else 0
                    state = BlackjackState(
                        deck = if (state.deck.size < 15) freshDeck() else state.deck,
                        chips = state.chips,
                        bet = nextBet,
                        phase = BlackjackPhase.BETTING
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────
// Chips Bar
// ─────────────────────────────────────────

@Composable
private fun ChipsBar(chips: Int, bet: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B5E20).copy(alpha = 0.6f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text("Chips", color = Color(0xFFA5D6A7), fontSize = 12.sp)
            Text("$chips", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Einsatz", color = Color(0xFFA5D6A7), fontSize = 12.sp)
            Text("$bet", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

// ─────────────────────────────────────────
// Hand section
// ─────────────────────────────────────────

@Composable
private fun HandSection(
    label: String,
    cards: List<Card>,
    revealed: Boolean,
    thinking: Boolean,
    showScore: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (showScore && cards.isNotEmpty()) {
                val score = if (revealed) handValue(cards) else handValue(listOf(cards.first()))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (handValue(cards) > 21) Color(0xFFD32F2F) else Color(0xFF1B5E20)
                ) {
                    Text(
                        text = if (revealed) "$score" else "$score+",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (thinking) {
                val infiniteTransition = rememberInfiniteTransition(label = "dots")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "alpha"
                )
                Text("...", color = Color.White.copy(alpha = alpha), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
            cards.forEachIndexed { index, card ->
                val isHidden = !revealed && index == 1
                PlayingCard(card = card, faceDown = isHidden, rotation = (index - cards.size / 2f) * 3f)
            }
        }
    }
}

// ─────────────────────────────────────────
// Card composable
// ─────────────────────────────────────────

@Composable
private fun PlayingCard(card: Card, faceDown: Boolean, rotation: Float = 0f) {
    val animRot by animateFloatAsState(
        targetValue = if (faceDown) 180f else 0f,
        animationSpec = tween(400),
        label = "flip"
    )

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 96.dp)
            .rotate(rotation)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(if (animRot > 90f) Color(0xFF1565C0) else Color.White)
            .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (animRot <= 90f) {
            // Corner labels
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(card.display, color = card.suit.color, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
                    Text(card.suit.symbol, color = card.suit.color, fontSize = 11.sp, lineHeight = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text(card.suit.symbol, color = card.suit.color, fontSize = 11.sp, lineHeight = 11.sp)
                    Text(card.display, color = card.suit.color, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
                }
            }
            // Unicode playing card character centered
            Text(
                text = card.unicodeChar,
                color = card.suit.color,
                fontSize = 38.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Card back: blue radial gradient + inner border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)))
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// Result Banner
// ─────────────────────────────────────────

@Composable
private fun ResultBanner(result: GameResult?, bet: Int) {
    val (text, color) = when (result) {
        GameResult.WIN       -> "Du gewinnst! +$bet" to Color(0xFF43A047)
        GameResult.BLACKJACK -> "Blackjack! +${(bet * 1.5).toInt()}" to Color(0xFFFFD600)
        GameResult.LOSE      -> "Verloren! -$bet" to Color(0xFFD32F2F)
        GameResult.BUST      -> "Überkauft! -$bet" to Color(0xFFD32F2F)
        GameResult.PUSH      -> "Unentschieden" to Color(0xFF90A4AE)
        null                 -> "" to Color.Transparent
    }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "banner"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color,
        shadowElevation = 8.dp,
        modifier = Modifier.scale(scale)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
        )
    }
}

// ─────────────────────────────────────────
// Action Area
// ─────────────────────────────────────────

@Composable
private fun ActionArea(
    state: BlackjackState,
    onBetChange: (Int) -> Unit,
    onDeal: () -> Unit,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onDouble: () -> Unit,
    onNextRound: () -> Unit
) {
    when (state.phase) {
        BlackjackPhase.BETTING -> BettingControls(
            bet = state.bet,
            chips = state.chips,
            onBetChange = onBetChange,
            onDeal = onDeal
        )

        BlackjackPhase.PLAYER_TURN -> PlayerControls(
            canDouble = state.chips >= state.bet && state.playerHand.size == 2,
            onHit = onHit,
            onStand = onStand,
            onDouble = onDouble
        )

        BlackjackPhase.RESULT -> {
            val broke = state.chips == 0
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (broke) {
                    Text("Keine Chips mehr!", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { onNextRound() /* resets chips via BlackjackState default */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Neu starten (1000 Chips)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNextRound,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Nächste Runde", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        else -> {
            // DEALING / DEALER_TURN: no buttons
            Box(modifier = Modifier.height(52.dp))
        }
    }
}

@Composable
private fun BettingControls(bet: Int, chips: Int, onBetChange: (Int) -> Unit, onDeal: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Setze deinen Einsatz", color = Color(0xFFA5D6A7), fontSize = 14.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 25, 50, 100).forEach { amount ->
                OutlinedButton(
                    onClick = { onBetChange(amount) },
                    enabled = bet + amount <= chips,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("+$amount", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onBetChange(-bet) },
                enabled = bet > 0,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF9A9A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A).copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Zurücksetzen", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onDeal,
                enabled = bet > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Austeilen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlayerControls(
    canDouble: Boolean,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onDouble: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onHit,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Text("Karte", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onStand,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Text("Passen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        if (canDouble) {
            Button(
                onClick = onDouble,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text("Doppeln", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
