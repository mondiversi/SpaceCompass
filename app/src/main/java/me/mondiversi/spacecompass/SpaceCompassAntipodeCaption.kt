package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em

/** Shape and align the antipode vector inside text, with a localized spoken alternative. */
@Composable
internal fun SpaceCompassAntipodeCaptionText(value: String, modifier: Modifier, color: Color, style: TextStyle) {
    val alternate = "(${stringResource(R.string.scene_antipode)})"
    val text = buildAnnotatedString {
        value.split(SPACE_COMPASS_ANTIPODE_CAPTION_ICON).forEachIndexed { index,part ->
            if (index > 0) appendInlineContent("antipode",alternate)
            append(part)
        }
    }
    Text(text,modifier,color=color,style=style,inlineContent=mapOf("antipode" to InlineTextContent(
        Placeholder(1.em,1.em,PlaceholderVerticalAlign.TextCenter)) {
        Canvas(Modifier.fillMaxSize()) {
            drawSpaceCompassAntipode(center,size.minDimension*.37f,color,size.minDimension*.08f)
        }
    }))
}
