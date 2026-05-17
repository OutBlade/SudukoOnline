package de.sudokuonline.app.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.sudokuonline.app.game.IntelligentHintService.IntelligentHint
import de.sudokuonline.app.game.IntelligentHintService.HintType
import de.sudokuonline.app.game.IntelligentHintService.HintDifficulty

/**
 * Beautiful animated hint overlay that shows intelligent hints with explanations
 */
@Composable
fun IntelligentHintOverlay(
    hint: IntelligentHint,
    onDismiss: () -> Unit,
    onApplyHint: () -> Unit,
    onLearnMore: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(hint.difficulty.color),
                                    Color(hint.difficulty.color).copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Technique icon
                            HintTypeIcon(hint.type)

                            // Difficulty badge
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = hint.difficulty.displayName,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = hint.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (hint.value != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Die Lösung ist: ${hint.value}",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Main content
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Explanation
                    Text(
                        text = hint.explanation,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )

                    // Step-by-step toggle
                    if (hint.detailedSteps.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDetails = !showDetails },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.FormatListNumbered,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Schritt-für-Schritt Anleitung",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showDetails,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                hint.detailedSteps.forEachIndexed { index, step ->
                                    StepItem(
                                        stepNumber = index + 1,
                                        text = step,
                                        isActive = index == currentStep,
                                        isCompleted = index < currentStep
                                    )
                                }

                                // Step navigation
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = { if (currentStep > 0) currentStep-- },
                                        enabled = currentStep > 0
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Zurück")
                                    }

                                    TextButton(
                                        onClick = { if (currentStep < hint.detailedSteps.size - 1) currentStep++ },
                                        enabled = currentStep < hint.detailedSteps.size - 1
                                    ) {
                                        Text("Weiter")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }

                    // Learning tip
                    hint.learningTip?.let { tip ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Lern-Tipp",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Highlighted cells info
                    if (hint.highlightCells.isNotEmpty() && hint.type != HintType.NO_HINT_NEEDED) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GridOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Relevante Zellen: ${hint.highlightCells.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider()

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onLearnMore,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tutorial")
                        }

                        Button(
                            onClick = onApplyHint,
                            modifier = Modifier.weight(1f),
                            enabled = hint.value != null
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Anwenden")
                        }
                    }

                    // Close button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Schließen")
                    }
                }
            }
        }
    }
}

@Composable
private fun HintTypeIcon(type: HintType) {
    val (icon, emoji) = when (type) {
        HintType.NAKED_SINGLE -> Icons.Default.Visibility to "🎯"
        HintType.HIDDEN_SINGLE -> Icons.Default.Search to "🔍"
        HintType.NAKED_PAIR -> Icons.Default.People to "👯"
        HintType.HIDDEN_PAIR -> Icons.Default.PeopleOutline to "🔎"
        HintType.NAKED_TRIPLE -> Icons.Default.Groups to "👥"
        HintType.POINTING_PAIR -> Icons.Default.ArrowForward to "👉"
        HintType.BOX_LINE_REDUCTION -> Icons.Default.GridView to "📦"
        HintType.X_WING -> Icons.Default.Flight to "✈️"
        HintType.SWORDFISH -> Icons.Default.Waves to "🐟"
        HintType.Y_WING -> Icons.Default.CallSplit to "🔱"
        HintType.NO_HINT_NEEDED -> Icons.Default.CheckCircle to "✅"
        HintType.STUCK -> Icons.Default.Help to "❓"
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    text: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor = when {
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val indicatorColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step indicator
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(indicatorColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Compact hint card for showing quick hints without full dialog
 */
@Composable
fun CompactHintCard(
    hint: IntelligentHint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(hint.difficulty.color).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(hint.difficulty.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(hint.difficulty.color),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hint.title,
                    fontWeight = FontWeight.Bold,
                    color = Color(hint.difficulty.color)
                )
                if (hint.value != null) {
                    Text(
                        text = "Setze ${hint.value} ein",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Details",
                tint = Color(hint.difficulty.color)
            )
        }
    }
}

/**
 * Mini tutorial popup for techniques
 */
@Composable
fun TechniqueMiniTutorial(
    type: HintType,
    onDismiss: () -> Unit,
    onFullTutorial: () -> Unit
) {
    val (title, description, tips) = getTechniqueInfo(type)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = description)

                if (tips.isNotEmpty()) {
                    Text(
                        text = "Tipps:",
                        fontWeight = FontWeight.Bold
                    )
                    tips.forEach { tip ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("•")
                            Text(tip, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onFullTutorial) {
                Text("Zum Tutorial")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Verstanden")
            }
        }
    )
}

private fun getTechniqueInfo(type: HintType): Triple<String, String, List<String>> {
    return when (type) {
        HintType.NAKED_SINGLE -> Triple(
            "Naked Single",
            "Ein Naked Single liegt vor, wenn für eine Zelle nur noch eine einzige Zahl möglich ist, " +
                    "weil alle anderen bereits in der Zeile, Spalte oder im Block vorkommen.",
            listOf(
                "Schaue welche Zahlen bereits in der Zeile stehen",
                "Prüfe die Spalte auf vorhandene Zahlen",
                "Vergiss nicht den 3×3 Block zu prüfen"
            )
        )
        HintType.HIDDEN_SINGLE -> Triple(
            "Hidden Single",
            "Ein Hidden Single liegt vor, wenn eine bestimmte Zahl in einer Zeile, Spalte oder Block " +
                    "nur noch an einer einzigen Stelle möglich ist.",
            listOf(
                "Frage: 'Wo kann die 5 in diesem Block hin?'",
                "Statt: 'Welche Zahl passt in diese Zelle?'",
                "Scanne systematisch alle Zahlen 1-9"
            )
        )
        HintType.X_WING -> Triple(
            "X-Wing",
            "X-Wing ist eine fortgeschrittene Technik. Sie nutzt die Tatsache, dass wenn eine Zahl " +
                    "in zwei Zeilen jeweils nur in denselben zwei Spalten vorkommen kann, " +
                    "die Zahl aus allen anderen Zellen dieser Spalten eliminiert werden kann.",
            listOf(
                "Suche eine Zahl die in einer Zeile nur 2x vorkommt",
                "Finde eine zweite Zeile mit der gleichen Einschränkung",
                "Die Spalten müssen identisch sein",
                "Eliminiere aus den restlichen Zellen dieser Spalten"
            )
        )
        else -> Triple(
            type.name.replace("_", " "),
            "Diese Technik hilft dir, Kandidaten zu eliminieren oder Zahlen zu finden.",
            emptyList()
        )
    }
}
