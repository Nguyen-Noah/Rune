package rune.scene.serialization

import glm_.vec3.Vec3
import glm_.vec4.Vec4
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
