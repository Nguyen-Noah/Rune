package rune.platforms.opengl

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import rune.renderer.Shader
import java.io.File
import java.nio.FloatBuffer

class OpenGLShader(private val name: String, private val vertexSrc: String, private val fragmentSrc: String) : Shader() {
    private var rendererID: Int = 0
    override fun getName() : String = name

    // TODO: find a way to only make this a single call
    constructor(filepath: String) : this(
        extractName(filepath),
        parseShader(filepath).first,
        parseShader(filepath).second
    )

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

    override fun uploadUniform(name: String, value: Float) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) glUniform1f(loc, value)
    }

    override fun uploadUniform(name: String, value: Int) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) glUniform1i(loc, value)
    }

    override fun uploadUniform(name: String, values: IntArray) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) glUniform1iv(loc, values)
    }

    override fun uploadUniform(name: String, value: Vec2) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) glUniform2f(loc, value.x, value.y)
    }

    override fun uploadUniform(name: String, value: Vec3) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) glUniform3f(loc, value.x, value.y, value.z)
    }

    override fun uploadUniform(name: String, value: Vec4) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) glUniform4f(loc, value.x, value.y, value.z, value.w)
    }

    override fun uploadUniform(name: String, value: Mat4) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) {
            MemoryStack.stackPush().use { stack ->
                val fb: FloatBuffer = stack.mallocFloat(16)
                value to fb
                glUniformMatrix4fv(loc, false, fb)
            }
        }
    }

    override fun bind() {
        glUseProgram(rendererID)
    }

    override fun unbind() {
        glUseProgram(0)
    }

    companion object {
        private fun parseShader(filepath: String): Pair<String, String> {
            val source = File(filepath).readText()
            val shaderParts = source.split("#type")
            var vertexShader = ""
            var fragmentShader = ""
            for (part in shaderParts) {
                when {
                    part.contains("vertex", ignoreCase = true) -> {
                        vertexShader = part.substringAfter("\n").trim()
                    }
                    part.contains("fragment", ignoreCase = true) -> {
                        fragmentShader = part.substringAfter("\n").trim()
                    }
                }
            }
            if (vertexShader.isEmpty() || fragmentShader.isEmpty()) {
                error("Shader file parsing error: Both vertex and fragment shaders must be defined.")
            }
            return Pair(vertexShader, fragmentShader)
        }

        private fun extractName(filepath: String): String {
            val glName = filepath.split('/').last()
            return glName.split('.').first()
        }
    }
}