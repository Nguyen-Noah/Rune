package rune.core

import kotlin.time.TimeSource

class Timer {
    private var startTime = TimeSource.Monotonic.markNow()

    fun reset() { startTime = TimeSource.Monotonic.markNow() }
    fun elapsed(): Float = startTime.elapsedNow().inWholeNanoseconds * 1e-9f
    fun elapsedMillis(): Float = elapsed() * 1000f
}