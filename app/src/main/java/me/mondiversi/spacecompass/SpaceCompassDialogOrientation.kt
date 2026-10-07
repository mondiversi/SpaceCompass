package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/** A portrait tablet may keep its Android window while the panorama uses a landscape viewport. */
@Composable
internal fun SpaceCompassDialogOrientation(rotated: Boolean, content: @Composable () -> Unit) {
    if (!rotated) {
        content()
        return
    }
    Layout(content = content) { children, constraints ->
        val child = children.single().measure(Constraints(
            maxWidth = constraints.maxHeight, maxHeight = constraints.maxWidth))
        val width = child.height.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = child.width.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            child.placeWithLayer((width - child.width) / 2, (height - child.height) / 2) { rotationZ = 90f }
        }
    }
}
