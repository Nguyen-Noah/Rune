package rune.core

import Platform.Windows.GlfwFrameTimer

abstract class FrameTimer {
    var deltaSeconds = 0.0
        protected set

    var fps = 0.0
        protected set

    private var frameCount = 0
    private var fpsAccum = 0.0

    protected abstract fun now(): Double

    fun tick() {
        if (lastTime < 0.0) lastTime = now()

        val current = now()
        deltaSeconds = current - lastTime
        lastTime = current

        frameCount++
        fpsAccum += deltaSeconds
        if (fpsAccum >= 1.0) {
            fps = frameCount / fpsAccum
            frameCount = 0
            fpsAccum = 0.0
        }
    }

    private var lastTime = -1.0

    // TODO: get this from a config or something
    companion object {
        fun create(): FrameTimer {
            return GlfwFrameTimer()
        }
    }
}