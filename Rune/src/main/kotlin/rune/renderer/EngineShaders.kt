package rune.renderer

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Classpath `rune/shaders` resources, extracted to disk for [rune.platforms.opengl.OpenGLShader]. */
object EngineShaders {
    private const val RESOURCE_PREFIX = "rune/shaders"

    private val names = listOf(
        "EquirectangularToSkybox.glsl",
        "Skybox.glsl",
        "Geometry.glsl",
        "Terrain.glsl",
        "Rune_PBR.glsl",
        "Renderer2D_Quad.glsl",
        "Renderer2D_Circle.glsl",
        "Renderer2D_Line.glsl",
        "StaticMesh.glsl",
        "AutoExposure.glsl",
        "Grid.glsl",
        "Tonemap.glsl",
        "Composite2D.glsl"
    )

    private var root: Path? = null

    fun root(): Path {
        val cached = root
        if (cached != null && Files.isDirectory(cached)) return cached

        val dir = Path.of(System.getProperty("java.io.tmpdir"), "rune3d-engine-shaders")
        Files.createDirectories(dir)
        val loader = EngineShaders::class.java.classLoader
        for (name in names) {
            val resource = "$RESOURCE_PREFIX/$name"
            val stream = loader.getResourceAsStream(resource)
                ?: error("Missing engine shader resource: $resource")
            stream.use {
                Files.copy(it, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        root = dir
        println(root)
        return dir
    }

    fun pathFor(fileName: String): String = root().resolve(fileName).toString()
}
