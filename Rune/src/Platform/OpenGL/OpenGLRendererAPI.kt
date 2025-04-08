package rune.platforms.opengl

import glm_.vec4.Vec4
import org.lwjgl.opengl.GL45.*
import rune.renderer.RendererAPI
import rune.renderer.VertexArray

class OpenGLRendererAPI : RendererAPI {
    override fun init() {
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)
    }

    override fun setClearColor(color: Vec4) {
        glClearColor(color.r, color.g, color.b, color.a)
    }

    override fun clear() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    override fun drawIndexed(vao: VertexArray, indexCount: Int) {
        val count = if (indexCount == 0) {
            vao.getIndexBuffer()?.getCount() ?: 0
        } else {
            indexCount
        }

        glDrawElements(GL_TRIANGLES, count, GL_UNSIGNED_INT, 0L)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        glViewport(x, y, width, height)
    }
}