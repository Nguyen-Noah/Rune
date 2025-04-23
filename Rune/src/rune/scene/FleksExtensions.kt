package rune.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

fun <C : Component<C>> World.hasComponent(e: Entity, type: ComponentType<C>) = e.has(type)
inline fun <reified C : Component<C>> World.getComponent(e: Entity, type: ComponentType<C>) = e[type]