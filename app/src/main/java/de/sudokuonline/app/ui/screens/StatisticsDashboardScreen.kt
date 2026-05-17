package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.repository.*
import de.sudokuonline.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDashboardScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val statisticsRepository = remember { StatisticsRepository.getInstance(context) }
    
    val statistics by statisticsRepository.statistics.collectAsState()
    val gameHistory by statisticsRepository.gameHistory.collectAsState()
    val extendedStats = remember(gameHistory) { statisticsRepository.getExtendedStats() }
    val winRateTrend = remember(gameHistory) { statisticsRepository.getWinRateTrend() }
    val todayStats = remember(gameHistory) { statisticsRepository.getTodayStats() }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Übersicht", "Sudoku", "TicTacToe", "Mühle")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiken") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content
            when (selectedTab) {
                0 -> OverviewTab(statistics, extendedStats, todayStats, winRateTrend, gameHistory)
                1 -> SudokuStatsTab(statistics)
                2 -> TicTacToeStatsTab(statistics)
                3 -> MuhleStatsTab(statistics)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    stats: GameStatistics,
    extendedStats: ExtendedStats,
    todayStats: TodayStats,
    winRateTrend: Float,
    history: List<GameHistoryItem>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Today's Summary Card
        item {
            TodaySummaryCard(todayStats)
        }
        
        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    title = "Gesamt",
                    value = "${stats.totalGamesPlayed}",
                    subtitle = "Spiele",
                    icon = Icons.Default.SportsEsports,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Siege",
                    value = "${stats.totalGamesWon}",
                    subtitle = "${(stats.totalGamesWon.toFloat() / maxOf(1, stats.totalGamesPlayed) * 100).toInt()}%",
                    icon = Icons.Default.EmojiEvents,
                    color = SuccessColor,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Spielzeit",
                    value = "${extendedStats.totalPlayTimeMinutes}",
                    subtitle = "Minuten",
                    icon = Icons.Default.Timer,
                    color = InfoColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Weekly Activity Chart
        item {
            WeeklyActivityChart(
                weeklyGames = extendedStats.weeklyGames,
                weeklyWins = extendedStats.weeklyWins
            )
        }
        
        // Win Rate Trend
        item {
            WinRateTrendCard(winRateTrend, stats)
        }
        
        // Game Type Distribution
        item {
            GameTypeDistributionCard(stats)
        }
        
        // Achievements Summary
        item {
            AchievementsSummaryCard(extendedStats, stats)
        }
        
        // Recent Games
        item {
            RecentGamesCard(history.take(5))
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TodaySummaryCard(todayStats: TodayStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Heute",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TodayStatItem(
                    value = todayStats.gamesPlayed.toString(),
                    label = "Spiele"
                )
                TodayStatItem(
                    value = todayStats.gamesWon.toString(),
                    label = "Siege"
                )
                TodayStatItem(
                    value = "${todayStats.totalTime / 60}m",
                    label = "Zeit"
                )
                TodayStatItem(
                    value = if (todayStats.gamesPlayed > 0) 
                        "${(todayStats.gamesWon * 100 / todayStats.gamesPlayed)}%" 
                    else "0%",
                    label = "Siegrate"
                )
            }
        }
    }
}

@Composable
private fun TodayStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivityChart(
    weeklyGames: List<Int>,
    weeklyWins: List<Int>
) {
    val days = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val maxGames = (weeklyGames.maxOrNull() ?: 1).coerceAtLeast(1)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Wochenaktivität",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyGames.forEachIndexed { index, games ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Games bar
                        val barHeight = (games.toFloat() / maxGames * 80).dp
                        val winHeight = (weeklyWins[index].toFloat() / maxGames * 80).dp
                        
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(barHeight.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(winHeight.coerceAtLeast(0.dp))
                                    .align(Alignment.BottomCenter)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = days[index],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gespielt", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gewonnen", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WinRateTrendCard(trend: Float, stats: GameStatistics) {
    val isPositive = trend >= 0
    val trendColor = if (isPositive) SuccessColor else ErrorColor
    val trendIcon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Siegrate Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Letzte 10 Spiele vs. vorherige 10",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    trendIcon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "${if (isPositive) "+" else ""}${(trend * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }
        }
    }
}

@Composable
private fun GameTypeDistributionCard(stats: GameStatistics) {
    val total = stats.totalGamesPlayed.coerceAtLeast(1)
    val sudokuPercent = (stats.sudokuGamesPlayed * 100f / total)
    val tttPercent = (stats.ticTacToeGamesPlayed * 100f / total)
    val muhlePercent = (stats.muhleGamesPlayed * 100f / total)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Spielverteilung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Distribution bars
            GameDistributionBar("Sudoku", sudokuPercent, stats.sudokuGamesPlayed, Color(0xFF1976D2))
            Spacer(modifier = Modifier.height(12.dp))
            GameDistributionBar("TicTacToe", tttPercent, stats.ticTacToeGamesPlayed, Color(0xFF9C27B0))
            Spacer(modifier = Modifier.height(12.dp))
            GameDistributionBar("Mühle", muhlePercent, stats.muhleGamesPlayed, Color(0xFF795548))
        }
    }
}

@Composable
private fun GameDistributionBar(
    name: String,
    percent: Float,
    count: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count (${percent.toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun AchievementsSummaryCard(extendedStats: ExtendedStats, stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Highlights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HighlightItem(
                    icon = Icons.Default.AutoAwesome,
                    value = "${extendedStats.perfectGamesCount}",
                    label = "Perfekte Spiele",
                    color = Color(0xFFFFD700)
                )
                HighlightItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${stats.sudokuBestStreak}",
                    label = "Beste Serie",
                    color = Color(0xFFFF5722)
                )
                HighlightItem(
                    icon = Icons.Default.Favorite,
                    value = extendedStats.favoriteGameType,
                    label = "Lieblingsspiel",
                    color = Color(0xFFE91E63)
                )
            }
        }
    }
}

@Composable
private fun HighlightItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecentGamesCard(recentGames: List<GameHistoryItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Letzte Spiele",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recentGames.isEmpty()) {
                Text(
                    text = "Noch keine Spiele gespielt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                recentGames.forEach { game ->
                    RecentGameItem(game)
                    if (game != recentGames.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentGameItem(game: GameHistoryItem) {
    val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    val gameIcon = when (game.gameType) {
        "SUDOKU" -> Icons.Default.Grid3x3
        "TICTACTOE" -> Icons.Default.Close
        "MUHLE" -> Icons.Default.Circle
        else -> Icons.Default.SportsEsports
    }
    val gameColor = when (game.gameType) {
        "SUDOKU" -> Color(0xFF1976D2)
        "TICTACTOE" -> Color(0xFF9C27B0)
        "MUHLE" -> Color(0xFF795548)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(gameColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                gameIcon,
                contentDescription = null,
                tint = gameColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (game.gameType) {
                    "SUDOKU" -> "Sudoku ${game.difficulty}"
                    "TICTACTOE" -> "TicTacToe"
                    "MUHLE" -> "Mühle"
                    else -> game.gameType
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateFormat.format(Date(game.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Result
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                game.won -> SuccessColor.copy(alpha = 0.15f)
                game.isDraw -> WarningColor.copy(alpha = 0.15f)
                else -> ErrorColor.copy(alpha = 0.15f)
            }
        ) {
            Text(
                text = when {
                    game.won -> "Sieg"
                    game.isDraw -> "Unent."
                    else -> "Niederlage"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    game.won -> SuccessColor
                    game.isDraw -> WarningColor
                    else -> ErrorColor
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        // Time
        Text(
            text = "${game.timeSeconds / 60}:${(game.timeSeconds % 60).toString().padStart(2, '0')}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Detailed Stats Tabs
@Composable
private fun SudokuStatsTab(stats: GameStatistics) {
    val sudoku = stats
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Main Stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Sudoku Statistiken",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatRow("Spiele gespielt", "${sudoku.sudokuGamesPlayed}")
                    StatRow("Spiele gewonnen", "${sudoku.sudokuGamesWon}")
                    StatRow("Siegrate", "${(sudoku.sudokuWinRate * 100).toInt()}%")
                    StatRow("Beste Zeit", formatTime(sudoku.sudokuBestTime))
                    StatRow("Gesamtspielzeit", "${sudoku.sudokuTotalTime / 60} Min.")
                    StatRow("Aktuelle Serie", "${sudoku.sudokuCurrentStreak}")
                    StatRow("Beste Serie", "${sudoku.sudokuBestStreak}")
                }
            }
        }
        
        // Difficulty Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Nach Schwierigkeit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    listOf("EASY" to "Einfach", "MEDIUM" to "Mittel", "HARD" to "Schwer", "EXPERT" to "Experte").forEach { (key, name) ->
                        val diffStats = sudoku.sudokuByDifficulty[key] ?: DifficultyStats()
                        DifficultyStatRow(name, diffStats)
                        if (key != "EXPERT") Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TicTacToeStatsTab(stats: GameStatistics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "TicTacToe Statistiken",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatRow("Spiele gespielt", "${stats.ticTacToeGamesPlayed}")
                    StatRow("Spiele gewonnen", "${stats.ticTacToeGamesWon}")
                    StatRow("Unentschieden", "${stats.ticTacToeDraws}")
                    StatRow("Siegrate", "${(stats.ticTacToeWinRate * 100).toInt()}%")
                    StatRow("Aktuelle Serie", "${stats.ticTacToeCurrentStreak}")
                    StatRow("Beste Serie", "${stats.ticTacToeBestStreak}")
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MuhleStatsTab(stats: GameStatistics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Mühle Statistiken",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatRow("Spiele gespielt", "${stats.muhleGamesPlayed}")
                    StatRow("Spiele gewonnen", "${stats.muhleGamesWon}")
                    StatRow("Siegrate", "${(stats.muhleWinRate * 100).toInt()}%")
                    StatRow("Aktuelle Serie", "${stats.muhleCurrentStreak}")
                    StatRow("Beste Serie", "${stats.muhleBestStreak}")
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DifficultyStatRow(name: String, stats: DifficultyStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${stats.won}/${stats.played}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = if (stats.bestTime > 0) formatTime(stats.bestTime) else "-",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

private fun formatTime(seconds: Int): String {
    if (seconds == 0) return "-"
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}:${secs.toString().padStart(2, '0')}"
}
