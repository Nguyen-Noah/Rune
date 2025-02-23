package rune.platforms.opengl

import glm_.vec4.Vec4
import org.lwjgl.opengl.GL45.*
import rune.renderer.RendererAPI
import rune.renderer.VertexArray

class OpenGLRendererAPI : RendererAPI {
    override fun setClearColor(color: Vec4) {
        glClearColor(color.r, color.g, color.b, color.a)
    }

    override fun clear() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    override fun drawIndexed(vao: VertexArray) {
        vao.getIndexBuffer()?.let { glDrawElements(GL_TRIANGLES, it.getCount(), GL_UNSIGNED_INT, 0) }
    }
}