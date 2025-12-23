package news.androiddev.composechristmas

import android.R.attr.startX
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.asAndroidPath
import news.androiddev.composechristmas.ui.theme.ComposeChristmasTheme
import kotlin.math.PI
import kotlin.math.pow
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

// Helper function to draw a drop shadow (oval shape with blur)
private fun DrawScope.drawDropShadow(
    center: Offset,
    width: Float,
    height: Float,
    blurRadius: Float = 20f,
    alpha: Float = 0.3f
) {
    // Draw multiple layers for a soft blur effect
    for (i in 0..3) {
        val layerAlpha = alpha * (1f - i * 0.2f)
        val layerWidth = width + i * blurRadius * 0.5f
        val layerHeight = height + i * blurRadius * 0.3f
        drawOval(
            color = Color.Black.copy(alpha = layerAlpha),
            topLeft = Offset(center.x - layerWidth / 2f, center.y - layerHeight / 2f),
            size = Size(layerWidth, layerHeight)
        )
    }
}

// Day 15: Snowflake drawing helper with customizable parameters
private fun DrawScope.drawSnowflake(
    center: Offset,
    size: Float,
    branches: Int = 6,
    complexity: Int = 2, // Number of detail levels per branch
    color: Color = Color.White,
    alpha: Float = 1f,
    rotation: Float = 0f
) {
    val strokeWidth = size * 0.08f

    withTransform({
        rotate(rotation, pivot = center)
    }) {
        // Draw each main branch radiating from center
        for (i in 0 until branches) {
            val angle = (i * 360f / branches) * (PI / 180f)
            val cosA = kotlin.math.cos(angle).toFloat()
            val sinA = kotlin.math.sin(angle).toFloat()

            // Main branch line
            val endX = center.x + size * cosA
            val endY = center.y + size * sinA
            drawLine(
                color = color,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
                alpha = alpha
            )

            // Add detail branches along the main branch
            for (level in 1..complexity) {
                val branchDist = size * (level.toFloat() / (complexity + 1))
                val branchLength = size * (0.35f - level * 0.08f)
                val branchX = center.x + branchDist * cosA
                val branchY = center.y + branchDist * sinA

                // Left side branch
                val leftAngle = angle - PI.toFloat() / 5f
                val leftEndX = branchX + branchLength * kotlin.math.cos(leftAngle).toFloat()
                val leftEndY = branchY + branchLength * kotlin.math.sin(leftAngle).toFloat()
                drawLine(
                    color = color,
                    start = Offset(branchX, branchY),
                    end = Offset(leftEndX, leftEndY),
                    strokeWidth = strokeWidth * 0.75f,
                    cap = StrokeCap.Round,
                    alpha = alpha
                )

                // Right side branch
                val rightAngle = angle + PI.toFloat() / 5f
                val rightEndX = branchX + branchLength * kotlin.math.cos(rightAngle).toFloat()
                val rightEndY = branchY + branchLength * kotlin.math.sin(rightAngle).toFloat()
                drawLine(
                    color = color,
                    start = Offset(branchX, branchY),
                    end = Offset(rightEndX, rightEndY),
                    strokeWidth = strokeWidth * 0.75f,
                    cap = StrokeCap.Round,
                    alpha = alpha
                )
            }

            // Optional: Add tiny crystalline details at the tips
            if (complexity > 1) {
                val tipSize = size * 0.12f
                drawCircle(
                    color = color,
                    radius = tipSize * 0.3f,
                    center = Offset(endX, endY),
                    alpha = alpha * 0.7f
                )
            }
        }

        // Center crystalline core
        drawCircle(
            color = color,
            radius = strokeWidth * 1.2f,
            center = center,
            alpha = alpha
        )
    }
}

@Composable
fun ChristmasScene(modifier: Modifier = Modifier, skyTheme: SkyTheme = SkyTheme.NightSky) {
    // State for light interaction (Day 22)
    // LightMode: 0 = rainbow cycling, 1 = original colors only, 2 = lights off
    var lightMode by remember { mutableStateOf(0) }
    
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
    // Sky animation cycle for sunset/sunrise effect
    val skyTime = twinkleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skyTime"
    )
    // Star twinkle time
    val starTwinkleTime = twinkleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTwinkleTime"
    )

    Canvas(modifier = modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    // Cycle through light modes: 0 -> 1 -> 2 -> 0
                    lightMode = (lightMode + 1) % 3
                }
            )
        }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // --- 1. Sky Background (Animated) ---
        // Cycle through night -> dawn -> day -> dusk -> night
        val skyPhase = (sin(2.0 * PI * skyTime.value) + 1.0) * 0.5 // 0..1 smooth cycle

        val skyBrush = when (skyTheme) {
            SkyTheme.NightSky -> {
                // Animate between deep night and dawn colors
                val nightColors = listOf(
                    Color(0xFF0B1026), // Deep Navy
                    Color(0xFF152044),
                    Color(0xFF1F2D5E)
                )
                val dawnColors = listOf(
                    Color(0xFF2B1308), // Purple dawn
                    Color(0xFF7A2E12),
                    Color(0xFFFF6A1A)
                )
                val blendedColors = nightColors.zip(dawnColors).map { (night, dawn) ->
                    androidx.compose.ui.graphics.lerp(night, dawn, skyPhase.toFloat())
                }
                Brush.verticalGradient(
                    colors = blendedColors,
                    startY = 0f,
                    endY = canvasHeight
                )
            }

            SkyTheme.WinterMorning -> {
                // Animate between bright morning and softer mid-day
                val morningColors = listOf(
                    Color(0xFFE3F2FD), // Pale Blue
                    Color(0xFFBBDEFB),
                    Color(0xFF90CAF9)
                )
                val midDayColors = listOf(
                    Color(0xFFB3E5FC),
                    Color(0xFF81D4FA),
                    Color(0xFF4FC3F7)
                )
                val blendedColors = morningColors.zip(midDayColors).map { (morning, midDay) ->
                    androidx.compose.ui.graphics.lerp(morning, midDay, skyPhase.toFloat())
                }
                Brush.verticalGradient(
                    colors = blendedColors,
                    startY = 0f,
                    endY = canvasHeight
                )
            }
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

        starPositions.forEachIndexed { idx, pos ->
            // Each star has its own phase offset for staggered twinkling
            val starPhase = ((idx * 23 + 7) % 100) / 100f
            val starBrightness =
                ((sin(2.0 * PI * (starTwinkleTime.value + starPhase)) + 1.0) * 0.5).toFloat()
            val brightness = 0.5f + 0.5f * starBrightness // 0.5..1.0 range

            // Glow Halo (animated)
            drawCircle(
                color = starColor.copy(alpha = starGlowAlpha * brightness),
                radius = starRadiusBase * (3.0f + 0.8f * starBrightness),
                center = pos
            )
            // Core (animated size and opacity)
            drawCircle(
                color = starColor.copy(alpha = brightness),
                radius = starRadiusBase * (0.9f + 0.2f * starBrightness),
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

        // --- 2c. Clouds (Animated, soft and fluffy) ---
        run {
            // Cloud colors based on sky theme
            val cloudColor = if (skyTheme == SkyTheme.NightSky) 
                Color(0xFF2B3A52) else Color(0xFFE1F5FE)
            val cloudHighlight = if (skyTheme == SkyTheme.NightSky) 
                Color(0xFF3A4A62) else Color(0xFFFFFFFFF)
            val cloudShadow = if (skyTheme == SkyTheme.NightSky) 
                Color(0xFF1A2333) else Color(0xFFB3E5FC)
            
            // Base alpha for clouds
            val baseAlpha = if (skyTheme == SkyTheme.NightSky) 0.6f else 0.7f
            
            // Define cloud positions and properties
            data class CloudData(
                val x: Float,
                val y: Float,
                val scale: Float,
                val speed: Float,
                val offset: Float
            )
            
            val clouds = listOf(
                CloudData(0.15f, 0.12f, 1.0f, 0.3f, 0.0f),
                CloudData(0.45f, 0.08f, 0.8f, 0.5f, 0.3f),
                CloudData(0.70f, 0.15f, 1.2f, 0.25f, 0.6f),
                CloudData(0.25f, 0.25f, 0.9f, 0.4f, 0.8f),
                CloudData(0.85f, 0.22f, 0.7f, 0.35f, 0.4f),
                CloudData(0.55f, 0.30f, 1.1f, 0.45f, 0.2f)
            )
            
            clouds.forEach { cloud ->
                // Animate clouds drifting slowly across the sky
                val driftOffset = ((skyTime.value * cloud.speed + cloud.offset) % 1f) * canvasWidth * 0.15f
                val cloudX = canvasWidth * cloud.x + driftOffset
                val cloudY = canvasHeight * cloud.y
                val cloudScale = cloud.scale
                
                // Base cloud size
                val baseRadius = canvasWidth * 0.06f * cloudScale
                
                // Draw fluffy cloud using overlapping circles
                // Main body circles
                listOf(
                    Offset(cloudX - baseRadius * 0.6f, cloudY) to baseRadius * 0.85f,
                    Offset(cloudX - baseRadius * 0.2f, cloudY - baseRadius * 0.3f) to baseRadius * 0.95f,
                    Offset(cloudX + baseRadius * 0.3f, cloudY - baseRadius * 0.2f) to baseRadius * 1.0f,
                    Offset(cloudX + baseRadius * 0.7f, cloudY + baseRadius * 0.1f) to baseRadius * 0.75f,
                    Offset(cloudX, cloudY + baseRadius * 0.2f) to baseRadius * 0.8f
                ).forEach { (center, radius) ->
                    // Shadow/darker bottom part
                    drawCircle(
                        color = cloudShadow.copy(alpha = baseAlpha * 0.3f),
                        radius = radius * 1.05f,
                        center = Offset(center.x, center.y + radius * 0.1f)
                    )
                    
                    // Main cloud body
                    drawCircle(
                        color = cloudColor.copy(alpha = baseAlpha),
                        radius = radius,
                        center = center
                    )
                    
                    // Highlight on top
                    drawCircle(
                        color = cloudHighlight.copy(alpha = baseAlpha * 0.4f),
                        radius = radius * 0.6f,
                        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.3f)
                    )
                }
                
                // Add some smaller puff details
                listOf(
                    Offset(cloudX - baseRadius * 0.4f, cloudY - baseRadius * 0.15f) to baseRadius * 0.5f,
                    Offset(cloudX + baseRadius * 0.5f, cloudY) to baseRadius * 0.55f
                ).forEach { (center, radius) ->
                    drawCircle(
                        color = cloudColor.copy(alpha = baseAlpha * 0.8f),
                        radius = radius,
                        center = center
                    )
                }
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
        val groundYAtCenter = canvasHeight * 0.81f
        val layerHeight = treeHeight / (numLayers + 1f)
        val layerOverlap = layerHeight * 0.35f
        
        // Calculate trunk dimensions first
        val trunkHeightAboveGround = layerHeight * 1.75f // Updated to match new trunk height
        
        // Position tree so bottom foliage layer sits just at/above the trunk top
        val treeTop = groundYAtCenter - trunkHeightAboveGround - ((numLayers - 1) * (layerHeight - layerOverlap) + layerHeight)
        val lastLayerTop = treeTop + (numLayers - 1) * (layerHeight - layerOverlap)
        val lastLayerBottom = lastLayerTop + layerHeight

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

            // Enhanced animation factors for star pulse and subtle rotation
            // Primary pulse - gentle breathing effect
            val starPulse =
                ((sin(2.0 * PI * (twinkleTime.value * 0.8 + 0.05)) + 1.0) * 0.5).toFloat()
            // Secondary faster pulse for glow intensity
            val glowPulse =
                ((sin(2.0 * PI * (twinkleTime.value * 1.5 + 0.2)) + 1.0) * 0.5).toFloat()
            // Smooth scaling with gentle pulse (0.92 to 1.10)
            val starScale = 0.92f + 0.18f * starPulse
            // Very subtle rotation
            val starRot = (sin(2.0 * PI * (twinkleTime.value * 0.4 + 0.15)) * 1.5).toFloat()

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
                // Multi-layer pulsing glow around the star
                // Outermost glow - very soft, large radius
                drawCircle(
                    color = Color(0xFFFFF8E1).copy(alpha = 0.15f + 0.25f * glowPulse),
                    radius = outerRadius * (3.0f + 0.8f * glowPulse),
                    center = starCenter
                )
                // Middle glow layer
                drawCircle(
                    color = Color(0xFFFFFDE7).copy(alpha = 0.25f + 0.30f * starPulse),
                    radius = outerRadius * (2.0f + 0.6f * starPulse),
                    center = starCenter
                )
                // Inner bright glow
                drawCircle(
                    color = Color(0xFFFFEB3B).copy(alpha = 0.35f + 0.35f * glowPulse),
                    radius = outerRadius * (1.2f + 0.4f * starPulse),
                    center = starCenter
                )
                // Core bright glow
                drawCircle(
                    color = Color(0xFFFFF59D).copy(alpha = 0.50f + 0.30f * glowPulse),
                    radius = outerRadius * (0.8f + 0.2f * starPulse),
                    center = starCenter
                )

                // Draw the star with the gradient
                drawPath(path = starPath, brush = starGradient)
                
                // Add a bright inner highlight that pulses
                drawPath(
                    path = starPath,
                    color = Color.White.copy(alpha = 0.15f + 0.25f * glowPulse)
                )
            }
        }

        // --- 6. Trunk Logic ---
        // Calculate ground level at center (approximate bezier Y at t=0.5)
        // Bezier P0y=0.85, P1y=0.80, P2y=0.80, P3y=0.85 -> Midpoint is ~0.8125

        // Make trunk bigger and taller
        val trunkWidthTop = treeWidth * 0.24f
        val trunkWidthBottom = treeWidth * 0.32f

        // Make trunk half the previous height
        val trunkTop = lastLayerBottom - layerHeight * 1.75f // Half of 3.5f
        val trunkBottom = groundYAtCenter + 25f // Half of 50f

        // Tree sway animation (gentle rotation around ground center)
        val treeSwayDeg = (sin(2.0 * PI * (treeSwayTime.value + 0.10)) * 1.1).toFloat()

        // --- Tree shadow (below trunk, before trunk and gifts) ---
        drawDropShadow(
            center = Offset(centerX, groundYAtCenter + 5f),
            width = treeWidth * 0.8f,
            height = treeWidth * 0.25f,
            blurRadius = 36f, // increased blur
            alpha = 0.18f // reduced alpha for subtlety
        )

        withTransform({ rotate(treeSwayDeg, pivot = Offset(centerX, groundYAtCenter)) }) {
            // Draw trunk as trapezoid (wider at bottom)
            val trunkPath = Path().apply {
                moveTo(centerX - trunkWidthTop / 2f, trunkTop)
                lineTo(centerX + trunkWidthTop / 2f, trunkTop)
                lineTo(centerX + trunkWidthBottom / 2f, trunkBottom)
                lineTo(centerX - trunkWidthBottom / 2f, trunkBottom)
                close()
            }
            
            // Main trunk color with gradient
            drawPath(
                path = trunkPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6D4C41),
                        Color(0xFF5D4037),
                        Color(0xFF4E342E)
                    ),
                    startY = trunkTop,
                    endY = trunkBottom
                )
            )
            
            // Wood grain texture lines
            val grainColor = Color(0xFF3E2723)
            for (i in 0..3) {
                val t = (i + 1) / 5f
                val y = trunkTop + (trunkBottom - trunkTop) * t
                val widthAtY = trunkWidthTop + (trunkWidthBottom - trunkWidthTop) * t
                
                // Curved grain lines
                val grainPath = Path().apply {
                    val leftX = centerX - widthAtY * 0.35f
                    val rightX = centerX + widthAtY * 0.35f
                    moveTo(leftX, y)
                    cubicTo(
                        centerX - widthAtY * 0.15f, y - layerHeight * 0.15f,
                        centerX + widthAtY * 0.15f, y + layerHeight * 0.15f,
                        rightX, y
                    )
                }
                drawPath(
                    path = grainPath,
                    color = grainColor.copy(alpha = 0.3f),
                    style = Stroke(width = trunkWidthTop * 0.03f, cap = StrokeCap.Round)
                )
            }
            
            // Add some knots/details
            drawCircle(
                color = grainColor.copy(alpha = 0.4f),
                radius = trunkWidthTop * 0.08f,
                center = Offset(centerX - trunkWidthTop * 0.2f, trunkTop + (trunkBottom - trunkTop) * 0.4f)
            )
            
            // Blending effect at trunk base
            val blendRadius = trunkWidthBottom * 0.6f
            val blendCenter = Offset(centerX, trunkBottom - blendRadius * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color(0xFFE0F7FA).copy(alpha = 0.35f), Color(0xFF5D4037).copy(alpha = 0.0f)),
                    center = blendCenter,
                    radius = blendRadius
                ),
                radius = blendRadius,
                center = blendCenter
            )
        }

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

        // --- 7. Tree Foliage Layers with Rounded Scalloped Edges ---
        val layerBounds = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until numLayers) {
            val progressFromTop = i / (numLayers - 1f)
            val layerTop = treeTop + i * (layerHeight - layerOverlap)
            val layerBottom = layerTop + layerHeight

            val baseWidth = treeWidth * (0.25f + 0.75f * progressFromTop)
            val layerWidth = baseWidth
            val layerLeft = centerX - layerWidth / 2f
            val layerRight = centerX + layerWidth / 2f

            layerBounds.add(layerLeft to layerRight)

            // Number of scallops increases with layer size
            val numScallops = 5 + i
            val scallopsPerSide = numScallops / 2

            // Create rounded, scalloped layer outline
            val path = Path().apply {
                moveTo(centerX, layerTop)

                // Right side scallops
                for (j in 0 until scallopsPerSide) {
                    val t1 = j / scallopsPerSide.toFloat()
                    val t2 = (j + 1) / scallopsPerSide.toFloat()
                    val y1 = layerTop + layerHeight * t1
                    val y2 = layerTop + layerHeight * t2
                    val yMid = (y1 + y2) / 2f
                    val x1 = centerX + (layerWidth / 2f) * t1
                    val x2 = centerX + (layerWidth / 2f) * t2
                    val xPeak = x2 + layerWidth * 0.08f

                    quadraticTo(xPeak, yMid, x2, y2)
                }

                // Bottom rounded edge
                val bottomY = layerBottom
                cubicTo(
                    layerRight * 0.8f + centerX * 0.2f, bottomY + layerHeight * 0.1f,
                    layerLeft * 0.8f + centerX * 0.2f, bottomY + layerHeight * 0.1f,
                    layerLeft, layerBottom
                )

                // Left side scallops
                for (j in scallopsPerSide - 1 downTo 0) {
                    val t1 = (j + 1) / scallopsPerSide.toFloat()
                    val t2 = j / scallopsPerSide.toFloat()
                    val y1 = layerTop + layerHeight * t1
                    val y2 = layerTop + layerHeight * t2
                    val yMid = (y1 + y2) / 2f
                    val x1 = centerX - (layerWidth / 2f) * t1
                    val x2 = centerX - (layerWidth / 2f) * t2
                    val xPeak = x1 - layerWidth * 0.08f

                    quadraticTo(xPeak, yMid, x2, y2)
                }

                close()
            }

            // Draw main layer with gradient
            drawPath(path = path, brush = foliageBrush)

            // Add darker shadow at bottom of layer
            val shadowPath = Path().apply {
                val shadowHeight = layerHeight * 0.25f
                moveTo(layerRight, layerBottom)
                cubicTo(
                    layerRight * 0.8f + centerX * 0.2f, layerBottom + layerHeight * 0.1f,
                    layerLeft * 0.8f + centerX * 0.2f, layerBottom + layerHeight * 0.1f,
                    layerLeft, layerBottom
                )
                lineTo(layerLeft * 0.9f + centerX * 0.1f, layerBottom - shadowHeight)
                cubicTo(
                    centerX, layerBottom - shadowHeight * 0.5f,
                    centerX, layerBottom - shadowHeight * 0.5f,
                    layerRight * 0.9f + centerX * 0.1f, layerBottom - shadowHeight
                )
                close()
            }
            drawPath(
                path = shadowPath,
                color = Color(0xFF1B5E20).copy(alpha = 0.5f)
            )

            // Subtle lighter highlights (very minimal)
            if (i % 2 == 0) {
                val highlightPath = Path().apply {
                    val y = layerTop + layerHeight * 0.3f
                    val hWidth = layerWidth * 0.4f
                    moveTo(centerX - hWidth * 0.5f, y)
                    cubicTo(
                        centerX - hWidth * 0.2f, y - layerHeight * 0.05f,
                        centerX + hWidth * 0.2f, y - layerHeight * 0.05f,
                        centerX + hWidth * 0.5f, y
                    )
                }
                drawPath(
                    path = highlightPath,
                    color = Color(0xFF4CAF50).copy(alpha = 0.25f),
                    style = Stroke(width = layerHeight * 0.04f, cap = StrokeCap.Round)
                )
            }
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
                val y =
                    treeTop + layerIndex * (layerHeight - layerOverlap) + layerHeight * 0.52f
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
                    val nextWobble =
                        (if (wraps[idx + 1] % 2 == 0) 1f else -1f) * nextBase * 0.05f
                    val nextWidth = nextBase + nextWobble
                    val nextY =
                        treeTop + wraps[idx + 1] * (layerHeight - layerOverlap) + layerHeight * 0.50f
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
                    add(
                        LightState(
                            position = pos,
                            color = color,
                            radius = radius,
                            isOn = true,
                            phase = phase
                        )
                    )
                    dist += bulbStep
                    idx++
                }
            }

            // Render bulbs with animated twinkling and color transitions
            lights.forEach { light ->
                // Check if lights should be off (lightMode == 2)
                if (lightMode == 2) {
                    // Draw very dim bulbs to show they're off
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.15f),
                        radius = light.radius,
                        center = light.position
                    )
                    return@forEach
                }
                
                // Twinkle factor: 0.0..1.0 based on shared time and per-light phase
                // Use faster, more varied twinkling patterns
                val twinkleSpeed = 1.5f + light.phase * 0.8f
                val sparkle =
                    ((sin(2.0 * PI * (twinkleTime.value * twinkleSpeed + light.phase)) + 1.0) * 0.5).toFloat()
                
                // Some lights blink on/off more dramatically
                val blinkThreshold = 0.15f + light.phase * 0.15f
                val isLightOn = sparkle > blinkThreshold
                val onFactor = if (isLightOn) 1f else 0.1f
                
                // Enhanced sparkle intensity
                val intensityFactor = (0.3f + 0.7f * sparkle) * onFactor
                
                // Determine color based on light mode
                val animatedColor = when (lightMode) {
                    1 -> {
                        // Mode 1: Original colors only (no cycling)
                        light.color
                    }
                    else -> {
                        // Mode 0: Rainbow color cycling (original behavior)
                        val colorShift = (twinkleTime.value * 0.3f + light.phase) % 1f
                        when {
                            colorShift < 0.17f -> {
                                // Transition from original to red
                                androidx.compose.ui.graphics.lerp(
                                    light.color,
                                    Color(0xFFFF5252),
                                    (colorShift / 0.17f)
                                )
                            }
                            colorShift < 0.34f -> {
                                // Transition from red to amber
                                androidx.compose.ui.graphics.lerp(
                                    Color(0xFFFF5252),
                                    Color(0xFFFFD740),
                                    ((colorShift - 0.17f) / 0.17f)
                                )
                            }
                            colorShift < 0.50f -> {
                                // Transition from amber to green
                                androidx.compose.ui.graphics.lerp(
                                    Color(0xFFFFD740),
                                    Color(0xFF69F0AE),
                                    ((colorShift - 0.34f) / 0.16f)
                                )
                            }
                            colorShift < 0.67f -> {
                                // Transition from green to cyan
                                androidx.compose.ui.graphics.lerp(
                                    Color(0xFF69F0AE),
                                    Color(0xFF40C4FF),
                                    ((colorShift - 0.50f) / 0.17f)
                                )
                            }
                            colorShift < 0.84f -> {
                                // Transition from cyan to violet
                                androidx.compose.ui.graphics.lerp(
                                    Color(0xFF40C4FF),
                                    Color(0xFFEA80FC),
                                    ((colorShift - 0.67f) / 0.17f)
                                )
                            }
                            else -> {
                                // Transition from violet back to original
                                androidx.compose.ui.graphics.lerp(
                                    Color(0xFFEA80FC),
                                    light.color,
                                    ((colorShift - 0.84f) / 0.16f)
                                )
                            }
                        }
                    }
                }
                
                // Dynamic glow radius based on sparkle
                val glowRadius = light.radius * (1.8f + 1.2f * sparkle)
                
                // Outer glow (largest, softest)
                drawCircle(
                    color = animatedColor.copy(alpha = 0.35f * intensityFactor),
                    radius = glowRadius,
                    center = light.position
                )
                
                // Middle glow
                drawCircle(
                    color = animatedColor.copy(alpha = 0.50f * intensityFactor),
                    radius = glowRadius * 0.65f,
                    center = light.position
                )
                
                // Inner bright glow
                drawCircle(
                    color = animatedColor.copy(alpha = 0.75f * intensityFactor),
                    radius = glowRadius * 0.40f,
                    center = light.position
                )
                
                // Bulb core (solid when on, dim when off)
                drawCircle(
                    color = animatedColor.copy(alpha = 0.95f * intensityFactor),
                    radius = light.radius,
                    center = light.position
                )
                
                // Animated highlight spec (brighter when sparkling)
                drawCircle(
                    color = Color.White.copy(alpha = (0.90f * sparkle) * onFactor),
                    radius = light.radius * 0.25f,
                    center = Offset(
                        light.position.x - light.radius * 0.22f,
                        light.position.y - light.radius * 0.22f
                    )
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
                val y =
                    treeTop + layerIndex * (layerHeight - layerOverlap) + layerHeight * 0.55f
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
                    val nextWobble =
                        (if (wraps[idx + 1] % 2 == 0) 1f else -1f) * nextBase * 0.06f
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

        // --- Festive Greeting at Top ---
        run {
            val greeting = "Merry Christmas!"
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = size.width * 0.08f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(8f, 0f, 4f, android.graphics.Color.argb(120, 0, 0, 0))
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
            }
            val x = size.width / 2f
            val y = size.height * 0.10f
            
            // --- Draw Christmas hat BEFORE text so it appears visible ---
            val cIndex = greeting.indexOf('C')
            if (cIndex != -1) {
                val textUpToC = greeting.substring(0, cIndex)
                val textWidthUpToC = paint.measureText(textUpToC)
                val cWidth = paint.measureText("C")
                // Since textAlign is CENTER, x is the center of the whole text
                val greetingWidth = paint.measureText(greeting)
                val cCenterX = x - greetingWidth / 2f + textWidthUpToC + cWidth / 2f
                val textTopY = y - paint.textSize * 0.75f // top of text
                // Hat size relative to font size
                val hatHeight = paint.textSize * 1.0f
                val hatWidth = hatHeight * 0.95f
                val hatBaseY = textTopY - hatHeight * 0.05f
                
                drawContext.canvas.nativeCanvas.apply {
                    // Draw curved, floppy hat body
                    val hatPath = Path().apply {
                        moveTo(cCenterX - hatWidth * 0.45f, hatBaseY)
                        // Left side curve up
                        quadraticBezierTo(
                            cCenterX - hatWidth * 0.35f, hatBaseY - hatHeight * 0.5f,
                            cCenterX - hatWidth * 0.15f, hatBaseY - hatHeight * 0.75f
                        )
                        // Top curve leaning right
                        quadraticBezierTo(
                            cCenterX + hatWidth * 0.05f, hatBaseY - hatHeight * 0.95f,
                            cCenterX + hatWidth * 0.35f, hatBaseY - hatHeight * 0.65f
                        )
                        // Right side curve down
                        quadraticBezierTo(
                            cCenterX + hatWidth * 0.45f, hatBaseY - hatHeight * 0.5f,
                            cCenterX + hatWidth * 0.45f, hatBaseY
                        )
                        close()
                    }
                    
                    // Draw shadow
                    val shadowPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(180, 30, 30)
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                    }
                    // Draw main hat
                    val hatPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(220, 38, 38) // red
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        setShadowLayer(8f, 2f, 3f, android.graphics.Color.argb(100, 0, 0, 0))
                    }
                    drawPath(hatPath.asAndroidPath(), hatPaint)
                    
                    // Draw highlight on left side
                    val highlightPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(240, 80, 80)
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        alpha = 120
                    }
                    val highlightPath = Path().apply {
                        moveTo(cCenterX - hatWidth * 0.40f, hatBaseY - hatHeight * 0.05f)
                        quadraticBezierTo(
                            cCenterX - hatWidth * 0.30f, hatBaseY - hatHeight * 0.45f,
                            cCenterX - hatWidth * 0.15f, hatBaseY - hatHeight * 0.70f
                        )
                        lineTo(cCenterX - hatWidth * 0.20f, hatBaseY - hatHeight * 0.65f)
                        quadraticBezierTo(
                            cCenterX - hatWidth * 0.32f, hatBaseY - hatHeight * 0.40f,
                            cCenterX - hatWidth * 0.42f, hatBaseY - hatHeight * 0.02f
                        )
                        close()
                    }
                    drawPath(highlightPath.asAndroidPath(), highlightPaint)
                    
                    // Draw white fur brim (thicker, fluffy)
                    val brimPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(60, 0, 0, 0))
                    }
                    val brimHeight = hatHeight * 0.28f
                    val brimRect = android.graphics.RectF(
                        cCenterX - hatWidth * 0.55f,
                        hatBaseY - brimHeight / 2f,
                        cCenterX + hatWidth * 0.55f,
                        hatBaseY + brimHeight / 2f
                    )
                    drawOval(brimRect, brimPaint)
                    
                    // Draw white pom-pom at the tip
                    val pomRadius = hatHeight * 0.22f
                    drawCircle(
                        cCenterX + hatWidth * 0.32f,
                        hatBaseY - hatHeight * 0.68f,
                        pomRadius,
                        brimPaint
                    )
                }
            }
            
            // Draw text AFTER hat so text appears in front
            drawContext.canvas.nativeCanvas.drawText(greeting, x, y, paint)
        }

        // --- 9. Gift Boxes with Ribbons and Bows ---
        run {
            fun groundYAt(x: Float): Float {
                val W = canvasWidth
                val H = canvasHeight
                val t = (x / W).coerceIn(0f, 1f)
                val P0 = Offset(0f, H * 0.85f)
                val P1 = Offset(W * 0.3f, H * 0.80f)
                val P2 = Offset(W * 0.7f, H * 0.80f)
                val P3 = Offset(W, H * 0.85f)
                val y = (1 - t).pow(3) * P0.y +
                        3 * (1 - t).pow(2) * t * P1.y +
                        3 * (1 - t) * t * t * P2.y +
                        t.pow(3) * P3.y
                return y
            }
            // Gift 1: Red box on the left
            val gift1X = centerX - treeWidth * 0.32f // shift left for larger size
            val gift1Width = treeWidth * 0.22f // was 0.18f
            val gift1Height = gift1Width * 1.05f // was 0.95f
            val gift1Color = Color(0xFFD32F2F)
            val gift1Bottom = groundYAt(gift1X + gift1Width / 2f)
            val gift1Top = gift1Bottom - gift1Height

            // Shadow for gift 1
            drawDropShadow(
                center = Offset(gift1X + gift1Width / 2f, gift1Bottom + 3f),
                width = gift1Width * 1.1f,
                height = gift1Width * 0.3f,
                blurRadius = 28f,
                alpha = 0.15f
            )

            // 3D Box - Front face with shading
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE53935),
                        Color(0xFFD32F2F),
                        Color(0xFFC62828)
                    )
                ),
                topLeft = Offset(gift1X, gift1Top),
                size = Size(gift1Width, gift1Height)
            )
            
            // 3D Box - Top face (perspective) - More visible
            val topDepth = gift1Width * 0.35f
            drawPath(
                path = Path().apply {
                    moveTo(gift1X, gift1Top)
                    lineTo(gift1X + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width, gift1Top)
                    close()
                },
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEF5350),
                        Color(0xFFE57373),
                        Color(0xFFEF5350)
                    ),
                    start = Offset(gift1X, gift1Top - topDepth * 0.5f),
                    end = Offset(gift1X + gift1Width, gift1Top - topDepth * 0.5f)
                )
            )
            
            // 3D Box - Right side face (darker)
            drawPath(
                path = Path().apply {
                    moveTo(gift1X + gift1Width, gift1Top)
                    lineTo(gift1X + gift1Width + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width + topDepth * 0.7f, gift1Bottom - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width, gift1Bottom)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9A0007),
                        Color(0xFF7F0000),
                        Color(0xFF6A0000)
                    )
                )
            )

            // Vertical ribbon with shading
            val ribbonWidth = gift1Width * 0.18f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFB300),
                        Color(0xFFFFC107),
                        Color(0xFFFFD54F),
                        Color(0xFFFFC107),
                        Color(0xFFFFB300)
                    )
                ),
                topLeft = Offset(gift1X + gift1Width / 2f - ribbonWidth / 2f, gift1Top),
                size = Size(ribbonWidth, gift1Height)
            )
            // Ribbon on top face
            drawPath(
                path = Path().apply {
                    val ribbonX = gift1X + gift1Width / 2f - ribbonWidth / 2f
                    moveTo(ribbonX, gift1Top)
                    lineTo(ribbonX + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(ribbonX + ribbonWidth + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(ribbonX + ribbonWidth, gift1Top)
                    close()
                },
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD54F),
                        Color(0xFFFFC107)
                    )
                )
            )
            // Ribbon on right face
            drawPath(
                path = Path().apply {
                    val ribbonX = gift1X + gift1Width / 2f - ribbonWidth / 2f
                    moveTo(ribbonX + ribbonWidth, gift1Top)
                    lineTo(ribbonX + ribbonWidth + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(ribbonX + ribbonWidth + topDepth * 0.7f, gift1Bottom - topDepth * 0.5f)
                    lineTo(ribbonX + ribbonWidth, gift1Bottom)
                    close()
                },
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFB300),
                        Color(0xFFFF8F00)
                    )
                )
            )

            // Horizontal ribbon on front face with shading
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFB300),
                        Color(0xFFFFC107),
                        Color(0xFFFFD54F),
                        Color(0xFFFFC107),
                        Color(0xFFFFB300)
                    )
                ),
                topLeft = Offset(gift1X, gift1Top + gift1Height / 2f - ribbonWidth / 2f),
                size = Size(gift1Width, ribbonWidth)
            )
            // Horizontal ribbon on top face
            drawPath(
                path = Path().apply {
                    val ribbonY = gift1Top + gift1Height / 2f - ribbonWidth / 2f
                    moveTo(gift1X, gift1Top)
                    lineTo(gift1X + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width + topDepth * 0.7f, gift1Top - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width, gift1Top)
                    close()
                },
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD54F),
                        Color(0xFFFFC107)
                    )
                )
            )
            // Horizontal ribbon on right face
            drawPath(
                path = Path().apply {
                    val ribbonY = gift1Top + gift1Height / 2f - ribbonWidth / 2f
                    moveTo(gift1X + gift1Width, ribbonY)
                    lineTo(gift1X + gift1Width + topDepth * 0.7f, ribbonY - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width + topDepth * 0.7f, ribbonY + ribbonWidth - topDepth * 0.5f)
                    lineTo(gift1X + gift1Width, ribbonY + ribbonWidth)
                    close()
                },
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFB300),
                        Color(0xFFFF8F00)
                    )
                )
            )

            // 3D Bow on top
            val bow1Y = gift1Top - gift1Height * 0.05f
            val bowSize = gift1Width * 0.45f
            val bowCenterX = gift1X + gift1Width / 2f
            // Ribbon tails
            drawPath(
                path = Path().apply {
                    moveTo(bowCenterX - bowSize * 0.08f, bow1Y + bowSize * 0.15f)
                    lineTo(bowCenterX - bowSize * 0.15f, bow1Y + bowSize * 0.35f)
                    lineTo(bowCenterX - bowSize * 0.05f, bow1Y + bowSize * 0.32f)
                    lineTo(bowCenterX, bow1Y + bowSize * 0.15f)
                    close()
                },
                color = Color(0xFFFFD54F)
            )
            drawPath(
                path = Path().apply {
                    moveTo(bowCenterX + bowSize * 0.08f, bow1Y + bowSize * 0.15f)
                    lineTo(bowCenterX + bowSize * 0.15f, bow1Y + bowSize * 0.35f)
                    lineTo(bowCenterX + bowSize * 0.05f, bow1Y + bowSize * 0.32f)
                    lineTo(bowCenterX, bow1Y + bowSize * 0.15f)
                    close()
                },
                color = Color(0xFFFFD54F)
            )
            // Left loop with 3D shading
            drawPath(
                path = Path().apply {
                    moveTo(bowCenterX - bowSize * 0.15f, bow1Y)
                    quadraticBezierTo(
                        bowCenterX - bowSize * 0.5f, bow1Y - bowSize * 0.3f,
                        bowCenterX - bowSize * 0.42f, bow1Y + bowSize * 0.05f
                    )
                    quadraticBezierTo(
                        bowCenterX - bowSize * 0.35f, bow1Y + bowSize * 0.15f,
                        bowCenterX - bowSize * 0.15f, bow1Y + bowSize * 0.08f
                    )
                    close()
                },
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F),
                        Color(0xFFFFC107),
                        Color(0xFFFFB300)
                    ),
                    center = Offset(bowCenterX - bowSize * 0.35f, bow1Y - bowSize * 0.1f)
                )
            )
            // Right loop with 3D shading
            drawPath(
                path = Path().apply {
                    moveTo(bowCenterX + bowSize * 0.15f, bow1Y)
                    quadraticBezierTo(
                        bowCenterX + bowSize * 0.5f, bow1Y - bowSize * 0.3f,
                        bowCenterX + bowSize * 0.42f, bow1Y + bowSize * 0.05f
                    )
                    quadraticBezierTo(
                        bowCenterX + bowSize * 0.35f, bow1Y + bowSize * 0.15f,
                        bowCenterX + bowSize * 0.15f, bow1Y + bowSize * 0.08f
                    )
                    close()
                },
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F),
                        Color(0xFFFFC107),
                        Color(0xFFFFB300)
                    ),
                    center = Offset(bowCenterX + bowSize * 0.35f, bow1Y - bowSize * 0.1f)
                )
            )
            // Center knot
            drawCircle(
                color = Color(0xFFFFA000),
                radius = bowSize * 0.12f,
                center = Offset(bowCenterX, bow1Y + bowSize * 0.04f)
            )

            // Gift 2: Green box in center (taller)
            val gift2X = centerX - treeWidth * 0.13f // shift left for larger size
            val gift2Width = treeWidth * 0.25f // was 0.20f
            val gift2Height = gift2Width * 1.18f // was 1.15f
            val gift2Color = Color(0xFF388E3C)
            val gift2CenterX = gift2X + gift2Width / 2f
            val gift2Bottom = groundYAt(gift2CenterX)
            val gift2Top = gift2Bottom - gift2Height

            // Shadow for gift 2
            drawDropShadow(
                center = Offset(gift2X + gift2Width / 2f, gift2Bottom + 3f),
                width = gift2Width * 1.1f,
                height = gift2Width * 0.3f,
                blurRadius = 28f,
                alpha = 0.15f
            )

            // 3D Box - Front face with shading
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF43A047),
                        Color(0xFF388E3C),
                        Color(0xFF2E7D32)
                    )
                ),
                topLeft = Offset(gift2X, gift2Top),
                size = Size(gift2Width, gift2Height)
            )
            
            // 3D Box - Top face (perspective) - More visible
            val topDepth2 = gift2Width * 0.35f
            drawPath(
                path = Path().apply {
                    moveTo(gift2X, gift2Top)
                    lineTo(gift2X + topDepth2 * 0.7f, gift2Top - topDepth2 * 0.5f)
                    lineTo(gift2X + gift2Width + topDepth2 * 0.7f, gift2Top - topDepth2 * 0.5f)
                    lineTo(gift2X + gift2Width, gift2Top)
                    close()
                },
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4CAF50),
                        Color(0xFF66BB6A),
                        Color(0xFF4CAF50)
                    ),
                    start = Offset(gift2X, gift2Top - topDepth2 * 0.5f),
                    end = Offset(gift2X + gift2Width, gift2Top - topDepth2 * 0.5f)
                )
            )
            
            // 3D Box - Right side face (darker)
            drawPath(
                path = Path().apply {
                    moveTo(gift2X + gift2Width, gift2Top)
                    lineTo(gift2X + gift2Width + topDepth2 * 0.7f, gift2Top - topDepth2 * 0.5f)
                    lineTo(gift2X + gift2Width + topDepth2 * 0.7f, gift2Bottom - topDepth2 * 0.5f)
                    lineTo(gift2X + gift2Width, gift2Bottom)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF003300),
                        Color(0xFF1B5E20),
                        Color(0xFF003300)
                    )
                )
            )

            // Vertical ribbon (silver/white) with shading
            val ribbonWidth2 = gift2Width * 0.18f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFBDBDBD),
                        Color(0xFFE0E0E0),
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0),
                        Color(0xFFBDBDBD)
                    )
                ),
                topLeft = Offset(gift2X + gift2Width / 2f - ribbonWidth2 / 2f, gift2Top),
                size = Size(ribbonWidth2, gift2Height)
            )
            // Ribbon on top face
            drawPath(
                path = Path().apply {
                    val ribbonX = gift2X + gift2Width / 2f - ribbonWidth2 / 2f
                    moveTo(ribbonX, gift2Top)
                    lineTo(ribbonX + topDepth2 * 0.6f, gift2Top - topDepth2 * 0.4f)
                    lineTo(ribbonX + ribbonWidth2 + topDepth2 * 0.6f, gift2Top - topDepth2 * 0.4f)
                    lineTo(ribbonX + ribbonWidth2, gift2Top)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            // Ribbon on right face
            drawPath(
                path = Path().apply {
                    val ribbonX = gift2X + gift2Width / 2f - ribbonWidth2 / 2f
                    moveTo(ribbonX + ribbonWidth2, gift2Top)
                    lineTo(ribbonX + ribbonWidth2 + topDepth2 * 0.6f, gift2Top - topDepth2 * 0.4f)
                    lineTo(ribbonX + ribbonWidth2 + topDepth2 * 0.6f, gift2Bottom - topDepth2 * 0.4f)
                    lineTo(ribbonX + ribbonWidth2, gift2Bottom)
                    close()
                },
                color = Color(0xFFBDBDBD)
            )

            // Horizontal ribbon on front face
            drawRect(
                color = Color(0xFFE0E0E0),
                topLeft = Offset(gift2X, gift2Top + gift2Height / 2f - ribbonWidth2 / 2f),
                size = Size(gift2Width, ribbonWidth2)
            )
            // Horizontal ribbon on top face
            drawPath(
                path = Path().apply {
                    moveTo(gift2X, gift2Top)
                    lineTo(gift2X + topDepth2 * 0.6f, gift2Top - topDepth2 * 0.4f)
                    lineTo(gift2X + gift2Width + topDepth2 * 0.6f, gift2Top - topDepth2 * 0.4f)
                    lineTo(gift2X + gift2Width, gift2Top)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            // Horizontal ribbon on right face
            drawPath(
                path = Path().apply {
                    val ribbonY = gift2Top + gift2Height / 2f - ribbonWidth2 / 2f
                    moveTo(gift2X + gift2Width, ribbonY)
                    lineTo(gift2X + gift2Width + topDepth2 * 0.6f, ribbonY - topDepth2 * 0.4f)
                    lineTo(gift2X + gift2Width + topDepth2 * 0.6f, ribbonY + ribbonWidth2 - topDepth2 * 0.4f)
                    lineTo(gift2X + gift2Width, ribbonY + ribbonWidth2)
                    close()
                },
                color = Color(0xFFBDBDBD)
            )

            // 3D Bow on top
            val bow2Y = gift2Top - gift2Height * 0.05f
            val bow2Size = gift2Width * 0.45f
            val bow2CenterX = gift2X + gift2Width / 2f
            // Ribbon tails
            drawPath(
                path = Path().apply {
                    moveTo(bow2CenterX - bow2Size * 0.08f, bow2Y + bow2Size * 0.15f)
                    lineTo(bow2CenterX - bow2Size * 0.15f, bow2Y + bow2Size * 0.35f)
                    lineTo(bow2CenterX - bow2Size * 0.05f, bow2Y + bow2Size * 0.32f)
                    lineTo(bow2CenterX, bow2Y + bow2Size * 0.15f)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            drawPath(
                path = Path().apply {
                    moveTo(bow2CenterX + bow2Size * 0.08f, bow2Y + bow2Size * 0.15f)
                    lineTo(bow2CenterX + bow2Size * 0.15f, bow2Y + bow2Size * 0.35f)
                    lineTo(bow2CenterX + bow2Size * 0.05f, bow2Y + bow2Size * 0.32f)
                    lineTo(bow2CenterX, bow2Y + bow2Size * 0.15f)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            // Left loop
            drawPath(
                path = Path().apply {
                    moveTo(bow2CenterX - bow2Size * 0.15f, bow2Y)
                    quadraticBezierTo(
                        bow2CenterX - bow2Size * 0.5f, bow2Y - bow2Size * 0.3f,
                        bow2CenterX - bow2Size * 0.42f, bow2Y + bow2Size * 0.05f
                    )
                    quadraticBezierTo(
                        bow2CenterX - bow2Size * 0.35f, bow2Y + bow2Size * 0.15f,
                        bow2CenterX - bow2Size * 0.15f, bow2Y + bow2Size * 0.08f
                    )
                    close()
                },
                color = Color(0xFFE0E0E0)
            )
            // Right loop
            drawPath(
                path = Path().apply {
                    moveTo(bow2CenterX + bow2Size * 0.15f, bow2Y)
                    quadraticBezierTo(
                        bow2CenterX + bow2Size * 0.5f, bow2Y - bow2Size * 0.3f,
                        bow2CenterX + bow2Size * 0.42f, bow2Y + bow2Size * 0.05f
                    )
                    quadraticBezierTo(
                        bow2CenterX + bow2Size * 0.35f, bow2Y + bow2Size * 0.15f,
                        bow2CenterX + bow2Size * 0.15f, bow2Y + bow2Size * 0.08f
                    )
                    close()
                },
                color = Color(0xFFE0E0E0)
            )
            // Center knot
            drawCircle(
                color = Color(0xFFBDBDBD),
                radius = bow2Size * 0.12f,
                center = Offset(bow2CenterX, bow2Y + bow2Size * 0.04f)
            )

            // Gift 3: Blue box on the right (smaller, wider)
            val gift3X = centerX + treeWidth * 0.17f // shift right for larger size
            val gift3Width = treeWidth * 0.28f // was 0.22f
            val gift3Height = gift3Width * 0.80f // was 0.75f
            val gift3Color = Color(0xFF1976D2)
            val gift3Bottom = groundYAt(gift3X + gift3Width / 2f)
            val gift3Top = gift3Bottom - gift3Height

            // Shadow for gift 3
            drawDropShadow(
                center = Offset(gift3X + gift3Width / 2f, gift3Bottom + 3f),
                width = gift3Width * 1.1f,
                height = gift3Width * 0.3f,
                blurRadius = 28f,
                alpha = 0.15f
            )

            // 3D Box - Front face with shading
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF1976D2),
                        Color(0xFF1565C0)
                    )
                ),
                topLeft = Offset(gift3X, gift3Top),
                size = Size(gift3Width, gift3Height)
            )
            
            // 3D Box - Top face (perspective) - More visible
            val topDepth3 = gift3Width * 0.35f
            drawPath(
                path = Path().apply {
                    moveTo(gift3X, gift3Top)
                    lineTo(gift3X + topDepth3 * 0.7f, gift3Top - topDepth3 * 0.5f)
                    lineTo(gift3X + gift3Width + topDepth3 * 0.7f, gift3Top - topDepth3 * 0.5f)
                    lineTo(gift3X + gift3Width, gift3Top)
                    close()
                },
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2196F3),
                        Color(0xFF42A5F5),
                        Color(0xFF2196F3)
                    ),
                    start = Offset(gift3X, gift3Top - topDepth3 * 0.5f),
                    end = Offset(gift3X + gift3Width, gift3Top - topDepth3 * 0.5f)
                )
            )
            
            // 3D Box - Right side face (darker)
            drawPath(
                path = Path().apply {
                    moveTo(gift3X + gift3Width, gift3Top)
                    lineTo(gift3X + gift3Width + topDepth3 * 0.7f, gift3Top - topDepth3 * 0.5f)
                    lineTo(gift3X + gift3Width + topDepth3 * 0.7f, gift3Bottom - topDepth3 * 0.5f)
                    lineTo(gift3X + gift3Width, gift3Bottom)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF01579B),
                        Color(0xFF0D47A1),
                        Color(0xFF01579B)
                    )
                )
            )

            // Vertical ribbon (white) with shading
            val ribbonWidth3 = gift3Width * 0.18f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFBDBDBD),
                        Color(0xFFE0E0E0),
                        Color(0xFFFFFFFF),
                        Color(0xFFE0E0E0),
                        Color(0xFFBDBDBD)
                    )
                ),
                topLeft = Offset(gift3X + gift3Width / 2f - ribbonWidth3 / 2f, gift3Top),
                size = Size(ribbonWidth3, gift3Height)
            )
            // Ribbon on top face
            drawPath(
                path = Path().apply {
                    val ribbonX = gift3X + gift3Width / 2f - ribbonWidth3 / 2f
                    moveTo(ribbonX, gift3Top)
                    lineTo(ribbonX + topDepth3 * 0.6f, gift3Top - topDepth3 * 0.4f)
                    lineTo(ribbonX + ribbonWidth3 + topDepth3 * 0.6f, gift3Top - topDepth3 * 0.4f)
                    lineTo(ribbonX + ribbonWidth3, gift3Top)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            // Ribbon on right face
            drawPath(
                path = Path().apply {
                    val ribbonX = gift3X + gift3Width / 2f - ribbonWidth3 / 2f
                    moveTo(ribbonX + ribbonWidth3, gift3Top)
                    lineTo(ribbonX + ribbonWidth3 + topDepth3 * 0.6f, gift3Top - topDepth3 * 0.4f)
                    lineTo(ribbonX + ribbonWidth3 + topDepth3 * 0.6f, gift3Bottom - topDepth3 * 0.4f)
                    lineTo(ribbonX + ribbonWidth3, gift3Bottom)
                    close()
                },
                color = Color(0xFFBDBDBD)
            )

            // Horizontal ribbon on front face
            drawRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(gift3X, gift3Top + gift3Height / 2f - ribbonWidth3 / 2f),
                size = Size(gift3Width, ribbonWidth3)
            )
            // Horizontal ribbon on top face
            drawPath(
                path = Path().apply {
                    moveTo(gift3X, gift3Top)
                    lineTo(gift3X + topDepth3 * 0.6f, gift3Top - topDepth3 * 0.4f)
                    lineTo(gift3X + gift3Width + topDepth3 * 0.6f, gift3Top - topDepth3 * 0.4f)
                    lineTo(gift3X + gift3Width, gift3Top)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            // Horizontal ribbon on right face
            drawPath(
                path = Path().apply {
                    val ribbonY = gift3Top + gift3Height / 2f - ribbonWidth3 / 2f
                    moveTo(gift3X + gift3Width, ribbonY)
                    lineTo(gift3X + gift3Width + topDepth3 * 0.6f, ribbonY - topDepth3 * 0.4f)
                    lineTo(gift3X + gift3Width + topDepth3 * 0.6f, ribbonY + ribbonWidth3 - topDepth3 * 0.4f)
                    lineTo(gift3X + gift3Width, ribbonY + ribbonWidth3)
                    close()
                },
                color = Color(0xFFBDBDBD)
            )

            // 3D Bow on top
            val bow3Y = gift3Top - gift3Height * 0.05f
            val bow3Size = gift3Width * 0.45f
            val bow3CenterX = gift3X + gift3Width / 2f
            // Ribbon tails
            drawPath(
                path = Path().apply {
                    moveTo(bow3CenterX - bow3Size * 0.08f, bow3Y + bow3Size * 0.15f)
                    lineTo(bow3CenterX - bow3Size * 0.15f, bow3Y + bow3Size * 0.35f)
                    lineTo(bow3CenterX - bow3Size * 0.05f, bow3Y + bow3Size * 0.32f)
                    lineTo(bow3CenterX, bow3Y + bow3Size * 0.15f)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            drawPath(
                path = Path().apply {
                    moveTo(bow3CenterX + bow3Size * 0.08f, bow3Y + bow3Size * 0.15f)
                    lineTo(bow3CenterX + bow3Size * 0.15f, bow3Y + bow3Size * 0.35f)
                    lineTo(bow3CenterX + bow3Size * 0.05f, bow3Y + bow3Size * 0.32f)
                    lineTo(bow3CenterX, bow3Y + bow3Size * 0.15f)
                    close()
                },
                color = Color(0xFFF5F5F5)
            )
            // Left loop
            drawPath(
                path = Path().apply {
                    moveTo(bow3CenterX - bow3Size * 0.15f, bow3Y)
                    quadraticBezierTo(
                        bow3CenterX - bow3Size * 0.5f, bow3Y - bow3Size * 0.3f,
                        bow3CenterX - bow3Size * 0.42f, bow3Y + bow3Size * 0.05f
                    )
                    quadraticBezierTo(
                        bow3CenterX - bow3Size * 0.35f, bow3Y + bow3Size * 0.15f,
                        bow3CenterX - bow3Size * 0.15f, bow3Y + bow3Size * 0.08f
                    )
                    close()
                },
                color = Color(0xFFFFFFFF)
            )
            // Right loop
            drawPath(
                path = Path().apply {
                    moveTo(bow3CenterX + bow3Size * 0.15f, bow3Y)
                    quadraticBezierTo(
                        bow3CenterX + bow3Size * 0.5f, bow3Y - bow3Size * 0.3f,
                        bow3CenterX + bow3Size * 0.42f, bow3Y + bow3Size * 0.05f
                    )
                    quadraticBezierTo(
                        bow3CenterX + bow3Size * 0.35f, bow3Y + bow3Size * 0.15f,
                        bow3CenterX + bow3Size * 0.15f, bow3Y + bow3Size * 0.08f
                    )
                    close()
                },
                color = Color(0xFFFFFFFF)
            )
            // Center knot
            drawCircle(
                color = Color(0xFFE0E0E0),
                radius = bow3Size * 0.12f,
                center = Offset(bow3CenterX, bow3Y + bow3Size * 0.04f)
            )
        }

        // --- 10. Snowfall (Day 16: Animated falling snowflakes) ---
        run {
            val baseSize = canvasWidth * 0.025f
            val skyHeight = canvasHeight * 0.78f // Keep snowflakes above ground/tree area

            // Generate deterministic pseudo-random snowflakes with animation
            val snowflakeCount = 45

            for (i in 0 until snowflakeCount) {
                // Deterministic pseudo-random values using index
                val xRandom = ((i * 37 + 13) % 100) / 100f
                val yRandom = ((i * 53 + 29) % 100) / 100f
                val sizeRandom = ((i * 17 + 7) % 100) / 100f
                val rotRandom = ((i * 41 + 19) % 360)
                val alphaRandom = ((i * 31 + 11) % 100) / 100f
                val branchRandom = ((i * 23 + 5) % 3)
                val complexityRandom = ((i * 19 + 3) % 3)

                // Cloud positions (matching the clouds defined earlier)
                val cloudPositions = listOf(
                    Pair(0.15f, 0.12f),
                    Pair(0.45f, 0.08f),
                    Pair(0.70f, 0.15f),
                    Pair(0.25f, 0.25f),
                    Pair(0.85f, 0.22f),
                    Pair(0.55f, 0.30f)
                )
                
                // Assign each snowflake to a cloud
                val cloudIndex = i % cloudPositions.size
                val (cloudXRatio, cloudYRatio) = cloudPositions[cloudIndex]
                
                // Cloud drift animation (matching cloud movement)
                val cloudSpeed = when (cloudIndex) {
                    0 -> 0.3f
                    1 -> 0.5f
                    2 -> 0.25f
                    3 -> 0.4f
                    4 -> 0.35f
                    else -> 0.45f
                }
                val cloudOffset = when (cloudIndex) {
                    0 -> 0.0f
                    1 -> 0.3f
                    2 -> 0.6f
                    3 -> 0.8f
                    4 -> 0.4f
                    else -> 0.2f
                }
                
                val driftOffset = ((skyTime.value * cloudSpeed + cloudOffset) % 1f) * canvasWidth * 0.15f
                
                // Base position - starts from cloud with some spread
                val cloudCenterX = canvasWidth * cloudXRatio + driftOffset
                val cloudCenterY = canvasHeight * cloudYRatio
                val baseRadius = canvasWidth * 0.06f
                
                // Spread snowflakes around the cloud area
                val spreadX = baseRadius * 2.5f * (xRandom - 0.5f)
                val baseX = cloudCenterX + spreadX

                // Falling animation - each snowflake has different fall speed and pattern
                // Fall speed varies per snowflake (0.3 to 1.0 relative speed)
                val fallSpeed = 0.3f + 0.7f * sizeRandom
                // Use skyTime for continuous falling motion
                val fallProgress = (skyTime.value + yRandom) % 1f
                
                // Vertical position - starts from cloud bottom and falls to ground
                val startY = cloudCenterY + baseRadius * 0.5f
                val endY = canvasHeight * 0.85f
                val animatedY = startY + (endY - startY) * fallProgress
                
                // Horizontal drift - gentle side-to-side motion as it falls
                val driftAmount = canvasWidth * 0.06f * (xRandom - 0.5f)
                val driftPhase = (twinkleTime.value * (0.4f + 0.3f * xRandom) + i * 0.1f) % 1f
                val drift = driftAmount * sin(2.0 * PI * driftPhase).toFloat()
                val animatedX = baseX + drift
                
                // Gentle rotation as it falls
                val rotationSpeed = 30f * (xRandom - 0.5f) // -15 to +15 degrees per cycle
                val animatedRotation = rotRandom + rotationSpeed * twinkleTime.value

                // Size variation (smaller to larger)
                val size = baseSize * (0.6f + 1.0f * sizeRandom)

                // Vary branches (6, 8, or 12)
                val branches = when (branchRandom) {
                    0 -> 6
                    1 -> 8
                    else -> 12
                }

                // Complexity (1-3)
                val complexity = 1 + complexityRandom

                // Alpha variation for depth - fade in/out at edges
                val baseAlpha = 0.50f + 0.35f * alphaRandom
                val fadeIn = (fallProgress * 5f).coerceAtMost(1f)
                val fadeOut = ((1f - fallProgress) * 5f).coerceAtMost(1f)
                val alpha = baseAlpha * fadeIn * fadeOut

                // Color variation (white with slight blue tints)
                val colorVariant = when {
                    i % 5 == 0 -> Color(0xFFE3F2FD)
                    i % 7 == 0 -> Color(0xFFF0F8FF)
                    else -> Color.White
                }

                // Only draw if within visible bounds
                if (animatedY > -size * 2 && animatedY < canvasHeight + size * 2) {
                    drawSnowflake(
                        center = Offset(animatedX, animatedY),
                        size = size,
                        branches = branches,
                        complexity = complexity,
                        color = colorVariant,
                        alpha = alpha,
                        rotation = animatedRotation
                    )
                }
            }
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