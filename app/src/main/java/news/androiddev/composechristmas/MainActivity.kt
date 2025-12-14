package news.androiddev.composechristmas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import news.androiddev.composechristmas.ui.theme.ComposeChristmasTheme
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeChristmasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChristmasScene(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

enum class SkyTheme { NightSky, WinterMorning }

// Day 12: Light state model (for future animation/twinkle)
data class LightState(
    val position: Offset,
    val color: Color,
    val radius: Float,
    val isOn: Boolean = true,
    val phase: Float = 0f // 0..1 phase offset for animations
)

@Composable
fun ChristmasScene(modifier: Modifier = Modifier, skyTheme: SkyTheme = SkyTheme.NightSky) {
    // Twinkle animation time source (0..1 repeating)
    val twinkleTransition = rememberInfiniteTransition(label = "twinkle")
    val twinkleTime = twinkleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkleTime"
    )
    // Separate time base for slow tree sway
    val treeSwayTime = twinkleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "treeSwayTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // --- 1. Sky Background ---
        val skyBrush = when (skyTheme) {
            SkyTheme.NightSky -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B1026), // Deep Navy
                    Color(0xFF152044),
                    Color(0xFF1F2D5E)
                ),
                startY = 0f,
                endY = canvasHeight
            )

            SkyTheme.WinterMorning -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE3F2FD), // Pale Blue
                    Color(0xFFBBDEFB),
                    Color(0xFF90CAF9)
                ),
                startY = 0f,
                endY = canvasHeight
            )
        }
        drawRect(brush = skyBrush, topLeft = Offset.Zero, size = size)

        // --- 2. Stars ---
        val starColor = if (skyTheme == SkyTheme.NightSky) Color(0xFFFFF9C4) else Color(0xFFFFFDE7)
        val starGlowAlpha = if (skyTheme == SkyTheme.NightSky) 0.25f else 0.20f
        val starRadiusBase = if (skyTheme == SkyTheme.NightSky) 5f else 4f

        val starPositions = listOf(
            Offset(canvasWidth * 0.10f, canvasHeight * 0.10f),
            Offset(canvasWidth * 0.18f, canvasHeight * 0.22f),
            Offset(canvasWidth * 0.22f, canvasHeight * 0.07f),
            Offset(canvasWidth * 0.32f, canvasHeight * 0.18f),
            Offset(canvasWidth * 0.38f, canvasHeight * 0.35f),
            Offset(canvasWidth * 0.42f, canvasHeight * 0.28f),
            Offset(canvasWidth * 0.48f, canvasHeight * 0.09f),
            Offset(canvasWidth * 0.54f, canvasHeight * 0.16f),
            Offset(canvasWidth * 0.60f, canvasHeight * 0.33f),
            Offset(canvasWidth * 0.68f, canvasHeight * 0.10f),
            Offset(canvasWidth * 0.72f, canvasHeight * 0.25f),
            Offset(canvasWidth * 0.78f, canvasHeight * 0.36f),
            Offset(canvasWidth * 0.82f, canvasHeight * 0.19f),
            Offset(canvasWidth * 0.86f, canvasHeight * 0.46f),
            Offset(canvasWidth * 0.92f, canvasHeight * 0.12f),
            Offset(canvasWidth * 0.95f, canvasHeight * 0.41f)
        ) +
                // Spread deterministic pseudo-random stars throughout the sky (top to ~70% height)
                List(30) { i ->
                    val x = (0.04f + 0.92f * ((i * 37) % 100) / 100f) * canvasWidth
                    val y = (0.04f + 0.66f * ((i * 53 + 17) % 100) / 100f) * canvasHeight
                    Offset(x, y)
                }

        for (pos in starPositions) {
            // Glow Halo
            drawCircle(
                color = starColor.copy(alpha = starGlowAlpha),
                radius = starRadiusBase * 3.5f,
                center = pos
            )
            // Core
            drawCircle(
                color = starColor,
                radius = starRadiusBase,
                center = pos
            )
        }

        // --- 2b. Moon (crescent with craters) ---
        run {
            // Place moon near top-right, size relative to canvas
            val moonCenter = Offset(canvasWidth * 0.80f, canvasHeight * 0.18f)
            val moonRadius = canvasWidth.coerceAtMost(canvasHeight) * 0.08f

            // Colors vary slightly by theme
            val moonBaseColor =
                if (skyTheme == SkyTheme.NightSky) Color(0xFFFFF3E0) else Color(0xFFFFFDE7)
            val moonShadowColor =
                if (skyTheme == SkyTheme.NightSky) Color(0xFF0B1026) else Color(0xFF90CAF9)

            // Soft glow behind moon
            drawCircle(
                color = moonBaseColor.copy(alpha = 0.18f),
                radius = moonRadius * 2.2f,
                center = moonCenter
            )
            drawCircle(
                color = moonBaseColor.copy(alpha = 0.28f),
                radius = moonRadius * 1.4f,
                center = moonCenter
            )

            // Full moon base
            drawCircle(
                color = moonBaseColor,
                radius = moonRadius,
                center = moonCenter
            )

            // Create crescent by overlaying a masking circle with sky color
            val phaseOffsetX = moonRadius * 0.55f // controls the crescent thickness
            val maskCenter = Offset(moonCenter.x + phaseOffsetX, moonCenter.y)
            drawCircle(
                color = moonShadowColor,
                radius = moonRadius * 0.98f,
                center = maskCenter
            )

            // Subtle terminator rim
            drawCircle(
                color = Color.White.copy(alpha = 0.20f),
                radius = moonRadius * 0.99f,
                center = moonCenter
            )

            // Craters: draw small circles with darker tint and faint highlight
            val craterDark = Color(0xFFE0C9A6) // slightly darker than base
            val craterHighlight = Color(0xFFFFF8E1)

            // Deterministic positions within the lit crescent area
            val craterPositions = listOf(
                Offset(moonCenter.x - moonRadius * 0.25f, moonCenter.y - moonRadius * 0.10f),
                Offset(moonCenter.x - moonRadius * 0.12f, moonCenter.y + moonRadius * 0.15f),
                Offset(moonCenter.x - moonRadius * 0.35f, moonCenter.y + moonRadius * 0.05f),
                Offset(moonCenter.x - moonRadius * 0.18f, moonCenter.y - moonRadius * 0.28f),
                Offset(moonCenter.x - moonRadius * 0.05f, moonCenter.y - moonRadius * 0.02f)
            )
            val craterRadii = listOf(
                moonRadius * 0.11f,
                moonRadius * 0.08f,
                moonRadius * 0.06f,
                moonRadius * 0.05f,
                moonRadius * 0.04f
            )

            craterPositions.forEachIndexed { idx, c ->
                val r = craterRadii[idx]
                // Shadow ring
                drawCircle(
                    color = craterDark.copy(alpha = 0.65f),
                    radius = r,
                    center = c
                )
                // Inner highlight slightly offset toward top-left
                drawCircle(
                    color = craterHighlight.copy(alpha = 0.45f),
                    radius = r * 0.55f,
                    center = Offset(c.x - r * 0.20f, c.y - r * 0.20f)
                )
            }
        }

        // --- 3. Snowy Hill (Ground) ---
        // Matches SVG: M 0 H L 0 0.85H C 0.3W 0.80H, 0.7W 0.80H, W 0.85H L W H Z
        val hillPath = Path().apply {
            moveTo(0f, canvasHeight)
            lineTo(0f, canvasHeight * 0.85f)
            cubicTo(
                canvasWidth * 0.3f, canvasHeight * 0.80f, // Control Point 1
                canvasWidth * 0.7f, canvasHeight * 0.80f, // Control Point 2
                canvasWidth, canvasHeight * 0.85f           // End Point
            )
            lineTo(canvasWidth, canvasHeight)
            close()
        }

        // Snow Gradient: White to Icy Blue
        val snowBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0F7FA)),
            startY = canvasHeight * 0.80f,
            endY = canvasHeight
        )

        drawPath(path = hillPath, brush = snowBrush)

        // --- 4. Tree Configuration ---
        val numLayers = 10
        val treeWidth = canvasWidth * 0.42f
        val treeHeight = canvasHeight * 0.62f
        // Move tree down to ~38% so it reaches the hill
        val treeTop = canvasHeight * 0.38f
        val layerHeight = treeHeight / (numLayers + 1f)
        val layerOverlap = layerHeight * 0.35f

        val deepGreen = Color(0xFF2E7D32)
        val midGreen = Color(0xFF2F7E33)
        val foliageBrush = Brush.verticalGradient(
            colors = listOf(deepGreen, midGreen),
            startY = treeTop,
            endY = treeTop + treeHeight
        )

        // --- 5. Star Topper (animated) ---
        val centerX = canvasWidth / 2f
        run {
            val starCenter = Offset(centerX, treeTop - layerHeight * 0.25f)
            val outerRadius = treeWidth * 0.10f
            val innerRadius = outerRadius * 0.45f

            // Animation factors for star pulse and subtle rotation
            val starPulse = ((sin(2.0 * PI * (twinkleTime.value * 1.20 + 0.05)) + 1.0) * 0.5).toFloat()
            val starScale = 0.96f + 0.08f * starPulse
            val starRot = (sin(2.0 * PI * (twinkleTime.value * 0.60 + 0.15)) * 2.0).toFloat()

            // Build a 5-point star path
            val starPath = Path().apply {
                val points = mutableListOf<Offset>()
                val tipAngle = -90f // top tip pointing straight up
                for (k in 0 until 10) {
                    val isOuter = k % 2 == 0
                    val radius = if (isOuter) outerRadius else innerRadius
                    val angleDeg = tipAngle + k * 36f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val x = starCenter.x + (radius * Math.cos(angleRad)).toFloat()
                    val y = starCenter.y + (radius * Math.sin(angleRad)).toFloat()
                    points.add(Offset(x, y))
                }
                moveTo(points[0].x, points[0].y)
                for (p in 1 until points.size) {
                    lineTo(points[p].x, points[p].y)
                }
                close()
            }

            // Shiny radial gradient with warm golden tones
            val starGradient = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF59D), // light gold center
                    Color(0xFFFFD54F),
                    Color(0xFFFFC107), // strong gold
                    Color(0x00FFC107)  // fade to transparent at edge
                ),
                center = starCenter,
                radius = outerRadius * 1.25f
            )

            withTransform({
                rotate(starRot, pivot = starCenter)
                scale(scaleX = starScale, scaleY = starScale, pivot = starCenter)
            }) {
                // Soft glow around the star, animated alpha and radius
                drawCircle(
                    color = Color(0xFFFFF8E1).copy(alpha = 0.28f + 0.20f * starPulse),
                    radius = outerRadius * (2.3f + 0.5f * starPulse),
                    center = starCenter
                )
                drawCircle(
                    color = Color(0xFFFFFDE7).copy(alpha = 0.18f + 0.14f * starPulse),
                    radius = outerRadius * (1.4f + 0.3f * starPulse),
                    center = starCenter
                )

                // Draw the star with the gradient
                drawPath(path = starPath, brush = starGradient)
            }
        }

        // --- 6. Trunk Logic ---
        // Calculate ground level at center (approximate bezier Y at t=0.5)
        // Bezier P0y=0.85, P1y=0.80, P2y=0.80, P3y=0.85 -> Midpoint is ~0.8125
        val groundYAtCenter = canvasHeight * 0.81f

        val trunkWidth = treeWidth * 0.14f
        val trunkLeft = (canvasWidth - trunkWidth) / 2f

        // Calculate bottom of foliage
        val lastLayerTop = treeTop + (numLayers - 1) * (layerHeight - layerOverlap)
        val lastLayerBottom = lastLayerTop + layerHeight

        // Connect trunk from inside foliage to slightly below ground
        val trunkTop = lastLayerBottom - layerHeight * 0.5f
        val trunkBottom = groundYAtCenter + 10f // Bury it slightly

        // Tree sway animation (gentle rotation around ground center)
        val treeSwayDeg = (sin(2.0 * PI * (treeSwayTime.value + 0.10)) * 1.1).toFloat()

        withTransform({ rotate(treeSwayDeg, pivot = Offset(centerX, groundYAtCenter)) }) {
            drawRect(
                color = Color(0xFF5D4037),
                topLeft = Offset(trunkLeft, trunkTop),
                size = Size(trunkWidth, trunkBottom - trunkTop)
            )

        // --- Candy canes hanging on the tree ---
        fun drawCandyCane(center: Offset, height: Float, thickness: Float, rotationDeg: Float) {
            val hookR = thickness * 2.2f
            val topY = center.y - height / 2f
            val path = Path().apply {
                // Hook curve to the right, then vertical stem
                moveTo(center.x, topY)
                quadraticTo(
                    center.x + hookR * 0.9f,
                    topY + hookR * 0.2f,
                    center.x + hookR * 1.4f,
                    topY + hookR * 0.8f
                )
                quadraticTo(
                    center.x + hookR * 1.8f,
                    topY + hookR * 1.6f,
                    center.x + hookR * 0.9f,
                    topY + hookR * 2.2f
                )
                lineTo(center.x, center.y + height / 2f)
            }

            withTransform({ rotate(rotationDeg, pivot = center) }) {
                // Base white cane body
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = thickness, cap = StrokeCap.Round)
                )
                // Red stripes via dash effect along the cane path
                val stripeLen = thickness * 1.6f
                drawPath(
                    path = path,
                    color = Color(0xFFD32F2F),
                    style = Stroke(
                        width = thickness,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(stripeLen, stripeLen),
                            0f
                        )
                    )
                )
            }
        }

        // Calls moved after foliage to ensure canes render in front of tree

        // --- 7. Tree Foliage Layers ---
        val layerBounds = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until numLayers) {
            val progressFromTop = i / (numLayers - 1f)
            val layerTop = treeTop + i * (layerHeight - layerOverlap)
            val layerBottom = layerTop + layerHeight

            val baseWidth = treeWidth * (0.25f + 0.75f * progressFromTop)

            // Wobble for organic feel
            val wobble = (if (i % 2 == 0) 1f else -1f) * baseWidth * 0.06f
            val layerWidth = baseWidth + wobble
            val layerLeft = centerX - layerWidth / 2f
            val layerRight = centerX + layerWidth / 2f

            layerBounds.add(layerLeft to layerRight)

            val controlYOffset = layerHeight * 0.35f

            val path = Path().apply {
                moveTo(centerX, layerTop)
                // Right Scallop
                quadraticTo(
                    centerX + layerWidth * 0.55f,
                    layerTop + controlYOffset * 0.9f,
                    layerRight,
                    layerBottom
                )
                // Bottom Curve (inward)
                quadraticTo(
                    centerX,
                    layerBottom - layerHeight * 0.15f,
                    layerLeft,
                    layerBottom
                )
                // Left Scallop
                quadraticTo(
                    centerX - layerWidth * 0.55f,
                    layerTop + controlYOffset * 0.9f,
                    centerX,
                    layerTop
                )
                close()
            }

            drawPath(path = path, brush = foliageBrush)
        }

        // --- Candy canes (draw AFTER foliage so they appear in front) ---
        run {
            val midX = centerX
            val leftCaneCenter = Offset(midX - treeWidth * 0.22f, treeTop + layerHeight * 5.4f)
            val rightCaneCenter = Offset(midX + treeWidth * 0.20f, treeTop + layerHeight * 3.6f)
            // Smaller size and thinner stripes
            drawCandyCane(
                center = leftCaneCenter,
                height = layerHeight * 1.2f,
                thickness = treeWidth * 0.035f,
                rotationDeg = -16f
            )
            drawCandyCane(
                center = rightCaneCenter,
                height = layerHeight * 1.2f,
                thickness = treeWidth * 0.032f,
                rotationDeg = 12f
            )
        }

        // --- 7a. Fairy Lights Wire (thin, wraps like garland) ---
        run {
            // Thin wire width
            val wireWidth = layerHeight * 0.06f
            val wirePath = Path()
            val wraps = listOf(2, 4, 6, 8) // distribute wire across layers
            var started = false
            for ((idx, layerIndex) in wraps.withIndex()) {
                val t = layerIndex / (numLayers - 1f)
                val baseWidth = treeWidth * (0.25f + 0.75f * t)
                val wobble = (if (layerIndex % 2 == 0) 1f else -1f) * baseWidth * 0.05f
                val layerWidth = baseWidth + wobble
                val y = treeTop + layerIndex * (layerHeight - layerOverlap) + layerHeight * 0.52f
                val leftX = centerX - layerWidth * 0.50f
                val rightX = centerX + layerWidth * 0.50f

                val amplitude = layerHeight * 0.18f
                val cp1 = Offset(centerX - layerWidth * 0.20f, y - amplitude * 0.8f)
                val cp2 = Offset(centerX + layerWidth * 0.20f, y + amplitude * 0.8f)

                if (!started) {
                    wirePath.moveTo(leftX, y)
                    started = true
                }
                // Wave from left to right
                wirePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, rightX, y)
                // Descent to next wrap
                if (idx != wraps.lastIndex) {
                    val nextT = wraps[idx + 1] / (numLayers - 1f)
                    val nextBase = treeWidth * (0.25f + 0.75f * nextT)
                    val nextWobble = (if (wraps[idx + 1] % 2 == 0) 1f else -1f) * nextBase * 0.05f
                    val nextWidth = nextBase + nextWobble
                    val nextY = treeTop + wraps[idx + 1] * (layerHeight - layerOverlap) + layerHeight * 0.50f
                    val nextLeftX = centerX - nextWidth * 0.48f
                    wirePath.cubicTo(
                        centerX + layerWidth * 0.08f, y + amplitude * 0.5f,
                        centerX - nextWidth * 0.16f, nextY - amplitude * 0.5f,
                        nextLeftX, nextY
                    )
                }
            }

            // Draw the wire: subtle dark color with slight transparency
            drawPath(
                path = wirePath,
                color = Color(0xFF37474F).copy(alpha = 0.85f),
                style = Stroke(width = wireWidth, cap = StrokeCap.Round)
            )

            // Add fairy light bulbs along the wire using LightState
            val measure = androidx.compose.ui.graphics.PathMeasure()
            measure.setPath(wirePath, false)
            val length = measure.length

            val bulbStep = layerHeight * 0.45f // denser spacing for a fuller look
            val baseRadius = layerHeight * 0.10f
            val palette = listOf(
                Color(0xFFFFF6E5), // warm white
                Color(0xFFFF5252), // red
                Color(0xFF69F0AE), // green
                Color(0xFF40C4FF), // cyan-blue
                Color(0xFFFFD740), // amber
                Color(0xFFEA80FC)  // violet
            )

            val lights = buildList {
                var dist = bulbStep * 0.5f
                var idx = 0
                while (dist <= length) {
                    val pos = measure.getPosition(dist)
                    val color = palette[idx % palette.size]
                    val phase = ((idx * 37) % 100) / 100f // deterministic phase offset
                    val jitter = ((idx * 17 + 7) % 100) / 100f // 0..1
                    val radius = baseRadius * (0.9f + 0.2f * jitter) // subtle size variance
                    add(LightState(position = pos, color = color, radius = radius, isOn = true, phase = phase))
                    dist += bulbStep
                    idx++
                }
            }

            // Render bulbs (static for now; LightState enables future animation)
            lights.forEach { light ->
                // Twinkle factor: 0.0..1.0 based on shared time and per-light phase
                val sparkle = ((sin(2.0 * PI * (twinkleTime.value + light.phase)) + 1.0) * 0.5).toFloat()
                val onFactor = if (light.isOn) 1f else 0.15f
                val factor = (0.45f + 0.55f * sparkle) * onFactor

                val glowRadius = light.radius * (2.0f + 0.6f * sparkle)
                // Soft outer glow
                drawCircle(
                    color = light.color.copy(alpha = 0.28f * factor),
                    radius = glowRadius,
                    center = light.position
                )
                drawCircle(
                    color = light.color.copy(alpha = 0.20f * factor),
                    radius = glowRadius * 0.65f,
                    center = light.position
                )
                // Bulb core
                drawCircle(
                    color = light.color.copy(alpha = 0.90f * factor),
                    radius = light.radius,
                    center = light.position
                )
                // Tiny white spec highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f * factor),
                    radius = light.radius * 0.18f,
                    center = Offset(light.position.x - light.radius * 0.25f, light.position.y - light.radius * 0.25f)
                )
            }
        }

        // --- 7b. Garland (tinsel wrapping with bezier path and PathMeasure) ---
        run {
            // Size unit for garland elements (similar to ornamentRadius but scoped here)
            val garlandUnit = layerHeight * 0.14f

            // Build a wavy garland that wraps around several layers of the tree
            val garlandPath = Path()
            val wraps = listOf(1, 3, 5, 7) // layers to anchor garland waves
            var started = false
            for ((idx, layerIndex) in wraps.withIndex()) {
                val t = layerIndex / (numLayers - 1f)
                val baseWidth = treeWidth * (0.25f + 0.75f * t)
                val wobble = (if (layerIndex % 2 == 0) 1f else -1f) * baseWidth * 0.06f
                val layerWidth = baseWidth + wobble
                val y = treeTop + layerIndex * (layerHeight - layerOverlap) + layerHeight * 0.55f
                val leftX = centerX - layerWidth * 0.48f
                val rightX = centerX + layerWidth * 0.48f

                val amplitude = layerHeight * 0.30f
                val cp1 = Offset(centerX - layerWidth * 0.18f, y - amplitude * 0.8f)
                val cp2 = Offset(centerX + layerWidth * 0.18f, y + amplitude * 0.8f)

                if (!started) {
                    garlandPath.moveTo(leftX, y)
                    started = true
                }
                // Wave from left to right
                garlandPath.cubicTo(
                    cp1.x, cp1.y,
                    cp2.x, cp2.y,
                    rightX, y
                )
                // Small descent between wraps to simulate vertical progression
                if (idx != wraps.lastIndex) {
                    val nextT = wraps[idx + 1] / (numLayers - 1f)
                    val nextBase = treeWidth * (0.25f + 0.75f * nextT)
                    val nextWobble = (if (wraps[idx + 1] % 2 == 0) 1f else -1f) * nextBase * 0.06f
                    val nextWidth = nextBase + nextWobble
                    val nextY =
                        treeTop + wraps[idx + 1] * (layerHeight - layerOverlap) + layerHeight * 0.45f
                    val nextLeftX = centerX - nextWidth * 0.45f
                    // Connect downwards with a gentle curve back to the left edge
                    garlandPath.cubicTo(
                        centerX + layerWidth * 0.10f, y + amplitude * 0.6f,
                        centerX - nextWidth * 0.20f, nextY - amplitude * 0.6f,
                        nextLeftX, nextY
                    )
                }
            }

            // Tinsel style: stroke-like dots and small sparkles along the path using PathMeasure
            val measure = androidx.compose.ui.graphics.PathMeasure()
            measure.setPath(garlandPath, false)
            val length = measure.length

            // Draw sampled dots
            val step = garlandUnit * 0.80f
            var dist = 0f
            while (dist <= length) {
                val pos = measure.getPosition(dist)
                val sparkleColor = Color(0xFFE0F7FA)
                // Base tinsel dot
                drawCircle(
                    color = sparkleColor.copy(alpha = 0.75f),
                    radius = garlandUnit * 0.10f,
                    center = pos
                )
                // Tiny star-like cross
                val crossLen = garlandUnit * 0.16f
                drawLine(
                    color = sparkleColor.copy(alpha = 0.65f),
                    start = Offset(pos.x - crossLen, pos.y),
                    end = Offset(pos.x + crossLen, pos.y)
                )
                drawLine(
                    color = sparkleColor.copy(alpha = 0.65f),
                    start = Offset(pos.x, pos.y - crossLen),
                    end = Offset(pos.x, pos.y + crossLen)
                )
                dist += step
            }

            // A soft metallic ribbon underlay to suggest a continuous strand
            val ribbonColor = Color(0xFFB0BEC5)
            // Approximate stroke by drawing many short segments along the path
            val segment = garlandUnit * 0.55f
            var d2 = 0f
            while (d2 < length - segment) {
                val p0 = measure.getPosition(d2)
                val p1 = measure.getPosition(d2 + segment)
                drawLine(
                    color = ribbonColor.copy(alpha = 0.35f),
                    start = p0,
                    end = p1,
                    strokeWidth = garlandUnit * 0.10f
                )
                d2 += segment
            }
        }

        // --- 8. Ornaments on Tree ---
        run {
            // Festive palette
            val ornamentColors = listOf(
                Color(0xFFE53935), // red
                Color(0xFF8E24AA), // purple
                Color(0xFF1E88E5), // blue
                Color(0xFF43A047), // green
                Color(0xFFFFA000), // amber
                Color(0xFFFFD54F)  // gold
            )
            // Make ornaments smaller
            val ornamentRadius = layerHeight * 0.14f

            // Hardcoded positions (relative to tree)
            val fixedOffsets = listOf(
                Offset(centerX - treeWidth * 0.12f, treeTop + layerHeight * 1.2f),
                Offset(centerX + treeWidth * 0.08f, treeTop + layerHeight * 1.5f),
                Offset(centerX - treeWidth * 0.18f, treeTop + layerHeight * 2.2f),
                Offset(centerX + treeWidth * 0.22f, treeTop + layerHeight * 2.8f),
                Offset(centerX - treeWidth * 0.25f, treeTop + layerHeight * 3.5f),
                Offset(centerX + treeWidth * 0.26f, treeTop + layerHeight * 4.0f),
                Offset(centerX - treeWidth * 0.10f, treeTop + layerHeight * 4.6f),
                Offset(centerX + treeWidth * 0.05f, treeTop + layerHeight * 5.3f),
                Offset(centerX - treeWidth * 0.22f, treeTop + layerHeight * 6.0f),
                Offset(centerX + treeWidth * 0.18f, treeTop + layerHeight * 6.6f)
            )

            // Deterministic pseudo-random ornaments
            val randomOffsets = buildList {
                val count = 24
                for (i in 0 until count) {
                    val li = (i * 7 + 3) % numLayers
                    val progressFromTop = li / (numLayers - 1f)
                    val layerTopY = treeTop + li * (layerHeight - layerOverlap)
                    val baseWidth = treeWidth * (0.25f + 0.75f * progressFromTop)
                    val wobble = (if (li % 2 == 0) 1f else -1f) * baseWidth * 0.06f
                    val layerWidth = baseWidth + wobble
                    val left = centerX - layerWidth / 2f + ornamentRadius
                    val right = centerX + layerWidth / 2f - ornamentRadius

                    val rx01 = ((i * 37 + 11) % 100) / 100f
                    val ry01 = ((i * 53 + 29) % 100) / 100f
                    val x = left + (right - left) * rx01
                    val y = layerTopY + ornamentRadius + (layerHeight - 2 * ornamentRadius) * ry01
                    add(Offset(x, y))
                }
            }

            val ornaments = fixedOffsets + randomOffsets

            // Draw ornaments with gradient, shine, and hook
            ornaments.forEachIndexed { idx, pos ->
                val baseColor = ornamentColors[idx % ornamentColors.size]

                // Subtle outer glow
                drawCircle(
                    color = baseColor.copy(alpha = 0.20f),
                    radius = ornamentRadius * 1.6f,
                    center = pos
                )
                drawCircle(
                    color = baseColor.copy(alpha = 0.30f),
                    radius = ornamentRadius * 1.15f,
                    center = pos
                )

                // Radial gradient for 3D shading (lighter top-left, darker bottom-right)
                val gradient = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        baseColor,
                        baseColor.copy(alpha = 0.95f),
                        Color.Black.copy(alpha = 0.20f)
                    ),
                    center = Offset(pos.x - ornamentRadius * 0.25f, pos.y - ornamentRadius * 0.25f),
                    radius = ornamentRadius * 1.2f
                )
                drawCircle(
                    brush = gradient,
                    radius = ornamentRadius,
                    center = pos
                )

                // Shine highlight (small white circle, offset to top-left)
                val shineCenter =
                    Offset(pos.x - ornamentRadius * 0.35f, pos.y - ornamentRadius * 0.35f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = ornamentRadius * 0.22f,
                    center = shineCenter
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = ornamentRadius * 0.12f,
                    center = Offset(
                        shineCenter.x + ornamentRadius * 0.10f,
                        shineCenter.y + ornamentRadius * 0.08f
                    )
                )

                // Hook: small cap + curved hook above the ornament
                val capWidth = ornamentRadius * 0.55f
                val capHeight = ornamentRadius * 0.18f
                val capLeft = pos.x - capWidth / 2f
                val capTop = pos.y - ornamentRadius - capHeight * 0.6f
                // Cap rectangle
                drawRect(
                    color = Color(0xFFB0BEC5),
                    topLeft = Offset(capLeft, capTop),
                    size = Size(capWidth, capHeight)
                )
                // Hook arc (simple bezier using Path)
                val hookPath = Path().apply {
                    moveTo(pos.x, capTop)
                    // Curve upward and then back down slightly
                    quadraticTo(
                        pos.x + ornamentRadius * 0.22f,
                        capTop - ornamentRadius * 0.35f,
                        pos.x + ornamentRadius * 0.05f,
                        capTop - ornamentRadius * 0.05f
                    )
                }
                drawPath(
                    path = hookPath,
                    color = Color(0xFF90A4AE),
                    alpha = 0.9f
                )
            }
        }
        // End tree sway transform
        }

        // --- 9. Gift Boxes with Ribbons and Bows ---
        run {
            // Position gifts on snowy ground, centered under tree
            // Bottom of gifts should be at ground level
            val giftBottom = groundYAtCenter
            
            // Gift 1: Red box on the left
            val gift1X = centerX - treeWidth * 0.28f
            val gift1Width = treeWidth * 0.18f
            val gift1Height = gift1Width * 0.95f
            val gift1Color = Color(0xFFD32F2F)
            val gift1Top = giftBottom - gift1Height
            
            // Box
            drawRect(
                color = gift1Color,
                topLeft = Offset(gift1X, gift1Top),
                size = Size(gift1Width, gift1Height)
            )
            // Vertical ribbon
            val ribbonWidth = gift1Width * 0.15f
            drawRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(gift1X + gift1Width / 2f - ribbonWidth / 2f, gift1Top),
                size = Size(ribbonWidth, gift1Height)
            )
            // Horizontal ribbon
            drawRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(gift1X, gift1Top + gift1Height / 2f - ribbonWidth / 2f),
                size = Size(gift1Width, ribbonWidth)
            )
            // Bow (two loops and center knot)
            val bowCenterX = gift1X + gift1Width / 2f
            val bowCenterY = gift1Top + gift1Height / 2f
            val bowSize = ribbonWidth * 2.2f
            // Left loop
            drawCircle(
                color = Color(0xFFFFD700),
                radius = bowSize * 0.5f,
                center = Offset(bowCenterX - bowSize * 0.6f, bowCenterY)
            )
            // Right loop
            drawCircle(
                color = Color(0xFFFFD700),
                radius = bowSize * 0.5f,
                center = Offset(bowCenterX + bowSize * 0.6f, bowCenterY)
            )
            // Center knot
            drawCircle(
                color = Color(0xFFFFAA00),
                radius = bowSize * 0.35f,
                center = Offset(bowCenterX, bowCenterY)
            )
            
            // Gift 2: Green box in center (taller)
            val gift2X = centerX - treeWidth * 0.10f
            val gift2Width = treeWidth * 0.20f
            val gift2Height = gift2Width * 1.15f
            val gift2Color = Color(0xFF388E3C)
            val gift2Top = giftBottom - gift2Height
            
            // Box
            drawRect(
                color = gift2Color,
                topLeft = Offset(gift2X, gift2Top),
                size = Size(gift2Width, gift2Height)
            )
            // Vertical ribbon (silver)
            drawRect(
                color = Color(0xFFC0C0C0),
                topLeft = Offset(gift2X + gift2Width / 2f - ribbonWidth / 2f, gift2Top),
                size = Size(ribbonWidth, gift2Height)
            )
            // Horizontal ribbon
            drawRect(
                color = Color(0xFFC0C0C0),
                topLeft = Offset(gift2X, gift2Top + gift2Height * 0.35f - ribbonWidth / 2f),
                size = Size(gift2Width, ribbonWidth)
            )
            // Bow on top (larger, at top of box)
            val bow2X = gift2X + gift2Width / 2f
            val bow2Y = gift2Top + gift2Height * 0.35f
            val bow2Size = ribbonWidth * 2.4f
            drawCircle(
                color = Color(0xFFC0C0C0),
                radius = bow2Size * 0.5f,
                center = Offset(bow2X - bow2Size * 0.6f, bow2Y)
            )
            drawCircle(
                color = Color(0xFFC0C0C0),
                radius = bow2Size * 0.5f,
                center = Offset(bow2X + bow2Size * 0.6f, bow2Y)
            )
            drawCircle(
                color = Color(0xFFB0B0B0),
                radius = bow2Size * 0.38f,
                center = Offset(bow2X, bow2Y)
            )
            
            // Gift 3: Blue box on the right (smaller, wider)
            val gift3X = centerX + treeWidth * 0.12f
            val gift3Width = treeWidth * 0.22f
            val gift3Height = gift3Width * 0.75f
            val gift3Color = Color(0xFF1976D2)
            val gift3Top = giftBottom - gift3Height
            
            // Box
            drawRect(
                color = gift3Color,
                topLeft = Offset(gift3X, gift3Top),
                size = Size(gift3Width, gift3Height)
            )
            // Vertical ribbon (white with hint of blue)
            drawRect(
                color = Color(0xFFF0F8FF),
                topLeft = Offset(gift3X + gift3Width * 0.40f - ribbonWidth / 2f, gift3Top),
                size = Size(ribbonWidth, gift3Height)
            )
            // Horizontal ribbon
            drawRect(
                color = Color(0xFFF0F8FF),
                topLeft = Offset(gift3X, gift3Top + gift3Height / 2f - ribbonWidth / 2f),
                size = Size(gift3Width, ribbonWidth)
            )
            // Bow
            val bow3X = gift3X + gift3Width * 0.40f
            val bow3Y = gift3Top + gift3Height / 2f
            val bow3Size = ribbonWidth * 2.0f
            drawCircle(
                color = Color(0xFFF0F8FF),
                radius = bow3Size * 0.5f,
                center = Offset(bow3X - bow3Size * 0.6f, bow3Y)
            )
            drawCircle(
                color = Color(0xFFF0F8FF),
                radius = bow3Size * 0.5f,
                center = Offset(bow3X + bow3Size * 0.6f, bow3Y)
            )
            drawCircle(
                color = Color(0xFFE0F0FF),
                radius = bow3Size * 0.35f,
                center = Offset(bow3X, bow3Y)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeChristmasTheme {
        ChristmasScene(skyTheme = SkyTheme.NightSky)
    }
}