package me.mondiversi.spacecompass

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassDropdownPositionProviderTest {
    @Test fun zoomListSharesItsAnchorsCenterInEitherLayoutDirection() {
        val provider=SpaceCompassDropdownPositionProvider(8,true,true)
        for(direction in LayoutDirection.entries) {
            val result=provider.calculatePosition(IntRect(400,480,440,520),IntSize(900,1000),direction,IntSize(80,300))
            assertEquals(IntOffset(380,350),result)
            assertEquals(420,result.x+40);assertEquals(500,result.y+150)
        }
    }
    @Test fun centeredListsStayInsideTopBottomAndSideEdges() {
        val provider=SpaceCompassDropdownPositionProvider(8,true,true)
        assertEquals(IntOffset(8,8),provider.calculatePosition(IntRect(0,0,40,40),IntSize(400,800),LayoutDirection.Ltr,IntSize(100,300)))
        assertEquals(IntOffset(292,492),provider.calculatePosition(IntRect(360,760,400,800),IntSize(400,800),LayoutDirection.Rtl,IntSize(100,300)))
    }
    @Test fun systemBarOffsetsDoNotCreateAnExtraBottomGap() {
        val provider=SpaceCompassDropdownPositionProvider(8,true,true,IntRect(0,80,400,800))
        assertEquals(IntOffset(150,692),provider.calculatePosition(IntRect(180,730,220,770),
            IntSize(400,720),LayoutDirection.Ltr,IntSize(100,100)))
        assertEquals(IntOffset(150,110),provider.calculatePosition(IntRect(180,140,220,180),
            IntSize(400,720),LayoutDirection.Ltr,IntSize(100,100)))
    }
    @Test fun otherMenusKeepTheirExistingBelowAboveAndRtlPlacement() {
        val provider=SpaceCompassDropdownPositionProvider(8)
        assertEquals(IntOffset(100,140),provider.calculatePosition(IntRect(100,100,140,140),IntSize(400,800),LayoutDirection.Ltr,IntSize(100,200)))
        assertEquals(IntOffset(40,140),provider.calculatePosition(IntRect(100,100,140,140),IntSize(400,800),LayoutDirection.Rtl,IntSize(100,200)))
        assertEquals(IntOffset(100,500),provider.calculatePosition(IntRect(100,700,140,740),IntSize(400,800),LayoutDirection.Ltr,IntSize(100,200)))
    }
}
