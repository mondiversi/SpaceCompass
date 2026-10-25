package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** One wall-clock ticker per visible host, including a frozen scenario. Never
 * start one timer per table row or keep it running while another app is open. */
@Composable
internal fun rememberSpaceCompassPointReminderNow(active: Boolean = true): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) resumed = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return produceState(System.currentTimeMillis(), resumed, active) {
        if (resumed && active) while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }.value
}

// Reserve only the glyph and 6 dp on either side in tables; floating reminders keep their own size.
internal val SpaceCompassTableReminderWidth = 32.dp

@Composable
internal fun SpaceCompassPointReminderButton(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
    viewedNowMs: Long, realNowMs: Long, isCurrent: Boolean, pointName: String,
    tint: Color, modifier: Modifier = Modifier, tag: String = "point-reminder", badgeBackground: Color? = null) {
    val context = LocalContext.current
    val request = spaceCompassPointReminder(point.timeMs, viewedNowMs, realNowMs, isCurrent)
    val description = stringResource(when (request?.kind) {
        SpaceCompassPointReminderKind.ALARM -> R.string.point_reminder_alarm
        SpaceCompassPointReminderKind.CALENDAR -> R.string.point_reminder_calendar
        null -> R.string.point_reminder_unavailable
    })
    val unavailableMessage = stringResource(R.string.point_reminder_unavailable)
    val bodyName = stringResource(body.nameResource)
    val title = listOf(bodyName, pointName).filter { it.isNotBlank() }.joinToString(" · ")
    val onClick: () -> Unit = {
        // Revalidate after a pause, an external time-zone change or an expired point.
        val fresh = spaceCompassPointReminder(point.timeMs, viewedNowMs, System.currentTimeMillis(), isCurrent)
        if (fresh == null) showSpaceCompassBottomMessage(context, unavailableMessage)
        else openSpaceCompassPointReminder(context, fresh, title)
    }
    val buttonModifier = modifier
        .then(if (request == null) Modifier.spaceCompassSceneInputShield() else Modifier).testTag(tag)
    if (badgeBackground != null) {
        SpaceCompassFloatingActionButton(description, onClick, tint, badgeBackground,
            buttonModifier, enabled = request != null, backgroundAlpha = SPACE_COMPASS_POINT_CAPTION_ALPHA) {
            SpaceCompassPointReminderGlyph(tint, request != null, Modifier.size(28.dp),
                iconStrokeWidth = SpaceCompassFloatingIconStrokeWidth)
        }
    } else {
        // Compact only the table slot width; retain the uniform glyph, 48 dp height and standard touch expansion.
        IconButton(onClick = onClick, enabled = request != null,
            modifier = buttonModifier.size(width = SpaceCompassTableReminderWidth, height = 48.dp)
                .semantics { contentDescription = description }) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpaceCompassPointReminderGlyph(tint, request != null, Modifier.size(20.dp),
                    iconStrokeWidth = 1.4.dp)
            }
        }
    }
}

@Composable
internal fun SpaceCompassPointReminderGlyph(tint: Color, enabled: Boolean, modifier: Modifier,
    iconStrokeWidth: Dp = 2.dp) {
    val tint = if (enabled) tint else tint.copy(alpha = .35f)
    SpaceCompassIconCanvas(tint, modifier) { color ->
        val stroke = Stroke(iconStrokeWidth.toPx(), cap = StrokeCap.Round)
        val bell = Path().apply {
            moveTo(size.width*.18f, size.height*.73f)
            cubicTo(size.width*.30f, size.height*.60f, size.width*.27f, size.height*.48f, size.width*.28f, size.height*.39f)
            cubicTo(size.width*.28f, size.height*.11f, size.width*.72f, size.height*.11f, size.width*.72f, size.height*.39f)
            cubicTo(size.width*.73f, size.height*.48f, size.width*.70f, size.height*.60f, size.width*.82f, size.height*.73f)
            close()
        }
        drawPath(bell, color, style = stroke)
        drawLine(color, Offset(size.width*.5f, size.height*.10f), Offset(size.width*.5f, size.height*.18f), iconStrokeWidth.toPx(), StrokeCap.Round)
        drawArc(color, 0f, 180f, false, topLeft = Offset(size.width*.4f, size.height*.72f),
            size = androidx.compose.ui.geometry.Size(size.width*.2f, size.height*.17f), style = stroke)
    }
}
