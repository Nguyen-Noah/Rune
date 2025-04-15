package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import rune.scene.ScriptableEntity

class ScriptComponent : Component<ScriptComponent> {
    lateinit var instance: ScriptableEntity
    val isBound: Boolean
        get() = this::instance.isInitialized

    override fun type() = ScriptComponent

    companion object : ComponentType<ScriptComponent>()

    /**
     * <ScriptEntity> is the raw base class, so set
     * instance to the return value of the init method
     */
    fun bind(script: ScriptableEntity): ScriptComponent {
        val initMethod = script.javaClass.getMethod("init")
        instance = initMethod.invoke(script) as ScriptableEntity

        return this
    }
}