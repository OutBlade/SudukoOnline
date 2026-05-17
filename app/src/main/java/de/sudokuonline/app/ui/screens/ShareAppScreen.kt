package de.sudokuonline.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.content.Intent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor

private const val DOWNLOAD_URL = "https://outblade.github.io/sudoku-online-download/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAppScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    var showReferralClaimed by remember { mutableStateOf(false) }

    val currencyRepository = remember { de.sudokuonline.app.data.repository.CurrencyRepository.getInstance(context) }
    val referralPrefs = remember { context.getSharedPreferences("referral_prefs", android.content.Context.MODE_PRIVATE) }
    val referralCount = remember { mutableIntStateOf(referralPrefs.getInt("referral_count", 0)) }
    val totalEarned = remember { mutableIntStateOf(referralPrefs.getInt("referral_coins_earned", 0)) }
    val myReferralCode = remember {
        var code = referralPrefs.getString("my_referral_code", null)
        if (code == null) {
            code = "SDK" + (100000 + (Math.random() * 899999).toInt())
            referralPrefs.edit().putString("my_referral_code", code).apply()
        }
        code!!
    }
    val hasClaimedReferral = remember { mutableStateOf(referralPrefs.getBoolean("has_claimed_referral", false)) }
    var referralCodeInput by remember { mutableStateOf("") }
    var showReferralError by remember { mutableStateOf(false) }
    var showReferralSuccess by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showCopiedSnackbar) {
        if (showCopiedSnackbar) {
            snackbarHostState.showSnackbar("Link kopiert!")
            showCopiedSnackbar = false
        }
    }

    LaunchedEffect(showReferralClaimed) {
        if (showReferralClaimed) {
            snackbarHostState.showSnackbar("+1000 Coins! Danke fürs Einladen!")
            showReferralClaimed = false
        }
    }
    
    // Generate QR Code
    val qrCodeBitmap = remember {
        generateQRCode(DOWNLOAD_URL, 512)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App teilen") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ComposeColor.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "🔢",
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Sudoku Online",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Teile die App mit Freunden!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // QR Code Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ComposeColor.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QR-Code scannen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ComposeColor.Black
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Zum direkten Download der App",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // QR Code
                    qrCodeBitmap?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ComposeColor.White)
                                .padding(8.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code zum Download",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // URL
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ComposeColor(0xFFF5F5F5)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = ComposeColor(0xFF667eea),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = DOWNLOAD_URL.removePrefix("https://"),
                                style = MaterialTheme.typography.bodySmall,
                                color = ComposeColor(0xFF667eea)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Copy Link Button
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(DOWNLOAD_URL))
                        showCopiedSnackbar = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link kopieren")
                }
                
                // Share Button
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Spiele Sudoku Online mit mir! \uD83D\uDD22\n\nLade die App hier herunter:\n$DOWNLOAD_URL\n\nGib meinen Einladungs-Code ein und wir bekommen beide 1000 Coins: $myReferralCode")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "App teilen")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Teilen")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Warum Sudoku Online?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureItem(icon = "🎮", text = "4 Spielmodi: Sudoku, Killer Sudoku, TicTacToe, Mühle")
                    FeatureItem(icon = "👥", text = "Spiele mit Freunden online")
                    FeatureItem(icon = "🏆", text = "Liga-System mit 7 Rängen")
                    FeatureItem(icon = "🎯", text = "Tägliche Herausforderungen")
                    FeatureItem(icon = "🎨", text = "Viele Themes freischaltbar")
                    FeatureItem(icon = "📊", text = "Detaillierte Statistiken")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Referral Reward Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ComposeColor.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ComposeColor(0xFF667eea),
                                    ComposeColor(0xFF764ba2)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ComposeColor.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\uD83C\uDF81", fontSize = 28.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Freunde einladen = 1000 Coins!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ComposeColor.White
                                )
                                Text(
                                    text = "Für jeden Freund, der die App über deinen QR-Code installiert, bekommst du 1000 Coins!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ComposeColor.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${referralCount.intValue}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ComposeColor.White
                                )
                                Text(
                                    text = "Eingeladen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ComposeColor.White.copy(alpha = 0.7f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalEarned.intValue}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ComposeColor(0xFFFFD700)
                                )
                                Text(
                                    text = "Coins verdient",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ComposeColor.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // My Referral Code Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ComposeColor(0xFFFFD700).copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dein Einladungs-Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor(0xFFB8860B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ComposeColor(0xFFFFD700).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = myReferralCode,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor(0xFFB8860B),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            letterSpacing = 4.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Teile diesen Code mit Freunden",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFFB8860B).copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enter Referral Code Card
            if (!hasClaimedReferral.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Einladungs-Code eingeben",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hast du einen Code von einem Freund? Gib ihn ein und ihr bekommt beide 1000 Coins!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = referralCodeInput,
                                onValueChange = {
                                    referralCodeInput = it.uppercase().take(9)
                                    showReferralError = false
                                },
                                placeholder = { Text("z.B. SDK123456") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    if (referralCodeInput.length >= 6 && referralCodeInput != myReferralCode && referralCodeInput.startsWith("SDK")) {
                                        // Valid code - award coins
                                        currencyRepository.addCoins(1000, de.sudokuonline.app.data.repository.CoinReason.LOOTBOX)
                                        hasClaimedReferral.value = true
                                        referralPrefs.edit().putBoolean("has_claimed_referral", true).apply()
                                        showReferralSuccess = true
                                        showReferralClaimed = true
                                    } else {
                                        showReferralError = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = referralCodeInput.length >= 6
                            ) {
                                Text("Einlösen")
                            }
                        }
                        if (showReferralError) {
                            Text(
                                text = "Ungültiger Code. Bitte überprüfe die Eingabe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (showReferralSuccess) {
                            Text(
                                text = "+1000 Coins erhalten!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ComposeColor(0xFF4CAF50),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Already claimed
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ComposeColor(0xFF4CAF50).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ComposeColor(0xFF4CAF50)
                        )
                        Text(
                            text = "Einladungs-Code bereits eingelöst",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ComposeColor(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FeatureItem(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun generateQRCode(text: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
