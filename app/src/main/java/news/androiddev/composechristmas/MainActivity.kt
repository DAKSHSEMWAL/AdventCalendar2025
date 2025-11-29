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
import androidx.compose.ui.graphics.Color
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

@Composable
fun ChristmasScene(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // You can access the DrawScope here to draw shapes.
        // The coordinate system's origin (0,0) is at the top-left corner. [2]

        // Example: Draw a green line
        val canvasWidth = size.width
        val canvasHeight = size.height

        // A simple example of drawing a line
        drawLine(
            start = Offset(x = canvasWidth / 2, y = canvasHeight / 4),
            end = Offset(x = canvasWidth / 2, y = canvasHeight * 3 / 4),
            color = Color.Green,
            strokeWidth = 20f
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeChristmasTheme {
        ChristmasScene()
    }
}