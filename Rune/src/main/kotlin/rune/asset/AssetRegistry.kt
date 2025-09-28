package rune.asset

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import rune.core.UUID

@Serializable(with = AssetRegistrySerializer::class)
class AssetRegistry(
    private val backing: MutableMap<UUID, AssetMetadata> = mutableMapOf()
) : MutableMap<UUID, AssetMetadata> by backing

// Surrogate for the desired JSON shape
@Serializable
private data class AssetRegistryWrapper(
    val assets: Map<UUID, AssetMetadata>
)

object AssetRegistrySerializer : KSerializer<AssetRegistry> {
    // Reuse compiler-generated serializer for the wrapper
    private val wrapperSerializer = AssetRegistryWrapper.serializer()
    override val descriptor: SerialDescriptor = wrapperSerializer.descriptor

    override fun serialize(encoder: Encoder, value: AssetRegistry) {
        val wrapper = AssetRegistryWrapper(assets = value.toMap())
        encoder.encodeSerializableValue(wrapperSerializer, wrapper)
    }

    override fun deserialize(decoder: Decoder): AssetRegistry {
        val wrapper = decoder.decodeSerializableValue(wrapperSerializer)
        return AssetRegistry(wrapper.assets.toMutableMap())
    }
}