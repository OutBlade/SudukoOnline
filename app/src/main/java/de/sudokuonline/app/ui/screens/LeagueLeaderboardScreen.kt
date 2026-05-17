package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.repository.LeagueRepository
import de.sudokuonline.app.data.repository.PlayerLeagueData
import de.sudokuonline.app.data.repository.SeasonInfo
import de.sudokuonline.app.data.repository.League
import de.sudokuonline.app.data.repository.LeaderboardEntry as LeagueLeaderboardEntry
import de.sudokuonline.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueLeaderboardScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val leagueRepository = remember { LeagueRepository.getInstance(context) }
    
    val playerData by leagueRepository.playerData.collectAsState()
    val leaderboard by leagueRepository.leaderboard.collectAsState()
    val weeklyLeaderboard by leagueRepository.weeklyLeaderboard.collectAsState()
    val seasonInfo by leagueRepository.seasonInfo.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Global", "Wöchentlich", "Ligen")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rangliste") },
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
            // Player Stats Card
            PlayerLeagueCard(
                playerData = playerData,
                globalRank = leagueRepository.getPlayerRank(),
                weeklyRank = leagueRepository.getWeeklyRank(),
                seasonInfo = seasonInfo
            )
            
            // Tab Row
            TabRow(
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
            
            // Content
            when (selectedTab) {
                0 -> GlobalLeaderboardTab(leaderboard, playerData)
                1 -> WeeklyLeaderboardTab(weeklyLeaderboard, playerData, seasonInfo)
                2 -> LeaguesInfoTab(playerData)
            }
        }
    }
}

@Composable
private fun PlayerLeagueCard(
    playerData: PlayerLeagueData,
    globalRank: Int,
    weeklyRank: Int,
    seasonInfo: SeasonInfo
) {
    val leagueColor = Color(playerData.league.color)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            leagueColor.copy(alpha = 0.3f),
                            leagueColor.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // League Badge and Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // League Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(leagueColor.copy(alpha = 0.2f))
                                .border(2.dp, leagueColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = playerData.league.icon,
                                fontSize = 28.sp
                            )
                        }
                        
                        Column {
                            Text(
                                text = playerData.league.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = leagueColor
                            )
                            Text(
                                text = "${playerData.rating} Punkte",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Progress to next league
                    Column(horizontalAlignment = Alignment.End) {
                        val nextLeague = League.entries.find { it.tier == playerData.league.tier + 1 }
                        if (nextLeague != null) {
                            val requiredRating = getRequiredRating(nextLeague)
                            val currentProgress = getLeagueProgress(playerData.rating, playerData.league)
                            
                            Text(
                                text = "Nächste Liga",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${requiredRating - playerData.rating} Pkt.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // League Progress Bar
                val progress = getLeagueProgress(playerData.rating, playerData.league)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = leagueColor,
                    trackColor = leagueColor.copy(alpha = 0.2f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("Global", if (globalRank > 0) "#$globalRank" else "-")
                    StatColumn("Woche", if (weeklyRank > 0) "#$weeklyRank" else "-")
                    StatColumn("Siege", "${playerData.totalWins}")
                    StatColumn("Siegrate", "${if (playerData.totalGames > 0) (playerData.totalWins * 100 / playerData.totalGames) else 0}%")
                }
                
                // Season Info
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Saison: ${seasonInfo.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "${seasonInfo.daysRemaining} Tage übrig",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GlobalLeaderboardTab(
    leaderboard: List<LeagueLeaderboardEntry>,
    playerData: PlayerLeagueData
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Top 3 Podium
        if (leaderboard.size >= 3) {
            item {
                PodiumCard(leaderboard.take(3))
            }
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Rest of leaderboard
        itemsIndexed(leaderboard.drop(3)) { index, entry ->
            LeaderboardEntryCard(
                rank = index + 4,
                entry = entry,
                isCurrentPlayer = false // Would need player ID comparison
            )
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun WeeklyLeaderboardTab(
    leaderboard: List<LeagueLeaderboardEntry>,
    playerData: PlayerLeagueData,
    seasonInfo: SeasonInfo
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Weekly Rewards Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFFFD700)
                        )
                        Text(
                            text = "Wöchentliche Belohnungen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Top 10: 500 Coins | Top 50: 200 Coins | Top 100: 100 Coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Leaderboard
        itemsIndexed(leaderboard) { index, entry ->
            LeaderboardEntryCard(
                rank = index + 1,
                entry = entry,
                isCurrentPlayer = false,
                showWeeklyPoints = true
            )
        }
        
        if (leaderboard.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Noch keine Einträge diese Woche",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LeaguesInfoTab(playerData: PlayerLeagueData) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        item {
            Text(
                text = "Liga-System",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // All leagues
        League.entries.reversed().forEach { league ->
            item {
                LeagueInfoCard(
                    league = league,
                    isCurrentLeague = league == playerData.league,
                    playerRating = playerData.rating
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Rewards info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Saison-Belohnungen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    listOf(
                        "Großmeister" to "5000 Coins + Exklusives Theme",
                        "Meister" to "3000 Coins + Seltenes Theme",
                        "Diamant" to "2000 Coins",
                        "Platin" to "1500 Coins",
                        "Gold" to "1000 Coins",
                        "Silber" to "500 Coins",
                        "Bronze" to "250 Coins"
                    ).forEach { (league, reward) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = league,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = reward,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PodiumCard(top3: List<LeagueLeaderboardEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd place
            if (top3.size > 1) {
                PodiumItem(
                    entry = top3[1],
                    rank = 2,
                    height = 100.dp,
                    color = Color(0xFFC0C0C0)
                )
            }
            
            // 1st place
            PodiumItem(
                entry = top3[0],
                rank = 1,
                height = 120.dp,
                color = Color(0xFFFFD700)
            )
            
            // 3rd place
            if (top3.size > 2) {
                PodiumItem(
                    entry = top3[2],
                    rank = 3,
                    height = 80.dp,
                    color = Color(0xFFCD7F32)
                )
            }
        }
    }
}

@Composable
private fun PodiumItem(
    entry: LeagueLeaderboardEntry,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        
        Text(
            text = "${entry.rating}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Podium block
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    else -> "🥉"
                },
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    rank: Int,
    entry: LeagueLeaderboardEntry,
    isCurrentPlayer: Boolean,
    showWeeklyPoints: Boolean = false
) {
    val league = try { League.valueOf(entry.league) } catch (e: Exception) { League.BRONZE }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.width(40.dp)
            )
            
            // League Icon
            Text(
                text = league.icon,
                fontSize = 20.sp
            )
            
            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = league.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(league.color)
                )
            }
            
            // Points
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (showWeeklyPoints) "${entry.weeklyPoints}" else "${entry.rating}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (showWeeklyPoints) "Woche" else "Rating",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LeagueInfoCard(
    league: League,
    isCurrentLeague: Boolean,
    playerRating: Int
) {
    val leagueColor = Color(league.color)
    val requiredRating = getRequiredRating(league)
    val isUnlocked = playerRating >= requiredRating
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentLeague) 
                leagueColor.copy(alpha = 0.2f)
            else if (!isUnlocked)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrentLeague) BorderStroke(2.dp, leagueColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // League Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(leagueColor.copy(alpha = if (isUnlocked) 0.2f else 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) league.icon else "🔒",
                    fontSize = 24.sp
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = league.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) leagueColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ab $requiredRating Punkte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isCurrentLeague) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = leagueColor
                ) {
                    Text(
                        text = "Aktuell",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun getRequiredRating(league: League): Int {
    return when (league) {
        League.BRONZE -> 0
        League.SILVER -> 500
        League.GOLD -> 1000
        League.PLATINUM -> 1500
        League.DIAMOND -> 2000
        League.MASTER -> 2500
        League.GRANDMASTER -> 3000
    }
}

private fun getLeagueProgress(rating: Int, currentLeague: League): Float {
    val currentMin = getRequiredRating(currentLeague)
    val nextLeague = League.entries.find { it.tier == currentLeague.tier + 1 }
    val nextMin = nextLeague?.let { getRequiredRating(it) } ?: (currentMin + 500)
    
    return ((rating - currentMin).toFloat() / (nextMin - currentMin)).coerceIn(0f, 1f)
}
