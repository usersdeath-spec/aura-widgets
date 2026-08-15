package com.prism.studio.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * One motion vocabulary for the whole app.
 *
 * Ad-hoc animation is the main reason apps feel busy rather than expensive: every screen invents
 * its own duration, and the result reads as noise even when each individual piece is fine. Prism
 * has four durations, three easings, and three springs. Nothing anywhere calls `tween(300)`
 * directly; if a motion needs a value that isn't here, the value is wrong or the token set is
 * incomplete, and either way it is a conversation rather than a local decision.
 *
 * The governing rule: **motion explains, it does not perform.** Every animation in this app answers
 * one of two questions — "where did this come from?" or "what just changed?" Anything that answers
 * neither is deleted, however nice it looks in isolation.
 */
object Motion {

    /**
     * Durations.
     *
     * [instant] is for things that must not feel animated at all — the live preview responding to a
     * slider. [quick] is the workhorse: selection, ripple, chip toggle. [standard] covers anything
     * that moves across the screen. [deliberate] is reserved for the two moments that should feel
     * like events: applying a setup, and completing the purchase.
     */
    const val instant = 90
    const val quick = 180
    const val standard = 280
    const val deliberate = 460

    /**
     * Easings.
     *
     * [enter] decelerates hard — things arriving should look like they were already moving.
     * [exit] accelerates — things leaving should get out of the way.
     * [standardEasing] is symmetric, for anything that stays on screen and changes shape.
     */
    val enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * Springs, for anything the finger is directly responsible for.
     *
     * Physical motion is the difference between an app that responds and an app that plays back a
     * recording. [gentle] is for large surfaces (sheets, hero transitions), [snappy] for controls,
     * [taut] for the preview reacting to a drag — barely any overshoot, because a preview that
     * wobbles reads as inaccurate.
     */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)

    fun <T> snappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)

    fun <T> taut(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)

    fun <T> enterSpec(duration: Int = standard): FiniteAnimationSpec<T> =
        tween(duration, easing = enter)

    fun <T> exitSpec(duration: Int = quick): FiniteAnimationSpec<T> =
        tween(duration, easing = exit)

    /**
     * Staggered list reveal.
     *
     * Capped at eight items: past that the last row is waiting on a queue rather than arriving, and
     * the effect turns from "the screen is assembling" into "the app is slow". The cap is the whole
     * reason this is a function rather than a per-screen loop index.
     */
    fun staggerDelay(index: Int, step: Int = 28, cap: Int = 8): Int =
        step * index.coerceAtMost(cap)

    /** Vertical offset used by every list-item entrance, so they all arrive from the same distance. */
    fun riseFrom(densityPx: Int): IntOffset = IntOffset(0, densityPx)
}

/**
 * Reduced motion.
 *
 * When the system setting is on, transitions collapse to cross-fades at [Motion.quick] and springs
 * become linear tweens — everything still *changes state visibly*, because removing motion entirely
 * makes an interface harder to follow, not easier. Nothing is ever simply cut.
 */
object ReducedMotion {
    const val crossFade = Motion.quick
    fun <T> spec(): FiniteAnimationSpec<T> = tween(crossFade, easing = Motion.standardEasing)
}
