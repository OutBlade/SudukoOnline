package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.repository.GameHistoryItem
import de.sudokuonline.app.data.repository.GameStatistics
import de.sudokuonline.app.data.repository.StatisticsRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val statsRepo = remember { StatisticsRepository.getInstance(context) }
    val statistics by statsRepo.statistics.collectAsState()
    val gameHistory by statsRepo.gameHistory.collectAsState()
    val todayStats = remember(gameHistory) { statsRepo.getTodayStats() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Übersicht", "Sudoku", "TicTacToe", "Mühle", "Verlauf")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiken") },
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
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> OverviewTab(statistics, todayStats)
                1 -> SudokuTab(statistics)
                2 -> TicTacToeTab(statistics)
                3 -> MuhleTab(statistics)
                4 -> HistoryTab(gameHistory)
            }
        }
    }
}

@Composable
private fun OverviewTab(statistics: GameStatistics, todayStats: de.sudokuonline.app.data.repository.TodayStats) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Heute",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            value = todayStats.gamesPlayed.toString(),
                            label = "Gespielt",
                            icon = Icons.Default.PlayArrow
                        )
                        StatItem(
                            value = todayStats.gamesWon.toString(),
                            label = "Gewonnen",
                            icon = Icons.Default.EmojiEvents
                        )
                        StatItem(
                            value = formatTime(todayStats.totalTime),
                            label = "Spielzeit",
                            icon = Icons.Default.Timer
                        )
                    }
                }
            }
        }

        // Overall Stats
        item {
            Text(
                text = "Gesamtstatistik",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Spiele",
                    value = statistics.totalGamesPlayed.toString(),
                    icon = Icons.Default.SportsEsports,
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Siege",
                    value = statistics.totalGamesWon.toString(),
                    icon = Icons.Default.EmojiEvents,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Game Type Summary
        item {
            Text(
                text = "Nach Spieltyp",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            GameTypeSummaryCard(
                gameType = "Sudoku",
                played = statistics.sudokuGamesPlayed,
                won = statistics.sudokuGamesWon,
                winRate = statistics.sudokuWinRate,
                icon = Icons.Default.GridOn
            )
        }

        item {
            GameTypeSummaryCard(
                gameType = "TicTacToe",
                played = statistics.ticTacToeGamesPlayed,
                won = statistics.ticTacToeGamesWon,
                winRate = statistics.ticTacToeWinRate,
                icon = Icons.Default.Close
            )
        }

        item {
            GameTypeSummaryCard(
                gameType = "Mühle",
                played = statistics.muhleGamesPlayed,
                won = statistics.muhleGamesWon,
                winRate = statistics.muhleWinRate,
                icon = Icons.Default.Circle
            )
        }
    }
}

@Composable
private fun SudokuTab(statistics: GameStatistics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Gespielt",
                    value = statistics.sudokuGamesPlayed.toString(),
                    icon = Icons.Default.PlayArrow,
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Gewonnen",
                    value = statistics.sudokuGamesWon.toString(),
                    icon = Icons.Default.Check,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Bestzeit",
                    value = if (statistics.sudokuBestTime > 0) formatTime(statistics.sudokuBestTime) else "-",
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.secondary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Siegesserie",
                    value = statistics.sudokuBestStreak.toString(),
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFFF9800)
                )
            }
        }

        // Win Rate
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Siegquote",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { statistics.sudokuWinRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Text(
                        text = "${(statistics.sudokuWinRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // By Difficulty
        item {
            Text(
                text = "Nach Schwierigkeit",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val difficulties = listOf("EASY" to "Leicht", "MEDIUM" to "Mittel", "HARD" to "Schwer", "EXPERT" to "Experte")
        items(difficulties) { (key, label) ->
            val stats = statistics.sudokuByDifficulty[key]
            DifficultyStatRow(
                difficulty = label,
                played = stats?.played ?: 0,
                won = stats?.won ?: 0,
                bestTime = stats?.bestTime ?: 0
            )
        }
    }
}

@Composable
private fun TicTacToeTab(statistics: GameStatistics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Gespielt",
                    value = statistics.ticTacToeGamesPlayed.toString(),
                    icon = Icons.Default.PlayArrow,
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Gewonnen",
                    value = statistics.ticTacToeGamesWon.toString(),
                    icon = Icons.Default.Check,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Unentschieden",
                    value = statistics.ticTacToeDraws.toString(),
                    icon = Icons.Default.Balance,
                    color = MaterialTheme.colorScheme.secondary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Siegesserie",
                    value = statistics.ticTacToeBestStreak.toString(),
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFFF9800)
                )
            }
        }

        // Win Rate
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Siegquote",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { statistics.ticTacToeWinRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Text(
                        text = "${(statistics.ticTacToeWinRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Current Streak
        if (statistics.ticTacToeCurrentStreak > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aktuelle Siegesserie",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${statistics.ticTacToeCurrentStreak} Siege",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuhleTab(statistics: GameStatistics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Gespielt",
                    value = statistics.muhleGamesPlayed.toString(),
                    icon = Icons.Default.PlayArrow,
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Gewonnen",
                    value = statistics.muhleGamesWon.toString(),
                    icon = Icons.Default.Check,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Verloren",
                    value = (statistics.muhleGamesPlayed - statistics.muhleGamesWon).toString(),
                    icon = Icons.Default.Close,
                    color = Color(0xFFF44336)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Siegesserie",
                    value = statistics.muhleBestStreak.toString(),
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFFF9800)
                )
            }
        }

        // Win Rate
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Siegquote",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { statistics.muhleWinRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Text(
                        text = "${(statistics.muhleWinRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(gameHistory: List<GameHistoryItem>) {
    if (gameHistory.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Noch keine Spiele gespielt",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gameHistory.reversed()) { game ->
                HistoryItem(game)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun GameTypeSummaryCard(
    gameType: String,
    played: Int,
    won: Int,
    winRate: Float,
    icon: ImageVector
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gameType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$played gespielt, $won gewonnen",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "${(winRate * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    winRate >= 0.7f -> Color(0xFF4CAF50)
                    winRate >= 0.5f -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFFF44336)
                }
            )
        }
    }
}

@Composable
private fun DifficultyStatRow(
    difficulty: String,
    played: Int,
    won: Int,
    bestTime: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = difficulty,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(80.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$played gespielt, $won gewonnen",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (bestTime > 0) {
                    Text(
                        text = "Bestzeit: ${formatTime(bestTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            if (played > 0) {
                Text(
                    text = "${(won.toFloat() / played * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(game: GameHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY) }
    val gameIcon = when (game.gameType) {
        "SUDOKU" -> Icons.Default.GridOn
        "TICTACTOE" -> Icons.Default.Close
        "MUHLE" -> Icons.Default.Circle
        else -> Icons.Default.SportsEsports
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            game.won -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            game.isDraw -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            else -> Color(0xFFF44336).copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    gameIcon,
                    contentDescription = null,
                    tint = when {
                        game.won -> Color(0xFF4CAF50)
                        game.isDraw -> MaterialTheme.colorScheme.secondary
                        else -> Color(0xFFF44336)
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.gameType + if (game.difficulty.isNotEmpty()) " - ${game.difficulty}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(Date(game.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        game.won -> "Gewonnen"
                        game.isDraw -> "Unentschieden"
                        else -> "Verloren"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        game.won -> Color(0xFF4CAF50)
                        game.isDraw -> MaterialTheme.colorScheme.secondary
                        else -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = formatTime(game.timeSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}
