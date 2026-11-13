package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.engine.WellnessIntelligenceEngine
import com.example.data.model.JournalEntry
import com.example.data.model.MoodType
import com.example.ui.components.GlassCard
import com.example.ui.components.MindMateAmbientBackground
import com.example.ui.components.MoodTrendChart
import com.example.ui.components.SegmentedPillSwitcher
import com.example.ui.components.moodColor
import com.example.ui.components.moodIcon
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidMidnight
import com.example.ui.theme.eyebrow
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MindMateDashboardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsAndJournalScreen(
    state: MindMateDashboardState,
    activeSubTab: Int,
    onSelectSubTab: (Int) -> Unit,
    onSelectRangeDays: (Int) -> Unit,
    onToggleSamplePreview: (Boolean) -> Unit,
    onCreateJournal: (String, String, String) -> Unit,
    onUpdateJournal: (JournalEntry) -> Unit,
    onDeleteJournal: (String) -> Unit,
    onGenerateJournalReflection: (JournalEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    MindMateAmbientBackground(
        accentTop = LuminousViolet,
        accentSide = CalmMint,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                SegmentedPillSwitcher(
                    tabs = listOf(
                        "Mood & Insights" to "insights_subtab",
                        "Personal Journal" to "journal_subtab"
                    ),
                    selectedIndex = activeSubTab,
                    onSelect = onSelectSubTab
                )
            }

            if (activeSubTab == 0) {
                InsightsContent(
                    state = state,
                    onSelectRangeDays = onSelectRangeDays,
                    onToggleSamplePreview = onToggleSamplePreview
                )
            } else {
                JournalContent(
                    state = state,
                    onCreateJournal = onCreateJournal,
                    onUpdateJournal = onUpdateJournal,
                    onDeleteJournal = onDeleteJournal,
                    onGenerateJournalReflection = onGenerateJournalReflection
                )
            }
        }
    }
}

@Composable
private fun InsightsContent(
    state: MindMateDashboardState,
    onSelectRangeDays: (Int) -> Unit,
    onToggleSamplePreview: (Boolean) -> Unit
) {
    val cutoffMillis = remember(state.insightsRangeDays) {
        System.currentTimeMillis() - state.insightsRangeDays.toLong() * 86_400_000L
    }
    val filteredMoods = remember(state.moodLogs, cutoffMillis) {
        state.moodLogs.filter { log ->
            val ts = log.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            ts >= cutoffMillis
        }
    }
    val observation = remember(filteredMoods, state.activityLogs) {
        WellnessIntelligenceEngine.generateReflectiveObservation(filteredMoods, state.activityLogs)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "EMOTIONAL PATTERNS",
                    style = MaterialTheme.typography.labelMedium.eyebrow(),
                    color = SoftLavender
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personal Insights",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (state.useSamplePreview) {
                        "Viewing Sample Preview Data (separate from your logs)"
                    } else {
                        "Based on your self-reported mood check-ins"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.useSamplePreview) CalmMint else TextSecondary
                )
            }
        }

        // Sample preview opt-in toggle when user has no personal logs yet
        if (state.moodLogs.isEmpty() || state.useSamplePreview) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ElevatedCardSurface,
                    border = BorderStroke(1.dp, CalmMint.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Explore with Sample Week Data",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Preview charts with realistic sample check-ins before building your own history.",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = state.useSamplePreview,
                            onCheckedChange = onToggleSamplePreview,
                            modifier = Modifier.testTag("toggle_sample_preview")
                        )
                    }
                }
            }
        }

        // Date Range Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "Last 7 days", 30 to "Last 30 days", 90 to "Last 90 days").forEach { (days, label) ->
                    val selected = state.insightsRangeDays == days
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectRangeDays(days) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LuminousViolet.copy(alpha = 0.25f),
                            selectedLabelColor = TextPrimary,
                            containerColor = DeepIndigoSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = GlassBorder,
                            selectedBorderColor = LuminousViolet
                        ),
                        modifier = Modifier.testTag("insights_range_$days")
                    )
                }
            }
        }

        // Reflective Observation Card
        item {
            GlassCard(accentGlow = CalmMint) {
                Text(
                    text = "REFLECTIVE OBSERVATION",
                    style = MaterialTheme.typography.labelMedium.eyebrow(),
                    color = CalmMint
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = observation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Non-clinical summary of your own logged check-ins.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }

        // Mood Trend Chart
        item {
            GlassCard {
                Text(
                    text = "Mood Trend (${state.insightsRangeDays}d)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                MoodTrendChart(moods = filteredMoods)
            }
        }

        // Mood Distribution
        item {
            GlassCard {
                Text(
                    text = "Mood Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                val total = filteredMoods.size.coerceAtLeast(1)
                MoodType.entries.forEach { mood ->
                    val count = filteredMoods.count { it.mood == mood.id }
                    val fraction = count.toFloat() / total.toFloat()
                    val mColor = moodColor(mood)
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = moodIcon(mood),
                                    contentDescription = null,
                                    tint = mColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(mood.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            }
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = { if (filteredMoods.isEmpty()) 0f else fraction },
                            color = mColor,
                            trackColor = ElevatedCardSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // Activity Completion History
        item {
            GlassCard {
                Text(
                    text = "Activity Engagement",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val completed = state.activityLogs.filter { it.status == "completed" }
                val saved = state.activityLogs.filter { it.status == "saved" }
                Text(
                    text = "${completed.size} completed • ${saved.size} saved • ${state.journalEntries.size} journal entries",
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = SoftLavender
                )
                if (completed.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    completed.take(5).forEach { act ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(act.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(
                                text = "${act.durationMinutes} min",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = CalmMint
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
private fun JournalContent(
    state: MindMateDashboardState,
    onCreateJournal: (String, String, String) -> Unit,
    onUpdateJournal: (JournalEntry) -> Unit,
    onDeleteJournal: (String) -> Unit,
    onGenerateJournalReflection: (JournalEntry) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editorTarget by remember { mutableStateOf<JournalEntry?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filtered = remember(state.journalEntries, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) state.journalEntries
        else state.journalEntries.filter {
            it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ENCRYPTED SPACE",
                        style = MaterialTheme.typography.labelMedium.eyebrow(),
                        color = SoftLavender
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Private Journal",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your entries stay private to your account.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuminousViolet,
                        contentColor = VoidMidnight
                    ),
                    modifier = Modifier.testTag("new_journal_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Write")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextMuted) },
                placeholder = { Text("Search your reflections...", color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuminousViolet,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = DeepIndigoSurface,
                    unfocusedContainerColor = DeepIndigoSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input")
            )
        }

        if (state.errorMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DeepIndigoSurface,
                    border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorCoral,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching entries" else "Your journal is waiting",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Write freely about your day, then optionally invite Gemini to offer a gentle summary and reflection question.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { entry ->
                val dateStr = remember(entry.createdAt) {
                    val d = entry.createdAt?.toDate() ?: Date()
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(d)
                }
                val isReflecting = state.journalReflectingId == entry.id
                val isSample = entry.id.startsWith("sample_")

                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title.ifBlank { "Untitled Reflection" },
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isSample) "$dateStr • Sample Preview" else dateStr,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }
                        if (entry.moodTag.isNotBlank()) {
                            val mood = MoodType.fromId(entry.moodTag)
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = moodColor(mood).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = mood.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = moodColor(mood),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (entry.aiSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DeepIndigoSurface,
                            border = BorderStroke(1.dp, CalmMint.copy(alpha = 0.38f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "AI REFLECTION",
                                    style = MaterialTheme.typography.labelMedium.eyebrow(),
                                    color = CalmMint
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.aiSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                if (entry.aiPromptQuestion.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Reflect: ${entry.aiPromptQuestion}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SoftLavender
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onGenerateJournalReflection(entry) },
                            enabled = !isReflecting,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("journal_reflect_btn_${entry.id}")
                        ) {
                            if (isReflecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = LuminousViolet
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reflecting...")
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Reflect")
                            }
                        }
                        if (!isSample) {
                            Row {
                                IconButton(onClick = { editorTarget = entry }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit entry", tint = SoftLavender)
                                }
                                IconButton(onClick = { onDeleteJournal(entry.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete entry", tint = ErrorCoral)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    if (showCreateDialog || editorTarget != null) {
        JournalEditorDialog(
            existing = editorTarget,
            onDismiss = {
                showCreateDialog = false
                editorTarget = null
            },
            onSave = { title, content, moodTag ->
                val target = editorTarget
                if (target == null) {
                    onCreateJournal(title, content, moodTag)
                } else {
                    onUpdateJournal(target.copy(title = title, content = content, moodTag = moodTag))
                }
                showCreateDialog = false
                editorTarget = null
            }
        )
    }
}

@Composable
private fun JournalEditorDialog(
    existing: JournalEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var content by remember(existing) { mutableStateOf(existing?.content.orEmpty()) }
    var moodTag by remember(existing) { mutableStateOf(existing?.moodTag.orEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = DeepIndigoSurface,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = if (existing == null) "New Journal Entry" else "Edit Entry",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_title_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_content_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Optional mood tag", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MoodType.entries.forEach { m ->
                        val selected = moodTag == m.id
                        FilterChip(
                            selected = selected,
                            onClick = { moodTag = if (selected) "" else m.id },
                            label = { Text(m.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, content, moodTag) },
                        enabled = content.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuminousViolet,
                            contentColor = VoidMidnight
                        ),
                        modifier = Modifier.testTag("save_journal_button")
                    ) {
                        Text("Save Privately")
                    }
                }
            }
        }
    }
}
