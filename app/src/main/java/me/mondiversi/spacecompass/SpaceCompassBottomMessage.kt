package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

internal const val SPACE_COMPASS_BOTTOM_MESSAGE_BACKGROUND: Long = 0xEE2A2A2E
private var activeSpaceCompassBottomMessage: Toast? = null

/** Uvir's always-dark, nonblocking message, including tablet scale and longer translations. */
@Suppress("DEPRECATION")
internal fun showSpaceCompassBottomMessage(context: Context, text: CharSequence,
    longDuration: Boolean = false): Toast {
    activeSpaceCompassBottomMessage?.cancel()
    val application = context.applicationContext
    val configuration = application.resources.configuration
    val shortestWidth = configuration.smallestScreenWidthDp.takeIf { it > 0 }
        ?: minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val scale = spaceCompassAutomaticScreenScale(shortestWidth)
    val density = application.resources.displayMetrics.density * scale
    val horizontalPadding = (20f * density).roundToInt()
    val verticalPadding = (13f * density).roundToInt()
    val sideMargin = (24f * density).roundToInt()
    val message = TextView(application).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 14f * scale
        gravity = Gravity.CENTER_VERTICAL
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        maxLines = 6
        setLineSpacing(0f, 1.08f)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        maxWidth = (application.resources.displayMetrics.widthPixels - sideMargin * 2).coerceAtLeast(1)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(SPACE_COMPASS_BOTTOM_MESSAGE_BACKGROUND.toInt())
            cornerRadius = 14f * density
        }
    }
    return Toast(application).apply {
        duration = if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (88f * density).roundToInt())
        view = message
        activeSpaceCompassBottomMessage = this
        show()
    }
}

/** Keep the gallery status inside its preview, with the same Uvir message colors and insets. */
@Composable
internal fun SpaceCompassBottomSnackbar(data: SnackbarData) {
    Surface(shape = RoundedCornerShape(14.dp),
        color = androidx.compose.ui.graphics.Color(SPACE_COMPASS_BOTTOM_MESSAGE_BACKGROUND),
        contentColor = androidx.compose.ui.graphics.Color.White) {
        Text(data.visuals.message, modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
            fontSize = 14.sp, lineHeight = 15.12.sp, maxLines = 6)
    }
}
