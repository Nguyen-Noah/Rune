package rune.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import rune.script.ScriptConfiguration
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Rune Script",
    fileExtension = "runescript.kts",
    compilationConfiguration = ScriptConfiguration::class
)
abstract class ScriptableEntity {
    lateinit var entity: Entity

    // internal lets the scene assign itself
    lateinit var scene: Scene

    inline fun <reified T : Component<T>> getComponent(type: ComponentType<T>): T {
        return with(scene.world) {
            entity[type]
        }
    }

    open fun onCreate() {}
    open fun onUpdate(dt: Float) {}
    open fun onDestroy() {}
}