package com.mainecare.effects.deck

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mainecare.effects.motion.cardDeckAlpha
import com.mainecare.effects.motion.cardDeckScale
import com.mainecare.effects.motion.MotionTrace
import kotlin.math.abs

/**
 * Three-card horizontal deck matching v13/v21: each slide recedes by 48dp and scales
 * to .97 at one page away, then 42dp/.94 at two pages, with opacity 1 - 0.87*distance.
 * Gesture and snapping are delegated to Compose's official Pager.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardDeck(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState { pageCount },
    pageSpacing: Dp = 0.dp,
    userScrollEnabled: Boolean = true,
    content: @Composable (Int) -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage + state.currentPageOffsetFraction }
            .collect { MotionTrace.emit("deck.progress", it) }
    }
    HorizontalPager(
        state = state,
        modifier = modifier,
        pageSpacing = pageSpacing,
        userScrollEnabled = userScrollEnabled,
        flingBehavior = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
        ),
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = state.currentPage + state.currentPageOffsetFraction
                    val distance = abs(page - progress)
                    val scale = cardDeckScale(distance)
                    scaleX = scale
                    scaleY = scale
                    alpha = cardDeckAlpha(distance)
                    transformOrigin = TransformOrigin.Center
                },
        ) {
            content(page)
        }
    }
}
