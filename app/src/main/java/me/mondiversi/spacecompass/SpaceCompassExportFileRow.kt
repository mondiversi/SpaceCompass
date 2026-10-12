package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact file choices: one accessible toggle, leading checkbox, glyph and label. */
@Composable
internal fun SpaceCompassExportFileRow(
    type: SpaceCompassCaptureExportType,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val tint = foreground.copy(alpha = if (enabled) 1f else .38f)
    Surface(
        modifier.fillMaxWidth().toggleable(checked, enabled = enabled,
            role = Role.Checkbox, onValueChange = onCheckedChange),
        shape = RoundedCornerShape(10.dp),
        color = if (checked) foreground.copy(alpha = .08f) else Color.Transparent,
        contentColor = tint
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Checkbox(checked, onCheckedChange = null, enabled = enabled,
                    modifier = Modifier.size(24.dp),
                    colors = spaceCompassCheckboxColors(uncheckedColor = foreground.copy(alpha = .72f)))
            }
            Spacer(Modifier.width(8.dp))
            SpaceCompassExportFileIcon(type)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(type.labelResource), Modifier.weight(1f),
                color = tint, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SpaceCompassExportFileIcon(type: SpaceCompassCaptureExportType) {
    val tint = LocalContentColor.current
    Box(Modifier.size(22.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        when (type) {
            SpaceCompassCaptureExportType.IMAGE,
            SpaceCompassCaptureExportType.CURRENT_IMAGE,
            SpaceCompassCaptureExportType.ROTATION_IMAGE -> SpaceCompassPanoramaGalleryIcon()
            SpaceCompassCaptureExportType.CSV,
            SpaceCompassCaptureExportType.TXT -> {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 1.8.dp.toPx()
                    fun line(ax: Float, ay: Float, bx: Float, by: Float) = drawLine(tint,
                        Offset(size.width * ax, size.height * ay),
                        Offset(size.width * bx, size.height * by), stroke, StrokeCap.Round)
                    if (type == SpaceCompassCaptureExportType.CSV) {
                        val document = Path().apply {
                            moveTo(size.width * .22f, size.height * .08f)
                            lineTo(size.width * .62f, size.height * .08f)
                            lineTo(size.width * .80f, size.height * .27f)
                            lineTo(size.width * .80f, size.height * .92f)
                            lineTo(size.width * .22f, size.height * .92f)
                            close()
                        }
                        drawPath(document, tint, style = Stroke(stroke, join = StrokeJoin.Round))
                        line(.62f, .08f, .62f, .27f)
                        line(.62f, .27f, .80f, .27f)
                    } else {
                        drawRoundRect(tint, Offset(size.width * .08f, size.height * .16f),
                            Size(size.width * .84f, size.height * .68f),
                            CornerRadius(stroke), style = Stroke(stroke))
                        drawRect(tint.copy(alpha = tint.alpha * .18f),
                            Offset(size.width * .08f, size.height * .16f),
                            Size(size.width * .84f, size.height * .20f))
                        line(.08f, .36f, .92f, .36f)
                        line(.08f, .60f, .92f, .60f)
                        line(.44f, .16f, .44f, .84f)
                    }
                }
                if (type == SpaceCompassCaptureExportType.CSV) {
                    Text("CSV", Modifier.padding(top = 5.dp), color = tint,
                        fontSize = 6.5.sp, lineHeight = 8.sp,
                        fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }
    }
}
