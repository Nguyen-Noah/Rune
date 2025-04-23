package rune.scene.serialization

import glm_.vec3.Vec3
import glm_.vec4.Vec4
import kotlinx.serialization.Serializable

@Serializable
sealed interface RuneComponent

@Serializable
data class RuneScene(
    val Scene: String,
    val Entities: List<RuneEntity>
) : RuneComponent

@Serializable
data class RuneEntity(
    val Entity: Long,
    val Components: List<RuneComponent> = emptyList()
) : RuneComponent

@Serializable
data class RuneTagComponent(val Tag: String) : RuneComponent

@Serializable
data class RuneTransformComponent(
    @Serializable(with = Vec3AsList::class) val Translation: Vec3,
    @Serializable(with = Vec3AsList::class) val Rotation: Vec3,
    @Serializable(with = Vec3AsList::class) val Scale: Vec3
) : RuneComponent

@Serializable
data class RuneCameraComponent(
    val Camera: RuneCamera,
    val Primary: Boolean,
    val FixedAspectRatio: Boolean
) : RuneComponent

@Serializable
data class RuneCamera(
    val ProjectionType: Int,
    val PerspectiveFOV: Float,
    val PerspectiveNear: Float,
    val PerspectiveFar: Float,
    val OrthographicSize: Float,
    val OrthographicNear: Float,
    val OrthographicFar: Float
)

@Serializable
data class RuneSpriteRendererComponent(
    @Serializable(with = Vec4AsList::class) val Color: Vec4
) : RuneComponent