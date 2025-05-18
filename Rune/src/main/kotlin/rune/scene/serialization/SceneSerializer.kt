package rune.scene.serialization

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import rune.core.Logger
import rune.scene.Scene
import java.io.File

class SceneSerializer(private val scene: Scene) {
    /* save the entire world to JSON */
    fun serialize(path: String) {
        val file = RuneSceneFile(
            version = FORMAT_VERSION,
            snapshot = scene.world.snapshot()
        )

        File(path).apply {
            parentFile?.mkdirs()
            writeText(json.encodeToString(RuneSceneFile.serializer(), file))
        }
        Logger.info("Scene saved: $path")
    }

    fun deserialize(path: String) {
        val raw = File(path).readText()
        val file = json.decodeFromString(RuneSceneFile.serializer(), raw)

        when (file.version) {
            1 -> loadV1(file.snapshot)
            else -> error("Unsupported scene file version ${file.version}")
        }
        Logger.info("Scene loaded: $path")
    }

    private fun loadV1(snapshot: Map<Entity, Snapshot>) {
        with(scene.world) {
            removeAll(clearRecycled = true)
            loadSnapshot(snapshot)
        }
    }
}