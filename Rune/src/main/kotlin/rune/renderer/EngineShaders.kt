package rune.renderer

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Classpath `rune/shaders` resources, extracted to disk for [rune.platforms.opengl.OpenGLShader]. */
object EngineShaders {
    private const val RESOURCE_PREFIX = "rune/shaders"

    private val names = listOf(
        "EquirectangularToSkybox.glsl",
        "Geometry.glsl",
        "Terrain.glsl",
        "Rune_PBR.glsl",
        "Renderer2D_Quad.glsl",
        "Renderer2D_Circle.glsl",
        "Renderer2D_Line.glsl",
        "StaticMesh.glsl",
        "Tonemap.glsl",
        "Composite2D.glsl",

        "common.glsl"
    )

    private var root: Path? = null

    fun root(): Path {
        val cached = root
        if (cached != null && Files.isDirectory(cached)) return cached

        val dir = Path.of(System.getProperty("java.io.tmpdir"), "rune3d-engine-shaders")
        Files.createDirectories(dir)

        val loader = EngineShaders::class.java.classLoader
        val resources = loader.getResources(RESOURCE_PREFIX)

        for (url in resources.asSequence()) {
            when (url.protocol) {
                "file" -> extractFromFileSystem(Path.of(url.toURI()), dir)
                "jar" -> extractFromJar(url, dir)
            }
        }

        root = dir
        return dir
    }

    private fun extractFromFileSystem(resourceDir: Path, targetDir: Path) {
        Files.walk(resourceDir)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".glsl") }
            .forEach { file ->
                val relative = resourceDir.relativize(file)
                val target = targetDir.resolve(relative.toString())
                Files.createDirectories(target.parent)
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
            }
    }

    /**
     * Once packaged as a JAR, searching the filesystem for shaders will not work, so we
     * need to store them in a temporary directory, usually
     * C://Users/{user}/AppData/Local/temp
     */
    private fun extractFromJar(url: java.net.URL, targetDir: Path) {
        // url looks like jar:file:/path/to.jar!/rune/shaders
        val jarUri = java.net.URI("jar", url.toString().substringBefore("!"), null)
        val env = mapOf<String, String>()

        val fs = try {
            java.nio.file.FileSystems.newFileSystem(jarUri, env)
        } catch (_: java.nio.file.FileSystemAlreadyExistsException) {
            java.nio.file.FileSystems.getFileSystem(jarUri)
        }

        val shaderRoot = fs.getPath(RESOURCE_PREFIX)
        Files.walk(shaderRoot)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".glsl") }
            .forEach { file ->
                val relative = shaderRoot.relativize(file).toString()
                val target = targetDir.resolve(relative)
                Files.createDirectories(target.parent)
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
            }
    }

    fun pathFor(fileName: String): String = root().resolve(fileName).toString()
}
