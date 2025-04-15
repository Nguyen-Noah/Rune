package runestone.scripts

import glm_.mat4x4.Mat4
import rune.components.TransformComponent
import rune.core.Input
import rune.core.Key
import rune.scene.ScriptableEntity


fun init(): ScriptableEntity = Camera()

class Camera : ScriptableEntity() {
    private lateinit var transform: Mat4
    private val speed = 5.0f

    override fun onCreate() {
        println("Script compiled.")
        transform = getComponent(TransformComponent).transform
    }

    override fun onUpdate(dt: Float) {
        if (Input.isKeyPressed(Key.A))
            transform[3][0] -= speed * dt
        if (Input.isKeyPressed(Key.D))
            transform[3][0] += speed * dt
        if (Input.isKeyPressed(Key.W))
            transform[3][1] += speed * dt
        if (Input.isKeyPressed(Key.S))
            transform[3][1] -= speed * dt
        println("(${transform[3][0]}, ${transform[3][1]})")
    }

    override fun onDestroy() {
        println(entity)
    }
}
