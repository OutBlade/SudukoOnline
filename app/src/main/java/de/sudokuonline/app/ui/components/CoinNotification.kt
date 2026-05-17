package de.sudokuonline.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.repository.CoinReason
import kotlinx.coroutines.delay

/**
 * Data class for coin notification
 */
data class CoinNotificationData(
    val amount: Int,
    val reason: CoinReason,
    val id: Long = System.currentTimeMillis()
)

/**
 * Animated coin notification that appears at the top of the screen
 */
@Composable
fun CoinNotification(
    notification: CoinNotificationData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(notification) {
        if (notification != null) {
            visible = true
            delay(2500) // Show for 2.5 seconds
            visible = false
            delay(300) // Wait for animation
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = visible && notification != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        notification?.let { data ->
            CoinNotificationContent(
                amount = data.amount,
                reason = data.reason
            )
        }
    }
}

@Composable
private fun CoinNotificationContent(
    amount: Int,
    reason: CoinReason
) {
    // Pulse animation for the coin icon
    val infiniteTransition = rememberInfiniteTransition(label = "coin_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .padding(horizontal = 32.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA000)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated coin icon
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .scale(scale)
                )
                
                Column {
                    Text(
                        text = "+$amount Coins",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = reason.displayName,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * Composable that wraps content and shows coin notifications
 */
@Composable
fun CoinNotificationHost(
    notification: CoinNotificationData?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        
        // Notification at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            CoinNotification(
                notification = notification,
                onDismiss = onDismiss
            )
        }
    }
}
