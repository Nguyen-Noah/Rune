package rune.components

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Rigidbody2D")
class RigidBody2DComponent(
    var type: BodyType = BodyType.Static,
    var fixedRotation: Boolean = false,

    @Transient
    var runtimeBody: Body? = null
) : Component<RigidBody2DComponent>, CopyableComponent<RigidBody2DComponent> {

    constructor(other: RigidBody2DComponent) : this(
        type = other.type,
        fixedRotation = other.fixedRotation,
        runtimeBody = other.runtimeBody
    )

    override fun type() = RigidBody2DComponent

    override fun copy(): RigidBody2DComponent = RigidBody2DComponent(this)

    companion object : ComponentType<RigidBody2DComponent>() {
        enum class BodyType(private val type: Int) {
            Static(0),
            Dynamic(1),
            Kinematic(2);
            companion object {
                fun fromInt(value: Int) = entries.first { it.type == value }
            }
        }
    }
}

// extension to parse to Box2D enum type
fun RigidBody2DComponent.Companion.BodyType.toBox2d(): BodyType = when (this) {
    RigidBody2DComponent.Companion.BodyType.Static -> BodyType.StaticBody
    RigidBody2DComponent.Companion.BodyType.Dynamic -> BodyType.DynamicBody
    RigidBody2DComponent.Companion.BodyType.Kinematic -> BodyType.KinematicBody
}