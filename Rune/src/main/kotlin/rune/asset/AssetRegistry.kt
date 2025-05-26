package rune.asset

import rune.core.UUID

object AssetRegistry {
    private val assetRegistry: Map<UUID, AssetMetadata> = mutableMapOf()
}