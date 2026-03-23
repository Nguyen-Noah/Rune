package rune.platforms.opengl

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
import rune.renderer.SubmitRender
import rune.renderer.gpu.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.LongBuffer
import java.nio.file.Files
import java.nio.file.Path

const val SPVC_DECORATION_BINDING = 33    // couldnt find this in Spvc.* for some reason

class OpenGLShader private constructor(
    private val name: String,
    private val filePath: String,
    private val stages: Map<Int, Pair<String, String>>
) : Shader() {
    constructor(name: String, vertexSrc: String, fragmentSrc: String) :
            this(
                name,
                "<memory>",
                mapOf(
                    GL_VERTEX_SHADER   to Pair(vertexSrc, "vertex"),
                    GL_FRAGMENT_SHADER to Pair(fragmentSrc, "fragment")
                )
            )

    constructor(filePath: String) : this(
        extractName(filePath),
        filePath,
        preprocess(File(filePath).readText())
    )

    // public API ----------------------------------------
    override fun getName() = name
    override fun bind() {
        // avoids unnecessary binding
        if (currentProgram != rendererID) {
            SubmitRender("GLShader-bind") { glUseProgram(rendererID) }
            currentProgram = rendererID
        }
    }
    override fun unbind() {
        if (currentProgram == rendererID)
            return
        SubmitRender { glUseProgram(0) }
    }

    override val reflection: ShaderReflection
        get() = _reflection

    // impl
    private var rendererID = -1
    private val vulkanSpv = LinkedHashMap<Int, ByteBuffer>()
    private val openGlSpv = LinkedHashMap<Int, ByteBuffer>()
    private var _reflection = ShaderReflection()

    private val enableCache = false
    private val timer: Timer = Timer()
    private var optimize = true

    init {
        optimize = stages.values.none { "compute" in it.type() }

        compileOrGetVulkanBinaries()
        compileOrGetOpenGLBinaries()
        createProgram()
        vulkanSpv.values.forEach { MemoryUtil.memFree(it) }
        openGlSpv.values.forEach { MemoryUtil.memFree(it) }
        vulkanSpv.clear()
        openGlSpv.clear()
        Logger.warn("Shader [${getName()}] compilation took ${timer.elapsedMillis()} ms.")
    }

    private fun compileOrGetVulkanBinaries() {
        // initialize the compiler and options
        val compiler = shaderc_compiler_initialize()
        val options = shaderc_compile_options_initialize()
        try {
            shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_2)
            /**
             * This will force object names to be retained during reflection,
             * results in larger .spv files. If shipping .spv,
             * do second compilation pass for these names and reflect them before compilation
             */
            shaderc_compile_options_set_generate_debug_info(options)
            if (optimize)
                shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance)

            for ((stage, shader) in stages) {
                vulkanSpv[stage] = compileOrLoad(
                    shader.source(),
                    stage,
                    compiler,
                    options,
                    true
                )
            }
        } finally {
            shaderc_compile_options_release(options)
            shaderc_compiler_release(compiler)
        }

        // Reflect
        val mergedUbos    = LinkedHashMap<String, UniformInfo>()
        val mergedSamplers = LinkedHashMap<String, SamplerInfo>()
        val mergedSsbos   = LinkedHashMap<String, SsboInfo>()
        for ((stage, sprv) in vulkanSpv) {
            reflect(stage, sprv, mergedUbos, mergedSamplers, mergedSsbos)
        }
        _reflection = ShaderReflection(mergedUbos, mergedSamplers, mergedSsbos)
    }

    private fun compileOrGetOpenGLBinaries() {
        val caps = GL.getCapabilities()
        // If the driver can specialize SPIR-V natively, we still need the
        // OpenGL-target SPIR-V (different entry-point decoration).
        // OPTIMIZATION: check caps once and reuse.
        @Suppress("UNUSED_VARIABLE")
        val canSpecialize = caps.OpenGL46 || caps.GL_ARB_gl_spirv

        val compiler = shaderc_compiler_initialize()
        val options = shaderc_compile_options_initialize()
        try {
            shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5)
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_zero)

            for ((stage, vSpv) in vulkanSpv) {
                val cached = cachePath(stage, vulkan = false)
                if (enableCache && Files.exists(cached)) {
                    val bytes = Files.readAllBytes(cached)
                    openGlSpv[stage] = MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
                    continue
                }

                val oglGlsl = crossCompileToGlsl(vSpv)

                val module =
                    shaderc_compile_into_spv(compiler, oglGlsl, glStageToShaderc(stage), filePath, "main", options)
                try {
                    if (shaderc_result_get_compilation_status(module) != shaderc_compilation_status_success) {
                        Logger.error(
                            "OpenGL-target compilation failed for stage $stage:\n {${
                                shaderc_result_get_error_message(
                                    module
                                )?.let { memUTF8(it) }
                            }"
                        )
                        continue
                    }

                    val size = shaderc_result_get_length(module).toInt()
                    val spv = MemoryUtil.memAlloc(size).put(shaderc_result_get_bytes(module)).flip() as ByteBuffer
                    openGlSpv[stage] = spv
                    if (enableCache) {
                        Files.createDirectories(cached.parent)
                        Files.write(cached, ByteArray(size).also { copy -> spv.get(copy); spv.rewind() })
                    }
                } finally {
                    shaderc_result_release(module)
                }
            }
        } finally {
            shaderc_compile_options_release(options)
            shaderc_compiler_release(compiler)
        }
    }

    private fun compileOrLoad(
        source: String,
        stage: Int,
        compiler: Long,
        options: Long,
        vulkan: Boolean
    ): ByteBuffer {
        val cached = cachePath(stage, vulkan)
        if (enableCache && Files.exists(cached)) {
            val bytes = Files.readAllBytes(cached)
            return MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
        }

        val module = shaderc_compile_into_spv(compiler, source, glStageToShaderc(stage), filePath, "main", options)
        try {
            val status = shaderc_result_get_compilation_status(module)
            if (status != shaderc_compilation_status_success) {
                val errMsg = shaderc_result_get_error_message(module)
                if (errMsg != null) Logger.error(memUTF8(errMsg).toString())
            }

            require(shaderc_result_get_compilation_status(module) == shaderc_compilation_status_success) {
                "Vulkan SPIR-V compilation failed for stage $stage"
            }

            val size = shaderc_result_get_length(module).toInt()
            val copy = MemoryUtil.memAlloc(size).put(shaderc_result_get_bytes(module)).flip() as ByteBuffer

            if (enableCache) {
                Files.createDirectories(cached.parent)
                Files.write(cached, ByteArray(size).also { copy.get(it) })

                copy.rewind()
            }

            return copy
        } finally {
            shaderc_result_release(module)
        }
    }

    private fun createProgram() {
        val program = glCreateProgram()
        // create an array with size same as openGLSpv to hold the IDs of shader programs
        val shaderIDs = ArrayList<Int>(openGlSpv.size)

        // TODO: move this to OpenGLCaps.kt
        val caps = GL.getCapabilities()
        val canSpecialize = caps.OpenGL46 || caps.GL_ARB_gl_spirv

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
                shaderIDs += shaderID
            } else {
                // fallback option -> convert back to GLSL and compile
                Logger.error("Error compiling shader")
            }
        }
        glLinkProgram(program)

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            Logger.error("Program link failed for shader '${getName()}':\n${glGetProgramInfoLog(program)}")
        }

        for (id in shaderIDs) {
            glDetachShader(program, id)
            glDeleteShader(id)
        }

        rendererID = program
    }

    private fun crossCompileToGlsl(vSpv: ByteBuffer): String =
        MemoryStack.stackPush().use { stack ->
            val ctx = stack.mallocPointer(1)
                .also { check(spvc_context_create(it) == SPVC_SUCCESS) }
                .first()

            val ir = stack.mallocPointer(1)
                .also {
                    check(spvc_context_parse_spirv(ctx, vSpv.asIntBuffer(), (vSpv.remaining() / 4).toLong(), it) == SPVC_SUCCESS)
                }.first()

            val comp = stack.mallocPointer(1)
                .also {
                    check(
                        spvc_context_create_compiler(ctx, SPVC_BACKEND_GLSL, ir, SPVC_CAPTURE_MODE_COPY, it)
                                == SPVC_SUCCESS
                    )
                }.first()

            val opts = stack.mallocPointer(1)
                .also { check(spvc_compiler_create_compiler_options(comp, it) == SPVC_SUCCESS) }.first()

            spvc_compiler_options_set_uint(opts, SPVC_COMPILER_OPTION_GLSL_VERSION, 450)
            spvc_compiler_install_compiler_options(comp, opts)

            val glslSrcPtr = stack.mallocPointer(1)
            spvc_compiler_compile(comp, glslSrcPtr)
            val result = memUTF8(glslSrcPtr.first())

            spvc_context_destroy(ctx)
            result
        }

    /**
     * Reflects all resource types from one SPIR-V stage and merges the results
     * into the caller-supplied maps.  Using SPVC_BACKEND_NONE means we get the
     * raw Vulkan decoration values (binding, set) with no backend-specific
     * remapping.
     *
     * Binding collisions across stages (e.g., vertex + fragment sharing a UBO
     * at the same slot) are silently de-duplicated; the last stage wins, which
     * is fine because the binding number must be identical in both stages for a
     * valid Vulkan/GL program.
     */
    private fun reflect(
        stage:          Int,
        spirv:          ByteBuffer,
        ubos:           MutableMap<String, UniformInfo>,
        samplers:       MutableMap<String, SamplerInfo>,
        ssbos:          MutableMap<String, SsboInfo>
    ) = MemoryStack.stackPush().use { stack ->
        val ctx = stack.mallocPointer(1)
            .also { check(spvc_context_create(it) == SPVC_SUCCESS) }.first()

        val ir = stack.mallocPointer(1)
            .also {
                check(
                    spvc_context_parse_spirv(ctx, spirv.asIntBuffer(), (spirv.remaining() / 4).toLong(), it)
                            == SPVC_SUCCESS
                )
            }.first()

        val compiler = stack.mallocPointer(1)
            .also {
                check(
                    spvc_context_create_compiler(ctx, SPVC_BACKEND_NONE, ir, SPVC_CAPTURE_MODE_COPY, it)
                            == SPVC_SUCCESS
                )
            }.first()

        val res = stack.mallocPointer(1)
            .also { check(spvc_compiler_create_shader_resources(compiler, it) == SPVC_SUCCESS) }.first()

        // ubos
        forEachResource(compiler, res, SPVC_RESOURCE_TYPE_UNIFORM_BUFFER, stack) { r, binding, name ->
            val typeHandle = spvc_compiler_get_type_handle(compiler, r.base_type_id())
            val size = stack.mallocPointer(1)
                .also { check(spvc_compiler_get_declared_struct_size(compiler, typeHandle, it) == SPVC_SUCCESS) }
                .first().toInt()
            ubos[name] = UniformInfo(name, size, binding)
            Logger.trace("UBO  [$stage] $name  size=$size  binding=$binding")
        }

        // samplers
        forEachResource(compiler, res, SPVC_RESOURCE_TYPE_SAMPLED_IMAGE, stack) { r, binding, name ->
            val set = spvc_compiler_get_decoration(compiler, r.id(), 2 /* SPVC_DECORATION_DESCRIPTOR_SET */)
            samplers[name] = SamplerInfo(name, binding, set)
            Logger.trace("SAMPLER [$stage] $name  binding=$binding  set=$set")
        }

        // ssbos
        forEachResource(compiler, res, SPVC_RESOURCE_TYPE_STORAGE_BUFFER, stack) { _, binding, name ->
            ssbos[name] = SsboInfo(name, binding)
            Logger.trace("SSBO [$stage] $name  binding=$binding")
        }

        spvc_context_destroy(ctx)
    }

    /**
     * Iterates every resource of [resourceType] in [res] and invokes [block]
     * for each one.  Keeps the raw pointer arithmetic in one place.
     */
    private inline fun forEachResource(
        compiler:     Long,
        res:          Long,
        resourceType: Int,
        stack:        MemoryStack,
        block:        (resource: SpvcReflectedResource, binding: Int, name: String) -> Unit
    ) {
        val listPtr = stack.mallocPointer(1)
        val cntPtr  = stack.mallocPointer(1)
        check(spvc_resources_get_resource_list_for_type(res, resourceType, listPtr, cntPtr) == SPVC_SUCCESS)

        val count    = cntPtr[0].toInt()
        val baseAddr = listPtr[0]
        val step     = SpvcReflectedResource.SIZEOF.toLong()

        for (i in 0 until count) {
            val r       = SpvcReflectedResource.create(baseAddr + i * step)
            val binding = spvc_compiler_get_decoration(compiler, r.id(), SPVC_DECORATION_BINDING)
            block(r, binding, r.nameString())
        }
    }

    private fun cachePath(stage: Int, vulkan: Boolean): Path {
        val dir = Path.of("assets/cache/shader/opengl")
        val ext = when (stage) {
            GL_VERTEX_SHADER   -> if (vulkan) ".cached_vulkan.vert" else ".cached_opengl.vert"
            GL_FRAGMENT_SHADER -> if (vulkan) ".cached_vulkan.frag" else ".cached_opengl.frag"
            GL_COMPUTE_SHADER  -> if (vulkan) ".cached_vulkan.compute" else ".cached_opengl.compute"
            else -> error("Unsupported stage $stage")
        }
        val fname = File(filePath).nameWithoutExtension + ext
        return dir.resolve(fname)
    }

    companion object {
        private fun glStageToShaderc(stage: Int) = when (stage) {
            GL_VERTEX_SHADER   -> shaderc_glsl_vertex_shader
            GL_FRAGMENT_SHADER -> shaderc_glsl_fragment_shader
            GL_COMPUTE_SHADER  -> shaderc_glsl_compute_shader
            else -> error("Unsupported GL stage $stage")
        }
        private fun extractName(path: String): String = File(path).nameWithoutExtension

        // "#type <vertex|fragment|compute>" pre‑processor.
        private fun preprocess(src: String): Map<Int, Pair<String, String>> {
            val token = "#type"
            var pos   = src.indexOf(token)
            if (pos == -1) error("No #type blocks found in shader file")
            val map = mutableMapOf<Int, Pair<String, String>>()
            while (pos != -1) {
                val eol   = src.indexOf('\n', pos)
                val type  = src.substring(pos + token.length, eol).trim()
                val stage = when (type) {
                    "vertex"   -> GL_VERTEX_SHADER
                    "fragment", "pixel" -> GL_FRAGMENT_SHADER
                    "compute" -> GL_COMPUTE_SHADER
                    else        -> error("Unknown shader type '$type'")
                }
                val nextLine = eol + 1
                pos = src.indexOf(token, nextLine)
                val code = if (pos == -1) src.substring(nextLine) else src.substring(nextLine, pos)
                map[stage] = Pair(code, type)
            }
            return map
        }
    }
}

// helpful extensions
fun PointerBuffer.first() = this[0]
fun LongBuffer.first() = this[0]

internal fun Pair<String, String>.source(): String =
    this.first
internal fun Pair<String, String>.type(): String =
    this.second