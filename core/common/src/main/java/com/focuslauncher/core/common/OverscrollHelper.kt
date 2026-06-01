package com.focuslauncher.core.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Custom Fling Behavior with adjustable scroll friction.
 * Lower friction multiplier makes scrolling feel buttery smooth with natural inertial deceleration.
 */
class FrictionFlingBehavior(
    private val decaySpec: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < 0.01f) return 0f
        
        var lastValue = 0f
        var velocityLeft = initialVelocity
        val animationState = AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity
        )
        
        try {
            animationState.animateDecay(decaySpec) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = velocity
                // If scroll is blocked (hit bounds), stop flinging
                if (abs(delta - consumed) > 0.5f) {
                    cancelAnimation()
                }
            }
        } catch (e: Exception) {
            // Cancelled
        }
        return velocityLeft
    }
}

@Composable
fun rememberFrictionFlingBehavior(frictionMultiplier: Float = 0.75f): FlingBehavior {
    val decaySpec = remember(frictionMultiplier) {
        exponentialDecay<Float>(frictionMultiplier = frictionMultiplier)
    }
    return remember(decaySpec) {
        FrictionFlingBehavior(decaySpec)
    }
}

/**
 * State to manage overscroll rubber-band calculations.
 */
class RubberBandOverscrollState(
    val coroutineScope: CoroutineScope
) {
    var overscrollAmount by mutableFloatStateOf(0f)
        private set

    private val animatable = Animatable(0f)

    fun onScroll(availableY: Float): Float {
        val current = overscrollAmount
        val resistance = 0.35f
        val delta = availableY * resistance
        overscrollAmount = current + delta
        return availableY
    }

    fun release() {
        coroutineScope.launch {
            animatable.snapTo(overscrollAmount)
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.6f, // Bouncy/Elastic snap-back
                    stiffness = 200f    // Natural deceleration speed
                )
            ) {
                overscrollAmount = value
            }
        }
    }

    fun absorbFling(target: Float) {
        coroutineScope.launch {
            animatable.snapTo(overscrollAmount)
            animatable.animateTo(
                targetValue = target,
                animationSpec = spring(stiffness = 300f)
            ) {
                overscrollAmount = value
            }
            release()
        }
    }
}

@Composable
fun rememberRubberBandOverscrollState(): RubberBandOverscrollState {
    val coroutineScope = rememberCoroutineScope()
    return remember { RubberBandOverscrollState(coroutineScope) }
}

/**
 * Captures unconsumed post-scroll movements to translate and stretch list items.
 */
class RubberBandNestedScrollConnection(
    val state: RubberBandOverscrollState
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val overscroll = state.overscrollAmount
        if (overscroll == 0f) return Offset.Zero

        val deltaY = available.y
        // If scrolling in the opposite direction of overscroll, reduce overscroll
        if (overscroll > 0f && deltaY < 0f) {
            val newOverscroll = (overscroll + deltaY).coerceAtLeast(0f)
            state.onScroll(newOverscroll - overscroll)
            return Offset(0f, deltaY)
        } else if (overscroll < 0f && deltaY > 0f) {
            val newOverscroll = (overscroll + deltaY).coerceAtMost(0f)
            state.onScroll(newOverscroll - overscroll)
            return Offset(0f, deltaY)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source == NestedScrollSource.Drag) {
            val deltaY = available.y
            if (deltaY != 0f) {
                state.onScroll(deltaY)
                return Offset(0f, deltaY)
            }
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        state.release()
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val velocityY = available.y
        if (abs(velocityY) > 100f) {
            // Absorb remaining fling speed into rubber stretch, then rebound
            val targetOverscroll = (velocityY * 0.05f).coerceIn(-180f, 180f)
            state.absorbFling(targetOverscroll)
            return available
        }
        state.release()
        return Velocity.Zero
    }
}

@Composable
fun rememberRubberBandNestedScrollConnection(state: RubberBandOverscrollState): RubberBandNestedScrollConnection {
    return remember(state) { RubberBandNestedScrollConnection(state) }
}

/**
 * Modifier to apply the elastic stretched spacing (One UI style) to list items.
 */
fun Modifier.rubberBandStretch(
    index: Int,
    totalItems: Int,
    state: RubberBandOverscrollState,
    horizontal: Boolean = false
): Modifier = this.graphicsLayer {
    val overscroll = state.overscrollAmount
    if (overscroll == 0f || totalItems <= 1) return@graphicsLayer
    
    if (horizontal) {
        val translationX = if (overscroll > 0f) {
            val multiplier = 0.12f + (index * 0.03f).coerceAtMost(0.3f)
            overscroll * multiplier
        } else {
            val j = totalItems - 1 - index
            val multiplier = 0.12f + (j * 0.03f).coerceAtMost(0.3f)
            overscroll * multiplier
        }
        this.translationX = translationX
    } else {
        val translationY = if (overscroll > 0f) {
            val multiplier = 0.12f + (index * 0.03f).coerceAtMost(0.3f)
            overscroll * multiplier
        } else {
            val j = totalItems - 1 - index
            val multiplier = 0.12f + (j * 0.03f).coerceAtMost(0.3f)
            overscroll * multiplier
        }
        this.translationY = translationY
    }
}
