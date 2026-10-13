package dev.lantern.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF121214)
private val Cream = Color(0xFFF5F1E8)
private val Orange = Color(0xFFFF5A1F)

/** The loop of the icon in a 364-unit box (the icon's own 512 units, cropped to its stroke), as eight curves. */
private val LoopCurves = listOf(
    floatArrayOf(256f, 190f, 210f, 150f, 166f, 150f), floatArrayOf(122f, 150f, 96f, 196f, 96f, 256f),
    floatArrayOf(96f, 316f, 122f, 362f, 166f, 362f), floatArrayOf(210f, 362f, 256f, 322f, 256f, 256f),
    floatArrayOf(256f, 190f, 302f, 150f, 346f, 150f), floatArrayOf(390f, 150f, 416f, 196f, 416f, 256f),
    floatArrayOf(416f, 316f, 390f, 362f, 346f, 362f), floatArrayOf(302f, 362f, 256f, 322f, 256f, 256f),
)

/** The opening screen: the loop draws itself, the semi-square settles in it, then "by Zimam" fades in. */
@Composable
fun Splash(onDone: () -> Unit) {
    val draw = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val byline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { draw.animateTo(1f, tween(700, easing = LinearEasing)) }
        launch { delay(560); pop.animateTo(1f, tween(300)) }
        launch { delay(760); byline.animateTo(1f, tween(380)) }
        delay(1400)
        onDone()
    }
    Column(Modifier.fillMaxSize().background(Ink), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(150.dp)) {
            val k = size.width / 364f
            fun x(v: Float) = (v - 74f) * k
            val loop = Path().apply {
                moveTo(x(256f), x(256f))
                LoopCurves.forEach { c -> cubicTo(x(c[0]), x(c[1]), x(c[2]), x(c[3]), x(c[4]), x(c[5])) }
                close()
            }
            val measure = PathMeasure().apply { setPath(loop, false) }
            val part = Path()
            measure.getSegment(0f, measure.length * draw.value, part, true)
            drawPath(part, Cream, style = Stroke(width = 44f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val s = 62f * k * pop.value
            drawRoundRect(Orange, Offset(x(346f) - s / 2, x(256f) - s / 2), Size(s, s), CornerRadius(16f * k * pop.value))
        }
        Text(Brand.NAME, Modifier.alpha(byline.value), color = Cream, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("by ${Brand.MAKER}", Modifier.alpha(byline.value), color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}
