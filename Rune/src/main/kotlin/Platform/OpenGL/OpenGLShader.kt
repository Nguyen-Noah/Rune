package rune.platforms.opengl

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.lwjgl.PointerBuffer
import org.lwjgl.opengl.ARBGLSPIRV
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL45.*
import org.lwjgl.opengl.GL46.GL_SHADER_BINARY_FORMAT_SPIR_V
import org.lwjgl.opengl.GL46.glSpecializeShader
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.util.shaderc.Shaderc.*
import org.lwjgl.util.spvc.Spvc.*
import org.lwjgl.util.spvc.SpvcReflectedResource
import rune.core.Logger
import rune.core.Timer
import rune.renderer.gpu.Shader
import java.io.File
import java.nio.ByteBuffer
import java.nio.LongBuffer
import java.nio.file.Files
import java.nio.file.Path

const val SPVC_DECORATION_BINDING = 33    // couldnt find this in Spvc.* for some reason

data class UniformInfo(
    val name:    String,
    val size:    Int,
    val binding: Int
)

class OpenGLShader private constructor(
    private val name: String,
    private val filePath: String,
    private val stages: Map<Int, String>
) : Shader() {
    constructor(name: String, vertexSrc: String, fragmentSrc: String) :
            this(
                name,
                "<memory>",
                mapOf(
                    GL_VERTEX_SHADER   to vertexSrc,
                    GL_FRAGMENT_SHADER to fragmentSrc
                )
            )

    constructor(filePath: String) : this(
        extractName(filePath),
        filePath,
        preprocess(File(filePath).readText())
    )

    // ────────────────────────── public API ─────────────────────────────────
    override fun getName() = name
    override fun bind() {
        // avoids unnecessary binding
        if (currentProgram != rendererID) {
            glUseProgram(rendererID)
            currentProgram = rendererID
        }
    }
    override fun unbind() {
        if (currentProgram == rendererID)
            return
        glUseProgram(0)
    }

    // ────────────────────────── impl details ───────────────────────────────
    private var rendererID = -1
    private val vulkanSpv = HashMap<Int, ByteBuffer>()
    private val openGlSpv = HashMap<Int, ByteBuffer>()

    private val enableCache = true
    private val timer: Timer = Timer()

    init {
        compileOrGetVulkanBinaries()
        compileOrGetOpenGLBinaries()
        createProgram()
        Logger.warn("Shader creation took ${timer.elapsedMillis()} ms.")
    }

    private fun compileOrGetVulkanBinaries() {
        // initialize the compiler and options
        val compiler = shaderc_compiler_initialize()
        val options = shaderc_compile_options_initialize()

        // set the compile version to Vulkan 1.2
        shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_2)
        val optimize = true
        if (optimize)
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance)

        for ((stage, source) in stages) {
            val cached = cachePath(stage, vulkan = true)
            vulkanSpv[stage] = if (enableCache && Files.exists(cached)) {      // TODO: do we need the spv in memory?
                // the binary was found, so read the cached version
                val bytes = Files.readAllBytes(cached)
                MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
            } else {
                // compile with shaderc
                val module = shaderc_compile_into_spv(
                    compiler, source, glStageToShaderc(stage),
                    filePath, "main", options
                )
                println(shaderc_result_get_error_message(module))
                require(shaderc_result_get_compilation_status(module) == shaderc_compilation_status_success)

                val size = shaderc_result_get_length(module).toInt()
                val copy = MemoryUtil.memAlloc(size)
                    .put(shaderc_result_get_bytes(module))
                    .flip() as ByteBuffer

                // cache to disk
                if (enableCache) {
                    Files.createDirectories(cached.parent)
                    Files.write(cached, ByteArray(size).also { copy.get(it) })
                }

                copy.rewind()

                shaderc_result_release(module)
                copy
            }
        }
        shaderc_compile_options_release(options)
        shaderc_compiler_release(compiler)

        for ((stage, sprv) in vulkanSpv) {
            reflect(stage, sprv)
        }
    }

    private fun compileOrGetOpenGLBinaries() {
        // If the driver can consume SPIR‑V directly there is nothing to do.
        val caps = GL.getCapabilities()
        val canSpecialize = caps.OpenGL46 || caps.GL_ARB_gl_spirv

        val compiler = shaderc_compiler_initialize()
        val options = shaderc_compile_options_initialize()

        shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5)
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_zero) // no aggressive passes

        for ((stage, vSpv) in vulkanSpv) {
            val cached = cachePath(stage, vulkan = false)
            if (enableCache && Files.exists(cached)) {
                // cached bin found, read from it
                val bytes = Files.readAllBytes(cached)
                openGlSpv[stage] = MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
                continue
            }

            // ── 1. VULKAN‑SPIR‑V ➜ GLSL (Spirv‑Cross) ────────────────────
            val oglGlsl: String = MemoryStack.stackPush().use { stack ->
                val ctxPtr = stack.mallocPointer(1)
                check(spvc_context_create(ctxPtr) == SPVC_SUCCESS)
                val ctx = ctxPtr[0]

                val irPtr = stack.mallocPointer(1)
                check(spvc_context_parse_spirv(ctx, vSpv.asIntBuffer(), (vSpv.remaining() / 4).toLong(), irPtr) == SPVC_SUCCESS)
                val ir = irPtr[0]

                val compPtr = stack.mallocPointer(1)
                check(spvc_context_create_compiler(ctx, SPVC_BACKEND_GLSL, ir, SPVC_CAPTURE_MODE_COPY, compPtr) == SPVC_SUCCESS)
                val glslComp = compPtr[0]

                val optPtr = stack.mallocPointer(1)
                check(spvc_compiler_create_compiler_options(glslComp, optPtr) == SPVC_SUCCESS)
                val opts = optPtr[0]
                spvc_compiler_options_set_uint(opts, SPVC_COMPILER_OPTION_GLSL_VERSION, 450)
                spvc_compiler_install_compiler_options(glslComp, opts)

                val glslSrcPtr = stack.mallocPointer(1)
                spvc_compiler_compile(glslComp, glslSrcPtr)
                memUTF8(glslSrcPtr[0]) // <-- returned Kotlin String lives on the heap
            }

            // ── 2. GLSL ➜ OPENGL‑SPIR‑V (shaderc) ────────────────────────
            val module = shaderc_compile_into_spv(
                compiler,
                oglGlsl,
                glStageToShaderc(stage),
                filePath,
                "main",
                options
            )
            if (shaderc_result_get_compilation_status(module) != shaderc_compilation_status_success) {
                Logger.error("OpenGL‑target compilation failed for stage $stage:\n " + shaderc_result_get_error_message(module)?.let { memUTF8(it) })
                shaderc_result_release(module)
                continue
            }

            val size = shaderc_result_get_length(module).toInt()
            val spv = MemoryUtil.memAlloc(size).put(shaderc_result_get_bytes(module)).flip() as ByteBuffer
            openGlSpv[stage] = spv

            if (enableCache) {
                Files.createDirectories(cached.parent)
                Files.write(cached, ByteArray(size).also { spv.get(it) })
                spv.rewind()
            }
            shaderc_result_release(module)
        }

        shaderc_compile_options_release(options)
        shaderc_compiler_release(compiler)
    }

    private fun createProgram() {
        val program = glCreateProgram()
        // create an array with size same as openGLSpv to hold the IDs of shader programs
        val shaderIDs = arrayOfNulls<Int>(openGlSpv.size)

        // TODO: move this to OpenGLCaps.kt
        val caps = GL.getCapabilities()
        val canSpecialize = caps.OpenGL46 || caps.GL_ARB_gl_spirv

        var i = 0
        for ((stage, spirv) in openGlSpv) {
            if (canSpecialize) {
                // GPU can use GL46
                val shaderID = glCreateShader(stage)

                // upload the spirv bin
                glShaderBinary(intArrayOf(shaderID), GL_SHADER_BINARY_FORMAT_SPIR_V, spirv)

                // specialize to entry point "main"
                if (caps.OpenGL46)
                    glSpecializeShader(shaderID, "main", IntArray(0), IntArray(0))
                else if (caps.GL_ARB_gl_spirv)
                    ARBGLSPIRV.glSpecializeShaderARB(shaderID, "main", IntArray(0), IntArray(0))
                else
                    Logger.error("This context can't consume SPIR-V; fell back earlier")

                val ok = glGetShaderi(shaderID, GL_COMPILE_STATUS)
                if (ok == GL_FALSE) {
                    val log = GL20.glGetShaderInfoLog(shaderID)
                    Logger.error("SPIR-V specialisation failed for stage $stage:\n$log")
                }
                // move out once fallback is implemented
                glAttachShader(program, shaderID)
                shaderIDs[i++] = shaderID
            } else {
                // fallback option -> convert back to GLSL and compile
                Logger.error("Error compiling shader")
            }
        }
        // TODO: check link
        glLinkProgram(program)

        for (id in shaderIDs) {
            if (id != null) {
                glDetachShader(program, id)
                glDeleteShader(id)
            }
        }

        rendererID = program
    }

    fun reflect(stage: Int, spirv: ByteBuffer) = MemoryStack.stackPush().use { s ->
        /* ---- context + compiler ------------------------------------------------ */
        val ctxPtr = s.mallocPointer(1)
        check(spvc_context_create(ctxPtr) == SPVC_SUCCESS)
        val ctx    = ctxPtr[0]

        val irPtr  = s.mallocPointer(1)
        check(spvc_context_parse_spirv(ctx,
            spirv.asIntBuffer(), (spirv.remaining() / 4).toLong(), irPtr) == SPVC_SUCCESS)
        val ir     = irPtr[0]

        val compPtr = s.mallocPointer(1)
        check(spvc_context_create_compiler(ctx, SPVC_BACKEND_NONE,
            ir, SPVC_CAPTURE_MODE_COPY, compPtr) == SPVC_SUCCESS)
        val compiler = compPtr[0]

        /* ---- resource list ----------------------------------------------------- */
        val resPtr = s.mallocPointer(1)
        check(spvc_compiler_create_shader_resources(compiler, resPtr) == SPVC_SUCCESS)
        val res    = resPtr[0]

        val listPtr = s.mallocPointer(1)   // out: const SpvcReflectedResource*
        val cntPtr  = s.mallocPointer(1)   // out: size_t*
        check(spvc_resources_get_resource_list_for_type(
            res, SPVC_RESOURCE_TYPE_UNIFORM_BUFFER, listPtr, cntPtr) == SPVC_SUCCESS)

        val count = cntPtr[0].toInt()
        if (count == 0) return@use

        // listPtr[0] points to the first struct in a row of `count` structs
        val baseAddr = listPtr[0]
        val step     = SpvcReflectedResource.SIZEOF.toLong()

        val uniforms = ArrayList<UniformInfo>(count)
        for (i in 0 until count) {
            val resourceAddr = baseAddr + i * step
            val resource     = SpvcReflectedResource.create(resourceAddr)

            val name = resource.nameString()

            // temp
            val typeHandle = spvc_compiler_get_type_handle(compiler, resource.base_type_id())

            val sizePtr = s.mallocPointer(1)
            check(spvc_compiler_get_declared_struct_size(
                compiler, typeHandle, sizePtr) == SPVC_SUCCESS)
            val size = sizePtr[0].toInt()

            val binding = spvc_compiler_get_decoration(
                compiler, resource.id(), SPVC_DECORATION_BINDING)

            val memberCount = spvc_type_get_num_member_types(typeHandle)

            uniforms += UniformInfo(name, size, binding)
        }

        /* --- cleanup & logging --- */
        spvc_context_destroy(ctx)
        uniforms.forEach {
            Logger.trace("UBO: ${it.name}  size=${it.size}  binding=${it.binding}")
        }
    }

    // ───────────────────────── helpers ──────────────────────────────────────

    private inline fun intUniform1(name: String, block: (Int) -> Unit) {
        val loc = glGetUniformLocation(rendererID, name)
        if (loc != -1) block(loc)
    }

    private fun cachePath(stage: Int, vulkan: Boolean): Path {
        val dir = Path.of("assets/cache/shader/opengl")
        val ext = when (stage) {
            GL_VERTEX_SHADER   -> if (vulkan) ".cached_vulkan.vert" else ".cached_opengl.vert"
            GL_FRAGMENT_SHADER -> if (vulkan) ".cached_vulkan.frag" else ".cached_opengl.frag"
            else -> error("Unsupported stage $stage")
        }
        val fname = File(filePath).nameWithoutExtension + ext
        return dir.resolve(fname)
    }

    companion object {
        private fun glStageToShaderc(stage: Int) = when (stage) {
            GL_VERTEX_SHADER   -> shaderc_glsl_vertex_shader
            GL_FRAGMENT_SHADER -> shaderc_glsl_fragment_shader
            else -> error("Unsupported GL stage $stage")
        }
        private fun extractName(path: String): String = File(path).nameWithoutExtension

        /** Hazel‑style "#type <vertex|fragment>" pre‑processor. */
        private fun preprocess(src: String): Map<Int, String> {
            val token = "#type"
            var pos   = src.indexOf(token)
            if (pos == -1) error("No #type blocks found in shader file")
            val map = mutableMapOf<Int, String>()
            while (pos != -1) {
                val eol   = src.indexOf('\n', pos)
                val type  = src.substring(pos + token.length, eol).trim()
                val stage = when (type) {
                    "vertex"   -> GL_VERTEX_SHADER
                    "fragment", "pixel" -> GL_FRAGMENT_SHADER
                    else        -> error("Unknown shader type '$type'")
                }
                val nextLine = eol + 1
                pos = src.indexOf(token, nextLine)
                val code = if (pos == -1) src.substring(nextLine) else src.substring(nextLine, pos)
                map[stage] = code
            }
            return map
        }
    }


}

// helpful extensions
fun PointerBuffer.first() = this[0]
fun LongBuffer.first() = this[0]