package com.example.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbState
import com.example.ui.theme.CalmMint
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.WarmRose
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * AGSL Siri-inspired volumetric translucent 3D plasma orb shader (Android 13+ / API 33+).
 * Renders layered chromatic aurora ribbons swirling in 3D inside a glass sphere,
 * driven continuously in real time by voice amplitude and state energy.
 */
@Language("AGSL")
private const val ORB_SIRI_VOLUMETRIC_AGSL = """
uniform float2 uCenter;
uniform float uRadius;
uniform float uTime;
uniform float uAmp;
uniform float uDeform;
uniform float2 uTilt;
uniform float3 uPrimary;
uniform float3 uSecondary;
uniform float3 uHighlight;

float ribbonWave(float3 p, float t, float freq, float speed, float twist) {
    float phase = p.x * freq + sin(p.y * 2.3 + t * speed) * twist + cos(p.z * 2.1 - t * speed * 0.7) * 0.9;
    float wave = exp(-abs(p.y + sin(phase) * (0.34 + uAmp * 0.22)) * (3.6 - uAmp * 1.1));
    return wave;
}

half4 main(float2 fragCoord) {
    float2 delta = (fragCoord - uCenter) / max(uRadius, 1.0);
    float dist = length(delta);
    float angle = atan(delta.y, delta.x);

    // Organic Siri-like boundary ripple driven by real-time voice amplitude
    float waveA = sin(angle * 3.0 + uTime * 2.4) * cos(angle * 2.0 - uTime * 1.6);
    float waveB = sin(angle * 5.0 - uTime * 3.4) * 0.5;
    float edgeWobble = (waveA + waveB) * (0.012 + uDeform * 0.036 + uAmp * 0.085);
    float rNorm = dist / (1.0 + edgeWobble);

    // Soft atmospheric corona glow around the sphere
    if (rNorm > 1.0) {
        float outside = rNorm - 1.0;
        float glow = exp(-outside * 4.2) * (0.34 + uAmp * 0.36);
        float shimmer = 0.84 + 0.16 * sin(angle * 3.0 + uTime * 1.6);
        float3 auraCol = mix(uPrimary, uHighlight, 0.5 + 0.5 * sin(angle * 2.0 - uTime * 0.8));
        float a = clamp(glow * shimmer, 0.0, 0.72);
        return half4(auraCol * a, a);
    }

    float z = sqrt(max(0.0001, 1.0 - rNorm * rNorm));
    float3 N = normalize(float3(delta.x, delta.y, z));
    float3 V = float3(0.0, 0.0, 1.0);

    // 3D rotated coordinates inside the translucent sphere
    float c1 = cos(uTime * 0.55 + uTilt.x * 0.8);
    float s1 = sin(uTime * 0.55 + uTilt.x * 0.8);
    float3 P = float3(
        N.x * c1 - N.z * s1,
        N.y + uTilt.y * 0.35,
        N.x * s1 + N.z * c1
    );

    // Three counter-flowing 3D iridescent Siri-like plasma ribbons
    float r1 = ribbonWave(P, uTime, 3.2, 1.8, 1.15);
    float r2 = ribbonWave(float3(P.y, P.z, P.x), -uTime * 0.9, 3.8, 2.2, 1.35);
    float r3 = ribbonWave(float3(P.z, -P.x, P.y), uTime * 1.15, 2.9, 1.5, 0.95);

    // Deep obsidian-violet glass interior
    float3 bodyCol = mix(float3(0.05, 0.05, 0.14), uSecondary * 0.45, pow(max(0.0, z), 1.2));

    // Additive luminous ribbon layering
    float boost = 0.85 + uAmp * 0.95;
    bodyCol += uPrimary * r1 * 0.82 * boost;
    bodyCol += uSecondary * r2 * 0.78 * boost;
    bodyCol += uHighlight * r3 * 0.88 * boost;

    // Bright intertwined ribbon intersection bloom
    float coreBloom = exp(-rNorm * rNorm * 2.8) * (0.28 + uAmp * 0.52);
    bodyCol += mix(uHighlight, float3(1.0, 1.0, 1.0), 0.35) * coreBloom * (0.55 + r1 * 0.45);

    // Crisp Fresnel glass rim & dual studio highlights
    float ndotv = max(dot(N, V), 0.0);
    float fresnel = pow(1.0 - ndotv, 2.65);
    bodyCol += mix(uPrimary, uHighlight, 0.65) * fresnel * (0.95 + uAmp * 0.55);

    float3 L1 = normalize(float3(-0.46 + uTilt.x * 0.3, -0.52 + uTilt.y * 0.3, 0.72));
    float3 H1 = normalize(L1 + V);
    float specKey = pow(max(dot(N, H1), 0.0), 48.0);
    float specSoft = pow(max(dot(N, H1), 0.0), 10.0) * 0.18;
    bodyCol += float3(1.0, 0.99, 1.0) * (specKey * 0.85 + specSoft);

    float edgeAA = smoothstep(1.0, 0.96, rNorm);
    float alpha = mix(0.84, 0.99, fresnel) * edgeAA;
    return half4(clamp(bodyCol, 0.0, 1.0) * alpha, alpha);
}
"""

@Composable
fun CompanionOrb(
    orbState: OrbState,
    audioAmplitude: Float = 0f,
    reducedMotion: Boolean = false,
    interactive: Boolean = true,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemReduced = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
    val disableMotion = reducedMotion || systemReduced

    // Keep latest audioAmplitude & orbState accessible inside the 60fps frame loop
    var latestAmp by remember { mutableFloatStateOf(audioAmplitude) }
    latestAmp = audioAmplitude

    var latestState by remember { androidx.compose.runtime.mutableStateOf(orbState) }
    latestState = orbState

    // Continuous 60fps time & silky real-time envelope follower (fast attack, smooth release)
    var timeSec by remember { mutableFloatStateOf(0f) }
    var energyPhase by remember { mutableFloatStateOf(0f) }
    var reactiveAmp by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(disableMotion) {
        if (disableMotion) {
            timeSec = 0f
            energyPhase = 0f
            reactiveAmp = 0f
            return@LaunchedEffect
        }
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = frameNanos
                timeSec += dt

                val targetEnv = when (latestState) {
                    OrbState.SPEAKING -> (latestAmp.coerceIn(0f, 1f) * 0.92f + 0.18f).coerceIn(0f, 1f)
                    OrbState.LISTENING -> (latestAmp.coerceIn(0f, 1f) * 1.0f + 0.08f).coerceIn(0f, 1f)
                    OrbState.THINKING -> 0.30f
                    OrbState.ERROR -> 0.06f
                    OrbState.IDLE -> 0.04f
                }

                // Fast attack so voice syllables pop immediately, smooth release for Siri fluidity
                val smoothing = if (targetEnv > reactiveAmp) 0.42f else 0.14f
                reactiveAmp += (targetEnv - reactiveAmp) * smoothing

                val speedMult = when (latestState) {
                    OrbState.SPEAKING -> 1.8f + reactiveAmp * 2.6f
                    OrbState.LISTENING -> 1.4f + reactiveAmp * 2.8f
                    OrbState.THINKING -> 2.1f
                    OrbState.ERROR -> 0.4f
                    OrbState.IDLE -> 0.72f
                }
                energyPhase += dt * speedMult
            }
        }
    }

    val targetPrimary = when (orbState) {
        OrbState.IDLE -> LuminousViolet
        OrbState.LISTENING -> CalmMint
        OrbState.SPEAKING -> ElectricIndigo
        OrbState.THINKING -> SoftLavender
        OrbState.ERROR -> ErrorCoral
    }
    val targetSecondary = when (orbState) {
        OrbState.IDLE -> ElectricIndigo
        OrbState.LISTENING -> ElectricIndigo
        OrbState.SPEAKING -> WarmRose
        OrbState.THINKING -> LuminousViolet
        OrbState.ERROR -> WarmRose
    }
    val targetHighlight = when (orbState) {
        OrbState.LISTENING -> CalmMint
        OrbState.SPEAKING -> WarmRose
        OrbState.THINKING -> CalmMint
        OrbState.ERROR -> Color(0xFFFEE2E2)
        OrbState.IDLE -> SoftLavender
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(380),
        label = "orb_primary_color"
    )
    val secondaryColor by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(380),
        label = "orb_secondary_color"
    )
    val highlightColor by animateColorAsState(
        targetValue = targetHighlight,
        animationSpec = tween(380),
        label = "orb_highlight_color"
    )

    val deformAmount by animateFloatAsState(
        targetValue = when {
            disableMotion -> 0f
            orbState == OrbState.SPEAKING -> 0.88f
            orbState == OrbState.LISTENING -> 0.80f
            orbState == OrbState.THINKING -> 0.42f
            else -> 0.12f
        },
        animationSpec = tween(320),
        label = "orb_deform"
    )

    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val agslShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(ORB_SIRI_VOLUMETRIC_AGSL)
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    val touchModifier = if (interactive && !disableMotion) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    scope.launch { tiltX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 210f)) }
                    scope.launch { tiltY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 210f)) }
                },
                onDragCancel = {
                    scope.launch { tiltX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 210f)) }
                    scope.launch { tiltY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 210f)) }
                }
            ) { change, dragAmount ->
                change.consume()
                scope.launch {
                    tiltX.snapTo((tiltX.value + dragAmount.x / 160f).coerceIn(-0.9f, 0.9f))
                    tiltY.snapTo((tiltY.value + dragAmount.y / 160f).coerceIn(-0.9f, 0.9f))
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(touchModifier)
            .semantics {
                contentDescription = "MindMate 3D Companion Orb, state: ${orbState.label}"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val minDim = this.size.minDimension
            val floatOffsetY = if (disableMotion) 0f else sin(timeSec * 1.45f) * (minDim * 0.022f)
            val center = Offset(
                x = this.size.width / 2f + tiltX.value * minDim * 0.03f,
                y = this.size.height / 2f + floatOffsetY + tiltY.value * minDim * 0.03f
            )

            // Direct per-frame scale (no spring lag fighting 60fps breathing/voice bounce)
            val voiceScale = when {
                disableMotion -> 1f
                orbState == OrbState.LISTENING -> 1.02f + reactiveAmp * 0.14f
                orbState == OrbState.SPEAKING -> 1.01f + reactiveAmp * 0.13f + 0.02f * sin(energyPhase * 2.4f)
                orbState == OrbState.THINKING -> 0.98f + 0.03f * sin(timeSec * 3.1f)
                orbState == OrbState.ERROR -> 0.94f
                else -> 0.97f + 0.035f * sin(timeSec * 1.4f)
            }
            val baseRadius = minDim * 0.31f * voiceScale

            // 1. Soft atmospheric outer bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = (0.26f + reactiveAmp * 0.24f).coerceIn(0f, 0.6f)),
                        secondaryColor.copy(alpha = (0.12f + reactiveAmp * 0.10f).coerceIn(0f, 0.35f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * (1.72f + reactiveAmp * 0.25f)
                ),
                radius = baseRadius * (1.72f + reactiveAmp * 0.25f),
                center = center
            )

            // 2. Volumetric 3D translucent sphere core
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslShader != null) {
                agslShader.setFloatUniform("uCenter", center.x, center.y)
                agslShader.setFloatUniform("uRadius", baseRadius)
                agslShader.setFloatUniform("uTime", energyPhase)
                agslShader.setFloatUniform("uAmp", reactiveAmp)
                agslShader.setFloatUniform("uDeform", deformAmount)
                agslShader.setFloatUniform("uTilt", tiltX.value, tiltY.value)
                agslShader.setFloatUniform("uPrimary", primaryColor.red, primaryColor.green, primaryColor.blue)
                agslShader.setFloatUniform("uSecondary", secondaryColor.red, secondaryColor.green, secondaryColor.blue)
                agslShader.setFloatUniform("uHighlight", highlightColor.red, highlightColor.green, highlightColor.blue)

                drawCircle(
                    brush = ShaderBrush(agslShader),
                    radius = baseRadius * 1.55f,
                    center = center
                )
            } else {
                drawSiriCanvasFallbackSphere(
                    center = center,
                    baseRadius = baseRadius,
                    phase = energyPhase,
                    reactiveAmp = reactiveAmp,
                    deformAmount = deformAmount,
                    tiltX = tiltX.value,
                    tiltY = tiltY.value,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    highlightColor = highlightColor
                )
            }

            // 3. Flowing 3D Siri-style luminous ribbons layered cleanly across the orb
            drawSiriFluidRibbons(
                center = center,
                baseRadius = baseRadius,
                phase = energyPhase,
                reactiveAmp = reactiveAmp,
                primary = primaryColor,
                secondary = secondaryColor,
                highlight = highlightColor,
                disableMotion = disableMotion
            )

            // 4. Real-time audio-reactive outer harmonic wave rings when listening or speaking
            if (orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING) {
                for (ring in 0 until 2) {
                    val ringRadius = baseRadius * (1.09f + ring * 0.13f + reactiveAmp * (0.14f + ring * 0.06f))
                    val wavePath = Path()
                    val steps = 72
                    for (i in 0..steps) {
                        val a = (i.toFloat() / steps) * (2f * PI.toFloat())
                        val ampPx = if (disableMotion) 0f else (2.5f + reactiveAmp * 16f)
                        val wave = sin(a * (3 + ring * 2) + energyPhase * (2.5f - ring * 3.8f)) *
                            cos(a * 2f - energyPhase * 1.4f) * ampPx
                        val r = ringRadius + wave
                        val px = center.x + cos(a) * r
                        val py = center.y + sin(a) * r
                        if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                    }
                    wavePath.close()
                    drawPath(
                        path = wavePath,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = (0.55f - ring * 0.2f).coerceIn(0f, 1f)),
                                highlightColor.copy(alpha = (0.78f - ring * 0.25f).coerceIn(0f, 1f)),
                                secondaryColor.copy(alpha = (0.48f - ring * 0.18f).coerceIn(0f, 1f)),
                                primaryColor.copy(alpha = (0.55f - ring * 0.2f).coerceIn(0f, 1f))
                            ),
                            center = center
                        ),
                        style = Stroke(width = (2.0f - ring * 0.6f).dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 5. Rotating orbital shimmer ring for THINKING state
            if (orbState == OrbState.THINKING) {
                drawTiltedShimmerRing(
                    center = center,
                    radiusX = baseRadius * 1.24f,
                    radiusY = baseRadius * 0.44f,
                    tiltRad = 0.58f + sin(timeSec * 1.1f) * 0.16f,
                    phase = -energyPhase * 1.4f,
                    primary = CalmMint,
                    secondary = SoftLavender,
                    strokeWidthPx = 2.2.dp.toPx()
                )
            }
        }
    }
}

private fun DrawScope.drawSiriFluidRibbons(
    center: Offset,
    baseRadius: Float,
    phase: Float,
    reactiveAmp: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    disableMotion: Boolean
) {
    val ribbonConfigs = listOf(
        Triple(primary, 0f, 0.38f),
        Triple(highlight, 1.05f, -0.45f),
        Triple(secondary, 2.15f, 0.72f)
    )
    ribbonConfigs.forEachIndexed { idx, (color, angleOffset, tilt) ->
        val ribbonPhase = if (disableMotion) angleOffset else phase * (1.1f + idx * 0.28f) + angleOffset
        val cosT = cos(tilt + sin(ribbonPhase * 0.35f) * 0.25f)
        val sinT = sin(tilt + sin(ribbonPhase * 0.35f) * 0.25f)
        val path = Path()
        val steps = 48
        val span = baseRadius * 0.86f
        val verticalSpread = baseRadius * (0.24f + reactiveAmp * 0.30f)

        for (i in 0..steps) {
            val u = (i.toFloat() / steps) * 2f - 1f // -1..1 across sphere diameter
            val envelope = kotlin.math.sqrt((1f - u * u * 0.82f).coerceAtLeast(0.05f))
            val lx = u * span
            val ly = sin(u * 4.2f + ribbonPhase * (1.8f + idx * 0.3f)) *
                cos(u * 2.1f - ribbonPhase * 1.1f) * verticalSpread * envelope
            val px = center.x + lx * cosT - ly * sinT
            val py = center.y + lx * sinT + ly * cosT
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = (0.42f + reactiveAmp * 0.38f).coerceIn(0f, 0.9f)),
                    Color.White.copy(alpha = (0.24f + reactiveAmp * 0.30f).coerceIn(0f, 0.65f)),
                    color.copy(alpha = (0.42f + reactiveAmp * 0.38f).coerceIn(0f, 0.9f)),
                    Color.Transparent
                ),
                startX = center.x - span,
                endX = center.x + span
            ),
            style = Stroke(
                width = (baseRadius * (0.11f + reactiveAmp * 0.08f)).coerceAtLeast(3f),
                cap = StrokeCap.Round
            )
        )
    }
}

private fun DrawScope.drawTiltedShimmerRing(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    tiltRad: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
    strokeWidthPx: Float
) {
    val cosT = cos(tiltRad)
    val sinT = sin(tiltRad)
    val steps = 54
    val path = Path()
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * (2f * PI.toFloat())
        val lx = cos(t) * radiusX
        val ly = sin(t) * radiusY
        val px = center.x + lx * cosT - ly * sinT
        val py = center.y + lx * sinT + ly * cosT
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    val pulseX = center.x + cos(phase) * radiusX * cosT
    val pulseY = center.y + cos(phase) * radiusX * sinT
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                secondary.copy(alpha = 0.85f),
                primary.copy(alpha = 0.36f),
                Color.Transparent
            ),
            center = Offset(pulseX, pulseY),
            radius = radiusX * 1.1f
        ),
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawSiriCanvasFallbackSphere(
    center: Offset,
    baseRadius: Float,
    phase: Float,
    reactiveAmp: Float,
    deformAmount: Float,
    tiltX: Float,
    tiltY: Float,
    primaryColor: Color,
    secondaryColor: Color,
    highlightColor: Color
) {
    val organicPath = Path()
    val steps = 72
    for (i in 0..steps) {
        val a = (i.toFloat() / steps) * (2f * PI.toFloat())
        val wobble = sin(a * 3f + phase * 2.2f) * cos(a * 2f - phase * 1.4f) *
            (0.012f + deformAmount * 0.032f + reactiveAmp * 0.065f)
        val r = baseRadius * (1f + wobble)
        val px = center.x + cos(a) * r
        val py = center.y + sin(a) * r
        if (i == 0) organicPath.moveTo(px, py) else organicPath.lineTo(px, py)
    }
    organicPath.close()

    val lightSpot = Offset(
        x = center.x - baseRadius * (0.32f - tiltX * 0.18f),
        y = center.y - baseRadius * (0.34f - tiltY * 0.18f)
    )

    // Deep translucent glass interior
    drawPath(
        path = organicPath,
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.72f),
                secondaryColor.copy(alpha = 0.58f),
                Color(0xFF0E0D22).copy(alpha = 0.94f)
            ),
            center = lightSpot,
            radius = baseRadius * 1.45f
        )
    )

    // Swirling nebula lobes inside sphere
    val lobe1 = Offset(
        x = center.x + cos(phase * 1.15f) * baseRadius * 0.24f,
        y = center.y + sin(phase * 1.35f) * baseRadius * 0.22f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                highlightColor.copy(alpha = (0.48f + reactiveAmp * 0.34f).coerceIn(0f, 0.9f)),
                primaryColor.copy(alpha = 0.20f),
                Color.Transparent
            ),
            center = lobe1,
            radius = baseRadius * 0.75f
        ),
        radius = baseRadius * 0.75f,
        center = lobe1
    )

    val lobe2 = Offset(
        x = center.x - cos(phase * 0.9f) * baseRadius * 0.26f,
        y = center.y + sin(phase * 1.05f) * baseRadius * 0.24f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                secondaryColor.copy(alpha = (0.44f + reactiveAmp * 0.30f).coerceIn(0f, 0.85f)),
                Color.Transparent
            ),
            center = lobe2,
            radius = baseRadius * 0.68f
        ),
        radius = baseRadius * 0.68f,
        center = lobe2
    )

    // Crisp Fresnel glass rim & key specular highlight
    drawPath(
        path = organicPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.62f),
                primaryColor.copy(alpha = 0.28f),
                highlightColor.copy(alpha = 0.52f)
            ),
            start = Offset(center.x - baseRadius, center.y - baseRadius),
            end = Offset(center.x + baseRadius, center.y + baseRadius)
        ),
        style = Stroke(width = 1.8.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.48f), Color.Transparent),
            center = lightSpot,
            radius = baseRadius * 0.38f
        ),
        radius = baseRadius * 0.38f,
        center = lightSpot
    )
}
