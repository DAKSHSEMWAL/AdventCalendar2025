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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import news.androiddev.composechristmas.ui.theme.ComposeChristmasTheme

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

@Composable
fun ChristmasScene(modifier: Modifier = Modifier, skyTheme: SkyTheme = SkyTheme.NightSky) {
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

        // --- 5. Star Topper (draw behind trunk and foliage) ---
        val centerX = canvasWidth / 2f
        run {
            val starCenter = Offset(centerX, treeTop - layerHeight * 0.25f)
            val outerRadius = treeWidth * 0.10f
            val innerRadius = outerRadius * 0.45f

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

            // Soft glow around the star
            drawCircle(
                color = Color(0xFFFFF8E1).copy(alpha = 0.35f),
                radius = outerRadius * 2.6f,
                center = starCenter
            )
            drawCircle(
                color = Color(0xFFFFFDE7).copy(alpha = 0.22f),
                radius = outerRadius * 1.6f,
                center = starCenter
            )

            // Draw the star with the gradient
            drawPath(path = starPath, brush = starGradient)
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

        drawRect(
            color = Color(0xFF5D4037),
            topLeft = Offset(trunkLeft, trunkTop),
            size = Size(trunkWidth, trunkBottom - trunkTop)
        )

        // --- 7. Tree Foliage Layers ---
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
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeChristmasTheme {
        ChristmasScene(skyTheme = SkyTheme.NightSky)
    }
}