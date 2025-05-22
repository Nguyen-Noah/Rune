package rune.renderer.renderer2d

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.components.CircleRendererComponent
import rune.components.SpriteRendererComponent
import rune.renderer.Renderer
import rune.renderer.gpu.Shader
import rune.renderer.gpu.Texture2D

const val FLOAT_MAT4_SIZE = 16 * 4

object Renderer2D {
    private lateinit var quadBatch: QuadBatch
    private lateinit var circleBatch: CircleBatch
    private lateinit var lineBatch: LineBatch

    fun init() {
        val whiteTex = Texture2D.create(1, 1).apply { setData(0xffffffff.toInt(), 4) }
        quadBatch = QuadBatch(shader = Shader.create("assets/shaders/Renderer2D_Quad.glsl"), whiteTex = whiteTex)
        circleBatch = CircleBatch(shader = Shader.create("assets/shaders/Renderer2D_Circle.glsl"))
        lineBatch = LineBatch(shader = Shader.create("assets/shaders/Renderer2D_Line.glsl"))
    }

    fun beginScene() {
        startBatch()
    }

    fun endScene() {
        quadBatch.flush()
        circleBatch.flush()
        lineBatch.flush()
    }

    private fun startBatch() {
        quadBatch.begin()
        circleBatch.begin()
        lineBatch.begin()
    }

    private fun nextBatch() {
        endScene()
        startBatch()
    }

    // * primitives
    // rotation in radians
    fun drawQuad(
        position: Vec3,
        size: Vec2,
        rotation: Float = 0f,
        color: Vec4 = Vec4(1f),
        texture: Texture2D? = null,
        tilingFactor: Float = 1f,
        entityId: Int = -1
    ) {
        if (quadBatch.isFull(vertexCount = 4, indexCount = 6)) {
            nextBatch()
        }

        val transform = glm.translate(Mat4(1f), position) *
                        glm.rotate   (Mat4(1f), rotation, Vec3(0f, 0f, 1f)) *
                        glm.scale    (Mat4(1f), Vec3(size, 1f))

        quadBatch.pushQuad(transform, color, texture, tilingFactor, entityId)
        Renderer.stats.quadCount++
    }

    fun drawQuad(
        position: Vec2,
        size: Vec2,
        rotation: Float = 0f,
        color: Vec4 = Vec4(1f),
        texture: Texture2D? = null,
        tilingFactor: Float = 1f,
        entityId: Int = -1
    ) {
        drawQuad(Vec3(position, 0f), size, rotation, color, texture, tilingFactor, entityId)
    }

    fun drawQuad(
        transform: Mat4,
        color: Vec4 = Vec4(1f),
        texture: Texture2D? = null,
        tilingFactor: Float = 1f,
        entityId: Int = -1
    ) {
        if (quadBatch.isFull(vertexCount = 4, indexCount = 6)) {
            nextBatch()
        }

        quadBatch.pushQuad(transform, color, texture, tilingFactor, entityId)
        Renderer.stats.quadCount++
    }

    fun drawSprite(transform: Mat4, src: SpriteRendererComponent, entityId: Int) {
        drawQuad(
            transform = transform,
            color = src.color,
            texture = src.texture,
            tilingFactor = src.tilingFactor,
            entityId = entityId
        )
    }

    fun drawCircle(transform: Mat4, color: Vec4, thickness: Float = 1f, fade: Float = 0.005f, entityId: Int = -1) {
        if (circleBatch.isFull(vertexCount = 4, indexCount = 6)) {
            nextBatch()
        }
        circleBatch.pushCircle(transform, color, thickness, fade, entityId)
    }

    fun drawCircle(transform: Mat4, comp: CircleRendererComponent, entityId: Int = -1) =
        drawCircle(transform, comp.color, comp.thickness, comp.fade, entityId)

    fun drawLine(p0: Vec3, p1: Vec3, color: Vec4, entityId: Int = -1) {
        if (lineBatch.isFull(vertexCount = 2)) {
            nextBatch()
        }
        lineBatch.pushLine(p0, p1, color, entityId)
    }

    fun drawRect(position: Vec3, size: Vec2, color: Vec4, entityId: Int = -1) {
        val p0 = Vec3(position.x - size.x * 0.5f, position.y - size.y * 0.5f, position.z)
        val p1 = Vec3(position.x + size.x * 0.5f, position.y - size.y * 0.5f, position.z)
        val p2 = Vec3(position.x + size.x * 0.5f, position.y + size.y * 0.5f, position.z)
        val p3 = Vec3(position.x - size.x * 0.5f, position.y + size.y * 0.5f, position.z)

        drawLine(p0, p1, color)
        drawLine(p1, p2, color)
        drawLine(p2, p3, color)
        drawLine(p3, p0, color)
    }

    fun drawRect(transform: Mat4, color: Vec4, entityId: Int = -1) {
        val corners = arrayOf(
            Vec4(-0.5f, -0.5f, 0f, 1f),
            Vec4( 0.5f, -0.5f, 0f, 1f),
            Vec4( 0.5f,  0.5f, 0f, 1f),
            Vec4(-0.5f,  0.5f, 0f, 1f)
        ).map { (transform * it).toVec3() }

        drawLine(corners[0], corners[1], color, entityId)
        drawLine(corners[1], corners[2], color, entityId)
        drawLine(corners[2], corners[3], color, entityId)
        drawLine(corners[3], corners[0], color, entityId)
    }
}