package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight

@Composable
internal fun spaceCompassDialogContentColor(): Color =
    if (isSystemInDarkTheme()) Color.White else Color(0xFF101418)

@Composable
internal fun SpaceCompassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = spaceCompassSettingsCardColor(),
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = spaceCompassDialogContentColor(),
    textContentColor: Color = spaceCompassDialogContentColor(),
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
    fixedBottomContent: @Composable (() -> Unit)? = null,
    scrollContent: Boolean = true,
    rotateContent: Boolean = false,
    confirmButtonFirst: Boolean = false
) {
    // Choice/information dialogs expose a Close footer in portrait and the header X in landscape.
    val footerConfirmButton: (@Composable () -> Unit)? = confirmButton ?: if (dismissButton == null) {
        { SpaceCompassDialogActionButton(stringResource(R.string.close), onDismissRequest,
            Modifier.testTag("dialog-close"), tone = SpaceCompassDialogActionTone.NEUTRAL, closeAction = true) }
    } else null
    // A Dialog owns another Android Compose view. Restore the host's density
    // AND logical configuration inside that window, before measuring anything.
    val hostDensity = LocalDensity.current
    val originalConfiguration = LocalConfiguration.current
    val hostConfiguration = remember(originalConfiguration, rotateContent) {
        Configuration(originalConfiguration).apply {
            if (rotateContent) {
                screenWidthDp = originalConfiguration.screenHeightDp
                screenHeightDp = originalConfiguration.screenWidthDp
            }
        }
    }
    val landscapeLayout = hostConfiguration.screenWidthDp > hostConfiguration.screenHeightDp
    val maximumDialogHeight = (hostConfiguration.screenHeightDp.dp -
        if (landscapeLayout) 16.dp else 48.dp).coerceAtLeast(1.dp)
    // Use the full available window width on every device, without platform side margins.
    val windowProperties = DialogProperties(
        dismissOnBackPress = properties.dismissOnBackPress,
        dismissOnClickOutside = properties.dismissOnClickOutside,
        securePolicy = properties.securePolicy,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = properties.decorFitsSystemWindows,
        windowTitle = properties.windowTitle
    )
    // All popup bodies share one bounded scroll viewport. Titles and actions
    // stay fixed, and the scrollbar is drawn at the window edge only on overflow.
    val scrollbar = rememberSpaceCompassDialogScrollbar(textContentColor.copy(alpha = 0.58f),
        maximumHeight = maximumDialogHeight)
    val displayedActions: @Composable (Modifier) -> Unit = { actionModifier ->
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.primary,
            LocalTextStyle provides MaterialTheme.typography.labelLarge,
            LocalSpaceCompassDialogHideCloseAction provides landscapeLayout
        ) {
            SpaceCompassDialogActionFlow(actionModifier) {
                if (confirmButtonFirst) footerConfirmButton?.invoke()
                dismissButton?.invoke()
                if (!confirmButtonFirst) footerConfirmButton?.invoke()
            }
        }
    }
    val displayedTitle: @Composable () -> Unit = {
        if (landscapeLayout) {
            SpaceCompassLandscapeDialogTitleContent(title = title ?: {},
                actions = { displayedActions(Modifier) }, onDismiss = onDismissRequest)
        } else {
            SpaceCompassClosableDialogTitleContent(
                onDismiss = onDismissRequest,
                title = title ?: {}
            )
        }
    }
    val displayedText: (@Composable () -> Unit)? = text?.let { body ->
        {
            val scrollableBody: @Composable (Modifier) -> Unit = { viewport ->
                Box(viewport.fillMaxWidth().then(scrollbar.viewportModifier)) {
                    Column(
                        Modifier.fillMaxWidth()
                            .verticalScroll(scrollbar.scrollState)
                            .testTag("spaceCompass_dialog_scroll")
                    ) {
                        body()
                    }
                }
            }
            if (!scrollContent) {
                body()
            } else if (fixedBottomContent == null) {
                scrollableBody(Modifier)
            } else {
                Column(Modifier.fillMaxWidth()) {
                    // Reserve the footer first; only the list consumes the remaining viewport.
                    scrollableBody(Modifier.weight(1f, fill = false))
                    fixedBottomContent()
                }
            }
        }
    }
    Dialog(onDismissRequest = onDismissRequest, properties = windowProperties) {
        CompositionLocalProvider(
            LocalDensity provides hostDensity,
            LocalConfiguration provides hostConfiguration,
            LocalSpaceCompassSettingsActionButtons provides false,
            LocalSpaceCompassTrailingChoiceControl provides true,
            LocalSpaceCompassActionGlyphStrokeScale provides 1f
        ) {
            SpaceCompassDialogSystemBars()
            SpaceCompassDialogOrientation(rotateContent) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .then(if (scrollContent) scrollbar.dialogModifier else Modifier.heightIn(
                            max = maximumDialogHeight)),
                    shape = shape,
                    color = containerColor,
                    contentColor = textContentColor,
                    tonalElevation = tonalElevation
                ) {
                    // Landscape uses the available width and puts actions in the title row.
                    // Portrait keeps its top inset; the action footer uses compact bottom spacing.
                    Column(Modifier.padding(
                        start = if (landscapeLayout) 16.dp else 24.dp,
                        end = if (landscapeLayout) 16.dp else 24.dp,
                        top = if (landscapeLayout) 8.dp else 24.dp,
                        bottom = if (landscapeLayout) 8.dp else 12.dp
                    )) {
                        if (icon != null) {
                            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = if (landscapeLayout) 8.dp else 16.dp)) {
                                CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                                    icon()
                                }
                            }
                        }
                        CompositionLocalProvider(
                            LocalContentColor provides titleContentColor,
                            LocalTextStyle provides MaterialTheme.typography.headlineSmall
                        ) {
                            Box(Modifier.fillMaxWidth().padding(bottom = if (landscapeLayout) 8.dp else 16.dp)) {
                                displayedTitle()
                            }
                        }
                        if (displayedText != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides textContentColor,
                                LocalTextStyle provides MaterialTheme.typography.bodyMedium
                            ) {
                                Box(Modifier.weight(1f, fill = false).padding(
                                    bottom = if (!landscapeLayout && (footerConfirmButton != null || dismissButton != null)) 12.dp else 0.dp
                                )) {
                                    displayedText()
                                }
                            }
                        }
                        if (!landscapeLayout && (footerConfirmButton != null || dismissButton != null)) {
                            displayedActions(Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }
    }
}

/** Measure actions first so long titles wrap while every action remains reachable. */
@Composable
private fun SpaceCompassLandscapeDialogTitleContent(
    title: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    onDismiss: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("dialog-landscape-header")) {
        val maximumActionsWidth = (maxWidth * .6f).coerceAtLeast(48.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(end = 12.dp)) { title() }
            Box(Modifier.widthIn(max = maximumActionsWidth)) { actions() }
            Spacer(Modifier.width(6.dp))
            SpaceCompassDialogCloseIcon(onDismiss)
        }
    }
}

/** Preserve Material's end-aligned actions and confirm-first order when wrapping. */
@Composable
private fun SpaceCompassDialogActionFlow(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val horizontalGap = 8.dp.roundToPx()
        val verticalGap = 12.dp.roundToPx()
        val rows = mutableListOf<List<Placeable>>()
        var row = mutableListOf<Placeable>()
        var rowWidth = 0
        measurables.forEach { measurable ->
            val child = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val gap = if (row.isEmpty()) 0 else horizontalGap
            if (row.isNotEmpty() && rowWidth + gap + child.width > constraints.maxWidth) {
                rows.add(row)
                row = mutableListOf()
                rowWidth = 0
            }
            rowWidth += (if (row.isEmpty()) 0 else horizontalGap) + child.width
            row.add(child)
        }
        if (row.isNotEmpty()) rows.add(row)
        val widths = rows.map { it.sumOf { child -> child.width } + horizontalGap * (it.size - 1) }
        val heights = rows.map { it.maxOf { child -> child.height } }
        val width = constraints.constrainWidth(widths.maxOrNull() ?: 0)
        val height = constraints.constrainHeight(heights.sum() + verticalGap * (rows.size - 1).coerceAtLeast(0))
        layout(width, height) {
            var y = 0
            rows.indices.reversed().forEach { index ->
                var x = width - widths[index]
                rows[index].forEach { child ->
                    child.placeRelative(x, y + (heights[index] - child.height) / 2)
                    x += child.width + horizontalGap
                }
                y += heights[index] + verticalGap
            }
        }
    }
}

@Composable
internal fun SpaceCompassClosableDialogTitle(
    title: String,
    @Suppress("UNUSED_PARAMETER") onDismiss: () -> Unit
) {
    Text(
        text = title,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SpaceCompassClosableDialogTitleContent(
    onDismiss: () -> Unit,
    title: @Composable () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f)) { title() }
        Spacer(Modifier.width(6.dp))
        SpaceCompassDialogCloseIcon(onDismiss)
    }
}

/** Keep the close affordance identical in portrait and landscape headers. */
@Composable
private fun SpaceCompassDialogCloseIcon(onDismiss: () -> Unit) {
    val closeDescription = stringResource(R.string.close)
    val iconColor = LocalContentColor.current
    SpaceCompassAccessibleIconButton(
        contentDescription = closeDescription,
        onClick = onDismiss,
        modifier = Modifier.size(36.dp)
    ) {
        Canvas(Modifier.size(18.dp).graphicsLayer(alpha = iconColor.alpha)) {
            val ink = iconColor.copy(alpha = 1f)
            val stroke = 2.2.dp.toPx()
            drawLine(ink, Offset(size.width * .22f, size.height * .22f),
                Offset(size.width * .78f, size.height * .78f), stroke, StrokeCap.Round)
            drawLine(ink, Offset(size.width * .78f, size.height * .22f),
                Offset(size.width * .22f, size.height * .78f), stroke, StrokeCap.Round)
        }
    }
}
