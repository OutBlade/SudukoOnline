package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.repository.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(
    friend: Friend,
    onBackClick: () -> Unit,
    onChallengeSent: () -> Unit
) {
    val context = LocalContext.current
    val friendsRepository = remember { FriendsRepository.getInstance(context) }
    
    var selectedGameType by remember { mutableStateOf(GameType.SUDOKU) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herausforderung") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Friend Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friend.displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Herausfordern:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = friend.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Game Type Selection
            Text(
                text = "Spieltyp wählen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GameType.entries.forEach { gameType ->
                    GameTypeOption(
                        gameType = gameType,
                        isSelected = selectedGameType == gameType,
                        onClick = { selectedGameType = gameType }
                    )
                }
            }
            
            // Difficulty Selection
            Text(
                text = "Schwierigkeit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyChip(
                        difficulty = difficulty,
                        isSelected = selectedDifficulty == difficulty,
                        onClick = { selectedDifficulty = difficulty },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Optional Message
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 100) message = it },
                label = { Text("Nachricht (optional)") },
                placeholder = { Text("z.B. Schlag das wenn du kannst!") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                supportingText = { Text("${message.length}/100") }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Send Button
            Button(
                onClick = {
                    isSending = true
                    friendsRepository.sendChallenge(
                        friend = friend,
                        gameType = selectedGameType,
                        difficulty = selectedDifficulty.name,
                        message = message
                    )
                    onChallengeSent()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Herausforderung senden", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun GameTypeOption(
    gameType: GameType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (icon, color) = when (gameType) {
        GameType.SUDOKU -> Icons.Default.GridOn to Color(0xFF2196F3)
        GameType.KILLER_SUDOKU -> Icons.Default.Dangerous to Color(0xFFE91E63)
        GameType.TICTACTOE -> Icons.Default.Grid3x3 to Color(0xFF4CAF50)
        GameType.MUHLE -> Icons.Default.Circle to Color(0xFF795548)
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(2.dp, color) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gameType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getGameTypeDescription(gameType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color
                )
            }
        }
    }
}

@Composable
private fun DifficultyChip(
    difficulty: Difficulty,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (difficulty) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.MEDIUM -> Color(0xFF2196F3)
        Difficulty.HARD -> Color(0xFFFF9800)
        Difficulty.EXPERT -> Color(0xFFE91E63)
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(2.dp, color) else null
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = difficulty.displayName.take(4),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getGameTypeDescription(gameType: GameType): String {
    return when (gameType) {
        GameType.SUDOKU -> "Klassisches 9x9 Zahlenrätsel"
        GameType.KILLER_SUDOKU -> "Sudoku mit Käfigen und Summen"
        GameType.TICTACTOE -> "Drei in einer Reihe"
        GameType.MUHLE -> "Klassisches Brettspiel"
    }
}

// Challenge Details / Accept Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailsScreen(
    challenge: Challenge,
    onBackClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    val friendsRepository = remember { FriendsRepository.getInstance(context) }
    
    val gameType = try { GameType.valueOf(challenge.gameType) } catch (e: Exception) { GameType.SUDOKU }
    val difficulty = try { Difficulty.valueOf(challenge.difficulty) } catch (e: Exception) { Difficulty.MEDIUM }
    
    val (gameIcon, gameColor) = when (gameType) {
        GameType.SUDOKU -> Icons.Default.GridOn to Color(0xFF2196F3)
        GameType.KILLER_SUDOKU -> Icons.Default.Dangerous to Color(0xFFE91E63)
        GameType.TICTACTOE -> Icons.Default.Grid3x3 to Color(0xFF4CAF50)
        GameType.MUHLE -> Icons.Default.Circle to Color(0xFF795548)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herausforderung") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Challenge Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                gameColor.copy(alpha = 0.3f),
                                gameColor.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SportsScore,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = gameColor
                )
            }
            
            // Challenger Info
            Text(
                text = challenge.fromPlayerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "fordert dich heraus!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Challenge Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(gameIcon, contentDescription = null, tint = gameColor)
                            Text(
                                text = gameType.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = getDifficultyColor(difficulty).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = difficulty.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = getDifficultyColor(difficulty),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    if (challenge.message.isNotEmpty()) {
                        HorizontalDivider()
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "\"${challenge.message}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = challenge.getTimeRemainingText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text("Ablehnen")
                }
                
                Button(
                    onClick = {
                        friendsRepository.acceptChallenge(challenge) {
                            onAccept()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Annehmen")
                }
            }
        }
    }
}

// Challenges List Screen (My Challenges)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyChallengesScreen(
    onBackClick: () -> Unit,
    onChallengeClick: (Challenge) -> Unit
) {
    val context = LocalContext.current
    val friendsRepository = remember { FriendsRepository.getInstance(context) }
    
    val challenges by friendsRepository.activeChallenges.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Erhalten", "Gesendet")
    
    // Separate received and sent challenges
    // For simplicity, showing all challenges - in real app would track sent separately
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Herausforderungen") },
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (challenges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.SportsScore,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Keine Herausforderungen",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Fordere einen Freund heraus!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(challenges.filter { !it.isExpired() }) { challenge ->
                        ChallengeCard(
                            challenge = challenge,
                            onClick = { onChallengeClick(challenge) }
                        )
                    }
                    
                    // Expired challenges
                    val expiredChallenges = challenges.filter { it.isExpired() }
                    if (expiredChallenges.isNotEmpty()) {
                        item {
                            Text(
                                text = "Abgelaufen",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(expiredChallenges) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                onClick = { },
                                isExpired = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: Challenge,
    onClick: () -> Unit,
    isExpired: Boolean = false
) {
    val gameType = try { GameType.valueOf(challenge.gameType) } catch (e: Exception) { GameType.SUDOKU }
    val difficulty = try { Difficulty.valueOf(challenge.difficulty) } catch (e: Exception) { Difficulty.MEDIUM }
    
    val (gameIcon, gameColor) = when (gameType) {
        GameType.SUDOKU -> Icons.Default.GridOn to Color(0xFF2196F3)
        GameType.KILLER_SUDOKU -> Icons.Default.Dangerous to Color(0xFFE91E63)
        GameType.TICTACTOE -> Icons.Default.Grid3x3 to Color(0xFF4CAF50)
        GameType.MUHLE -> Icons.Default.Circle to Color(0xFF795548)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isExpired) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gameColor.copy(alpha = if (isExpired) 0.1f else 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    gameIcon,
                    contentDescription = null,
                    tint = gameColor.copy(alpha = if (isExpired) 0.5f else 1f)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.fromPlayerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isExpired) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${gameType.displayName} • ${difficulty.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isExpired -> MaterialTheme.colorScheme.surfaceVariant
                        challenge.status == ChallengeStatus.PENDING.name -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = when {
                            isExpired -> "Abgelaufen"
                            challenge.status == ChallengeStatus.PENDING.name -> "Offen"
                            else -> "Angenommen"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isExpired -> MaterialTheme.colorScheme.onSurfaceVariant
                            challenge.status == ChallengeStatus.PENDING.name -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                if (!isExpired) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = challenge.getTimeRemainingText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getDifficultyColor(difficulty: Difficulty): Color {
    return when (difficulty) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.MEDIUM -> Color(0xFF2196F3)
        Difficulty.HARD -> Color(0xFFFF9800)
        Difficulty.EXPERT -> Color(0xFFE91E63)
    }
}
