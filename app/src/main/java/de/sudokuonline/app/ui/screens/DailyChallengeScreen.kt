package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.repository.DailyChallengeRepository
import de.sudokuonline.app.data.repository.DailyChallengeState
import de.sudokuonline.app.data.repository.StreakInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    onBackClick: () -> Unit,
    onStartChallenge: (difficulty: Difficulty, board: List<List<Int>>, solution: List<List<Int>>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DailyChallengeRepository.getInstance(context) }
    val challengeState by repository.challengeState.collectAsState()
    val streakInfo by repository.streakInfo.collectAsState()

    var timeUntilNext by remember { mutableLongStateOf(repository.getTimeUntilNextChallenge()) }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timeUntilNext = repository.getTimeUntilNextChallenge()
            if (timeUntilNext <= 0) {
                repository.refresh()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tägliche Herausforderung") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Date Header
            DateHeader(challengeState)

            Spacer(modifier = Modifier.height(24.dp))

            // Streak Info
            StreakCard(streakInfo)

            Spacer(modifier = Modifier.height(24.dp))

            // Challenge Card
            ChallengeCard(
                challengeState = challengeState,
                onStartChallenge = {
                    val board = repository.getBoardArray()
                    val solution = repository.getSolutionArray()
                    if (board.isNotEmpty() && solution.isNotEmpty()) {
                        onStartChallenge(challengeState.difficulty, board, solution)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Next Challenge Timer (if completed)
            if (challengeState.completed) {
                NextChallengeTimer(timeUntilNext)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stats Summary
            StatsSummary(streakInfo)
        }
    }
}

@Composable
private fun DateHeader(state: DailyChallengeState) {
    val dateFormat = remember { SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.GERMANY) }
    val today = remember { dateFormat.format(Date()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = today,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = getDifficultyLabel(state.difficulty),
            style = MaterialTheme.typography.bodyLarge,
            color = getDifficultyColor(state.difficulty)
        )
    }
}

@Composable
private fun StreakCard(streakInfo: StreakInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (streakInfo.currentStreak > 0) {
                Color(0xFFFF9800).copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current Streak
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (streakInfo.currentStreak > 0) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = streakInfo.currentStreak.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (streakInfo.currentStreak > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Aktuelle Serie",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )

            // Best Streak
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = streakInfo.bestStreak.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Beste Serie",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )

            // Total Completed
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = streakInfo.totalCompleted.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gesamt",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challengeState: DailyChallengeState,
    onStartChallenge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (challengeState.completed) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (challengeState.completed) {
                // Completed State
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Abgeschlossen!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Deine Zeit: ${formatTime(challengeState.bestTime)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Komm morgen wieder für eine neue Herausforderung!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                // Not Started State
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Heute's Sudoku",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Schwierigkeit: ${getDifficultyLabel(challengeState.difficulty)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = getDifficultyColor(challengeState.difficulty)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onStartChallenge,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Herausforderung starten",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun NextChallengeTimer(timeUntilNext: Long) {
    val hours = (timeUntilNext / (1000 * 60 * 60)).toInt()
    val minutes = ((timeUntilNext / (1000 * 60)) % 60).toInt()
    val seconds = ((timeUntilNext / 1000) % 60).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nächste Herausforderung in:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeUnit(hours, "Std")
                Text(" : ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TimeUnit(minutes, "Min")
                Text(" : ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TimeUnit(seconds, "Sek")
            }
        }
    }
}

@Composable
private fun TimeUnit(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatsSummary(streakInfo: StreakInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            icon = Icons.Default.EmojiEvents,
            value = streakInfo.totalCompleted.toString(),
            label = "Abgeschlossen"
        )
        if (streakInfo.currentStreak >= 7) {
            StatItem(
                icon = Icons.Default.Star,
                value = "Woche",
                label = "Serie!"
            )
        }
        if (streakInfo.currentStreak >= 30) {
            StatItem(
                icon = Icons.Default.Verified,
                value = "Monat",
                label = "Serie!"
            )
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun getDifficultyLabel(difficulty: Difficulty): String {
    return when (difficulty) {
        Difficulty.EASY -> "Leicht"
        Difficulty.MEDIUM -> "Mittel"
        Difficulty.HARD -> "Schwer"
        Difficulty.EXPERT -> "Experte"
    }
}

@Composable
private fun getDifficultyColor(difficulty: Difficulty): Color {
    return when (difficulty) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.MEDIUM -> Color(0xFF2196F3)
        Difficulty.HARD -> Color(0xFFFF9800)
        Difficulty.EXPERT -> Color(0xFFF44336)
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
