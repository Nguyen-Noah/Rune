package rune.project

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rune.asset.AssetType
import rune.asset.manager.EditorAssetManager
import rune.core.Logger
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * When making changes to this file, such as adding asset types, add the new asset to the following:
 *
 * 1. RuneProject.Roots
 * 2. Project constructor
 * 3. Project.resolve()
 * 4. Project.ensureDirs()
 * 5. Project.validate()
 * 6. Project.load()
 */


@Serializable
data class RuneProject(
    val name: String = "Untitled Project",

    val assetRegistryPath: String = "assets/assetRegistry.rnr",

    val assets: String = "assets",
    val meshes: String = "assets/meshes",
    val scenes: String = "assets/scenes",
    val shaders: String = "assets/shaders",
    val textures: String = "assets/textures"
)

class Project private constructor(
    val root: Path,
    val config: RuneProject,

    val assets: Path,
    val meshes: Path,
    val scenes: Path,
    val shaders: Path,
    val textures: Path
) {

    // resolve by kind + relative path
    fun resolve(type: AssetType, relative: String): Path = when(type) {
        AssetType.Mesh -> meshes.resolve(relative)
        AssetType.Scene -> scenes.resolve(relative)
        AssetType.Shader -> shaders.resolve(relative)
        AssetType.Texture -> textures.resolve(relative)
        else -> {
            Logger.warn("Unknown asset type: $type")
            TODO()
        }
    }

    /** URI-like convenience: mesh://, scene://, shader://, texture://, skybox://, cache:// */
    fun resolve(uri: String): Path {
        val i = uri.indexOf("://")
        if (i < 0) {
            Logger.warn("Not an asset URI: $uri")
            val p = Paths.get(uri)
            return if (p.isAbsolute) p.normalize() else assets.resolve(uri).normalize()
        }
        val scheme = uri.substring(0, i).lowercase(Locale.ROOT)
        val rest = uri.substring(i + 3)
        return when (scheme) {
            "mesh"    -> resolve(AssetType.Mesh, rest)
            "scene"   -> resolve(AssetType.Scene, rest)
            "shader"  -> resolve(AssetType.Shader, rest)
            "texture" -> resolve(AssetType.Texture, rest)
            else -> error("Unknown asset scheme: $scheme")
        }
    }

    fun ensureDirs() {
        listOf(assets, meshes, scenes, shaders, textures).forEach { it.createDirectories() }
    }

    fun save() {
        val file = root.resolve("${root.fileName}$PROJECT_FILE_EXTENSION")
        val json = JSON.encodeToString(RuneProject.serializer(), config)
        file.writeText(json)
    }

    private fun validate() {
        fun mustDir(p: Path, name: String) {
            if (!p.exists()) return
            require(p.isDirectory()) { "Configured $name is not a directory: $p" }
        }
        mustDir(assets, "assets")
        mustDir(meshes, "meshes")
        mustDir(scenes, "scenes")
        mustDir(shaders, "shaders")
        mustDir(textures, "textures")
    }

    companion object {
        const val PROJECT_FILE_EXTENSION = ".rproject"
        @OptIn(ExperimentalSerializationApi::class)
        private val JSON = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        fun load(rootHint: Path): Project {
            Logger.info("Opening ${rootHint.fileName}")
            val root = rootHint.toAbsolutePath().normalize()
            val preferred = rootHint.resolve("${root.fileName}$PROJECT_FILE_EXTENSION")
            val file = when {
                preferred.exists() -> preferred
                else -> Files.newDirectoryStream(root, "*$PROJECT_FILE_EXTENSION").use { it.firstOrNull() }
                    ?: error("No $PROJECT_FILE_EXTENSION file found in $rootHint")
            }

            require(file.exists()) { "Missing ${file.toAbsolutePath()}" }
            val config = JSON.decodeFromString<RuneProject>(file.readText())

            val env = System.getenv()
            fun abs(p: String): Path {
                val expanded = p.replace(Regex("""\$\{([^}]+)}""")) { m -> env[m.groupValues[1]] ?: "" }
                    .let { if (it.startsWith("~")) System.getProperty("user.home") + it.drop(1) else it }
                val path = Paths.get(expanded)
                return if (path.isAbsolute) path.normalize() else root.resolve(path).normalize()
            }

            val pfs = Project(
                root = root.normalize(),
                config = config,
                assets = abs(config.assets),
                meshes = abs(config.meshes),
                scenes = abs(config.scenes),
                shaders = abs(config.shaders),
                textures = abs(config.textures),
            )
            pfs.ensureDirs()
            pfs.validate()
            return pfs
        }

        fun init(at: Path, config: RuneProject = RuneProject()): Project {
            at.createDirectories()
            val base = safeFileName(at.name)
            val file = at.resolve("$base$PROJECT_FILE_EXTENSION")
            val json = JSON.encodeToString<RuneProject>(config)
            file.writeText(json)
            Logger.info("Created new project at ${at.toAbsolutePath()}")
            return load(at)
        }

        fun discoverRoot(): Path {
            System.getProperty("rune.root")?.let { return Paths.get(it).toAbsolutePath().normalize() }
            System.getenv("RUNE_ROOT")?.let { return Paths.get(it).toAbsolutePath().normalize() }

            var p = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
            while (true) {
                val preferred = p.resolve("${p.fileName}$PROJECT_FILE_EXTENSION")
                if (preferred.exists()) return p
                val any = Files.newDirectoryStream(p, "*$PROJECT_FILE_EXTENSION").use { it.firstOrNull() }
                if (any != null) return p
                p = p.parent ?: break
            }
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        }

        private fun safeFileName(name: String): String =
            name.trim()
                .replace(Regex("""[^\w\- .]"""), "")
                .ifBlank { "Project" }
                .replace(' ', '_')
    }
}

object ProjectManager {
    private val ref = AtomicReference<Project?>(null)
    // TODO: make this an instance of AssetManager then add RuntimeAssetManager
    private val assetManager = EditorAssetManager()

    /** True if a project is currently active */
    val isOpen: Boolean get() = ref.get() != null

    val current: Project
        get() = ref.get() ?: error("No project is active. Call ProjectManager.open() or init()")

    fun open(root: Path): Project {
        val p = try {
            Logger.info("Loaded project $root.")
            Project.load(root)
        } catch (e: NoSuchFileException) {
            Logger.warn("Project $root not found. Creating new project.")
            Project.init(root)
        }
        ref.set(p)
        assetManager.loadAssetRegistry()
        return p
    }

    fun init(at: Path, config: RuneProject = RuneProject()): Project {
        val p = Project.init(at, config)
        ref.set(p)
        return p
    }

    fun save() { ref.get()?.save() }
    fun close() { ref.set(null) }
    fun switchTo(root: Path): Project = open(root)

    fun resolve(type: AssetType, path: String): Path = current.resolve(type, path)
    fun resolve(uri: String): Path = current.resolve(uri)

    fun getConfig() = current.config
    fun getAssetRegistryPath() = getConfig().assetRegistryPath.toPath()

    fun getEditorAssetManager() = assetManager as EditorAssetManager
}

private fun String.toPath() = Path.of(this)