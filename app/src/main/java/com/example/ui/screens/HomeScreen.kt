package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.engine.WellnessIntelligenceEngine
import com.example.data.model.MoodType
import com.example.data.model.OrbState
import com.example.data.model.WellnessRecommendation
import com.example.ui.components.CompanionOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.MindMateAmbientBackground
import com.example.ui.components.RecommendationCard
import com.example.ui.components.WellnessProgressRing
import com.example.ui.components.moodColor
import com.example.ui.components.moodIcon
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSpecularRim
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidMidnight
import com.example.ui.theme.WarmRose
import com.example.ui.theme.eyebrow
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MindMateDashboardState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: MindMateDashboardState,
    onCheckInMood: (MoodType, String, Int) -> Unit,
    onAddHydration: (Long) -> Unit,
    onTalkNow: () -> Unit,
    onOpenJournal: () -> Unit,
    onStartActivity: (WellnessRecommendation) -> Unit,
    onActivityStatus: (WellnessRecommendation, String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = state.profile
    val firstName = profile?.displayName?.substringBefore(" ")?.ifBlank { "Friend" } ?: "Friend"
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Quiet night"
        }
    }
    val dateLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    var selectedMood by remember { mutableStateOf<MoodType?>(null) }
    var followUpNote by remember { mutableStateOf("") }
    var energyLevel by remember { mutableIntStateOf(3) }
    var showReminderInfo by remember { mutableStateOf(false) }

    val latestMood = state.moodLogs.firstOrNull()?.let { MoodType.fromId(it.mood) }
    val dailyPlan = remember(latestMood, profile, state.activityLogs) {
        WellnessIntelligenceEngine.buildDailyPlan(latestMood, profile, state.activityLogs)
    }
    val activityMap = remember(state.activityLogs) {
        state.activityLogs.associateBy { it.recommendationId }
    }

    val completedCount = state.activityLogs.count { it.status == "completed" }
    val hydrationGoal = (profile?.hydrationGoalMl ?: 2000L).coerceAtLeast(500L)
    val hydrationNow = profile?.hydrationTodayMl ?: 0L
    val hydrationProgress = (hydrationNow.toFloat() / hydrationGoal.toFloat()).coerceIn(0f, 1f)
    val planProgress = if (dailyPlan.isEmpty()) 0f else (completedCount.coerceAtMost(dailyPlan.size).toFloat() / dailyPlan.size.toFloat())

    MindMateAmbientBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Greeting Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dateLabel.uppercase(),
                            style = MaterialTheme.typography.labelMedium.eyebrow(),
                            color = SoftLavender.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$greeting, $firstName.",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Let's make today a little gentler.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(
                                onClick = { showReminderInfo = !showReminderInfo },
                                modifier = Modifier.testTag("home_notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsNone,
                                    contentDescription = "Wellness notifications",
                                    tint = SoftLavender
                                )
                            }
                            if (profile?.notificationsEnabled == true) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 10.dp, end = 10.dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(CalmMint)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(LuminousViolet.copy(alpha = 0.32f), ElectricIndigo.copy(alpha = 0.24f))
                                    )
                                )
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(listOf(LuminousViolet, CalmMint.copy(alpha = 0.7f))),
                                    CircleShape
                                )
                                .clickable(onClick = onOpenProfile)
                                .testTag("home_avatar_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showReminderInfo,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ElevatedCardSurface,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = if (profile?.notificationsEnabled == true) {
                                "Gentle check-in nudges are active. Today's cue: sip water and take a 5-minute screen pause."
                            } else {
                                "Notifications are currently paused in your Profile preferences."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Companion Spotlight Hero Card
            item {
                val heroShape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 14.dp,
                            shape = heroShape,
                            ambientColor = LuminousViolet.copy(alpha = 0.25f),
                            spotColor = Color.Black.copy(alpha = 0.6f)
                        )
                        .clip(heroShape)
                        .background(DeepIndigoSurface)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(GlassSpecularRim.copy(alpha = 0.35f), LuminousViolet.copy(alpha = 0.35f))
                            ),
                            shape = heroShape
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mindmate_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.28f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        DeepIndigoSurface.copy(alpha = 0.94f),
                                        DeepIndigoSurface.copy(alpha = 0.66f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = CalmMint.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "AI COMPANION",
                                    style = MaterialTheme.typography.labelMedium.eyebrow(),
                                    color = CalmMint,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Need someone to talk to?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Voice or text — listen, reflect, or unwind.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onTalkNow,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuminousViolet,
                                    contentColor = VoidMidnight
                                ),
                                modifier = Modifier.testTag("home_talk_now_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Talk Now", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        CompanionOrb(
                            orbState = OrbState.IDLE,
                            reducedMotion = profile?.reducedMotionEnabled == true,
                            interactive = false,
                            size = 118.dp
                        )
                    }
                }
            }

            // Voluntary Mood Check-In
            item {
                GlassCard(accentGlow = LuminousViolet) {
                    Text(
                        text = "How are you feeling right now?",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Voluntary check-in • adapts your daily rhythm",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MoodType.entries.forEach { mood ->
                            val isSelected = selectedMood == mood
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1f,
                                animationSpec = spring(dampingRatio = 0.6f),
                                label = "mood_scale"
                            )
                            val tint = moodColor(mood)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) tint.copy(alpha = 0.22f) else ElevatedCardSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) tint else GlassBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedMood = mood
                                        energyLevel = mood.score
                                    }
                                    .testTag("mood_option_${mood.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = moodIcon(mood),
                                        contentDescription = mood.label,
                                        tint = if (isSelected) tint else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = mood.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedMood != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val active = selectedMood ?: MoodType.OKAY
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = active.followUpPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftLavender
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = followUpNote,
                                onValueChange = { followUpNote = it },
                                placeholder = { Text("Optional quick note...", color = TextMuted) },
                                singleLine = false,
                                maxLines = 2,
                                shape = RoundedCornerShape(16.dp),
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
                                    .testTag("mood_note_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Energy level",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "$energyLevel / 5",
                                    style = MaterialTheme.typography.labelLarge.tabular(),
                                    color = SoftLavender
                                )
                            }
                            Slider(
                                value = energyLevel.toFloat(),
                                onValueChange = { energyLevel = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3,
                                colors = SliderDefaults.colors(
                                    thumbColor = LuminousViolet,
                                    activeTrackColor = LuminousViolet
                                )
                            )
                            Button(
                                onClick = {
                                    onCheckInMood(active, followUpNote, energyLevel)
                                    selectedMood = null
                                    followUpNote = ""
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CalmMint,
                                    contentColor = VoidMidnight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_mood_button")
                            ) {
                                Text("Save Mood Check-In", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // Today's Wellness Summary
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Rhythm",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Hydration goal: ${hydrationGoal} ml",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = TextMuted
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = ElevatedCardSurface,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = latestMood?.let { "Mood: ${it.label}" } ?: "No mood yet",
                                style = MaterialTheme.typography.labelMedium,
                                color = latestMood?.let { moodColor(it) } ?: TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WellnessProgressRing(
                            progress = planProgress,
                            label = "Daily Plan",
                            valueText = "$completedCount/${dailyPlan.size.coerceAtLeast(1)}",
                            accentColor = LuminousViolet
                        )
                        WellnessProgressRing(
                            progress = hydrationProgress,
                            label = "Hydration",
                            valueText = "${hydrationNow}ml",
                            accentColor = CalmMint
                        )
                        WellnessProgressRing(
                            progress = (state.journalEntries.size.coerceAtMost(5) / 5f),
                            label = "Journals",
                            valueText = "${state.journalEntries.size}",
                            accentColor = WarmRose
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { hydrationProgress },
                        color = CalmMint,
                        trackColor = ElevatedCardSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAddHydration(250L) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CalmMint.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_add_water_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = CalmMint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+250 ml Water", color = CalmMint, style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onOpenJournal,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_open_journal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditNote,
                                contentDescription = null,
                                tint = SoftLavender,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Journal", color = SoftLavender, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Personalized Daily Plan Header
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "Personalized Daily Plan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = latestMood?.let { "Adapted for feeling ${it.label}" } ?: "Balanced suggestions for today",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            items(dailyPlan, key = { it.id }) { rec ->
                RecommendationCard(
                    recommendation = rec,
                    status = activityMap[rec.id]?.status,
                    onStart = { onStartActivity(rec) },
                    onComplete = { onActivityStatus(rec, "completed") },
                    onSave = { onActivityStatus(rec, "saved") },
                    onDismiss = { onActivityStatus(rec, "dismissed") }
                )
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}
