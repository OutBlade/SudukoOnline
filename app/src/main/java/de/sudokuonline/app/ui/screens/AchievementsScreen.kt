package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.repository.Achievement
import de.sudokuonline.app.data.repository.AchievementCategory
import de.sudokuonline.app.data.repository.AchievementsRepository
import de.sudokuonline.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val achievementsRepository = remember { AchievementsRepository.getInstance(context) }
    
    val unlockedAchievements by achievementsRepository.unlockedAchievements.collectAsState()
    val progress by achievementsRepository.progress.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    
    // Calculate stats
    val totalAchievements = Achievement.entries.size
    val unlockedCount = unlockedAchievements.size
    val totalPoints = Achievement.entries
        .filter { unlockedAchievements.contains(it.id) }
        .sumOf { it.points }
    
    // Group achievements by category
    val achievementsByCategory = Achievement.entries.groupBy { it.category }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Erfolge") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Card
            item {
                AchievementStatsCard(
                    unlockedCount = unlockedCount,
                    totalCount = totalAchievements,
                    totalPoints = totalPoints
                )
            }
            
            // Category Filter
            item {
                CategoryFilterRow(
                    categories = AchievementCategory.entries,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    unlockedByCategory = achievementsByCategory.mapValues { (_, achievements) ->
                        achievements.count { unlockedAchievements.contains(it.id) }
                    }
                )
            }
            
            // Achievements List
            val filteredAchievements = if (selectedCategory != null) {
                achievementsByCategory[selectedCategory] ?: emptyList()
            } else {
                Achievement.entries.toList()
            }
            
            items(filteredAchievements) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    isUnlocked = unlockedAchievements.contains(achievement.id),
                    progress = achievementsRepository.getAchievementProgress(achievement)
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AchievementStatsCard(
    unlockedCount: Int,
    totalCount: Int,
    totalPoints: Int
) {
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trophy icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA000)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "$unlockedCount / $totalCount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Erfolge freigeschaltet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { unlockedCount.toFloat() / totalCount },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFD700),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Points
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$totalPoints Punkte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB8860B)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<AchievementCategory>,
    selectedCategory: AchievementCategory?,
    onCategorySelected: (AchievementCategory?) -> Unit,
    unlockedByCategory: Map<AchievementCategory, Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Alle") },
            leadingIcon = if (selectedCategory == null) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        
        categories.forEach { category ->
            val count = unlockedByCategory[category] ?: 0
            val total = Achievement.entries.count { it.category == category }
            
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text("${category.displayName} ($count/$total)") },
                leadingIcon = if (selectedCategory == category) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    isUnlocked: Boolean,
    progress: Float
) {
    val icon = getAchievementIcon(achievement.icon)
    val categoryColor = getCategoryColor(achievement.category)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.6f),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                categoryColor.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isUnlocked) {
                            Brush.linearGradient(
                                colors = listOf(
                                    categoryColor,
                                    categoryColor.copy(alpha = 0.7f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Gray.copy(alpha = 0.3f),
                                    Color.Gray.copy(alpha = 0.2f)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) categoryColor else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isUnlocked) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Progress bar for non-unlocked achievements with progress
                if (!isUnlocked && progress > 0f && progress < 1f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = categoryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Points badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isUnlocked) {
                    Color(0xFFFFD700).copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = if (isUnlocked) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${achievement.points}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) Color(0xFFB8860B) else Color.Gray
                    )
                }
            }
        }
    }
}

private fun getAchievementIcon(iconName: String): ImageVector {
    return when (iconName) {
        "trophy" -> Icons.Default.EmojiEvents
        "star" -> Icons.Default.Star
        "star_half" -> Icons.Default.StarHalf
        "stars" -> Icons.Default.Stars
        "verified" -> Icons.Default.Verified
        "bolt" -> Icons.Default.Bolt
        "psychology" -> Icons.Default.Psychology
        "military_tech" -> Icons.Default.MilitaryTech
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "whatshot" -> Icons.Default.Whatshot
        "celebration" -> Icons.Default.Celebration
        "calendar_today" -> Icons.Default.CalendarToday
        "date_range" -> Icons.Default.DateRange
        "event_available" -> Icons.Default.EventAvailable
        "grid_3x3" -> Icons.Default.Grid3x3
        "emoji_events" -> Icons.Default.EmojiEvents
        "smart_toy" -> Icons.Default.SmartToy
        "circle" -> Icons.Default.Circle
        "workspace_premium" -> Icons.Default.WorkspacePremium
        "favorite" -> Icons.Default.Favorite
        else -> Icons.Default.Star
    }
}

private fun getCategoryColor(category: AchievementCategory): Color {
    return when (category) {
        AchievementCategory.SUDOKU -> Color(0xFF1976D2)
        AchievementCategory.STREAK -> Color(0xFFFF5722)
        AchievementCategory.DAILY -> Color(0xFF4CAF50)
        AchievementCategory.TICTACTOE -> Color(0xFF9C27B0)
        AchievementCategory.MUHLE -> Color(0xFF795548)
        AchievementCategory.GENERAL -> Color(0xFFFF9800)
    }
}
