package rune.asset.manager

import rune.core.UUID

interface IAssetManager {
    fun getAssetType(id: UUID)
    fun getAsset(id: UUID)
}