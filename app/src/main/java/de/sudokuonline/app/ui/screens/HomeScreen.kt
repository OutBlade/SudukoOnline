package de.sudokuonline.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.model.GameMode
import de.sudokuonline.app.data.repository.DailyChallengeRepository
import de.sudokuonline.app.data.repository.SavedGameRepository
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.DailyBonusRepository
import de.sudokuonline.app.data.repository.DailyBonusResult
import de.sudokuonline.app.data.repository.FriendsRepository
import de.sudokuonline.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playerName: String,
    onSinglePlayerClick: (Difficulty) -> Unit,
    onOnlineClick: () -> Unit = {},
    onTicTacToeAIClick: () -> Unit = {},
    onMuhleAIClick: () -> Unit = {},
    onDameAIClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onDailyChallengeClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onContinueGameClick: () -> Unit = {},
    onTutorialClick: () -> Unit = {},
    onStatisticsDashboardClick: () -> Unit = {},
    onKillerSudokuClick: (Difficulty) -> Unit = {},
    onLeagueLeaderboardClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    onMyChallengesClick: () -> Unit = {},
    onShareAppClick: () -> Unit = {},
    onLootboxClick: () -> Unit = {},
    onBrainrotWordleClick: () -> Unit = {},
    on2048Click: () -> Unit = {},
    onMathTrainerClick: () -> Unit = {},
    onExamSimulatorClick: () -> Unit = {},
    onLENQuizClick: () -> Unit = {},
    onLENTrainerClick: () -> Unit = {},
    onBlackjackClick: () -> Unit = {},
    onSevenClick: () -> Unit = {},
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showKillerSudokuDifficultyDialog by remember { mutableStateOf(false) }
    var showDailyBonusDialog by remember { mutableStateOf(false) }
    var dailyBonusResult by remember { mutableStateOf<DailyBonusResult?>(null) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Repositories for saved game and daily challenge
    val savedGameRepository = remember { SavedGameRepository.getInstance(context) }
    val dailyChallengeRepository = remember { DailyChallengeRepository.getInstance(context) }
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }
    val dailyBonusRepository = remember { DailyBonusRepository.getInstance(context) }
    val friendsRepository = remember { FriendsRepository.getInstance(context) }
    val lootboxRepository = remember { de.sudokuonline.app.data.repository.LootboxRepository.getInstance(context) }

    val hasSavedGame by savedGameRepository.hasSavedGame.collectAsState()
    val savedGameInfo by savedGameRepository.savedGameInfo.collectAsState()
    val streakInfo by dailyChallengeRepository.streakInfo.collectAsState()
    val currentStreak = streakInfo.currentStreak
    val coins by currencyRepository.coins.collectAsState()
    val canClaimDailyBonus by dailyBonusRepository.canClaimBonus.collectAsState()
    val loginStreak by dailyBonusRepository.loginStreak.collectAsState()
    val pendingChallenges by friendsRepository.activeChallenges.collectAsState()
    val pendingRequests by friendsRepository.pendingRequests.collectAsState()
    val ownedLootboxes by lootboxRepository.ownedLootboxes.collectAsState()
    
    // Refresh daily bonus status when screen appears
    LaunchedEffect(Unit) {
        dailyBonusRepository.refresh()
    }

    // Animated logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val logoGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with profile, coins, streak, and settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile button
                FilledTonalIconButton(onClick = onProfileClick) {
                    Icon(Icons.Default.Person, contentDescription = "Profil")
                }
                
                // Center section: Coins and Streak
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coins display
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        modifier = Modifier.clickable { onThemeClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$coins",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
                            )
                        }
                    }

                    // Daily Streak Badge (if streak > 0)
                    if (currentStreak > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.2f),
                            modifier = Modifier.clickable { onDailyChallengeClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "$currentStreak",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                // Settings button
                FilledTonalIconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Logo/Title
            Box(
                modifier = Modifier.scale(logoScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Text(
                    text = "SUDOKU",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = logoGlow),
                    letterSpacing = 8.sp,
                    modifier = Modifier
                        .offset(x = 2.dp, y = 2.dp)
                )
                Text(
                    text = "SUDOKU",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 8.sp
                )
            }
            Text(
                text = "ONLINE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Welcome message
            Text(
                text = "Willkommen, $playerName!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Access Buttons (horizontal row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAccessButton(
                    icon = Icons.Default.People,
                    label = "Freunde",
                    color = Color(0xFF4CAF50),
                    onClick = onFriendsClick,
                    modifier = Modifier.weight(1f),
                    badge = if (pendingRequests.isNotEmpty()) pendingRequests.size else null
                )
                QuickAccessButton(
                    icon = Icons.Default.QrCode2,
                    label = "Teilen",
                    color = Color(0xFF9C27B0),
                    onClick = onShareAppClick,
                    modifier = Modifier.weight(1f)
                )
                QuickAccessButton(
                    icon = Icons.Default.EmojiEvents,
                    label = "Erfolge",
                    color = Color(0xFFFFD700),
                    onClick = onAchievementsClick,
                    modifier = Modifier.weight(1f)
                )
                QuickAccessButton(
                    icon = Icons.Default.BarChart,
                    label = "Stats",
                    color = Color(0xFF2196F3),
                    onClick = onStatisticsDashboardClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Pending Challenges Banner
            val activeChallenges = pendingChallenges.filter { !it.isExpired() }
            if (activeChallenges.isNotEmpty()) {
                ChallengeBanner(
                    challengeCount = activeChallenges.size,
                    latestChallenger = activeChallenges.firstOrNull()?.fromPlayerName ?: "",
                    onClick = onMyChallengesClick
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Daily Bonus Banner (show if can claim)
            if (canClaimDailyBonus) {
                DailyBonusBanner(
                    bonusAmount = dailyBonusRepository.getCurrentBonusAmount(),
                    loginStreak = loginStreak,
                    onClaim = {
                        val result = dailyBonusRepository.claimDailyBonus(currencyRepository)
                        if (result != null) {
                            dailyBonusResult = result
                            showDailyBonusDialog = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Saved Game Banner
            if (hasSavedGame && savedGameInfo != null) {
                SavedGameBanner(
                    progress = savedGameInfo!!.progress,
                    difficulty = savedGameInfo!!.difficulty.displayName,
                    onContinue = onContinueGameClick,
                    onNewGame = { showDifficultyDialog = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main menu buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Daily Challenge button with streak indicator
                MainMenuButton(
                    icon = Icons.Default.CalendarToday,
                    title = "Tägliche Herausforderung",
                    subtitle = if (currentStreak > 0) "$currentStreak Tage Serie!" else "Neues Rätsel jeden Tag!",
                    onClick = onDailyChallengeClick,
                    containerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                    contentColor = Color(0xFFE65100),
                    badge = if (currentStreak >= 7) "🔥" else null
                )

                // Single player Sudoku button
                MainMenuButton(
                    icon = Icons.Default.Person,
                    title = "Sudoku Einzelspieler",
                    subtitle = "Spiele alleine in deinem Tempo",
                    onClick = { showDifficultyDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Killer Sudoku button
                MainMenuButton(
                    icon = Icons.Default.Dangerous,
                    title = "Killer Sudoku",
                    subtitle = "Sudoku mit Käfigen und Summen",
                    onClick = { showKillerSudokuDifficultyDialog = true },
                    containerColor = Color(0xFFE91E63).copy(alpha = 0.15f),
                    contentColor = Color(0xFFC2185B),
                )

                // Brainrot Wordle button
                MainMenuButton(
                    icon = Icons.Default.Abc,
                    title = "Brainrot Wordle",
                    subtitle = "Errate Gen-Z Wörter im Wordle-Stil",
                    onClick = onBrainrotWordleClick,
                    containerColor = Color(0xFF00BCD4).copy(alpha = 0.15f),
                    contentColor = Color(0xFF00838F),
                )

                // 2048 button
                MainMenuButton(
                    icon = Icons.Default.Grid4x4,
                    title = "2048",
                    subtitle = "Kombiniere Zahlen bis 2048!",
                    onClick = on2048Click,
                    containerColor = Color(0xFFEDC22E).copy(alpha = 0.15f),
                    contentColor = Color(0xFFB8960B),
                )

                // HM1 Mathe Trainer button
                MainMenuButton(
                    icon = Icons.Default.Calculate,
                    title = "HM1 Trainer",
                    subtitle = "Übe für die Mathe-Klausur!",
                    onClick = onMathTrainerClick,
                    containerColor = Color(0xFF3F51B5).copy(alpha = 0.15f),
                    contentColor = Color(0xFF283593)
                )

                // Klausur-Simulator button
                MainMenuButton(
                    icon = Icons.Default.School,
                    title = "Klausur-Simulator",
                    subtitle = "Echte KIT-Klausuraufgaben mit Timer",
                    onClick = onExamSimulatorClick,
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.15f),
                    contentColor = Color(0xFF0D47A1),
                )

                // LEN Trainer button
                MainMenuButton(
                    icon = Icons.Default.Bolt,
                    title = "LEN Klausur-Sim",
                    subtitle = "Offene Klausurfragen · Selbstbewertung",
                    onClick = onLENQuizClick,
                    containerColor = Color(0xFF00695C).copy(alpha = 0.15f),
                    contentColor = Color(0xFF004D40)
                )

                // LEN MC Trainer button
                MainMenuButton(
                    icon = Icons.Default.ElectricBolt,
                    title = "LEN MC-Trainer",
                    subtitle = "Multiple Choice · Sofort-Feedback · KIT",
                    onClick = onLENTrainerClick,
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.15f),
                    contentColor = Color(0xFF0D47A1),
                )

                // Blackjack button
                MainMenuButton(
                    icon = Icons.Default.Casino,
                    title = "Blackjack",
                    subtitle = "Schlage den Dealer auf 21!",
                    onClick = onBlackjackClick,
                    containerColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                    contentColor = Color(0xFF1B5E20),
                )

                // Flip 7 button
                MainMenuButton(
                    icon = Icons.Default.Style,
                    title = "Flip 7",
                    subtitle = "Karten aufdecken · Punkte sammeln · Nicht busten!",
                    onClick = onSevenClick,
                    containerColor = Color(0xFFE53935).copy(alpha = 0.12f),
                    contentColor = Color(0xFFE53935),
                )

                // Online Spielen button
                MainMenuButton(
                    icon = Icons.Default.Public,
                    title = "Online Spielen",
                    subtitle = "Sudoku, TicTacToe & Dame online",
                    onClick = onOnlineClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                // VS KI Section Header
                Text(
                    text = "Gegen KI spielen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                // TicTacToe AI button
                MainMenuButton(
                    icon = Icons.Default.SmartToy,
                    title = "TicTacToe vs KI",
                    subtitle = "Alle Modi gegen eine starke KI",
                    onClick = onTicTacToeAIClick,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

                // Muhle AI button
                MainMenuButton(
                    icon = Icons.Default.Circle,
                    title = "Mühle vs KI",
                    subtitle = "Klassisches Brettspiel gegen KI",
                    onClick = onMuhleAIClick,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Dame AI button
                MainMenuButton(
                    icon = Icons.Default.GridOn,
                    title = "Dame vs KI",
                    subtitle = "Checkers gegen eine starke KI",
                    onClick = onDameAIClick,
                    containerColor = Color(0xFF795548).copy(alpha = 0.15f),
                    contentColor = Color(0xFF5D4037),
                )

                // Lootboxen button
                MainMenuButton(
                    icon = Icons.Default.Redeem,
                    title = "Lootboxen",
                    subtitle = "Öffne Truhen für Themes, Coins & Emojis",
                    onClick = onLootboxClick,
                    containerColor = Color(0xFF9C27B0).copy(alpha = 0.15f),
                    contentColor = Color(0xFF7B1FA2),
                    badge = if (ownedLootboxes.isNotEmpty()) "${ownedLootboxes.size}" else null
                )

                // Mehr Section
                Text(
                    text = "Mehr",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                // Tutorial button
                MainMenuButton(
                    icon = Icons.Default.School,
                    title = "Tutorial",
                    subtitle = "Lerne Sudoku-Techniken Schritt für Schritt",
                    onClick = onTutorialClick,
                    containerColor = Color(0xFF8BC34A).copy(alpha = 0.2f),
                    contentColor = Color(0xFF558B2F)
                )

                // Liga Leaderboard button
                MainMenuButton(
                    icon = Icons.Default.MilitaryTech,
                    title = "Liga & Rangliste",
                    subtitle = "Steige in den Ligen auf!",
                    onClick = onLeagueLeaderboardClick,
                    containerColor = Color(0xFF673AB7).copy(alpha = 0.15f),
                    contentColor = Color(0xFF512DA8),
                )
                
                // Classic Leaderboard button
                MainMenuButton(
                    icon = Icons.Default.Leaderboard,
                    title = "Klassische Rangliste",
                    subtitle = "Sieh dir die besten Spieler an",
                    onClick = onLeaderboardClick,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "Version 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Difficulty selection dialog
    if (showDifficultyDialog) {
        DifficultySelectionDialog(
            onDifficultySelected = { difficulty ->
                showDifficultyDialog = false
                onSinglePlayerClick(difficulty)
            },
            onDismiss = { showDifficultyDialog = false }
        )
    }
    
    // Killer Sudoku Difficulty selection dialog
    if (showKillerSudokuDifficultyDialog) {
        KillerSudokuDifficultyDialog(
            onDifficultySelected = { difficulty ->
                showKillerSudokuDifficultyDialog = false
                onKillerSudokuClick(difficulty)
            },
            onDismiss = { showKillerSudokuDifficultyDialog = false }
        )
    }
    
    // Daily Bonus claimed dialog
    if (showDailyBonusDialog && dailyBonusResult != null) {
        DailyBonusClaimedDialog(
            result = dailyBonusResult!!,
            onDismiss = { 
                showDailyBonusDialog = false 
                dailyBonusResult = null
            }
        )
    }
}

@Composable
private fun QuickAccessButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Badge
            if (badge != null && badge > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                ) {
                    Text("$badge")
                }
            }
        }
    }
}

@Composable
private fun ChallengeBanner(
    challengeCount: Int,
    latestChallenger: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Challenge icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9800).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SportsScore,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (challengeCount == 1) "Neue Herausforderung!" else "$challengeCount Herausforderungen!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = if (challengeCount == 1) "$latestChallenger fordert dich heraus" else "$latestChallenger und andere",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8F00)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun SavedGameBanner(
    progress: Int,
    difficulty: String,
    onContinue: () -> Unit,
    onNewGame: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Pause,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spiel fortsetzen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = difficulty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "$progress% abgeschlossen",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNewGame,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Neues Spiel")
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fortsetzen")
                }
            }
        }
    }
}

@Composable
private fun MainMenuButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    badge: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = contentColor
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = badge, fontSize = 16.sp)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultySelectionDialog(
    onDifficultySelected: (Difficulty) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schwierigkeit wählen",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyOption(
                        difficulty = difficulty,
                        onClick = { onDifficultySelected(difficulty) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun DifficultyOption(
    difficulty: Difficulty,
    onClick: () -> Unit
) {
    val color = when (difficulty) {
        Difficulty.EASY -> SuccessColor
        Difficulty.MEDIUM -> InfoColor
        Difficulty.HARD -> WarningColor
        Difficulty.EXPERT -> ErrorColor
    }

    val description = when (difficulty) {
        Difficulty.EASY -> "Perfekt für Anfänger"
        Difficulty.MEDIUM -> "Für etwas Uebung"
        Difficulty.HARD -> "Eine echte Herausforderung"
        Difficulty.EXPERT -> "Nur für Profis"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = difficulty.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KillerSudokuDifficultyDialog(
    onDifficultySelected: (Difficulty) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = Color(0xFFE91E63)
                )
                Text(
                    text = "Killer Sudoku",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Schwierigkeit wählen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Difficulty.entries.forEach { difficulty ->
                    KillerDifficultyOption(
                        difficulty = difficulty,
                        onClick = { onDifficultySelected(difficulty) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun KillerDifficultyOption(
    difficulty: Difficulty,
    onClick: () -> Unit
) {
    val color = when (difficulty) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.MEDIUM -> Color(0xFF2196F3)
        Difficulty.HARD -> Color(0xFFFF9800)
        Difficulty.EXPERT -> Color(0xFFE91E63)
    }

    val description = when (difficulty) {
        Difficulty.EASY -> "Einfache Käfig-Summen"
        Difficulty.MEDIUM -> "Mittlere Herausforderung"
        Difficulty.HARD -> "Komplexe Käfige"
        Difficulty.EXPERT -> "Maximale Schwierigkeit"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = difficulty.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color
            )
        }
    }
}

@Composable
private fun DailyBonusBanner(
    bonusAmount: Int,
    loginStreak: Int,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gift icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Täglicher Bonus!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = if (loginStreak > 0) {
                        "$bonusAmount Coins - Tag ${loginStreak + 1}"
                    } else {
                        "$bonusAmount Coins warten auf dich!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF388E3C)
                )
            }
            
            Button(
                onClick = onClaim,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Abholen")
            }
        }
    }
}

@Composable
private fun DailyBonusClaimedDialog(
    result: DailyBonusResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(40.dp)
                )
            }
        },
        title = {
            Text(
                text = "+${result.coinsEarned} Coins!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8860B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Täglicher Bonus eingelöst!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Streak info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "${result.currentStreak} Tage Serie",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "Morgen: ${result.nextDayBonus} Coins",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF8F00)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Super!")
            }
        }
    )
}
