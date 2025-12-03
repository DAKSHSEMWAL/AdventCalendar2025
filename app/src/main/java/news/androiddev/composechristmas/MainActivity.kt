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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
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

        // Background gradient sky
        val skyBrush = when (skyTheme) {
            SkyTheme.NightSky -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B1026), // deep navy
                    Color(0xFF152044),
                    Color(0xFF1F2D5E)
                ),
                startY = 0f,
                endY = canvasHeight
            )
            SkyTheme.WinterMorning -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE3F2FD), // pale blue
                    Color(0xFFBBDEFB),
                    Color(0xFF90CAF9)
                ),
                startY = 0f,
                endY = canvasHeight
            )
        }
        drawRect(brush = skyBrush, topLeft = Offset.Zero, size = size)

        // Stars (visible for both, brightest on Night Sky)
        val starColor = if (skyTheme == SkyTheme.NightSky) Color(0xFFFFF9C4) else Color(0xFFFFFDE7)
        val starGlow = if (skyTheme == SkyTheme.NightSky) 1f else 0.8f
        val starRadius = if (skyTheme == SkyTheme.NightSky) 5f else 4f
        // Deterministic positions proportionally distributed near upper half
        val starPositions = listOf(
            Offset(canvasWidth * 0.18f, canvasHeight * 0.12f),
            Offset(canvasWidth * 0.32f, canvasHeight * 0.08f),
            Offset(canvasWidth * 0.68f, canvasHeight * 0.10f),
            Offset(canvasWidth * 0.78f, canvasHeight * 0.16f),
            Offset(canvasWidth * 0.54f, canvasHeight * 0.06f),
            Offset(canvasWidth * 0.42f, canvasHeight * 0.18f),
            Offset(canvasWidth * 0.86f, canvasHeight * 0.26f)
        )
        for (p in starPositions) {
            // glow halo
            drawCircle(
                color = starColor.copy(alpha = 0.25f * starGlow),
                radius = starRadius * 3.5f,
                center = p
            )
            // core
            drawCircle(
                color = starColor,
                radius = starRadius,
                center = p
            )
        }

        // Tree overall geometry
        val numLayers = 10
        val treeWidth = canvasWidth * 0.42f
        val treeHeight = canvasHeight * 0.62f
        val treeTop = canvasHeight * 0.16f
        val layerHeight = treeHeight / (numLayers + 1f)
        val layerOverlap = layerHeight * 0.35f

        // Colors & shading (flatter gradient to match stylized mockup)
        val deepGreen = Color(0xFF2E7D32)
        val midGreen = Color(0xFF2F7E33)
        val tipGreen = Color(0xFF6CCB6B)
        val foliageBrush = Brush.verticalGradient(
            colors = listOf(deepGreen, midGreen),
            startY = treeTop,
            endY = treeTop + treeHeight
        )

        // Draw curved, overlapping foliage layers
        for (layerIndex in 0 until numLayers) {
            val progressFromTop = layerIndex / (numLayers - 1f)
            val layerTop = treeTop + layerIndex * (layerHeight - layerOverlap)
            val layerBottom = layerTop + layerHeight

            // Width taper: wider at TOP and taper towards bottom (reverse)
            val baseWidth = treeWidth * (0.25f + 0.75f * (progressFromTop))
            val wobble = (if (layerIndex % 2 == 0) 1f else -1f) * baseWidth * 0.06f
            val layerWidth = baseWidth + wobble
            val layerLeft = (canvasWidth - layerWidth) / 2f
            val layerRight = layerLeft + layerWidth

            val centerX = canvasWidth / 2f
            val controlYOffset = layerHeight * 0.35f

            // Symmetric scalloped cap per layer
            val path = Path().apply {
                moveTo(centerX, layerTop)
                // Right scallop
                quadraticBezierTo(
                    centerX + layerWidth * 0.55f,
                    layerTop + controlYOffset * 0.9f,
                    layerRight,
                    layerBottom
                )
                // Bottom slight inward curve
                quadraticBezierTo(
                    centerX,
                    layerBottom - layerHeight * 0.15f,
                    layerLeft,
                    layerBottom
                )
                // Left scallop
                quadraticBezierTo(
                    centerX - layerWidth * 0.55f,
                    layerTop + controlYOffset * 0.9f,
                    centerX,
                    layerTop
                )
                close()
            }
            drawPath(path = path, brush = foliageBrush)

            // Side tips removed per request
        }

        // Top sprig removed per request
        val midX = canvasWidth / 2f

        // Trunk
        val trunkWidth = treeWidth * 0.14f
        val trunkHeight = treeHeight * 0.15f
        val trunkLeft = midX - trunkWidth / 2f
        val trunkTop = treeTop + (numLayers - 1) * (layerHeight - layerOverlap) + layerHeight
        drawRect(
            color = Color(0xFF5D4037),
            topLeft = Offset(trunkLeft, trunkTop),
            size = Size(trunkWidth, trunkHeight)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeChristmasTheme {
        ChristmasScene(skyTheme = SkyTheme.NightSky)
    }
}