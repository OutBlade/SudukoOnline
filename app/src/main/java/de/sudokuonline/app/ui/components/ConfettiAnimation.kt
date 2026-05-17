package de.sudokuonline.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti Animation Component
 * Shows a celebratory confetti animation when triggered
 */
@Composable
fun ConfettiAnimation(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    colors: List<Color> = defaultConfettiColors,
    durationMillis: Int = 3000,
    onAnimationEnd: () -> Unit = {}
) {
    if (!isVisible) return

    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }
    var animationProgress by remember { mutableStateOf(0f) }

    val density = LocalDensity.current

    // Initialize particles
    LaunchedEffect(isVisible) {
        if (isVisible) {
            particles = List(particleCount) {
                ConfettiParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * -0.5f, // Start above screen
                    velocityX = (Random.nextFloat() - 0.5f) * 0.3f,
                    velocityY = Random.nextFloat() * 0.5f + 0.3f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 10f,
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 8f + 4f,
                    shape = ConfettiShape.entries[Random.nextInt(ConfettiShape.entries.size)]
                )
            }
            animationProgress = 0f
        }
    }

    // Animate
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Update particles each frame
    LaunchedEffect(progress, isVisible) {
        if (isVisible && particles.isNotEmpty()) {
            particles = particles.map { particle ->
                particle.copy(
                    x = particle.x + particle.velocityX * 0.02f,
                    y = particle.y + particle.velocityY * 0.02f,
                    velocityY = particle.velocityY + 0.005f, // Gravity
                    rotation = particle.rotation + particle.rotationSpeed
                )
            }
            animationProgress += 50f / durationMillis
            if (animationProgress >= 1f) {
                onAnimationEnd()
            }
        }
    }

    // Auto-hide after duration
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(durationMillis.toLong())
            onAnimationEnd()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val x = particle.x * canvasWidth
            val y = particle.y * canvasHeight

            // Fade out near bottom and end of animation
            val alpha = ((1f - particle.y.coerceIn(0.7f, 1f) / 0.3f) * (1f - animationProgress)).coerceIn(0f, 1f)

            if (y < canvasHeight && alpha > 0) {
                rotate(particle.rotation, pivot = Offset(x, y)) {
                    when (particle.shape) {
                        ConfettiShape.RECTANGLE -> {
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 2, y - particle.size),
                                size = Size(particle.size, particle.size * 2)
                            )
                        }
                        ConfettiShape.SQUARE -> {
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 2),
                                size = Size(particle.size, particle.size)
                            )
                        }
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = particle.color.copy(alpha = alpha),
                                radius = particle.size / 2,
                                center = Offset(x, y)
                            )
                        }
                        ConfettiShape.STAR -> {
                            // Simple star approximation with overlapping rectangles
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 4, y - particle.size / 2),
                                size = Size(particle.size / 2, particle.size)
                            )
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 4),
                                size = Size(particle.size, particle.size / 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confetti explosion from a specific point
 */
@Composable
fun ConfettiExplosion(
    isVisible: Boolean,
    originX: Float = 0.5f,
    originY: Float = 0.5f,
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    colors: List<Color> = defaultConfettiColors,
    durationMillis: Int = 2000,
    onAnimationEnd: () -> Unit = {}
) {
    if (!isVisible) return

    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }
    var animationProgress by remember { mutableStateOf(0f) }

    // Initialize particles with explosion pattern
    LaunchedEffect(isVisible) {
        if (isVisible) {
            particles = List(particleCount) {
                val angle = Random.nextFloat() * 2 * PI.toFloat()
                val speed = Random.nextFloat() * 0.8f + 0.2f
                ConfettiParticle(
                    x = originX,
                    y = originY,
                    velocityX = cos(angle) * speed * 0.5f,
                    velocityY = sin(angle) * speed * 0.5f - 0.3f, // Upward bias
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 15f,
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 10f + 5f,
                    shape = ConfettiShape.entries[Random.nextInt(ConfettiShape.entries.size)]
                )
            }
            animationProgress = 0f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "explosion")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    LaunchedEffect(progress, isVisible) {
        if (isVisible && particles.isNotEmpty()) {
            particles = particles.map { particle ->
                particle.copy(
                    x = particle.x + particle.velocityX * 0.03f,
                    y = particle.y + particle.velocityY * 0.03f,
                    velocityX = particle.velocityX * 0.98f, // Air resistance
                    velocityY = particle.velocityY + 0.008f, // Gravity
                    rotation = particle.rotation + particle.rotationSpeed,
                    rotationSpeed = particle.rotationSpeed * 0.99f
                )
            }
            animationProgress += 50f / durationMillis
            if (animationProgress >= 1f) {
                onAnimationEnd()
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(durationMillis.toLong())
            onAnimationEnd()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val x = particle.x * canvasWidth
            val y = particle.y * canvasHeight
            val alpha = (1f - animationProgress).coerceIn(0f, 1f)

            if (x > -50 && x < canvasWidth + 50 && y < canvasHeight + 50 && alpha > 0) {
                rotate(particle.rotation, pivot = Offset(x, y)) {
                    when (particle.shape) {
                        ConfettiShape.RECTANGLE -> {
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 2, y - particle.size),
                                size = Size(particle.size, particle.size * 2)
                            )
                        }
                        ConfettiShape.SQUARE -> {
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 2),
                                size = Size(particle.size, particle.size)
                            )
                        }
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = particle.color.copy(alpha = alpha),
                                radius = particle.size / 2,
                                center = Offset(x, y)
                            )
                        }
                        ConfettiShape.STAR -> {
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 4, y - particle.size / 2),
                                size = Size(particle.size / 2, particle.size)
                            )
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 4),
                                size = Size(particle.size, particle.size / 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float,
    val shape: ConfettiShape
)

private enum class ConfettiShape {
    RECTANGLE, SQUARE, CIRCLE, STAR
}

private val defaultConfettiColors = listOf(
    Color(0xFFFF6B6B), // Red
    Color(0xFFFFE66D), // Yellow
    Color(0xFF4ECDC4), // Teal
    Color(0xFF45B7D1), // Blue
    Color(0xFFFF9F43), // Orange
    Color(0xFFA55EEA), // Purple
    Color(0xFF26DE81), // Green
    Color(0xFFFF78C5), // Pink
    Color(0xFF778BEB), // Lavender
    Color(0xFF20BF6B)  // Emerald
)
