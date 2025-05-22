package rune.scene.serialization

import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import rune.core.UUID
import rune.renderer.gpu.Texture2D
import rune.renderer.renderer3d.*
import rune.scene.ProjectionType
import rune.scene.SceneCamera

object Vec2AsList : KSerializer<Vec2> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor = buildSerialDescriptor("Vec2", SerialKind.CONTEXTUAL) {
        element<Float>("x")
        element<Float>("y")
    }
    override fun serialize(encoder: Encoder, value: Vec2) {
        encoder.encodeSerializableValue(ListSerializer(Float.serializer()),
            listOf(value.x, value.y))
    }
    override fun deserialize(decoder: Decoder): Vec2 {
        val (x, y) = decoder.decodeSerializableValue(ListSerializer(Float.serializer()))
        return Vec2(x, y)
    }
}
object Vec3AsList : KSerializer<Vec3> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor = buildSerialDescriptor("Vec3", SerialKind.CONTEXTUAL) {
        element<Float>("x")
        element<Float>("y")
        element<Float>("z")
    }
    override fun serialize(encoder: Encoder, value: Vec3) {
        encoder.encodeSerializableValue(ListSerializer(Float.serializer()),
            listOf(value.x, value.y, value.z))
    }
    override fun deserialize(decoder: Decoder): Vec3 {
        val (x, y, z) = decoder.decodeSerializableValue(ListSerializer(Float.serializer()))
        return Vec3(x, y, z)
    }
}
object Vec4AsList : KSerializer<Vec4> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor = buildSerialDescriptor("Vec4", SerialKind.CONTEXTUAL) {
        element<Float>("x"); element<Float>("y"); element<Float>("z"); element<Float>("w")
    }
    override fun serialize(encoder: Encoder, value: Vec4) =
        encoder.encodeSerializableValue(ListSerializer(Float.serializer()),
            listOf(value.x, value.y, value.z, value.w))

    override fun deserialize(decoder: Decoder): Vec4 {
        val (x, y, z, w) = decoder.decodeSerializableValue(ListSerializer(Float.serializer()))
        return Vec4(x, y, z, w)
    }
}

object SceneCameraSerializer : KSerializer<SceneCamera> {

    /* the shape that is saved to disk */
    @Serializable
    private data class Surrogate(
        val ProjectionType: Int,
        val PerspectiveFOV: Float,
        val PerspectiveNear: Float,
        val PerspectiveFar: Float,
        val OrthographicSize: Float,
        val OrthographicNear: Float,
        val OrthographicFar: Float
    )

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SceneCamera") {
        Surrogate.serializer().descriptor.elementNames.forEach { elem ->
            element<Float>(elem)
        }
    }

    override fun serialize(encoder: Encoder, value: SceneCamera) {
        val dto = Surrogate(
            ProjectionType   = value.projectionType.ordinal,
            PerspectiveFOV   = value.perspectiveFOV,
            PerspectiveNear  = value.perspectiveNear,
            PerspectiveFar   = value.perspectiveFar,
            OrthographicSize = value.orthographicSize,
            OrthographicNear = value.orthographicNear,
            OrthographicFar  = value.orthographicFar
        )
        encoder.encodeSerializableValue(Surrogate.serializer(), dto)
    }

    override fun deserialize(decoder: Decoder): SceneCamera {
        val dto = decoder.decodeSerializableValue(Surrogate.serializer())
        return SceneCamera().apply {
            projectionType    = ProjectionType.fromInt(dto.ProjectionType)
            perspectiveFOV    = dto.PerspectiveFOV
            perspectiveNear   = dto.PerspectiveNear
            perspectiveFar    = dto.PerspectiveFar
            orthographicSize  = dto.OrthographicSize
            orthographicNear  = dto.OrthographicNear
            orthographicFar   = dto.OrthographicFar
        }
    }
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID =
        UUID(decoder.decodeString().toULong())
}

object Texture2DPath : KSerializer<Texture2D?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Texture2D", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Texture2D?) {
        value?.assetPath?.let(encoder::encodeString) ?: encoder.encodeNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Texture2D? {
        return decoder.decodeNullableSerializableValue(String.serializer())?.let { Texture2D.create(it) }
    }
}


object AABBSerializer : KSerializer<AABB> {
    @Serializable
    private data class Surrogate(
        @Serializable(with = Vec3AsList::class) val min: Vec3,
        @Serializable(with = Vec3AsList::class) val max: Vec3
    )

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AABB") {
            element<Vec3>("Min")   // re-uses Vec3AsList
            element<Vec3>("Max")
        }

    override fun serialize(encoder: Encoder, value: AABB) =
        encoder.encodeSerializableValue(
            Surrogate.serializer(),
            Surrogate(value.min, value.max)
        )

    override fun deserialize(decoder: Decoder): AABB {
        val dto = decoder.decodeSerializableValue(Surrogate.serializer())
        return AABB(dto.min, dto.max)
    }
}
