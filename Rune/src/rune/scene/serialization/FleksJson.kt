package rune.scene.serialization

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import rune.components.*

const val FORMAT_VERSION = 1

private val componentModule = SerializersModule {
    polymorphic(Component::class) {
        subclass(BoxCollider2DComponent::class, BoxCollider2DComponent.serializer())
        subclass(CircleCollider2DComponent::class, CircleCollider2DComponent.serializer())
        subclass(CameraComponent::class, CameraComponent.serializer())
        subclass(IDComponent::class, IDComponent.serializer())
        subclass(RigidBody2DComponent::class, RigidBody2DComponent.serializer())
        // TODO: Script Component
        subclass(SpriteRendererComponent::class, SpriteRendererComponent.serializer())
        subclass(CircleRendererComponent::class, CircleRendererComponent.serializer())
        subclass(TagComponent::class, TagComponent.serializer())
        subclass(TransformComponent::class, TransformComponent.serializer())
    }
}

val json = Json {
    prettyPrint = true
    classDiscriminator = "__type"     // easier to read: { "type": "BoxCollider2D", ... }
    encodeDefaults = false          // smaller files
    serializersModule = componentModule

    allowStructuredMapKeys = true       // required for fleks snapshots
}

@Serializable
data class RuneSceneFile(
    val version: Int = FORMAT_VERSION,
    val snapshot: Map<Entity, Snapshot>
)