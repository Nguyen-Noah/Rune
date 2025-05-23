package rune.renderer.gpu

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.platforms.opengl.OpenGLShader
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

abstract class Shader {
    abstract fun bind()
    abstract fun unbind()
    abstract fun getName(): String

    companion object {
        internal var currentProgram = 0

        fun create(name: String, vertexSrc: String, fragmentSrc: String): Shader {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLShader(name, vertexSrc, fragmentSrc)
                RendererPlatform.None -> TODO()
            }
        }
        fun create(path: String): Shader {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLShader(path)
                RendererPlatform.None -> TODO()
            }
        }
    }
}