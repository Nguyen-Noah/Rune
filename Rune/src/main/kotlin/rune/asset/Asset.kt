package rune.asset

import rune.core.UUID

abstract class Asset {
    val id: UUID = UUID(0UL)
    val type: AssetType = AssetType.None
    fun isValid(): Boolean = true

    // dependencies this asset references (StaticMesh -> MeshSource UUID)
    fun dependencies(): Set<UUID> = emptySet()

    // called when a dependency was updated
    fun updateDependency(changed: UUID) {}
}
