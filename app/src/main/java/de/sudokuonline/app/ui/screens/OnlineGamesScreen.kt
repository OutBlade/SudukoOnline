package de.sudokuonline.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineGamesScreen(
    onBackClick: () -> Unit,
    onSudokuMultiplayerClick: () -> Unit,
    onTicTacToeClick: () -> Unit,
    onDameOnlineClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Online Spielen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zuruck")
                    }
                }
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
            // Sudoku Section
            OnlineSection(
                title = "Sudoku",
                color = MaterialTheme.colorScheme.primary
            ) {
                OnlineGameOption(
                    icon = Icons.Default.Groups,
                    title = "Sudoku Multiplayer",
                    subtitle = "1v1 oder Koop mit Freunden",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onSudokuMultiplayerClick
                )
            }

            // TicTacToe Section
            OnlineSection(
                title = "TicTacToe",
                color = MaterialTheme.colorScheme.tertiary
            ) {
                OnlineGameOption(
                    icon = Icons.Default.Grid3x3,
                    title = "Klassik",
                    subtitle = "Das klassische Spiel online",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onTicTacToeClick
                )
                OnlineGameOption(
                    icon = Icons.Default.FlashOn,
                    title = "Bomben",
                    subtitle = "Mit Bomben die Gegner-Steine zerstoeren",
                    color = Color(0xFFFF5722),
                    onClick = onTicTacToeClick
                )
                OnlineGameOption(
                    icon = Icons.Default.Dashboard,
                    title = "Ultimate",
                    subtitle = "9 Bretter - wer 3 gewinnt siegt!",
                    color = Color(0xFF9C27B0),
                    onClick = onTicTacToeClick
                )
            }

            // Dame Section
            OnlineSection(
                title = "Dame",
                color = Color(0xFF795548)
            ) {
                OnlineGameOption(
                    icon = Icons.Default.GridOn,
                    title = "Dame Online",
                    subtitle = "Spiele Dame gegen echte Gegner",
                    color = Color(0xFF795548),
                    onClick = onDameOnlineClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnlineSection(
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

@Composable
private fun OnlineGameOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = subtitle,
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
