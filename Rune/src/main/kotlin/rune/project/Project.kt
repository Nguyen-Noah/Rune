package rune.project

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import rune.asset.AssetType
import rune.asset.manager.EditorAssetManager
import rune.core.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Serialized project settings (JSON `.rproject`). Paths are relative to [Project.root] unless absolute.
 */
@Serializable
data class RuneProject(
    val name: String = "Untitled Project",
    val assetRegistryPath: String = "assets/assetRegistry.rnr",
    val assets: String = "assets",
    val meshes: String = "assets/meshes",
    val scenes: String = "assets/scenes",
    /** Project-only GLSL; built-in renderer shaders ship in the Rune module (`rune/shaders`). */
    val shaders: String = "assets/shaders",
    val textures: String = "assets/textures",
    val skyboxes: String = "assets/skyboxes"
)

/**
 * Resolved on-disk project: root folder, paths for each asset category, and the loaded `.rproject` file.
 */
class Project private constructor(
    val root: Path,
    val projectFile: Path,
    val config: RuneProject,
    val assets: Path,
    val meshes: Path,
    val scenes: Path,
    val shaders: Path,
    val textures: Path,
    val skyboxes: Path
) {

    /** Any path relative to the project root (e.g. `"assets/foo/bar.png"`). */
    fun path(relativeToRoot: String): Path =
        root.resolve(relativeToRoot).normalize()

    /** Resolved asset registry file (config paths are relative to [root]). */
    fun assetRegistryPath(): Path =
        path(config.assetRegistryPath)

    fun resolve(type: AssetType, relative: String): Path = when (type) {
        AssetType.None -> assets.resolve(relative)
        AssetType.Scene -> scenes.resolve(relative)
        AssetType.Mesh, AssetType.StaticMesh -> meshes.resolve(relative)
        AssetType.Material, AssetType.Script -> assets.resolve(relative)
        AssetType.Texture -> textures.resolve(relative)
        AssetType.Shader -> shaders.resolve(relative)
    }

    /** mesh://, scene://, shader://, texture://, skybox:// */
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
            "mesh" -> resolve(AssetType.Mesh, rest)
            "scene" -> resolve(AssetType.Scene, rest)
            "shader" -> resolve(AssetType.Shader, rest)
            "texture" -> resolve(AssetType.Texture, rest)
            "skybox" -> skyboxes.resolve(rest).normalize()
            else -> error("Unknown asset scheme: $scheme")
        }
    }

    fun ensureDirs() {
        listOf(assets, meshes, scenes, shaders, textures, skyboxes).forEach { it.createDirectories() }
    }

    fun save() {
        val json = JSON.encodeToString(RuneProject.serializer(), config)
        projectFile.parent?.createDirectories()
        projectFile.writeText(json)
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
        mustDir(skyboxes, "skyboxes")
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

        fun findProjectFile(root: Path): Path? {
            val r = root.toAbsolutePath().normalize()
            if (!r.exists() || !r.isDirectory())
                return null

            val preferred = r.resolve("${r.fileName}$PROJECT_FILE_EXTENSION")
            if (preferred.isRegularFile())
                return preferred

            return Files.newDirectoryStream(r, "*$PROJECT_FILE_EXTENSION").use { stream ->
                stream.firstOrNull { it.isRegularFile() }
            }
        }

        fun load(rootHint: Path): Project {
            val root = rootHint.toAbsolutePath().normalize()
            require(root.exists() && root.isDirectory()) { "Project root is not a directory: $root" }
            val file = findProjectFile(root)
                ?: error("No $PROJECT_FILE_EXTENSION file found in $root")
            return loadFromFile(root, file)
        }

        fun loadFromFile(root: Path, projectFile: Path): Project {
            Logger.info("Opening ${projectFile.fileName}")
            val r = root.toAbsolutePath().normalize()
            require(projectFile.isRegularFile()) { "Not a project file: $projectFile" }
            val config = JSON.decodeFromString<RuneProject>(projectFile.readText())
            return build(r, projectFile, config)
        }

        fun init(at: Path, config: RuneProject = RuneProject()): Project {
            at.createDirectories()
            val base = safeFileName(at.name)
            val file = at.resolve("$base$PROJECT_FILE_EXTENSION")
            val json = JSON.encodeToString(RuneProject.serializer(), config)
            file.writeText(json)
            Logger.info("Created project file at ${file.toAbsolutePath()}")
            return loadFromFile(at.toAbsolutePath().normalize(), file)
        }

        private fun build(root: Path, projectFile: Path, config: RuneProject): Project {
            val env = System.getenv()
            fun abs(p: String): Path {
                val expanded = p.replace(Regex("""\$\{([^}]+)}""")) { m -> env[m.groupValues[1]] ?: "" }
                    .let { if (it.startsWith("~")) System.getProperty("user.home") + it.drop(1) else it }
                val path = Paths.get(expanded)
                return if (path.isAbsolute) path.normalize() else root.resolve(path).normalize()
            }

            val pfs = Project(
                root = root.normalize(),
                projectFile = projectFile.toAbsolutePath().normalize(),
                config = config,
                assets = abs(config.assets),
                meshes = abs(config.meshes),
                scenes = abs(config.scenes),
                shaders = abs(config.shaders),
                textures = abs(config.textures),
                skyboxes = abs(config.skyboxes),
            )
            pfs.ensureDirs()
            pfs.validate()
            return pfs
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

/**
 * Optional hint for where the game/editor should look for a project folder.
 * Override with system property `rune.project` or env `RUNE_PROJECT` (absolute or relative path).
 */
object ProjectRoots {
    fun resolve(hint: String): Path {
        System.getProperty("rune.project")?.let { return Paths.get(it).toAbsolutePath().normalize() }
        System.getenv("RUNE_PROJECT")?.let { return Paths.get(it).toAbsolutePath().normalize() }
        return Paths.get(hint).toAbsolutePath().normalize()
    }
}

/**
 * Global active [Project] for the running app. Call [open] once early (e.g. first layer) before loading assets.
 */
object ProjectManager {
    private val ref = AtomicReference<Project?>(null)
    private val assetManager = EditorAssetManager()

    val isOpen: Boolean get() = ref.get() != null

    val projectOrNull: Project? get() = ref.get()

    val current: Project
        get() = ref.get() ?: error("No project is active. Call ProjectManager.open() first.")

    /**
     * Loads `.rproject` in [root] if present; otherwise creates the folder (if needed) and an Untitled project file.
     */
    fun open(root: Path): Project {
        val normalized = root.toAbsolutePath().normalize()
        val p = when {
            !normalized.exists() -> {
                Logger.info("Project path does not exist; creating Untitled project at $normalized")
                Project.init(normalized, RuneProject(name = "Untitled Project"))
            }
            normalized.isDirectory() -> {
                val file = Project.findProjectFile(normalized)
                if (file != null) Project.loadFromFile(normalized, file)
                else {
                    Logger.info("No .rproject in $normalized; creating Untitled project")
                    Project.init(normalized, RuneProject(name = "Untitled Project"))
                }
            }
            else -> error("Project path is not a directory: $normalized")
        }
        ref.set(p)
        assetManager.loadAssetRegistry()
        return p
    }

    fun init(at: Path, config: RuneProject = RuneProject()): Project {
        val p = Project.init(at, config)
        ref.set(p)
        assetManager.loadAssetRegistry()
        return p
    }

    fun save() {
        ref.get()?.save()
    }

    fun close() {
        ref.set(null)
    }

    fun switchTo(root: Path): Project = open(root)

    fun path(relativeToRoot: String): Path = current.path(relativeToRoot)

    fun resolve(type: AssetType, path: String): Path = current.resolve(type, path)

    fun resolve(uri: String): Path = current.resolve(uri)

    fun getConfig(): RuneProject = current.config

    fun getAssetRegistryPath(): Path = current.assetRegistryPath()

    fun getEditorAssetManager(): EditorAssetManager = assetManager
}
