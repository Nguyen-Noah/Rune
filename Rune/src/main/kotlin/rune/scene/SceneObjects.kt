package rune.scene

import glm_.vec3.Vec3
import org.lwjgl.system.MemoryUtil
import rune.renderer.gpu.U_LIGHTS
import rune.renderer.gpu.UniformBuffer

//*////////////////////////////*//
//*////       LIGHTS       ////*//
//*////////////////////////////*//

data class SceneLights(
    var maxDirectionalLight: Int = 4,
    val directionalLights: Array<DirectionalLight?> = arrayOfNulls(maxDirectionalLight),
    var light: DirectionalLight? = null,
    private val lightBuffer: UniformBuffer = UniformBuffer.create(48, U_LIGHTS, name = "Lights")
    ) {
    fun bake() {
        light?.let {
            MemoryUtil.memAlloc(48).apply {
                putFloat(it.color.r)
                putFloat(it.color.g)
                putFloat(it.color.b)

                putFloat(it.diffuseIntensity)

                putFloat(it.direction.x)
                putFloat(it.direction.y)
                putFloat(it.direction.z)

                putFloat(0f)

                flip()
                lightBuffer.setData(this)
                MemoryUtil.memFree(this)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SceneLights

        if (maxDirectionalLight != other.maxDirectionalLight) return false
        if (!directionalLights.contentEquals(other.directionalLights)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxDirectionalLight
        result = 31 * result + directionalLights.contentHashCode()
        return result
    }
}

abstract class Light(
    open var color: Vec3 = Vec3(1f),
    open var diffuseIntensity: Float = 1f
)

data class PointLight(
    var radius: Float = 1f
) : Light()

data class DirectionalLight(
    override var color: Vec3 = Vec3(1f),
    override var diffuseIntensity: Float = 1f,
    var direction: Vec3 = Vec3(0f)
) : Light()