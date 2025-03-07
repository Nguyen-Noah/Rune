package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.platforms.opengl.OpenGLShader

class Renderer2D {
    companion object {
        private val data: Renderer2DStorage = Renderer2DStorage()

        data class Renderer2DStorage(
            var vao: VertexArray? = null,
            var shader: Shader? = null
        )

        fun init() {
            val vertices = floatArrayOf(
                -0.5f, -0.5f, 0.0f,
                 0.5f, -0.5f, 0.0f,
                 0.5f,  0.5f, 0.0f,
                -0.5f,  0.5f, 0.0f
            )

            val vbo = VertexBuffer.create(vertices, vertices.size)
            val ibo = IndexBuffer.create(intArrayOf(0, 1, 2, 2, 3, 0), 6)

            data.vao = VertexArray.create(vbo, bufferLayout {
                attribute("a_Position", 3)
            })
            data.vao!!.setIndexBuffer(ibo)

            data.shader = Shader.create("assets/shaders/FlatColor.glsl")
        }

        fun shutdown() {

        }

        fun beginScene(camera: OrthographicCamera) {
            data.shader?.uploadUniform {
                uniform("u_ViewProjection", camera.getViewProjectionMatrix())
                uniform("u_Transform", Mat4(1.0))
            }
        }

        fun endScene() {

        }

        // primitives
        fun drawQuad(position: Vec2, size: Vec2, color: Vec4) {
            drawQuad(Vec3(position.x, position.y, 0.0), size, color)
        }

        fun drawQuad(position: Vec3, size: Vec2, color: Vec4) {
            (data.shader as OpenGLShader).uploadUniform {
                uniform("u_Color", color)
            }

            data.vao!!.bind()
            RenderCommand.drawIndexed(data.vao!!)
        }
    }


}