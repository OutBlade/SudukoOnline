package de.sudokuonline.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.sudokuonline.app.data.repository.AppTheme
import de.sudokuonline.app.data.repository.ThemeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ThemeRepository.getInstance(context) }
    val currentTheme by repository.currentTheme.collectAsState()
    val isDarkMode by repository.isDarkMode.collectAsState()
    val useSystemTheme by repository.useSystemTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Design") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark Mode Settings
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Erscheinungsbild",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Use System Theme
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Systemeinstellung verwenden",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Folgt dem System-Darkmode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = useSystemTheme,
                                onCheckedChange = { repository.setUseSystemTheme(it) }
                            )
                        }

                        if (!useSystemTheme) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            // Manual Dark Mode Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isDarkMode) "Dunkelmodus" else "Hellmodus",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { repository.setDarkMode(it) }
                                )
                            }
                        }
                    }
                }
            }

            // Theme Selection Header
            item {
                Text(
                    text = "Farbschema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Theme Cards
            items(AppTheme.entries) { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = theme == currentTheme,
                    isDarkMode = isDarkMode,
                    onClick = { repository.setTheme(theme) }
                )
            }

            // Preview Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vorschau",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ThemePreview(
                    theme = currentTheme,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = if (isDarkMode) theme.primaryDark else theme.primaryLight
    val secondaryColor = if (isDarkMode) theme.secondaryDark else theme.secondaryLight
    val tertiaryColor = if (isDarkMode) theme.tertiaryDark else theme.tertiaryLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Preview Circles
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .border(2.dp, Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(secondaryColor)
                        .border(2.dp, Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tertiaryColor)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Ausgewählt",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ThemePreview(
    theme: AppTheme,
    isDarkMode: Boolean
) {
    val primary = if (isDarkMode) theme.primaryDark else theme.primaryLight
    val secondary = if (isDarkMode) theme.secondaryDark else theme.secondaryLight
    val tertiary = if (isDarkMode) theme.tertiaryDark else theme.tertiaryLight
    val background = if (isDarkMode) theme.backgroundDark else theme.backgroundLight
    val surface = if (isDarkMode) theme.surfaceDark else theme.surfaceLight
    val onSurface = if (isDarkMode) Color.White else Color.Black

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Mini App Bar Preview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sudoku Online",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini Cards Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    color = surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Karte 1",
                            color = onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    color = surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Karte 2",
                            color = onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Buttons Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text("Primär", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = secondary)
                ) {
                    Text("Sekundär", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = tertiary)
                ) {
                    Text("Tertiär", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
