package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import rune.scene.SceneCamera

class CameraComponent(
    val camera: SceneCamera = SceneCamera(),
    var primary: Boolean = true,
    var fixedAspectRatio: Boolean = false
) : Component<CameraComponent> {
    override fun type() = CameraComponent

    companion object : ComponentType<CameraComponent>()
}