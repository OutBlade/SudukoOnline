package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.sudokuonline.app.ads.RewardedAdManager
import de.sudokuonline.app.data.repository.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeShopScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val themeRepository = remember { ThemeRepository.getInstance(context) }
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }
    val adManager = remember { RewardedAdManager.getInstance(context) }
    
    val currentTheme by themeRepository.currentTheme.collectAsState()
    val ownedThemes by themeRepository.ownedThemes.collectAsState()
    val coins by currencyRepository.coins.collectAsState()
    val adState by adManager.adState.collectAsState()
    
    var selectedTheme by remember { mutableStateOf<AppTheme?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showInsufficientFundsDialog by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf<PurchaseResult?>(null) }
    
    // Initialize ads
    LaunchedEffect(Unit) {
        adManager.initialize(context)
    }
    
    val themesWithStatus = remember(ownedThemes, currentTheme, coins) {
        themeRepository.getAllThemesWithStatus(currencyRepository)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Theme Shop")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Coin balance
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 16.sp)
                            Text(
                                text = coins.toString(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header - Earn coins section
            item {
                EarnCoinsCard(
                    adManager = adManager,
                    currencyRepository = currencyRepository,
                    adState = adState
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verfügbare Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Free themes
            item {
                Text(
                    text = "Kostenlos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(themesWithStatus.filter { it.theme.price == 0 }) { themeStatus ->
                ThemeCard(
                    themeStatus = themeStatus,
                    onClick = {
                        if (themeStatus.isOwned) {
                            themeRepository.setTheme(themeStatus.theme)
                        }
                    }
                )
            }
            
            // Premium themes
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Premium",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFD700)
                )
            }
            
            items(themesWithStatus.filter { it.theme.price > 0 }.sortedBy { it.theme.price }) { themeStatus ->
                ThemeCard(
                    themeStatus = themeStatus,
                    onClick = {
                        if (themeStatus.isOwned) {
                            themeRepository.setTheme(themeStatus.theme)
                        } else {
                            selectedTheme = themeStatus.theme
                            showPurchaseDialog = true
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Purchase confirmation dialog
    if (showPurchaseDialog && selectedTheme != null) {
        PurchaseConfirmationDialog(
            theme = selectedTheme!!,
            currentCoins = coins,
            onConfirm = {
                val result = themeRepository.purchaseTheme(selectedTheme!!.id, currencyRepository)
                purchaseResult = result
                showPurchaseDialog = false
                when (result) {
                    is PurchaseResult.Success -> showSuccessDialog = true
                    is PurchaseResult.InsufficientFunds -> showInsufficientFundsDialog = true
                    else -> {}
                }
            },
            onDismiss = {
                showPurchaseDialog = false
                selectedTheme = null
            }
        )
    }
    
    // Success dialog
    if (showSuccessDialog && selectedTheme != null) {
        PurchaseSuccessDialog(
            theme = selectedTheme!!,
            onApply = {
                themeRepository.setTheme(selectedTheme!!)
                showSuccessDialog = false
                selectedTheme = null
            },
            onDismiss = {
                showSuccessDialog = false
                selectedTheme = null
            }
        )
    }
    
    // Insufficient funds dialog
    if (showInsufficientFundsDialog && purchaseResult is PurchaseResult.InsufficientFunds) {
        val funds = purchaseResult as PurchaseResult.InsufficientFunds
        InsufficientFundsDialog(
            required = funds.required,
            current = funds.current,
            onWatchAd = {
                showInsufficientFundsDialog = false
                val activity = context as? android.app.Activity ?: return@InsufficientFundsDialog
                adManager.showAd(
                    activity = activity,
                    onRewardEarned = { reward ->
                        currencyRepository.addCoins(50, CoinReason.WATCH_AD)
                    },
                    onAdClosed = {}
                )
            },
            onDismiss = {
                showInsufficientFundsDialog = false
            },
            isAdReady = adState == RewardedAdManager.AdState.READY
        )
    }
}

@Composable
private fun EarnCoinsCard(
    adManager: RewardedAdManager,
    currencyRepository: CurrencyRepository,
    adState: RewardedAdManager.AdState
) {
    val context = LocalContext.current
    var showRewardDialog by remember { mutableStateOf(false) }
    
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🪙", fontSize = 24.sp)
                Text(
                    text = "Coins verdienen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Schau eine kurze Werbung und erhalte 50 Coins!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    val activity = context as? android.app.Activity ?: return@Button
                    adManager.showAd(
                        activity = activity,
                        onRewardEarned = { 
                            currencyRepository.addCoins(50, CoinReason.WATCH_AD)
                            showRewardDialog = true
                        },
                        onAdClosed = {}
                    )
                },
                enabled = adState == RewardedAdManager.AdState.READY,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                if (adState == RewardedAdManager.AdState.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wird geladen...")
                } else {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Werbung ansehen +50 🪙",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    if (showRewardDialog) {
        AlertDialog(
            onDismissRequest = { showRewardDialog = false },
            icon = {
                Text("🎉", fontSize = 48.sp)
            },
            title = {
                Text(
                    text = "+50 Coins!",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Du hast 50 Coins erhalten!",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = { showRewardDialog = false }) {
                    Text("Super!")
                }
            }
        )
    }
}

@Composable
private fun ThemeCard(
    themeStatus: ThemeWithStatus,
    onClick: () -> Unit
) {
    val theme = themeStatus.theme
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (themeStatus.isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (themeStatus.isActive) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme preview circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                theme.primaryLight,
                                theme.secondaryLight,
                                theme.tertiaryLight
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = theme.icon,
                    fontSize = 24.sp
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (themeStatus.isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "AKTIV",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Price or status
            when {
                themeStatus.isOwned -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Besitzt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                theme.price == 0 -> {
                    Text(
                        text = "GRATIS",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 14.sp)
                            Text(
                                text = theme.price.toString(),
                                fontWeight = FontWeight.Bold,
                                color = if (themeStatus.canAfford)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        if (!themeStatus.canAfford) {
                            Text(
                                text = "Nicht genug",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseConfirmationDialog(
    theme: AppTheme,
    currentCoins: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Theme preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    theme.primaryLight,
                                    theme.secondaryLight,
                                    theme.tertiaryLight
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(theme.icon, fontSize = 36.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Price
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🪙", fontSize = 24.sp)
                        Text(
                            text = theme.price.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Dein Guthaben: $currentCoins 🪙",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = currentCoins >= theme.price
                    ) {
                        Text("Kaufen")
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseSuccessDialog(
    theme: AppTheme,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 64.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Kauf erfolgreich!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Du hast ${theme.displayName} freigeschaltet!",
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Jetzt anwenden")
                }
                
                TextButton(onClick = onDismiss) {
                    Text("Später")
                }
            }
        }
    }
}

@Composable
private fun InsufficientFundsDialog(
    required: Int,
    current: Int,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
    isAdReady: Boolean
) {
    val missing = required - current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("😅", fontSize = 48.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Nicht genug Coins",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Dir fehlen noch $missing Coins",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Benötigt", style = MaterialTheme.typography.labelSmall)
                            Text("$required 🪙", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Vorhanden", style = MaterialTheme.typography.labelSmall)
                            Text("$current 🪙", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onWatchAd,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isAdReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Werbung ansehen +50 🪙", fontWeight = FontWeight.Bold)
                }
                
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    }
}
