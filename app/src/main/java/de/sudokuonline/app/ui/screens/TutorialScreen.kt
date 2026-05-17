package de.sudokuonline.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.sudokuonline.app.tutorial.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val tutorialManager = remember { TutorialManager.getInstance(context) }

    val currentLesson by tutorialManager.currentLesson.collectAsState()
    val currentStepIndex by tutorialManager.currentStepIndex.collectAsState()
    val completedLessons by tutorialManager.completedLessons.collectAsState()

    if (currentLesson != null) {
        LessonView(
            lesson = currentLesson!!,
            currentStepIndex = currentStepIndex,
            tutorialManager = tutorialManager,
            onExit = { tutorialManager.exitLesson() }
        )
    } else {
        LessonListView(
            lessons = tutorialManager.lessons,
            completedLessons = completedLessons,
            tutorialManager = tutorialManager,
            onBackClick = onBackClick,
            onStartLesson = { tutorialManager.startLesson(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonListView(
    lessons: List<TutorialLesson>,
    completedLessons: Set<Int>,
    tutorialManager: TutorialManager,
    onBackClick: () -> Unit,
    onStartLesson: (Int) -> Unit
) {
    val progress = tutorialManager.getProgress()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sudoku Lernen") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Dein Fortschritt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${completedLessons.size} von ${lessons.size} Lektionen abgeschlossen",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (completedLessons.size == lessons.size) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700)
                                )
                                Text(
                                    text = "Alle Lektionen abgeschlossen!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Lessons
            itemsIndexed(lessons) { index, lesson ->
                LessonCard(
                    lesson = lesson,
                    index = index,
                    isCompleted = tutorialManager.isLessonCompleted(index),
                    isUnlocked = tutorialManager.isLessonUnlocked(index),
                    onClick = { if (tutorialManager.isLessonUnlocked(index)) onStartLesson(index) }
                )
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: TutorialLesson,
    index: Int,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (isUnlocked) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lesson number with icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary
                        else Color(lesson.difficulty.color).copy(alpha = alpha)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Abgeschlossen",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (!isUnlocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Gesperrt",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = lesson.icon,
                        fontSize = 24.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Lektion ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )

                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )

                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(lesson.difficulty.color).copy(alpha = 0.2f * alpha),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = lesson.difficulty.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(lesson.difficulty.color).copy(alpha = alpha),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "~${lesson.estimatedMinutes} Min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                    )
                }
            }

            if (isUnlocked) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Starten",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonView(
    lesson: TutorialLesson,
    currentStepIndex: Int,
    tutorialManager: TutorialManager,
    onExit: () -> Unit
) {
    val currentStep = lesson.steps.getOrNull(currentStepIndex) ?: return
    var showExitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Schritt ${currentStepIndex + 1} von ${lesson.steps.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Beenden")
                    }
                }
            )
        },
        bottomBar = {
            LessonNavigationBar(
                currentStepIndex = currentStepIndex,
                totalSteps = lesson.steps.size,
                onPrevious = { tutorialManager.previousStep() },
                onNext = {
                    if (!tutorialManager.nextStep()) {
                        tutorialManager.completeLesson(lesson.id)
                        onExit()
                    }
                },
                isLastStep = currentStepIndex == lesson.steps.size - 1
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStepIndex + 1f) / lesson.steps.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )

            AnimatedContent(
                targetState = currentStepIndex,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "step_animation"
            ) { stepIndex ->
                val step = lesson.steps.getOrNull(stepIndex)
                if (step != null) {
                    when (step) {
                        is TutorialStep.Explanation -> ExplanationStepView(step)
                        is TutorialStep.Exercise -> ExerciseStepView(
                            step = step,
                            onCorrect = {
                                if (!tutorialManager.nextStep()) {
                                    tutorialManager.completeLesson(lesson.id)
                                    onExit()
                                }
                            }
                        )
                        is TutorialStep.Quiz -> QuizStepView(
                            step = step,
                            onCorrect = {
                                if (!tutorialManager.nextStep()) {
                                    tutorialManager.completeLesson(lesson.id)
                                    onExit()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Lektion beenden?") },
            text = { Text("Dein Fortschritt in dieser Lektion geht verloren.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text("Beenden")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Weitermachen")
                }
            }
        )
    }
}

@Composable
private fun LessonNavigationBar(
    currentStepIndex: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isLastStep: Boolean
) {
    Surface(
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrevious,
                enabled = currentStepIndex > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Zurueck")
            }

            // Step indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentStepIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Button(onClick = onNext) {
                Text(if (isLastStep) "Fertig" else "Weiter")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun ExplanationStepView(step: TutorialStep.Explanation) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )

        // Mini Sudoku Board visualization if cells are highlighted
        if (step.highlightCells.isNotEmpty() || step.exampleBoard != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MiniSudokuBoard(
                        board = step.exampleBoard ?: Array(9) { IntArray(9) },
                        highlightCells = step.highlightCells,
                        highlightRegion = step.highlightRegion
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseStepView(
    step: TutorialStep.Exercise,
    onCorrect: () -> Unit
) {
    var selectedNumber by remember { mutableStateOf<Int?>(null) }
    var showHint by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge
        )

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Technik: ${step.technique}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                MiniSudokuBoard(
                    board = step.board,
                    highlightCells = listOf(step.targetCell),
                    selectedCell = step.targetCell,
                    enteredValue = selectedNumber
                )
            }
        }

        // Number selection
        Text(
            text = "Wähle die richtige Zahl:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (num in 1..9) {
                NumberButton(
                    number = num,
                    isSelected = selectedNumber == num,
                    onClick = {
                        selectedNumber = num
                        showResult = true
                        isCorrect = num == step.correctAnswer
                    }
                )
            }
        }

        // Result
        AnimatedVisibility(visible = showResult) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect)
                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else
                        Color(0xFFF44336).copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isCorrect) "Richtig!" else "Leider falsch",
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        if (!isCorrect) {
                            Text(
                                text = "Versuche es noch einmal oder nutze den Hinweis.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (isCorrect) {
            Button(
                onClick = onCorrect,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Weiter")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            TextButton(
                onClick = { showHint = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hinweis anzeigen")
            }
        }

        AnimatedVisibility(visible = showHint && !isCorrect) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(text = step.hint)
                }
            }
        }
    }
}

@Composable
private fun QuizStepView(
    step: TutorialStep.Quiz,
    onCorrect: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = step.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Options
        step.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == index
            val isCorrectOption = index == step.correctIndex
            val showCorrectness = showResult && isSelected

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !showResult) {
                        selectedOption = index
                        showResult = true
                    },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        showCorrectness && isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        showCorrectness && !isCorrectOption -> Color(0xFFF44336).copy(alpha = 0.2f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (showCorrectness) {
                        Icon(
                            if (isCorrectOption) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isCorrectOption) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }

        // Explanation
        AnimatedVisibility(visible = showResult) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (selectedOption == step.correctIndex) "Richtig!" else "Erklaerung:",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedOption == step.correctIndex)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = step.explanation,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (showResult && selectedOption == step.correctIndex) {
            Button(
                onClick = onCorrect,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Weiter")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else if (showResult && selectedOption != step.correctIndex) {
            TextButton(
                onClick = {
                    selectedOption = null
                    showResult = false
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Noch einmal versuchen")
            }
        }
    }
}

@Composable
private fun NumberButton(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        color = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniSudokuBoard(
    board: Array<IntArray>,
    highlightCells: List<Pair<Int, Int>> = emptyList(),
    highlightRegion: HighlightRegion? = null,
    selectedCell: Pair<Int, Int>? = null,
    enteredValue: Int? = null
) {
    val cellSize = 28.dp

    Column {
        for (row in 0..8) {
            Row {
                for (col in 0..8) {
                    val isHighlighted = (row to col) in highlightCells
                    val isSelected = selectedCell == (row to col)
                    val value = if (isSelected && enteredValue != null) enteredValue else board[row][col]

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                width = when {
                                    col % 3 == 0 && col > 0 -> 2.dp
                                    else -> 0.5.dp
                                },
                                color = MaterialTheme.colorScheme.outline.copy(
                                    alpha = if (col % 3 == 0 && col > 0) 1f else 0.3f
                                )
                            )
                            .then(
                                if (row % 3 == 0 && row > 0) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value != 0) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
