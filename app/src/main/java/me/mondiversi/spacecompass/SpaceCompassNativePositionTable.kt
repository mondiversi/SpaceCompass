package me.mondiversi.spacecompass

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/** Fixed label/value columns, natural value wrapping and a subordinate GPS accuracy row. */
internal class SpaceCompassNativePositionTable(context: Context, private val density: Float,
    private val fontScale: Float, private val foregroundArgb: Int, private val accent: Int,
    private val dark: Boolean, scrollbarArgb: Int) : SpaceCompassNativeScrollView(context, density, scrollbarArgb) {
    private var current: SpaceCompassPositionDetailsData? = null
    private fun dp(value: Float) = (value * density).roundToInt()
    private fun label(value: String, secondary: Boolean = false, small: Boolean = false) = TextView(context).apply {
        text = value
        setTextColor(if (secondary) android.graphics.Color.argb(185, android.graphics.Color.red(foregroundArgb),
            android.graphics.Color.green(foregroundArgb), android.graphics.Color.blue(foregroundArgb)) else foregroundArgb)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, (if (small) 11f else 13f) * density * fontScale)
        setLineSpacing(dp(3f).toFloat(), 1f)
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
    }
    // The split viewport owns the shared outer insets; this table retains its card padding.
    init { tag = "position-details-table"; isFillViewport = true; clipToPadding = false }
    fun update(data: SpaceCompassPositionDetailsData) {
        if (current == data) return
        val sameStructure = current?.let { previous ->
            previous.rows.map { it.tag to it.label } == data.rows.map { it.tag to it.label } &&
                previous.notes == data.notes && previous.weatherAttribution == data.weatherAttribution
        } == true
        if (sameStructure) {
            val accuracy = data.rows.firstOrNull { it.tag == "sun-info-accuracy" }
            data.rows.filter { it.tag != "sun-info-accuracy" }.forEach { row ->
                findViewWithTag<TextView>(row.tag + "-value")?.text = row.value
                findViewWithTag<View>(row.tag)?.contentDescription = row.announcement +
                    if (row.tag == "sun-info-coordinates" && accuracy != null) ", ${accuracy.announcement}" else ""
            }
            accuracy?.let { findViewWithTag<TextView>("sun-info-accuracy-value")?.text = "(${it.value})" }
            current = data
            return
        }
        current = data
        val oldScroll = scrollY
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), dp(4f), dp(12f), dp(4f))
            background = GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat()
                setColor(if (dark) 0xff282d33.toInt() else 0xffe6e9eb.toInt())
            }
        }
        val accuracy = data.rows.firstOrNull { it.tag == "sun-info-accuracy" }
        data.rows.filter { it.tag != "sun-info-accuracy" }.forEachIndexed { index, row ->
            if (index > 0) card.addView(View(context).apply {
                setBackgroundColor(if (dark) 0x22ffffff else 0x22101418)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f)))
            val line = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; tag = row.tag
                minimumHeight = dp(42f); setPadding(0, dp(9f), 0, dp(9f))
                contentDescription = row.announcement + if (row.tag == "sun-info-coordinates" && accuracy != null)
                    ", ${accuracy.announcement}" else ""
            }
            val heading = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            heading.addView(label(row.label, secondary = true))
            if (row.tag == "sun-info-coordinates" && accuracy != null) {
                heading.addView(label("(${accuracy.value})", secondary = true, small = true).apply { tag = "sun-info-accuracy-value" })
            }
            line.addView(heading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, .43f)
                .apply { marginEnd = dp(12f) })
            line.addView(label(row.value).apply {
                gravity = Gravity.END; fontFeatureSettings = "tnum"; tag = row.tag + "-value"
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, .57f))
            card.addView(line)
        }
        column.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(spaceCompassDetailScrollInset.value)
            marginEnd = dp(spaceCompassDetailScrollInset.value)
        })
        data.notes.forEach { note -> column.addView(label(note, secondary = true, small = true).apply {
            setPadding(dp(spaceCompassDetailScrollInset.value), dp(12f), dp(spaceCompassDetailScrollInset.value), 0)
        }) }
        if (data.weatherAttribution) column.addView(label("Open-Meteo · CC BY 4.0", small = true).apply {
            setPadding(dp(spaceCompassDetailScrollInset.value), 0, dp(spaceCompassDetailScrollInset.value), 0)
            setTextColor(accent); minimumHeight = dp(48f); gravity = Gravity.CENTER_VERTICAL
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener { runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/en/licence")))
            }.onFailure { SpaceCompassErrorLog.record(context, "position:weather_attribution", it) } }
        })
        removeAllViews(); addView(column)
        post { scrollTo(0, oldScroll) }
    }
}
