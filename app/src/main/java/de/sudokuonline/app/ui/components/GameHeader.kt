package de.sudokuonline.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.model.GameMode
import de.sudokuonline.app.ui.theme.*

@Composable
fun GameHeader(
    elapsedSeconds: Int,
    errors: Int,
    maxErrors: Int = 3,
    hintsUsed: Int = 0,
    maxHints: Int = 3,
    difficulty: Difficulty,
    gameMode: GameMode,
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Multiplayer info
    myProgress: Int = 0,
    opponentProgress: Int = 0,
    opponentName: String = ""
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Top bar with back button, timer, pause
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurueck",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Timer and difficulty
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty badge
                DifficultyBadge(difficulty = difficulty)
                
                // Timer
                TimerDisplay(
                    seconds = elapsedSeconds,
                    isPaused = isPaused
                )
                
                // Errors
                ErrorsDisplay(errors = errors, maxErrors = maxErrors)
                
                // Hints
                HintsDisplay(hintsUsed = hintsUsed, maxHints = maxHints)
            }
            
            IconButton(onClick = onPauseClick) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Fortsetzen" else "Pause",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Multiplayer progress bar (only show in competitive mode)
        if (gameMode == GameMode.COMPETITIVE && opponentName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            MultiplayerProgress(
                myProgress = myProgress,
                opponentProgress = opponentProgress,
                opponentName = opponentName
            )
        }
    }
}

@Composable
private fun TimerDisplay(
    seconds: Int,
    isPaused: Boolean
) {
    val minutes = seconds / 60
    val secs = seconds % 60
    val timeString = String.format("%02d:%02d", minutes, secs)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = timeString,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (isPaused) 
                MaterialTheme.colorScheme.onSurfaceVariant 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorsDisplay(errors: Int, maxErrors: Int = 3) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (errors > 0) ErrorColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$errors/$maxErrors",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (errors > 0) ErrorColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HintsDisplay(hintsUsed: Int, maxHints: Int) {
    val remaining = maxHints - hintsUsed
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (remaining > 0) WarningColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$remaining",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (remaining > 0) WarningColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val color = when (difficulty) {
        Difficulty.EASY -> SuccessColor
        Difficulty.MEDIUM -> InfoColor
        Difficulty.HARD -> WarningColor
        Difficulty.EXPERT -> ErrorColor
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = difficulty.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun MultiplayerProgress(
    myProgress: Int,
    opponentProgress: Int,
    opponentName: String
) {
    val totalCells = 81
    val myPercent = myProgress.toFloat() / totalCells
    val opponentPercent = opponentProgress.toFloat() / totalCells
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // My progress
        ProgressRow(
            label = "Du",
            progress = myPercent,
            color = Player1Color,
            progressText = "$myProgress / $totalCells"
        )
        
        // Opponent progress
        ProgressRow(
            label = opponentName,
            progress = opponentPercent,
            color = Player2Color,
            progressText = "$opponentProgress / $totalCells"
        )
    }
}

@Composable
private fun ProgressRow(
    label: String,
    progress: Float,
    color: Color,
    progressText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = progressText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface,
        )
    }
}
