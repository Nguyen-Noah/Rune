
fun init(): ScriptableEntity = Camera()

class Camera : ScriptableEntity() {
    private lateinit var transform: TransformComponent
    private val speed = 5.0f

    override fun onCreate() {
        println("Script compiled.")
        transform = getComponent(TransformComponent)
    }

    override fun onUpdate(dt: Float) {
        val t = transform.transform
        if (Input.isKeyPressed(Key.A))
            t[3][0] -= speed * dt
        if (Input.isKeyPressed(Key.D))
            t[3][0] += speed * dt
        if (Input.isKeyPressed(Key.W))
            t[3][1] += speed * dt
        if (Input.isKeyPressed(Key.S))
            t[3][1] -= speed * dt
    }

    override fun onDestroy() {
        println(entity)
    }
}
