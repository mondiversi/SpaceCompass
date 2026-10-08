package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

internal const val SPACE_COMPASS_BOTTOM_MESSAGE_BACKGROUND: Long = 0xEE2A2A2E
private var activeSpaceCompassBottomMessage: Toast? = null

/** Uvir's always-dark, nonblocking message, including tablet scale and longer translations. */
@Suppress("DEPRECATION")
internal fun showSpaceCompassBottomMessage(context: Context, text: CharSequence): Toast {
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
        duration = Toast.LENGTH_SHORT
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (88f * density).roundToInt())
        view = message
        activeSpaceCompassBottomMessage = this
        show()
    }
}
