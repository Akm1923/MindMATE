package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MoodLog
import com.example.data.model.MoodType
import com.example.data.model.OrbState
import com.example.data.model.WellnessCategory
import com.example.data.model.WellnessRecommendation
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSpecularRim
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceTop
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.MoodGoodColor
import com.example.ui.theme.MoodGreatColor
import com.example.ui.theme.MoodLowColor
import com.example.ui.theme.MoodOkayColor
import com.example.ui.theme.MoodStressedColor
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SubtleHairline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidMidnight
import com.example.ui.theme.WarmRose
import com.example.ui.theme.eyebrow
import com.example.ui.theme.tabular
import kotlinx.coroutines.delay

fun moodColor(mood: MoodType): Color = when (mood) {
    MoodType.GREAT -> MoodGreatColor
    MoodType.GOOD -> MoodGoodColor
    MoodType.OKAY -> MoodOkayColor
    MoodType.LOW -> MoodLowColor
    MoodType.STRESSED -> MoodStressedColor
}

fun moodIcon(mood: MoodType): ImageVector = when (mood) {
    MoodType.GREAT -> Icons.Rounded.SentimentVerySatisfied
    MoodType.GOOD -> Icons.Rounded.SentimentSatisfied
    MoodType.OKAY -> Icons.Rounded.SentimentNeutral
    MoodType.LOW -> Icons.Rounded.SentimentDissatisfied
    MoodType.STRESSED -> Icons.Rounded.Spa
}

fun categoryIcon(cat: WellnessCategory): ImageVector = when (cat) {
    WellnessCategory.MOVE -> Icons.Rounded.Favorite
    WellnessCategory.EAT_WELL -> Icons.Rounded.Spa
    WellnessCategory.RELAX -> Icons.Rounded.SelfImprovement
    WellnessCategory.GET_OUTSIDE -> Icons.Rounded.Park
    WellnessCategory.DAILY_HABITS -> Icons.Rounded.WaterDrop
}

/**
 * Shared atmospheric dark canvas across all screens so MindMate feels like
 * one continuous, softly illuminated evening product.
 */
@Composable
fun MindMateAmbientBackground(
    accentTop: Color = LuminousViolet,
    accentSide: Color = ElectricIndigo,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidMidnight)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentTop.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.16f, size.height * 0.08f),
                        radius = size.maxDimension * 0.52f
                    ),
                    radius = size.maxDimension * 0.52f,
                    center = Offset(size.width * 0.16f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentSide.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.42f),
                        radius = size.maxDimension * 0.46f
                    ),
                    radius = size.maxDimension * 0.46f,
                    center = Offset(size.width * 0.92f, size.height * 0.42f)
                )
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentGlow: Color? = null,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = if (accentGlow != null) {
        Brush.verticalGradient(
            listOf(accentGlow.copy(alpha = 0.48f), GlassBorder, SubtleHairline)
        )
    } else {
        Brush.verticalGradient(
            listOf(GlassSpecularRim.copy(alpha = 0.28f), GlassBorder, SubtleHairline)
        )
    }
    val surfaceBrush = Brush.verticalGradient(
        listOf(GlassSurfaceTop, GlassSurface)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = (accentGlow ?: LuminousViolet).copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(surfaceBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SegmentedPillSwitcher(
    tabs: List<Pair<String, String>>, // (label, testTag)
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = DeepIndigoSurface,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, (label, tag) ->
                val selected = index == selectedIndex
                val bg by animateColorAsState(
                    targetValue = if (selected) LuminousViolet.copy(alpha = 0.24f) else Color.Transparent,
                    animationSpec = spring(stiffness = 380f),
                    label = "pill_bg"
                )
                val borderCol by animateColorAsState(
                    targetValue = if (selected) LuminousViolet.copy(alpha = 0.55f) else Color.Transparent,
                    animationSpec = tween(200),
                    label = "pill_border"
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(index) }
                        .testTag(tag)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WellnessProgressRing(
    progress: Float,
    label: String,
    valueText: String,
    accentColor: Color,
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val clamped by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "ring_progress"
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val stroke = 7.5.dp.toPx()
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.14f), Color.Transparent)
                    ),
                    radius = this.size.minDimension * 0.52f
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(accentColor, LuminousViolet, accentColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun CrisisSupportBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF28182C)),
        border = BorderStroke(1.dp, WarmRose.copy(alpha = 0.52f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalHospital,
                    contentDescription = "Crisis support resources",
                    tint = WarmRose
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You don't have to carry this alone",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MindMate is an AI wellness companion, not a therapist or emergency service. If things feel overwhelming or unsafe, please reach out to a trusted person, local emergency services (911 / 112), or free confidential support:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• US & Canada: Call or text 988 (Suicide & Crisis Lifeline)\n• UK: Call 111 or Samaritans 116 123\n• India: Tele-MANAS 14416 or Kiran 1800-599-0019",
                style = MaterialTheme.typography.labelLarge,
                color = WarmRose
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Hide notice", color = SoftLavender)
                }
            }
        }
    }
}

@Composable
fun MoodTrendChart(
    moods: List<MoodLog>,
    modifier: Modifier = Modifier
) {
    val ordered = remember(moods) { moods.take(14).reversed() }
    if (ordered.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log mood check-ins to reveal your trend line",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.height(156.dp).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Great", style = MaterialTheme.typography.labelMedium, color = CalmMint.copy(alpha = 0.8f))
                Text("Okay", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text("Low", style = MaterialTheme.typography.labelMedium, color = WarmRose.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(156.dp)
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            ) {
                val w = size.width
                val h = size.height
                val stepX = if (ordered.size <= 1) 0f else w / (ordered.size - 1)

                for (level in 1..5) {
                    val y = h - ((level - 1) / 4f) * h
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = ordered.mapIndexed { idx, log ->
                    val score = MoodType.fromId(log.mood).score
                    val x = if (ordered.size == 1) w / 2f else idx * stepX
                    val y = h - ((score - 1) / 4f) * h
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cx = (prev.x + curr.x) / 2f
                            cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                        }
                    }
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, h)
                        lineTo(points.first().x, h)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                LuminousViolet.copy(alpha = 0.28f),
                                ElectricIndigo.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(listOf(ElectricIndigo, LuminousViolet, CalmMint)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                points.forEachIndexed { idx, pt ->
                    val mood = MoodType.fromId(ordered[idx].mood)
                    val dotColor = moodColor(mood)
                    drawCircle(color = dotColor.copy(alpha = 0.26f), radius = 10.dp.toPx(), center = pt)
                    drawCircle(color = VoidMidnight, radius = 6.dp.toPx(), center = pt)
                    drawCircle(color = dotColor, radius = 4.dp.toPx(), center = pt)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Earlier", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text("Latest (${ordered.size} logs)", style = MaterialTheme.typography.labelMedium.tabular(), color = TextMuted)
        }
    }
}

@Composable
fun ActivityGuidanceDialog(
    recommendation: WellnessRecommendation,
    isCompleted: Boolean,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val isBreathing = recommendation.category == WellnessCategory.RELAX
    var breathingRunning by remember { mutableStateOf(false) }
    var phaseText by remember { mutableStateOf("Tap Start Guided Timer") }
    var cycleCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(breathingRunning) {
        if (breathingRunning) {
            for (cycle in 1..4) {
                cycleCount = cycle
                phaseText = "Inhale gently (4s)"
                delay(4000L)
                phaseText = "Hold softly (4s)"
                delay(4000L)
                phaseText = "Exhale slowly (6s)"
                delay(6000L)
            }
            breathingRunning = false
            phaseText = "4 calm cycles complete"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DeepIndigoSurface,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = LuminousViolet.copy(alpha = 0.16f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoryIcon(recommendation.category),
                                contentDescription = recommendation.category.label,
                                tint = LuminousViolet
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recommendation.category.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium.eyebrow(),
                            color = CalmMint
                        )
                        Text(
                            text = recommendation.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Why suggested: ${recommendation.whySuggested}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftLavender
                )

                if (isBreathing) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CompanionOrb(
                            orbState = if (breathingRunning) OrbState.LISTENING else OrbState.IDLE,
                            audioAmplitude = if (breathingRunning) 0.45f else 0.1f,
                            size = 148.dp
                        )
                    }
                    Text(
                        text = if (cycleCount > 0) "Cycle $cycleCount/4 • $phaseText" else phaseText,
                        style = MaterialTheme.typography.titleMedium.tabular(),
                        color = CalmMint,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { breathingRunning = !breathingRunning },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("guided_breathing_toggle")
                    ) {
                        Icon(Icons.Rounded.Air, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (breathingRunning) "Pause Guide" else "Start Guided Breath")
                    }
                }

                if (recommendation.steps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Simple Steps",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    recommendation.steps.forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            onComplete()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_complete_activity"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalmMint,
                            contentColor = VoidMidnight
                        )
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCompleted) "Done" else "Complete")
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: WellnessRecommendation,
    status: String?, // "saved", "completed", or null
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCompleted = status == "completed"
    val isSaved = status == "saved"
    val shape = RoundedCornerShape(22.dp)

    val borderBrush = if (isCompleted) {
        Brush.verticalGradient(listOf(CalmMint.copy(alpha = 0.6f), CalmMint.copy(alpha = 0.2f)))
    } else {
        Brush.verticalGradient(listOf(GlassSpecularRim.copy(alpha = 0.22f), GlassBorder))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = LuminousViolet.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(ElevatedCardSurface, DeepIndigoSurface)))
            .border(width = 1.dp, brush = borderBrush, shape = shape)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) CalmMint.copy(alpha = 0.18f)
                                else LuminousViolet.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Rounded.CheckCircle else categoryIcon(recommendation.category),
                            contentDescription = recommendation.category.label,
                            tint = if (isCompleted) CalmMint else LuminousViolet,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${recommendation.category.label.uppercase()} • ${recommendation.durationMinutes} MIN",
                            style = MaterialTheme.typography.labelMedium.eyebrow().tabular(),
                            color = if (isCompleted) CalmMint else SoftLavender
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.testTag("rec_save_${recommendation.id}")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = if (isSaved) "Saved" else "Save activity",
                            tint = if (isSaved) ElectricIndigo else TextMuted
                        )
                    }
                    if (onDismiss != null) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss suggestion",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CalmMint.copy(alpha = 0.08f)
            ) {
                Text(
                    text = recommendation.whySuggested,
                    style = MaterialTheme.typography.labelMedium,
                    color = CalmMint.copy(alpha = 0.92f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rec_start_${recommendation.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuminousViolet,
                        contentColor = VoidMidnight
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Begin", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onComplete,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("rec_complete_${recommendation.id}"),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) CalmMint.copy(alpha = 0.7f) else GlassBorder
                    )
                ) {
                    Text(
                        text = if (isCompleted) "Done ✓" else "Complete",
                        color = if (isCompleted) CalmMint else TextPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
