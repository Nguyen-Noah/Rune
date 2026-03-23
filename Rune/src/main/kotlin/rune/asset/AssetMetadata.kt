package rune.asset

import kotlinx.serialization.Serializable
import rune.core.UUID

enum class AssetStatus { Unknown, Loading, Ready }

@Serializable
data class AssetMetadata(
    var id: UUID,     // id used to match rune asset to source
    val path: String,
    val type: String,
    val status: AssetStatus = AssetStatus.Unknown,
    val isDataLoaded: Boolean = false,
    val fileLastWriteTime: Long = 0L
)
