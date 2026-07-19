package com.krya1012.clearance.util

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Semantic haptic feedback wrapper, mirroring iOS `HapticsManager`'s vocabulary but mapped onto
 * `View.performHapticFeedback(HapticFeedbackConstants.*)` — richer than Compose's own
 * `LocalHapticFeedback` (which exposes only two generic constants), matching the mapping
 * `ANDROID_PLAN.md` documents for this port.
 */
object Haptics {
    fun taskToggled(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    fun moduleToggled(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    fun skipped(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    fun reset(view: View) = view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    fun checklistCompleted(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}
