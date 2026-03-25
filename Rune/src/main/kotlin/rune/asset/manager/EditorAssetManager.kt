package rune.asset.manager

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rune.asset.Asset
import rune.asset.AssetMetadata
import rune.asset.AssetRegistry
import rune.core.Logger
import rune.core.UUID
import rune.project.ProjectManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class EditorAssetManager {
    private val assetRegistry = AssetRegistry()
    init {
        assetRegistry[UUID()] = AssetMetadata(UUID(), "test", "test")
    }

    fun writeToFile() {
        val j = Json.encodeToString(assetRegistry)
    }

    fun loadAssetRegistry() {
        Logger.info("Loading Asset Registry")

        val assetRegistryPath = ProjectManager.getAssetRegistryPath()
        if (Files.notExists(assetRegistryPath)) {
            Logger.warn("Asset Registry Path not found")
            return
        }
    }

    fun importAsset(path: Path): UUID {
        val absolutePath = path.toAbsolutePath()
        println("SKDJHFSDJKFHJKDSHF$absolutePath")

        return UUID()
    }

    //filename: String, directoryPath: String, noinline factory: () -> T
    inline fun <reified T> createAsset() where T : Asset {
//        val metadata = AssetMetadata(
//            UUID(),
//            path = if (directoryPath.isEmpty() || directoryPath == ".") {
//                filename
//            } else {
//                getRelativePath("$directoryPath/$filename")
//            }
//            type = when ( val t = T::class.objectInstance) {
//                is StaticTypedAsset
//            }
//        )
        println(T::class.objectInstance)
    }
}
