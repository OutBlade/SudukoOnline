package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.repository.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBackClick: () -> Unit,
    onChallengeClick: (Friend) -> Unit,
    onActivityClick: (FriendActivity) -> Unit = {}
) {
    val context = LocalContext.current
    val friendsRepository = remember { FriendsRepository.getInstance(context) }
    val clipboardManager = LocalClipboardManager.current
    
    val friends by friendsRepository.friends.collectAsState()
    val pendingRequests by friendsRepository.pendingRequests.collectAsState()
    val sentRequests by friendsRepository.sentRequests.collectAsState()
    val activityFeed by friendsRepository.activityFeed.collectAsState()
    val friendCode by friendsRepository.friendCode.collectAsState()
    val isLoading by friendsRepository.isLoading.collectAsState()
    val error by friendsRepository.error.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Freunde", "Anfragen", "Aktivität")
    
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showFriendOptionsDialog by remember { mutableStateOf<Friend?>(null) }
    var codeCopiedMessage by remember { mutableStateOf(false) }
    
    // Show snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            friendsRepository.clearError()
        }
    }
    
    LaunchedEffect(codeCopiedMessage) {
        if (codeCopiedMessage) {
            snackbarHostState.showSnackbar("Freundescode kopiert!")
            codeCopiedMessage = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freunde") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddFriendDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Freund hinzufügen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Friend Code Card
            FriendCodeCard(
                code = friendCode,
                onCopyCode = {
                    clipboardManager.setText(AnnotatedString(friendCode))
                    codeCopiedMessage = true
                },
                onShareCode = {
                    // Share intent would go here
                }
            )
            
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    val badgeCount = when (index) {
                        1 -> pendingRequests.size
                        2 -> activityFeed.count { 
                            System.currentTimeMillis() - it.timestamp < 24 * 60 * 60 * 1000 
                        }
                        else -> 0
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(title)
                                if (badgeCount > 0) {
                                    Badge {
                                        Text("$badgeCount")
                                    }
                                }
                            }
                        }
                    )
                }
            }
            
            // Content
            when (selectedTab) {
                0 -> FriendsListTab(
                    friends = friends,
                    onFriendClick = { showFriendOptionsDialog = it },
                    onChallengeClick = onChallengeClick
                )
                1 -> RequestsTab(
                    pendingRequests = pendingRequests,
                    sentRequests = sentRequests,
                    onAccept = { friendsRepository.acceptFriendRequest(it) },
                    onDecline = { friendsRepository.declineFriendRequest(it) }
                )
                2 -> ActivityTab(
                    activities = activityFeed,
                    onActivityClick = onActivityClick
                )
            }
        }
    }
    
    // Add Friend Dialog
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onAddFriend = { code ->
                friendsRepository.sendFriendRequest(code) { success, message ->
                    if (success) {
                        showAddFriendDialog = false
                    }
                    // Error will be shown via snackbar
                }
            },
            isLoading = isLoading
        )
    }
    
    // Friend Options Dialog
    showFriendOptionsDialog?.let { friend ->
        FriendOptionsDialog(
            friend = friend,
            onDismiss = { showFriendOptionsDialog = null },
            onChallenge = {
                showFriendOptionsDialog = null
                onChallengeClick(friend)
            },
            onRemove = {
                friendsRepository.removeFriend(friend)
                showFriendOptionsDialog = null
            }
        )
    }
}

@Composable
private fun FriendCodeCard(
    code: String,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Dein Freundescode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = code,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = onCopyCode) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren")
                    }
                    FilledTonalIconButton(onClick = onShareCode) {
                        Icon(Icons.Default.Share, contentDescription = "Teilen")
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsListTab(
    friends: List<Friend>,
    onFriendClick: (Friend) -> Unit,
    onChallengeClick: (Friend) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyState(
            icon = Icons.Default.People,
            title = "Noch keine Freunde",
            subtitle = "Füge Freunde hinzu um gemeinsam zu spielen!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Online friends first
            val onlineFriends = friends.filter { it.isOnline() }
            val offlineFriends = friends.filter { !it.isOnline() }
            
            if (onlineFriends.isNotEmpty()) {
                item {
                    Text(
                        text = "Online (${onlineFriends.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(onlineFriends) { friend ->
                    FriendCard(
                        friend = friend,
                        onClick = { onFriendClick(friend) },
                        onChallengeClick = { onChallengeClick(friend) }
                    )
                }
            }
            
            if (offlineFriends.isNotEmpty()) {
                item {
                    Text(
                        text = "Offline (${offlineFriends.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(offlineFriends) { friend ->
                    FriendCard(
                        friend = friend,
                        onClick = { onFriendClick(friend) },
                        onChallengeClick = { onChallengeClick(friend) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: Friend,
    onClick: () -> Unit,
    onChallengeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with online indicator
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Online indicator
                if (friend.isOnline()) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = friend.getLastSeenText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (friend.isOnline()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Challenge button
            FilledTonalButton(
                onClick = onChallengeClick,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    Icons.Default.SportsScore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fordern")
            }
        }
    }
}

@Composable
private fun RequestsTab(
    pendingRequests: List<FriendRequest>,
    sentRequests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit
) {
    if (pendingRequests.isEmpty() && sentRequests.isEmpty()) {
        EmptyState(
            icon = Icons.Default.MailOutline,
            title = "Keine Anfragen",
            subtitle = "Freundschaftsanfragen erscheinen hier"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pendingRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Empfangene Anfragen",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(pendingRequests) { request ->
                    FriendRequestCard(
                        request = request,
                        isReceived = true,
                        onAccept = { onAccept(request) },
                        onDecline = { onDecline(request) }
                    )
                }
            }
            
            if (sentRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Gesendete Anfragen",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(sentRequests) { request ->
                    FriendRequestCard(
                        request = request,
                        isReceived = false,
                        onAccept = {},
                        onDecline = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    isReceived: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReceived) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (if (isReceived) request.fromPlayerName else request.toPlayerName)
                        .firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReceived) request.fromPlayerName else request.toPlayerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isReceived) "Möchte dein Freund sein" else "Anfrage gesendet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isReceived) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onDecline,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Ablehnen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    FilledIconButton(onClick = onAccept) {
                        Icon(Icons.Default.Check, contentDescription = "Annehmen")
                    }
                }
            } else {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Wartend",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActivityTab(
    activities: List<FriendActivity>,
    onActivityClick: (FriendActivity) -> Unit
) {
    if (activities.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Timeline,
            title = "Keine Aktivitäten",
            subtitle = "Aktivitäten deiner Freunde erscheinen hier"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activities) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity) }
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: FriendActivity,
    onClick: () -> Unit
) {
    val (icon, color) = when (activity.type) {
        ActivityType.FRIEND_ACCEPTED.name -> Icons.Default.PersonAdd to Color(0xFF4CAF50)
        ActivityType.CHALLENGE_RECEIVED.name -> Icons.Default.SportsScore to Color(0xFFFF9800)
        ActivityType.CHALLENGE_ACCEPTED.name -> Icons.Default.CheckCircle to Color(0xFF2196F3)
        ActivityType.CHALLENGE_COMPLETED.name -> Icons.Default.EmojiEvents to Color(0xFFFFD700)
        ActivityType.GAME_COMPLETED.name -> Icons.Default.Star to Color(0xFF9C27B0)
        else -> Icons.Default.Notifications to MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatActivityTime(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (activity.challengeId != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAddFriend: (String) -> Unit,
    isLoading: Boolean
) {
    var friendCode by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Freund hinzufügen")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Gib den 6-stelligen Freundescode ein:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = friendCode,
                    onValueChange = { 
                        if (it.length <= 6) {
                            friendCode = it.uppercase()
                        }
                    },
                    label = { Text("Freundescode") },
                    placeholder = { Text("z.B. ABC123") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (friendCode.length == 6) {
                                onAddFriend(friendCode)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddFriend(friendCode) },
                enabled = friendCode.length == 6 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Hinzufügen")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun FriendOptionsDialog(
    friend: Friend,
    onDismiss: () -> Unit,
    onChallenge: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }
    
    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Freund entfernen?") },
            text = {
                Text("Möchtest du ${friend.displayName} wirklich aus deiner Freundesliste entfernen?")
            },
            confirmButton = {
                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Entfernen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friend.displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(friend.displayName)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onChallenge,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.SportsScore, contentDescription = null)
                            Text("Herausfordern")
                        }
                    }
                    
                    Surface(
                        onClick = { showRemoveConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonRemove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Freund entfernen",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
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
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatActivityTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60 * 1000 -> "Gerade eben"
        diff < 60 * 60 * 1000 -> "Vor ${diff / (60 * 1000)} Min."
        diff < 24 * 60 * 60 * 1000 -> "Vor ${diff / (60 * 60 * 1000)} Std."
        diff < 7 * 24 * 60 * 60 * 1000 -> "Vor ${diff / (24 * 60 * 60 * 1000)} Tagen"
        else -> "Vor über einer Woche"
    }
}
