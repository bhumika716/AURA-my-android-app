package com.aura.app.ui.screens.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.aura.app.data.local.entities.JournalEntry
import com.aura.app.ui.components.AdaptiveCard
import com.aura.app.ui.theme.AuraTheme
import com.aura.app.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val ageGroup = AuraTheme.ageGroup

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (ageGroup == "kids") "📔 My Journal" else "Journal",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            FilledTonalButton(
                onClick = { viewModel.startWriting() },
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Write", fontWeight = FontWeight.Bold)
            }
        }

        // ── Mood Summary Card ──
        MoodSummaryCard(
            averageMoodWeek = uiState.averageMoodWeek,
            averageMoodMonth = uiState.averageMoodMonth,
            totalEntries = uiState.totalEntries,
        )

        // ── Recent Mood Row (Horizontal scroll of mood dots) ──
        if (uiState.entries.isNotEmpty()) {
            Text(
                text = "Recent Moods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.entries.take(14)) { entry ->
                    MoodBubble(entry = entry, onClick = { viewModel.selectEntry(entry) })
                }
            }
        }

        // ── Entry List ──
        Text(
            text = if (ageGroup == "kids") "📝 My Entries" else "All Entries",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (uiState.entries.isEmpty()) {
            EmptyJournalState()
        } else {
            uiState.entries.forEach { entry ->
                JournalEntryCard(entry = entry, onClick = { viewModel.selectEntry(entry) })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // ── Compose Dialog ──
    if (uiState.isWriting) {
        ComposeEntryDialog(
            draftText = uiState.draftText,
            onTextChange = { viewModel.updateDraft(it) },
            onSubmit = { mood, emoji ->
                viewModel.submitEntry(mood, emoji)
            },
            onDismiss = { viewModel.cancelWriting() },
        )
    }

    // ── Analyzing Overlay ──
    if (uiState.isAnalyzing) {
        AnalyzingOverlay()
    }

    // ── AI Insight Dialog ──
    uiState.latestInsight?.let { insight ->
        InsightDialog(insight = insight, onDismiss = { viewModel.dismissInsight() })
    }

    // ── Entry Detail Dialog ──
    if (uiState.showEntryDetail && uiState.selectedEntry != null) {
        EntryDetailDialog(
            entry = uiState.selectedEntry!!,
            onDismiss = { viewModel.dismissEntryDetail() },
            onDelete = { viewModel.deleteEntry(it) },
        )
    }
}

// ──────────────────────────────────────────────────────
// COMPOSABLE COMPONENTS
// ──────────────────────────────────────────────────────

@Composable
private fun MoodSummaryCard(
    averageMoodWeek: Float?,
    averageMoodMonth: Float?,
    totalEntries: Int,
) {
    AdaptiveCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Mood Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MoodStatItem(
                    emoji = getMoodEmoji(averageMoodWeek),
                    label = "This Week",
                    value = averageMoodWeek?.toInt()?.toString() ?: "—",
                )
                MoodStatItem(
                    emoji = getMoodEmoji(averageMoodMonth),
                    label = "This Month",
                    value = averageMoodMonth?.toInt()?.toString() ?: "—",
                )
                MoodStatItem(
                    emoji = "📔",
                    label = "Total Entries",
                    value = "$totalEntries",
                )
            }
        }
    }
}

@Composable
private fun MoodStatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MoodBubble(entry: JournalEntry, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd\nMMM", Locale.getDefault()) }
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(getMoodColor(entry.moodScore).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = entry.moodEmoji, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateFormat.format(Date(entry.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    AdaptiveCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // Mood indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getMoodColor(entry.moodScore).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = entry.moodEmoji, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.rawText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(entry.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    if (entry.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        entry.tags.split(",").take(2).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.padding(end = 4.dp),
                            ) {
                                Text(
                                    text = tag.trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
            // Mood score pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getMoodColor(entry.moodScore).copy(alpha = 0.15f),
            ) {
                Text(
                    text = "${entry.moodScore}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = getMoodColor(entry.moodScore),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyJournalState() {
    AdaptiveCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📝", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your journal is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start writing to track your mood\nand earn XP along the way!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ComposeEntryDialog(
    draftText: String,
    onTextChange: (String) -> Unit,
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "✍️ How are you feeling?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "Write freely. Our AI will analyze your mood and give you a personalized insight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                Text(
                    text = "Select your mood",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                val moodOptions = listOf(
                    "😊" to "happy",
                    "😔" to "sad",
                    "😟" to "anxious",
                    "🤩" to "excited",
                    "😌" to "calm",
                    "😡" to "angry",
                    "😐" to "neutral",
                )

                var selectedMood by remember { mutableStateOf<String?>(null) }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(moodOptions) { (emoji, mood) ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { selectedMood = mood },
                            label = {
                                Text("$emoji ${mood.replaceFirstChar { it.uppercase() }}")
                            },
                        )
                    }
                }
                val selectedEmoji = moodOptions
                    .firstOrNull { it.second == selectedMood }
                    ?.first

                OutlinedTextField(
                    value = draftText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = { Text("Today I felt...") },
                    shape = RoundedCornerShape(16.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (selectedMood != null && selectedEmoji != null) {
                                onSubmit(selectedMood!!, selectedEmoji!!)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = draftText.length >= 10 && selectedMood != null,
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze ✨", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "+20 XP  •  Min 10 characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AnalyzingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "🧠",
                    fontSize = 48.sp,
                    modifier = Modifier.graphicsLayer(alpha = alpha),
                )
                Text(
                    text = "Analyzing your mood...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    text = "Our AI is reading your emotions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun InsightDialog(insight: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("💡", fontSize = 40.sp)
                Text(
                    text = "AI Insight",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                ) {
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                    )
                }
                Text(
                    text = "+20 XP earned!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Got it ✨", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EntryDetailDialog(
    entry: JournalEntry,
    onDismiss: () -> Unit,
    onDelete: (JournalEntry) -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd yyyy • hh:mm a", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header with mood
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.moodEmoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.primaryMood.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = dateFormat.format(Date(entry.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        )
                    }
                }

                // Mood Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MetricPill("Mood", "${entry.moodScore}/100", getMoodColor(entry.moodScore))
                    MetricPill("Energy", entry.energyLevel, MaterialTheme.colorScheme.tertiary)
                    MetricPill("Burnout", "${entry.burnoutScore}%",
                        if (entry.burnoutScore > 60) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Entry text
                Text(
                    text = entry.rawText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                )

                // AI Insight
                if (entry.aiInsight.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Text("💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.aiInsight,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                // Tags
                if (entry.tags.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.tags.split(",").forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    text = "#${tag.trim()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// UTILITY FUNCTIONS
// ──────────────────────────────────────────────────────

private fun getMoodColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)   // Green — great
    score >= 60 -> Color(0xFF8BC34A)   // Light green — good
    score >= 40 -> Color(0xFFFFCA28)   // Yellow — neutral
    score >= 20 -> Color(0xFFFF9800)   // Orange — low
    else -> Color(0xFFE53935)          // Red — very low
}

private fun getMoodEmoji(score: Float?): String = when {
    score == null -> "🆕"
    score >= 80 -> "😄"
    score >= 60 -> "🙂"
    score >= 40 -> "😐"
    score >= 20 -> "😔"
    else -> "😢"
}
