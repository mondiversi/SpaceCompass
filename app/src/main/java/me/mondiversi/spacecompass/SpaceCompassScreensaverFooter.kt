package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.withSave
import kotlin.math.roundToInt

/** Compact real-world clock and battery, independent of the decorative scene and scenario. */
internal class SpaceCompassScreensaverFooter(context: Context) : LinearLayout(context) {
    private val time = label(Typeface.create("sans-serif-light", Typeface.NORMAL))
    private val date = label(Typeface.create("sans-serif", Typeface.NORMAL))
    private val percent = label(Typeface.create("sans-serif", Typeface.NORMAL))
    private val batteryIcon = ScreensaverBatteryIcon(context)
    private val battery = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(batteryIcon, LayoutParams(dp(22), dp(16)).apply { marginEnd = dp(6) })
        addView(percent)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    private var scale = 1f

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        addView(time)
        addView(date)
        addView(battery)
        // Only the battery row supplies a combined accessible label.
        percent.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun label(typeface: Typeface) = TextView(context).apply {
        setTextColor(Color.WHITE)
        this.typeface = typeface
        gravity = Gravity.CENTER
        setSingleLine()
        ellipsize = TextUtils.TruncateAt.END
        setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Color.BLACK)
    }

    fun resize(width: Int, height: Int) {
        val density = resources.displayMetrics.density
        scale = spaceCompassAutomaticScreenScale((minOf(width, height) / density).roundToInt())
        val horizontal = width > height || height / density < 320
        time.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        for (row in listOf(date, battery)) {
            row.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (dp(4) * scale).roundToInt()
            }
        }
        time.textSize = (if (horizontal) 24f else 36f) * scale
        date.textSize = (if (horizontal) 13f else 14f) * scale
        percent.textSize = 12f * scale
        batteryIcon.layoutParams = LayoutParams((dp(22) * scale).roundToInt(), (dp(16) * scale).roundToInt())
            .apply { marginEnd = (dp(6) * scale).roundToInt() }
    }

    /** Rebuild cached glyphs after the immersive window moves the footer below old system insets. */
    fun refreshAfterResize() {
        time.text = time.text
        time.invalidate()
        date.invalidate()
        battery.invalidate()
        invalidate()
    }

    fun update(info: SpaceCompassScreensaverInfo) {
        layoutDirection = if (info.rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        if (time.text != info.time) time.text = info.time
        if (date.text != info.date) date.text = info.date
        if (percent.text != info.batteryText) percent.text = info.batteryText
        battery.contentDescription = info.batteryDescription
        batteryIcon.update(info.batteryPercent, info.charging)
    }
}

private class ScreensaverBatteryIcon(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bolt = Path()
    private var percent: Int? = null
    private var charging = false

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    fun update(value: Int?, isCharging: Boolean) {
        if (percent != value || charging != isCharging) { percent = value; charging = isCharging; invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.withSave {
            canvas.scale(width / 22f, height / 16f)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(1f, 2f, 18.5f, 14f, 2f, 2f, paint)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(19.5f, 5f, 21f, 11f, .5f, .5f, paint)
            paint.alpha = if (charging) 90 else 220
            val end = 3f + 13.5f * (percent ?: 0) / 100f
            if (end > 3f) canvas.drawRoundRect(3f, 4f, end, 12f, .8f, .8f, paint)
            paint.alpha = 255
            if (charging) {
                bolt.reset()
                bolt.moveTo(11f, 3f); bolt.lineTo(6.5f, 8.5f); bolt.lineTo(9.5f, 8.5f)
                bolt.lineTo(8f, 13f); bolt.lineTo(13f, 7f); bolt.lineTo(10f, 7f); bolt.close()
                canvas.drawPath(bolt, paint)
            }
        }
    }
}
