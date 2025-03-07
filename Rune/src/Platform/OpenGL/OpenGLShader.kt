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


    class UniformDSL {
        internal val uniforms = mutableMapOf<String, Any>()

        fun uniform(name: String, value: Float) { uniforms[name] = value }
        fun uniform(name: String, value: Int)   { uniforms[name] = value }
        fun uniform(name: String, value: Vec2)  { uniforms[name] = value }
        fun uniform(name: String, value: Vec3)  { uniforms[name] = value }
        fun uniform(name: String, value: Vec4)  { uniforms[name] = value }
        fun uniform(name: String, value: Mat4)  { uniforms[name] = value }
    }

    fun uploadUniform(block: UniformDSL.() -> Unit) {
        val dsl = UniformDSL()
        dsl.block()

        bind()
        dsl.uniforms.forEach { (name, value) ->
            val location = glGetUniformLocation(rendererID, name)
            if (location != -1) {
                when (value) {
                    is Float -> glUniform1f(location, value)
                    is Int -> glUniform1i(location, value)
                    is Vec2 -> glUniform2f(location, value.x, value.y)
                    is Vec3 -> glUniform3f(location, value.x, value.y, value.z)
                    is Vec4 -> glUniform4f(location, value.x, value.y, value.z, value.w)
                    is Mat4 -> {
                        MemoryStack.stackPush().use { stack ->
                            val fb: FloatBuffer = stack.mallocFloat(16)
                            value to fb
                            glUniformMatrix4fv(location, false, fb)
                        }
                    }
                    else -> println("Uniform type for '$name' not supported: $value")
                }
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