package rune.scene.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import rune.components.ScriptComponent
import rune.scene.ScriptableEntity

class ScriptSystem : IteratingSystem( family { all(ScriptComponent) } ) {
    override fun onTickEntity(entity: Entity) {
        //entity[ScriptComponent].instance.onUpdate()
        val scriptComp = entity[ScriptComponent]
        if (scriptComp.isBound) {
            scriptComp.instance.onUpdate(0.5f)
        }
    }
}