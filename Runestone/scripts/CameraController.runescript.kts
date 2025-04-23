
import rune.core.Input
import rune.core.Key
import rune.components.*
import rune.scene.ScriptableEntity

fun init(): ScriptableEntity = Camera()

class Camera : ScriptableEntity() {
    private lateinit var transform: TransformComponent
    private val speed = 5.0f

    override fun onCreate() {
        println("Script compiled.")
        transform = getComponent(TransformComponent)
    }

    override fun onUpdate(dt: Float) {
        val t = transform.translation
        if (Input.isKeyPressed(Key.A))
            t.x -= speed * dt
        if (Input.isKeyPressed(Key.D))
            t.x += speed * dt
        if (Input.isKeyPressed(Key.W))
            t.y += speed * dt
        if (Input.isKeyPressed(Key.S))
            t.y -= speed * dt
    }

    override fun onDestroy() {
        println(entity)
    }
}
