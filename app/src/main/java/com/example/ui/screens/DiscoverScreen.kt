package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.engine.DiscoverFilters
import com.example.data.engine.WellnessIntelligenceEngine
import com.example.data.model.MoodType
import com.example.data.model.WellnessCategory
import com.example.data.model.WellnessRecommendation
import com.example.ui.components.GlassCard
import com.example.ui.components.MindMateAmbientBackground
import com.example.ui.components.RecommendationCard
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSpecularRim
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidMidnight
import com.example.ui.theme.eyebrow
import com.example.ui.viewmodel.MindMateDashboardState

@Composable
fun DiscoverScreen(
    state: MindMateDashboardState,
    onUpdateFilters: (DiscoverFilters) -> Unit,
    onStartActivity: (WellnessRecommendation) -> Unit,
    onActivityStatus: (WellnessRecommendation, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = state.discoverFilters
    val results = remember(filters, state.profile, state.activityLogs) {
        WellnessIntelligenceEngine.filterDiscover(filters, state.profile, state.activityLogs)
    }
    val statusMap = remember(state.activityLogs) {
        state.activityLogs.associateBy { it.recommendationId }
    }
    var showRefineFilters by remember { mutableStateOf(false) }

    val activeRefineCount = remember(filters) {
        listOf(
            filters.maxMinutes != 60,
            filters.indoorOutdoor != "all",
            filters.budget != "all",
            filters.moodFilter != null
        ).count { it }
    }

    MindMateAmbientBackground(
        accentTop = ElectricIndigo,
        accentSide = CalmMint,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURATED WELLNESS",
                            style = MaterialTheme.typography.labelMedium.eyebrow(),
                            color = CalmMint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Discover",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Everyday ideas shaped around your time, space, and mood.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        onClick = { showRefineFilters = !showRefineFilters },
                        shape = RoundedCornerShape(14.dp),
                        color = if (activeRefineCount > 0 || showRefineFilters) {
                            LuminousViolet.copy(alpha = 0.22f)
                        } else {
                            DeepIndigoSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (activeRefineCount > 0 || showRefineFilters) LuminousViolet else GlassBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Toggle detail filters",
                                tint = SoftLavender,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeRefineCount > 0) "Refine ($activeRefineCount)" else "Refine",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Editorial Visual Showcase Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VisualCategoryHighlight(
                        title = "Eat Well",
                        subtitle = "Indian & balanced",
                        drawableRes = R.drawable.img_discover_nutrition,
                        selected = filters.category == WellnessCategory.EAT_WELL,
                        onClick = {
                            onUpdateFilters(
                                filters.copy(
                                    category = if (filters.category == WellnessCategory.EAT_WELL) null else WellnessCategory.EAT_WELL
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VisualCategoryHighlight(
                        title = "Get Outside",
                        subtitle = "Parks & fresh air",
                        drawableRes = R.drawable.img_discover_nature,
                        selected = filters.category == WellnessCategory.GET_OUTSIDE,
                        onClick = {
                            onUpdateFilters(
                                filters.copy(
                                    category = if (filters.category == WellnessCategory.GET_OUTSIDE) null else WellnessCategory.GET_OUTSIDE
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filters.category == null,
                        onClick = { onUpdateFilters(filters.copy(category = null)) },
                        label = { Text("All") },
                        shape = RoundedCornerShape(14.dp),
                        colors = discoverChipColors(),
                        border = discoverChipBorder(filters.category == null),
                        modifier = Modifier.testTag("discover_cat_all")
                    )
                    WellnessCategory.entries.forEach { cat ->
                        val selected = filters.category == cat
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateFilters(filters.copy(category = if (selected) null else cat)) },
                            label = { Text(cat.label) },
                            shape = RoundedCornerShape(14.dp),
                            colors = discoverChipColors(),
                            border = discoverChipBorder(selected),
                            modifier = Modifier.testTag("discover_cat_${cat.id}")
                        )
                    }
                }
            }

            // Collapsible Time, Indoor/Outdoor, Budget & Mood Refine Panel
            item {
                AnimatedVisibility(
                    visible = showRefineFilters,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GlassCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REFINE SUGGESTIONS",
                                style = MaterialTheme.typography.labelMedium.eyebrow(),
                                color = SoftLavender
                            )
                            if (activeRefineCount > 0) {
                                TextButton(onClick = { onUpdateFilters(DiscoverFilters(category = filters.category)) }) {
                                    Text("Reset", color = CalmMint)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10 to "≤10 min", 20 to "≤20 min", 60 to "Any time").forEach { (mins, label) ->
                                val selected = filters.maxMinutes == mins
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateFilters(filters.copy(maxMinutes = mins)) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = discoverChipColors(),
                                    border = discoverChipBorder(selected)
                                )
                            }
                            listOf("all" to "Indoor & Out", "indoor" to "Indoor", "outdoor" to "Outdoor").forEach { (key, label) ->
                                val selected = filters.indoorOutdoor == key
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateFilters(filters.copy(indoorOutdoor = key)) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = discoverChipColors(),
                                    border = discoverChipBorder(selected)
                                )
                            }
                            listOf("all" to "Any budget", "free" to "Free").forEach { (key, label) ->
                                val selected = filters.budget == key
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateFilters(filters.copy(budget = key)) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = discoverChipColors(),
                                    border = discoverChipBorder(selected)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mood:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                            MoodType.entries.forEach { mood ->
                                val selected = filters.moodFilter == mood
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onUpdateFilters(filters.copy(moodFilter = if (selected) null else mood))
                                    },
                                    label = { Text(mood.label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = discoverChipColors(),
                                    border = discoverChipBorder(selected)
                                )
                            }
                        }
                    }
                }
            }

            if (results.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No activities match all active filters",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try widening the time limit or resetting filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = { onUpdateFilters(DiscoverFilters()) }) {
                                Text("Reset Filters", color = CalmMint)
                            }
                        }
                    }
                }
            } else {
                items(results, key = { it.id }) { rec ->
                    RecommendationCard(
                        recommendation = rec,
                        status = statusMap[rec.id]?.status,
                        onStart = { onStartActivity(rec) },
                        onComplete = { onActivityStatus(rec, "completed") },
                        onSave = { onActivityStatus(rec, "saved") },
                        onDismiss = { onActivityStatus(rec, "dismissed") }
                    )
                }
            }

            item {
                Text(
                    text = "Suggestions are general wellness ideas, not medical or clinical prescriptions.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun VisualCategoryHighlight(
    title: String,
    subtitle: String,
    drawableRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) LuminousViolet else GlassSpecularRim.copy(alpha = 0.28f)
        ),
        modifier = modifier
            .height(122.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = if (selected) LuminousViolet.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.45f)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                VoidMidnight.copy(alpha = 0.12f),
                                DeepIndigoSurface.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = SoftLavender)
            }
        }
    }
}

@Composable
private fun discoverChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LuminousViolet.copy(alpha = 0.25f),
    selectedLabelColor = TextPrimary,
    containerColor = DeepIndigoSurface,
    labelColor = TextSecondary
)

@Composable
private fun discoverChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = GlassBorder,
    selectedBorderColor = LuminousViolet
)
