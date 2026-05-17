package de.sudokuonline.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.data.model.*
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.LootboxRepository
import de.sudokuonline.app.data.repository.ThemeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LootboxScreen(
    onBackClick: () -> Unit,
    onOpenLootbox: (OwnedLootbox) -> Unit
) {
    val context = LocalContext.current
    val lootboxRepository = remember { LootboxRepository.getInstance(context) }
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }

    val ownedBoxes by lootboxRepository.ownedLootboxes.collectAsState()
    val coins by currencyRepository.coins.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showPurchaseConfirm by remember { mutableStateOf<LootboxRarity?>(null) }
    var showInsufficientFunds by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lootboxen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zuruck")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f)
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
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Meine Boxen")
                            if (ownedBoxes.isNotEmpty()) {
                                Badge { Text("${ownedBoxes.size}") }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Shop") }
                )
            }

            when (selectedTab) {
                0 -> LootboxInventoryTab(
                    boxes = ownedBoxes,
                    onOpenBox = onOpenLootbox
                )
                1 -> LootboxShopTab(
                    coins = coins,
                    onBuyClick = { rarity -> showPurchaseConfirm = rarity }
                )
            }
        }
    }

    // Purchase confirmation dialog
    showPurchaseConfirm?.let { rarity ->
        AlertDialog(
            onDismissRequest = { showPurchaseConfirm = null },
            icon = {
                Text(text = rarity.chestEmoji, fontSize = 48.sp)
            },
            title = {
                Text(
                    text = "${rarity.displayName} Lootbox kaufen?",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${rarity.rewardCount} zufällige Belohnungen")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
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
                            text = "${rarity.price} Coins",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB8860B)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = lootboxRepository.purchaseLootbox(rarity, currencyRepository)
                        showPurchaseConfirm = null
                        if (!success) {
                            showInsufficientFunds = true
                        } else {
                            selectedTab = 0 // Switch to inventory
                        }
                    }
                ) {
                    Text("Kaufen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseConfirm = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Insufficient funds dialog
    if (showInsufficientFunds) {
        AlertDialog(
            onDismissRequest = { showInsufficientFunds = false },
            icon = {
                Icon(
                    Icons.Default.MoneyOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Nicht genug Coins") },
            text = { Text("Du hast nicht genügend Coins für diese Lootbox. Spiele mehr Spiele oder schau Werbung, um Coins zu verdienen!") },
            confirmButton = {
                Button(onClick = { showInsufficientFunds = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun LootboxInventoryTab(
    boxes: List<OwnedLootbox>,
    onOpenBox: (OwnedLootbox) -> Unit
) {
    if (boxes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "\uD83D\uDCE6", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Keine Lootboxen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kaufe Lootboxen im Shop oder verdiene sie durch Siege!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(boxes, key = { it.id }) { box ->
                LootboxCard(
                    lootbox = box,
                    isShopItem = false,
                    onClick = { onOpenBox(box) }
                )
            }
        }
    }
}

@Composable
private fun LootboxShopTab(
    coins: Int,
    onBuyClick: (LootboxRarity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Wähle eine Lootbox",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LootboxRarity.entries.forEach { rarity ->
            ShopLootboxCard(
                rarity = rarity,
                canAfford = coins >= rarity.price,
                onClick = { onBuyClick(rarity) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LootboxCard(
    lootbox: OwnedLootbox,
    isShopItem: Boolean,
    onClick: () -> Unit
) {
    val rarityColor = Color(lootbox.rarity.colorValue)
    val infiniteTransition = rememberInfiniteTransition(label = "chest_${lootbox.id}")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val chestScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = rarityColor.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            // Glow background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rarityColor.copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = lootbox.rarity.chestEmoji,
                    fontSize = 48.sp,
                    modifier = Modifier.scale(chestScale)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lootbox.rarity.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor
                )
                Text(
                    text = "Tippe zum Öffnen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShopLootboxCard(
    rarity: LootboxRarity,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    val rarityColor = Color(rarity.colorValue)
    val infiniteTransition = rememberInfiniteTransition(label = "shop_${rarity.name}")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = rarityColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chest icon with glow
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rarityColor.copy(alpha = 0.3f),
                                rarityColor.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = rarity.chestEmoji, fontSize = 36.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${rarity.displayName} Lootbox",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor
                )
                Text(
                    text = "${rarity.rewardCount} Belohnungen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val contentHint = when (rarity) {
                    LootboxRarity.BRONZE -> "Coins + seltene Emojis"
                    LootboxRarity.SILVER -> "Coins, Emojis + Themes möglich"
                    LootboxRarity.GOLD -> "Coins, Themes + epische Items"
                    LootboxRarity.LEGENDARY -> "Garantiert episch+ Item!"
                }
                Text(
                    text = contentHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = rarityColor.copy(alpha = 0.8f)
                )
            }

            // Price button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) rarityColor else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canAfford) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${rarity.price}", fontWeight = FontWeight.Bold)
            }
        }
    }
}
