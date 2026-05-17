package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.util.SoundManager
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class OpeningPhase {
    SHAKE,      // Chest shaking
    BURST,      // Chest bursts open
    REVEAL,     // Rewards revealed one by one
    SUMMARY     // All rewards shown
}

@Composable
fun LootboxOpeningScreen(
    lootbox: OwnedLootbox,
    rewards: List<LootboxReward>,
    onDone: () -> Unit,
    onOpenAnother: (() -> Unit)? = null
) {
    val rarityColor = Color(lootbox.rarity.colorValue)
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    var phase by remember { mutableStateOf(OpeningPhase.SHAKE) }
    var revealedCount by remember { mutableIntStateOf(0) }
    var showFlash by remember { mutableStateOf(false) }
    var sparkParticles by remember { mutableStateOf(emptyList<SparkParticle>()) }
    var burstParticles by remember { mutableStateOf(emptyList<BurstParticle>()) }

    // Initialize spark particles
    LaunchedEffect(Unit) {
        sparkParticles = List(30) {
            SparkParticle(
                angle = Random.nextFloat() * 360f,
                distance = Random.nextFloat() * 0.15f + 0.05f,
                speed = Random.nextFloat() * 2f + 1f,
                size = Random.nextFloat() * 5f + 2f,
                alpha = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    // Phase timing
    LaunchedEffect(Unit) {
        // Phase 1: Shake (2.5s) — with escalating sound
        soundManager.playLootboxShake()
        delay(1200)
        soundManager.playLootboxShake()
        delay(1300)

        // Phase 2: Burst (1.5s)
        soundManager.playLootboxBurst()
        showFlash = true
        burstParticles = List(80) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = Random.nextFloat() * 8f + 3f
            BurstParticle(
                x = 0.5f,
                y = 0.4f,
                vx = cos(angle) * speed * 0.01f,
                vy = sin(angle) * speed * 0.01f - 0.005f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 15f,
                color = listOf(rarityColor, Color.White, Color(0xFFFFD700), rarityColor.copy(alpha = 0.7f)).random(),
                size = Random.nextFloat() * 12f + 4f,
                life = 1f
            )
        }
        phase = OpeningPhase.BURST
        delay(150)
        showFlash = false
        delay(1350)

        // Phase 3: Reveal rewards one by one (~1.8s each)
        phase = OpeningPhase.REVEAL
        for (i in rewards.indices) {
            delay(1800)
            revealedCount = i + 1
            val reward = rewards[i]
            if (reward.rarity.ordinal >= RewardRarity.EPIC.ordinal) {
                soundManager.playRareReveal()
            } else {
                soundManager.playRewardReveal()
            }
        }

        // Phase 4: Summary
        delay(800)
        soundManager.playSuccess()
        phase = OpeningPhase.SUMMARY
    }

    // Animate burst particles
    val burstProgress = rememberInfiniteTransition(label = "burst_tick")
    val burstTick by burstProgress.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )

    LaunchedEffect(burstTick) {
        if (burstParticles.isNotEmpty()) {
            burstParticles = burstParticles.map {
                it.copy(
                    x = it.x + it.vx,
                    y = it.y + it.vy,
                    vy = it.vy + 0.0003f,
                    rotation = it.rotation + it.rotationSpeed,
                    life = (it.life - 0.02f).coerceAtLeast(0f)
                )
            }.filter { it.life > 0f }
        }
    }

    val shakeTransition = rememberInfiniteTransition(label = "shake_rot")
    val shakeAngle by shakeTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "angle"
    )

    // Scale pulse during shake
    val scalePulse by shakeTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow pulse
    val glowPulse by shakeTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Burst scale
    val burstScale by animateFloatAsState(
        targetValue = if (phase == OpeningPhase.BURST || phase == OpeningPhase.REVEAL || phase == OpeningPhase.SUMMARY) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
        label = "burst_scale"
    )

    // Light rays
    val rayAlpha by animateFloatAsState(
        targetValue = if (phase == OpeningPhase.BURST) 1f else if (phase == OpeningPhase.REVEAL) 0.3f else 0f,
        animationSpec = tween(500),
        label = "rays"
    )
    val rayRotation by shakeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ray_rot"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tap to skip to summary if in reveal phase
                if (phase == OpeningPhase.REVEAL) {
                    revealedCount = rewards.size
                    phase = OpeningPhase.SUMMARY
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Light rays background
        if (rayAlpha > 0f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(rayAlpha)
            ) {
                val cx = size.width / 2
                val cy = size.height * 0.4f
                for (i in 0 until 12) {
                    val angle = (i * 30f + rayRotation) * PI.toFloat() / 180f
                    val endX = cx + cos(angle) * size.width
                    val endY = cy + sin(angle) * size.height
                    drawLine(
                        color = rarityColor.copy(alpha = 0.15f),
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 30f
                    )
                }
            }
        }

        // Burst particles
        if (burstParticles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                burstParticles.forEach { p ->
                    val px = p.x * size.width
                    val py = p.y * size.height
                    rotate(p.rotation, pivot = Offset(px, py)) {
                        drawRect(
                            color = p.color.copy(alpha = p.life),
                            topLeft = Offset(px - p.size / 2, py - p.size / 2),
                            size = Size(p.size, p.size)
                        )
                    }
                }
            }
        }

        // Screen flash
        if (showFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }

        // Spark particles around chest (during shake phase)
        if (phase == OpeningPhase.SHAKE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height * 0.4f
                sparkParticles.forEach { spark ->
                    val animAngle = (spark.angle + burstTick * spark.speed * 360f) * PI.toFloat() / 180f
                    val dist = spark.distance * size.width
                    val sx = cx + cos(animAngle) * dist
                    val sy = cy + sin(animAngle) * dist
                    drawCircle(
                        color = rarityColor.copy(alpha = spark.alpha * glowPulse),
                        radius = spark.size,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            // Chest (visible during shake, scales out during burst)
            if (phase == OpeningPhase.SHAKE || burstScale > 0.01f) {
                Box(contentAlignment = Alignment.Center) {
                    // Glow behind chest
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        rarityColor.copy(alpha = glowPulse * 0.5f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = lootbox.rarity.chestEmoji,
                        fontSize = 80.sp,
                        modifier = Modifier
                            .scale(if (phase == OpeningPhase.SHAKE) scalePulse else burstScale * 1.5f)
                            .rotate(if (phase == OpeningPhase.SHAKE) shakeAngle else 0f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rarity label
                AnimatedVisibility(
                    visible = phase == OpeningPhase.SHAKE,
                    exit = fadeOut(tween(200))
                ) {
                    Text(
                        text = "${lootbox.rarity.displayName} Lootbox",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = rarityColor
                    )
                }
            }

            // Reward reveal
            if (phase == OpeningPhase.REVEAL || phase == OpeningPhase.SUMMARY) {
                Spacer(modifier = Modifier.height(16.dp))

                if (phase == OpeningPhase.REVEAL && revealedCount > 0) {
                    // Show current reward being revealed
                    val currentReward = rewards[revealedCount - 1]
                    RewardRevealCard(
                        reward = currentReward,
                        rarityColor = rarityColor
                    )
                }

                if (phase == OpeningPhase.SUMMARY) {
                    Text(
                        text = "Deine Belohnungen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // All rewards in a column
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rewards.forEach { reward ->
                            RewardSummaryRow(reward = reward)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Action buttons (summary phase)
            if (phase == OpeningPhase.SUMMARY) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenAnother != null) {
                        Button(
                            onClick = onOpenAnother,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = rarityColor
                            )
                        ) {
                            Icon(Icons.Default.Redeem, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nächste Box öffnen", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Weiter")
                    }
                }
            }

            // Skip hint during reveal
            if (phase == OpeningPhase.REVEAL) {
                Text(
                    text = "Tippe zum Überspringen",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.05f))
        }
    }
}

@Composable
private fun RewardRevealCard(
    reward: LootboxReward,
    rarityColor: Color
) {
    val rewardColor = Color(reward.rarity.colorValue)
    val isRare = reward.rarity.ordinal >= RewardRarity.EPIC.ordinal

    // Entry animation
    val entryScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "entry"
    )

    // Pulse for rare items
    val infiniteTransition = rememberInfiniteTransition(label = "reward_pulse")
    val rarePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRare) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rare_pulse"
    )
    val rareGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isRare) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rare_glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(entryScale)
    ) {
        // Glow behind icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rewardColor.copy(alpha = rareGlow * 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Text(
                text = reward.icon,
                fontSize = 56.sp,
                modifier = Modifier.scale(rarePulse)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reward name
        Text(
            text = reward.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = rewardColor,
            textAlign = TextAlign.Center
        )

        // Amount (for coins)
        if (reward.type == LootboxRewardType.COINS) {
            AnimatedCoinCounter(targetAmount = reward.amount, color = rewardColor)
        }

        // Rarity badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = rewardColor.copy(alpha = 0.2f),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = reward.rarity.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = rewardColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AnimatedCoinCounter(targetAmount: Int, color: Color) {
    var displayAmount by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetAmount) {
        val steps = 30
        val stepDelay = 600L / steps
        for (i in 1..steps) {
            displayAmount = (targetAmount * i) / steps
            delay(stepDelay)
        }
        displayAmount = targetAmount
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.MonetizationOn,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "+$displayAmount",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun RewardSummaryRow(reward: LootboxReward) {
    val rewardColor = Color(reward.rarity.colorValue)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = rewardColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(rewardColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = reward.icon, fontSize = 22.sp)
            }

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reward.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = reward.rarity.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = rewardColor
                )
            }

            // Amount for coins
            if (reward.type == LootboxRewardType.COINS) {
                Text(
                    text = "+${reward.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

private data class SparkParticle(
    val angle: Float,
    val distance: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float
)

private data class BurstParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float,
    val life: Float
)
