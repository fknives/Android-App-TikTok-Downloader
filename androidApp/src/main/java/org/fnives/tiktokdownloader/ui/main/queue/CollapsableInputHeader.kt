package org.fnives.tiktokdownloader.ui.main.queue

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CollapsableInputHeader(
    modifier: Modifier = Modifier,
    notice: @Composable () -> Unit,
    input: @Composable () -> Unit,
    action: @Composable () -> Unit,
    horizontalGap: Dp = 8.dp,
    verticalGap: Dp = 8.dp,
    heightEasing: Easing = EaseIn,
    inputYEasing: Easing = EaseOut,
    inputWidthEasing: Easing = EaseOut,
    buttonXEasing: Easing = remember { Easing { progress -> (sin(progress * PI.toFloat()) / 5f) } },
    noticeAlphaEasing: Easing = remember {
        Easing { progress -> (1f - progress * 4).coerceIn(0f, 1f) }
    },
    state: CollapsableInputHeaderState = rememberCollapsableInputHeaderState()
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Box(
                Modifier.graphicsLayer { alpha = noticeAlphaEasing.transform(state.progress) }) {
                notice()
            }
            action()
            input()
        }
    ) { measurables, constraints ->
        val notice = measurables[0].measure(constraints)
        val action = measurables[1].measure(Constraints())
        val collapsedInputWidth = constraints.maxWidth - horizontalGap.roundToPx() - action.width
        val extendedInputWidth = constraints.maxWidth
        val travelInputWidth =
            inputWidthEasing.transform(state.progress) * (extendedInputWidth - collapsedInputWidth)
        val currentInputWidth = (extendedInputWidth - travelInputWidth).roundToInt()
        // animated width to make place for the action
        val input = measurables[2]
            .measure(
                Constraints(
                    minWidth = min(constraints.minWidth, currentInputWidth),
                    minHeight = constraints.minHeight,
                    maxHeight = constraints.maxHeight,
                    maxWidth = currentInputWidth
                )
            )
        state.extendedHeightPx =
            notice.height + verticalGap.roundToPx() + input.height + verticalGap.roundToPx() + action.height
        state.collapsedHeightPx = max(input.height, action.height)
        val travelParentHeight =
            (state.travelDistancePx * heightEasing.transform(state.progress)).roundToInt()

        layout(
            constraints.maxWidth,
            state.extendedHeightPx - travelParentHeight
        ) {
            // translate it up as it fades out
            val noticeOutTravelY = -(notice.height * state.progress).roundToInt()
            notice.placeRelative(x = 0, y = noticeOutTravelY)

            // centered
            val collapsedInputY = (max(action.height, input.height) - input.height) / 2 // centered
            // below Notice
            val expandedInputY = (notice.height + verticalGap.roundToPx())
            val travelInputY =
                inputYEasing.transform(state.progress) * (expandedInputY - collapsedInputY)
            input.placeRelative(
                x = 0,
                y = expandedInputY - travelInputY.roundToInt()
            )
            // centered
            val collapsedActionY =
                (max(action.height, input.height) - action.height) / 2
            // below Input
            val expandedActionY =
                expandedInputY + input.height + verticalGap.roundToPx()

            // Curve the Y travel by slightly moving the X
            val buttonWidthAnimation = buttonXEasing.transform(state.progress) * action.width
            val travelActionY =
                heightEasing.transform(state.progress) * (expandedActionY - collapsedActionY)
            action.placeRelative(
                x = constraints.maxWidth - action.width + buttonWidthAnimation.roundToInt(),
                y = expandedActionY - travelActionY.roundToInt()
            )
        }
    }
}

@Composable
fun rememberCollapsableInputHeaderState() = remember { CollapsableInputHeaderState() }

class CollapsableInputHeaderState {
    var extendedHeightPx by mutableIntStateOf(0)
    var collapsedHeightPx by mutableIntStateOf(1)
    val travelDistancePx get() = max(extendedHeightPx - collapsedHeightPx, 1)
    private var collapsedPx by mutableFloatStateOf(0f)
    val progress: Float get() = (collapsedPx / travelDistancePx).coerceIn(0f, 1f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            if (dy < 0f && collapsedPx < travelDistancePx) {
                val consumed = (-dy).coerceAtMost(travelDistancePx - collapsedPx)
                collapsedPx += consumed
                return Offset(0f, -consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val dy = available.y
            if (dy > 0f && collapsedPx > 0f) {
                val give = dy.coerceAtMost(collapsedPx)
                collapsedPx -= give
                return Offset(0f, give)
            }
            return Offset.Zero
        }
    }
}