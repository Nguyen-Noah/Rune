package rune.asset

import rune.core.UUID

class AssetRegistry {
    private val assetRegistry: MutableMap<UUID, AssetMetadata> = mutableMapOf()

    fun get(id: UUID) = assetRegistry[id]
    fun set(id: UUID, metadata: AssetMetadata) { assetRegistry[id] = metadata }
    fun contains(id: UUID) = assetRegistry.containsKey(id)
    fun remove(id: UUID) { assetRegistry.remove(id) }
    fun clear() { assetRegistry.clear() }
}