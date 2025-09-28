package rune.asset.manager

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rune.asset.AssetRegistry
import rune.core.Logger
import rune.project.ProjectManager
import java.nio.file.Files

class EditorAssetManager {
    private val assetRegistry = AssetRegistry()

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
}
