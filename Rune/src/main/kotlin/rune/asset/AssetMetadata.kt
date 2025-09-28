package rune.asset

import kotlinx.serialization.Serializable
import rune.core.UUID

@Serializable
data class AssetMetadata(
    var uuid: UUID,     // id used to match rune asset to source
    val path: String,
    val type: String
)