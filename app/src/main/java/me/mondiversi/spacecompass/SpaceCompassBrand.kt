package me.mondiversi.spacecompass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun SpaceCompassLaunchGate(
    showLaunchScreen: Boolean = true,
    content: @Composable () -> Unit
) {
    var splashVisible by remember {
        mutableStateOf(showLaunchScreen)
    }

    LaunchedEffect(showLaunchScreen) {
        if (showLaunchScreen) {
            delay(1_050L)
            splashVisible = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!splashVisible) content()

        AnimatedVisibility(
            visible = splashVisible,
            exit = fadeOut(tween(320))
        ) {
            SpaceCompassLaunchScreen()
        }
    }
}

@Composable
internal fun SpaceCompassLaunchScreen() {
    val darkTheme = isSystemInDarkTheme()
    var measuredLogoCenter by remember {
        mutableStateOf<Offset?>(null)
    }
    val transition =
        rememberInfiniteTransition(
            label = "space_compass_launch"
        )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1_350),
                repeatMode = RepeatMode.Reverse
            ),
        label = "planet_pulse"
    )
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2_200),
                repeatMode = RepeatMode.Restart
            ),
        label = "orbital_wave"
    )

    val spectrum =
        listOf(
            Color(0xFF5260FF),
            Color(0xFF36D5FF),
            Color(0xFF06B6D4),
            Color(0xFFB56DFF),
            Color(0xFFFACC15),
            Color(0xFFFF668D),
            Color(0xFFFFD166)
        )
    val backgroundColors =
        if (darkTheme) {
            listOf(
                Color(0xFF050815),
                Color(0xFF111044),
                Color(0xFF180B35),
                Color(0xFF050711)
            )
        } else {
            listOf(
                Color(0xFFF8F9FF),
                Color(0xFFECEEFF),
                Color(0xFFF5EEFF),
                Color(0xFFF5F7FF)
            )
        }
    val logoContainerColor =
        Color(0xFF161334)
    val titleColor =
        if (darkTheme) Color.White else Color(0xFF17143F)
    val subtitleColor =
        if (darkTheme) Color(0xFFD7D7F8) else Color(0xFF545474)
    val glowPrimaryAlpha = if (darkTheme) 0.30f else 0.18f
    val glowSecondaryAlpha = if (darkTheme) 0.13f else 0.09f
    val ringBaseAlpha = if (darkTheme) 0.14f else 0.20f

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        backgroundColors
                    )
                )
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center =
                measuredLogoCenter
                    ?: Offset(
                        size.width / 2f,
                        size.height * 0.42f
                    )
            val baseRadius = size.minDimension * 0.27f

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color(0xFF6D28D9).copy(alpha = glowPrimaryAlpha),
                                Color(0xFF36D5FF).copy(alpha = glowSecondaryAlpha),
                                Color.Transparent
                            ),
                        center = center,
                        radius = baseRadius * 1.70f * pulse
                    ),
                radius = baseRadius * 1.70f * pulse,
                center = center
            )

            repeat(3) { index ->
                drawCircle(
                    color =
                        spectrum[index]
                            .copy(alpha = ringBaseAlpha - index * 0.025f),
                    radius =
                        baseRadius *
                                (1.02f + index * 0.20f) *
                                pulse,
                    center = center,
                    style = Stroke(
                        width = (1.4f + index * 0.45f).dp.toPx()
                    )
                )
            }

            val orbitRadius = baseRadius * 1.45f
            drawArc(Brush.sweepGradient(spectrum, center), phase * 360f, 250f, false,
                topLeft = center - Offset(orbitRadius, orbitRadius * .48f),
                size = androidx.compose.ui.geometry.Size(orbitRadius * 2, orbitRadius * .96f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round), alpha = .65f)
            repeat(24) { index ->
                val x = size.width * ((index * .6180339f) % 1f)
                val y = size.height * ((index * .381966f + .13f) % 1f)
                drawCircle(titleColor.copy(alpha = .12f + .12f * pulse), (if (index % 5 == 0) 1.5f else .8f).dp.toPx(), Offset(x, y))
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier =
                    Modifier
                        .size(196.dp)
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInRoot()
                            val measuredCenter =
                                Offset(
                                    x = position.x + coordinates.size.width / 2f,
                                    y = position.y + coordinates.size.height / 2f
                                )
                            if (measuredLogoCenter != measuredCenter) {
                                measuredLogoCenter = measuredCenter
                            }
                        }
                        .clip(CircleShape)
                        .background(logoContainerColor)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(spectrum),
                            shape = CircleShape
                        ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_space_compass),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(168.dp)
                            .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = titleColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = stringResource(R.string.pc_loading),
                color = subtitleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 0.7.sp
            )

        }
    }
}
