package com.prism.studio.design

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * A haptic vocabulary of five signals, each bound to a meaning.
 *
 * The failure mode with haptics is sprinkling: buzz on every tap, and within a day the user turns
 * system haptics off entirely — losing the two or three moments where feedback genuinely helps.
 * Prism spends its budget deliberately, and there is nothing on scroll.
 *
 * | Signal | When | Why |
 * |---|---|---|
 * | [tick] | Slider crossing a detent, facet chip toggling | Confirms a discrete change the eye may miss |
 * | [select] | Picking a widget, wallpaper, or preset | Marks a choice as taken |
 * | [confirm] | Widget placed, setup applied | The two moments worth marking as events |
 * | [reject] | An action that cannot proceed | Says no without a dialog |
 * | [longPress] | Entering reorder or a context menu | Signals a mode change |
 *
 * Everything routes through `View.performHapticFeedback`, so the OS setting is respected for free.
 */
class Haptics(private val view: View) {

    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    fun select() = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

    fun confirm() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

    fun reject() = view.performHapticFeedback(HapticFeedbackConstants.REJECT)

    fun longPress() = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
