package rune.scene

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityUpdateContext
import com.github.quillraven.fleks.World

@JvmInline
value class RuneEntity(val raw: Entity) {
    val id: Int get() = raw.id
    val version: UInt get() = raw.version

    companion object {
        val NONE = RuneEntity(Entity.NONE)
    }

    override fun toString(): String = "RuneEntity(id=$id, version=$version"
}