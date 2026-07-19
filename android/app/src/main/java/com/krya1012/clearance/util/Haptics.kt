package com.krya1012.clearance.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic feedback abstraction, mirroring iOS's injected `HapticsManager` —
 * kept as an interface so `ChecklistViewModel` can be constructed with
 * [NoopHaptics] in tests, with no Android `Context` dependency.
 */
interface Haptics {
    fun prepare()
    fun taskToggled(completed: Boolean)
    fun moduleToggled()
    fun skipped()
    fun reset()
    fun checklistCompleted()
}

/** Real device implementation using the system [Vibrator] service. */
class VibratorHaptics(context: Context) : Haptics {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun click() = vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    private fun tick() = vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    private fun doubleClick() = vibrate(
        VibrationEffect.createWaveform(longArrayOf(0, 20, 40, 60), -1)
    )

    private fun vibrate(effect: VibrationEffect) {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(effect)
    }

    override fun prepare() = Unit
    override fun taskToggled(completed: Boolean) = click()
    override fun moduleToggled() = tick()
    override fun skipped() = tick()
    override fun reset() = click()
    override fun checklistCompleted() = doubleClick()
}

/** No-op implementation used in unit tests. */
object NoopHaptics : Haptics {
    override fun prepare() = Unit
    override fun taskToggled(completed: Boolean) = Unit
    override fun moduleToggled() = Unit
    override fun skipped() = Unit
    override fun reset() = Unit
    override fun checklistCompleted() = Unit
}
