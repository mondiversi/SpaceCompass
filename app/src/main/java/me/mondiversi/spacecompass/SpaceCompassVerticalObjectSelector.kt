package me.mondiversi.spacecompass

import android.icu.text.BreakIterator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact landscape reads the colour marker and upright graphemes from top to bottom. */
@Composable
internal fun SpaceCompassVerticalObjectSelector(name: String?, tint: Color, primary: Color,
    navigation: SpaceCompassCelestialBodyNavigation?, onName: (() -> Unit)?, onDailyPath: (() -> Unit)?) {
    val titleTutorial=rememberSpaceCompassTutorialAnchor(SpaceCompassTutorialFeature.OBSERVATORY,enabled=onName!=null)
    val dailyTutorial=rememberSpaceCompassTutorialAnchor(SpaceCompassTutorialFeature.DAILY,enabled=onDailyPath!=null)
    val interaction=remember { MutableInteractionSource() }
    val letters=remember(name) {
        val text=name.orEmpty()
        val iterator=BreakIterator.getCharacterInstance(java.util.Locale.ROOT).apply { setText(text) }
        buildList {
            var start=iterator.first();var end=iterator.next()
            while(end!=BreakIterator.DONE) { add(text.substring(start,end));start=end;end=iterator.next() }
        }
    }
    val density=LocalDensity.current
    val marker=with(density) { (14.sp*.75f).toDp() }
    val lineHeight=with(density) { 16.sp.toDp() }
    Column(Modifier.fillMaxSize().testTag("sun-finder-body-data").clip(RoundedCornerShape(20.dp))
        .spaceCompassSceneInputShield()
        .background(spaceCompassSettingsCardColor().copy(alpha=SPACE_COMPASS_MAIN_OVERLAY_ALPHA))
        .padding(vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxSize()
                .padding(vertical=if(navigation!=null) SpaceCompassObjectHeaderGlyph.CHEVRON.width+12.dp else 0.dp)
                .testTag("celestial-body-island-title").then(titleTutorial?.touchModifier ?: Modifier)
                .then(if(onName!=null) Modifier.clickable(interactionSource=interaction,indication=null,
                    role=Role.Button,onClickLabel=stringResource(R.string.celestial_details_title),onClick=onName)
                    .spaceCompassAccessibleAction(name.orEmpty(),onClick=onName)
                    else Modifier.clearAndSetSemantics { contentDescription=name.orEmpty() }),
                contentAlignment=Alignment.Center) {
                val capacity=((maxHeight-marker-6.dp)/lineHeight).toInt().coerceAtLeast(1)
                val visible=if(letters.size<=capacity) letters else letters.take((capacity-1).coerceAtLeast(0))+"…"
                Column(Modifier.then(titleTutorial?.visualModifier ?: Modifier),
                    horizontalAlignment=Alignment.CenterHorizontally) {
                    if(!name.isNullOrEmpty()) {
                        SpaceCompassCelestialColorDot(tint,primary,Modifier.size(marker)
                            .testTag("celestial-body-color-dot").clearAndSetSemantics {})
                        Spacer(Modifier.height(6.dp))
                    }
                    visible.forEach { letter ->
                        Text(letter,Modifier.height(lineHeight).wrapContentHeight().testTag("celestial-body-name")
                            .clearAndSetSemantics {},color=primary,fontSize=14.sp,lineHeight=16.sp,
                            fontWeight=FontWeight.Medium,textAlign=TextAlign.Center,maxLines=1,
                            softWrap=false,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
            if(navigation!=null) {
                VerticalObjectArrow(navigation.nextLabel,navigation.onNext,primary,-90f,
                    Modifier.align(Alignment.TopCenter),"celestial-body-next")
                VerticalObjectArrow(navigation.previousLabel,navigation.onPrevious,primary,90f,
                    Modifier.align(Alignment.BottomCenter),"celestial-body-previous")
            }
        }
        if(onDailyPath!=null) {
            val ink=spaceCompassExportIconInkSize(1.4.dp,20.dp)
            val halfGap=(2.dp+34.dp-ink.width)/2
            val slot=ink.height+halfGap-1.dp
            Spacer(Modifier.height(1.dp))
            SpaceCompassCelestialIconControl(false,stringResource(R.string.celestial_daily_path),onDailyPath,
                Modifier.fillMaxWidth().height(slot).offset(y=(slot-ink.height)/2)
                    .testTag("celestial-body-daily-path").then(dailyTutorial?.touchModifier ?: Modifier),
                role=Role.Button,plainTint=primary) {
                SpaceCompassTutorialIconHighlight(dailyTutorial) { SpaceCompassDailyPathIcon(20.dp,1.4.dp) }
            }
        }
    }
}

@Composable
private fun VerticalObjectArrow(label: String,action: ()->Unit,tint: Color,rotation: Float,modifier: Modifier,tag: String) {
    val interaction=remember { MutableInteractionSource() }
    val tutorial=rememberSpaceCompassTutorialAnchor(SpaceCompassTutorialFeature.NAVIGATION)
    Box(modifier.fillMaxWidth().height(48.dp).testTag(tag).then(tutorial?.touchModifier ?: Modifier)
        .clickable(interactionSource=interaction,indication=null,onClick=action)
        .spaceCompassAccessibleAction(label,onClick=action),
        contentAlignment=if(rotation<0) Alignment.TopCenter else Alignment.BottomCenter) {
        SpaceCompassTutorialIconHighlight(tutorial) {
            Layout(content={ SpaceCompassObjectHeaderIcon(SpaceCompassObjectHeaderGlyph.CHEVRON,Modifier,tint) }) { children,constraints ->
                val glyph=children.single().measure(constraints.copy(minWidth=0,minHeight=0))
                layout(glyph.height,glyph.width) {
                    glyph.placeWithLayer((glyph.height-glyph.width)/2,(glyph.width-glyph.height)/2) { rotationZ=rotation }
                }
            }
        }
    }
}
