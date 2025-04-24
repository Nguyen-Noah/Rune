package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import glm_.vec3.Vec3

data class TransformComponent(
    var translation: Vec3 = Vec3(0.0f),
    var rotation:    Vec3 = Vec3(0.0f),
    var scale:       Vec3 = Vec3(1.0f)
) : Component<TransformComponent> {
    override fun type(): ComponentType<TransformComponent> = TransformComponent

    fun getTransform(): Mat4 {
        return glm.translate(Mat4(1f), translation) *
                Quat(rotation).toMat4() *
                glm.scale(Mat4(1f), scale)
    }

    companion object : ComponentType<TransformComponent>()
}
