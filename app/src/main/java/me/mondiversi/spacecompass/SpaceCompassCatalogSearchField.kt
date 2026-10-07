package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact inline search, keeping native text editing and a full-size clear action. */
@Composable
internal fun SpaceCompassCatalogSearchField(query: String, onQuery: (String) -> Unit,
    modifier: Modifier = Modifier) {
    val label = stringResource(R.string.catalog_search)
    val clearLabel = stringResource(R.string.catalog_search_clear)
    val foreground = MaterialTheme.colorScheme.onSurface
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    BasicTextField(query, { onQuery(it.take(128)) },
        modifier = modifier.height(48.dp).border(if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.primary else foreground.copy(alpha = .45f),
            RoundedCornerShape(10.dp)).testTag("catalog-search").semantics { contentDescription = label },
        singleLine = true, textStyle = TextStyle(color = foreground, fontSize = 13.sp),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), interactionSource = interaction,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
        decorationBox = { input ->
            Row(Modifier.fillMaxSize().padding(start = 10.dp, end = if (query.isEmpty()) 10.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(label, fontSize = 13.sp, maxLines = 1,
                        color = foreground.copy(alpha = .65f), overflow = TextOverflow.Ellipsis)
                    input()
                }
                if (query.isNotEmpty()) IconButton(onClick = { onQuery("") },
                    modifier = Modifier.size(48.dp).testTag("catalog-search-clear")
                        .semantics { contentDescription = clearLabel }) {
                    Canvas(Modifier.size(16.dp)) {
                        drawLine(foreground, Offset(size.width * .2f, size.height * .2f),
                            Offset(size.width * .8f, size.height * .8f), 1.8.dp.toPx(), StrokeCap.Round)
                        drawLine(foreground, Offset(size.width * .8f, size.height * .2f),
                            Offset(size.width * .2f, size.height * .8f), 1.8.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
        })
}
