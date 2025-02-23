package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.lwjgl.opengl.GL33.*

class Shader(private val vertexSrc: String, private val fragmentSrc: String) {
    private var rendererID: Int = 0

    init {
        // compile the vertex shader
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, vertexSrc)
        glCompileShader(vertexShader)

        var success = glGetShaderi(vertexShader, GL_COMPILE_STATUS)
        if (success == GL_FALSE) {
            val length = glGetShaderi(vertexShader, GL_INFO_LOG_LENGTH)
            val infoLog = glGetShaderInfoLog(vertexShader, length)
            glDeleteShader(vertexShader)
            error("Vertex shader compilation failure: \n$infoLog")
        }

        // compile the fragment shader
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, fragmentSrc)
        glCompileShader(fragmentShader)

        success = glGetShaderi(fragmentShader, GL_COMPILE_STATUS)
        if (success == GL_FALSE) {
            val length = glGetShaderi(fragmentShader, GL_INFO_LOG_LENGTH)
            val infoLog = glGetShaderInfoLog(fragmentShader, length)
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)
            error("Fragment shader compilation failure: \n$infoLog")
        }

        // link the shaders into a program
        rendererID = glCreateProgram()
        glAttachShader(rendererID, vertexShader)
        glAttachShader(rendererID, fragmentShader)
        glLinkProgram(rendererID)

        val linkStatus = glGetProgrami(rendererID, GL_LINK_STATUS)
        if (linkStatus == GL_FALSE) {
            val length = glGetProgrami(rendererID, GL_INFO_LOG_LENGTH)
            val infoLog = glGetProgramInfoLog(rendererID, length)
            glDeleteProgram(rendererID)
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)
            error("Shader link failure:\n$infoLog")
        }

        // detach and optionally delete shaders now that theyre linked
        glDetachShader(rendererID, vertexShader)
        glDetachShader(rendererID, fragmentShader)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    fun render(vao: VertexArray, uniforms: Map<String, Any> = emptyMap(), renderMode: Int = GL_TRIANGLES) {
        bind()

        for ((name, value) in uniforms) {
            val location = glGetUniformLocation(rendererID, name)
            if (location != -1) {
                when (value) {
                    is Float -> glUniform1f(location, value)
                    is Int -> glUniform1i(location, value)
                    is Vec2 -> glUniform2f(location, value.x, value.y)
                    is Vec3 -> glUniform3f(location, value.x, value.y, value.z)
                    is Vec4 -> glUniform4f(location, value.x, value.y, value.z, value.w)
                    else -> println("Uniform type for $name not supported.")
                }
            }
        }

        vao.bind()
        val ibo = vao.getIndexBuffer()
        if (ibo != null) {
            // draw using index buffer
            glDrawElements(renderMode, ibo.getCount(), GL_UNSIGNED_INT, 0)
        } else {
            // fallback if there's no index buffer
            // ideally you have a known vertex count from the VBO or VAO
            glDrawArrays(renderMode, 0, 3)
        }
        vao.unbind()

        unbind()
    }

    fun bind() {
        glUseProgram(rendererID)
    }

    fun unbind() {
        glUseProgram(0)
    }
}