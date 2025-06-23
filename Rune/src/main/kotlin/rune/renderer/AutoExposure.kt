package rune.renderer

import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.gpu.Shader
import rune.renderer.gpu.Texture
import rune.rhi.ComputePipeline
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

class AutoExposure(
    private val bins: Int = 256,
    private val lowPct: Float = 0.5f,
    private val highPct: Float = 99.5f
    ) {

    private val histogramSSBO: StorageBuffer = StorageBuffer.create(bins)
    private val shader: Shader = Renderer.getShader("AutoExposure")
    private val computePipeline = ComputePipeline.create(shader)

    private var exposure = 1.0f

    fun update(dt: Float, hdrTex: Int, screenW: Int, screenH: Int) {
        //histogramSSBO.clear()

        shader.bind()
        SubmitRender("AutoExposure-update") {
            histogramSSBO.bind(1)
            // TODO: abstract
            glBindImageTexture(0, hdrTex, 0, false, 0, GL_READ_ONLY, GL_RGBA16F)

            computePipeline.dispatch((screenW + 15) / 16, (screenH + 15) / 16, 1)
            computePipeline.end()

            val tmp = MemoryUtil.memAllocInt(bins)
            glGetNamedBufferSubData(histogramSSBO.rendererId, 0, tmp)

            val totalPixels = screenW * screenH
            val lowCut = totalPixels * (lowPct / 100f)
            val highCut = totalPixels * (highPct / 100f)

            var cumulative = 0
            var minBin = 0
            var maxBin = bins - 1
            // find low percentile
            for (i in 0 until bins) {
                cumulative += tmp[i]
                if (cumulative >= lowCut) {
                    minBin = i
                    break
                }
            }
            // find high percentile
            cumulative = 0
            for (i in bins - 1 downTo 0) {
                cumulative += tmp[i]
                if (cumulative >= totalPixels - highCut) {
                    maxBin = i
                    break
                }
            }

            // average log luminance in percentile window ---------------------------------------------
            var logSum = 0.0
            var logCount = 0
            for (i in minBin..maxBin) {
                val count = tmp[i]
                logSum += count * (i + 0.5) / 10.666 - 12.0  // inverse mapping (see shader)
                logCount += count
            }
            MemoryUtil.memFree(tmp)

            if (logCount == 0) return@SubmitRender
            val avgLogLum = logSum / logCount.toDouble()
            val avgLum = 2.0.pow(avgLogLum)

            // classic middle‑gray exposure -----------------------------------------------------------
            val key = 0.18f
            var target = (key / max(avgLum.toFloat(), 1e-4f))
            target = target.coerceIn(0.05f, 20.0f)           // clamp comfort range

            // smooth (different rates for up vs down) -----------------------------------------------
            val tau = if (target > exposure) 0.5f else 1.5f  // seconds
            val alpha = (1f - exp((-dt / tau)))
            exposure += (target - exposure) * alpha
            println(exposure)
        }
    }
}