package rune.asset

import kotlinx.serialization.Serializable

@Serializable
enum class AssetType {
    None,
    Scene,
    Mesh,
    StaticMesh,
    Material,
    Texture,
    Script,
    Shader
}