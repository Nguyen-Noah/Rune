package rune.renderer

import rune.components.RigidBody2DComponent

enum class TextureType(private val slot: Int) {
    Albedo(0),
    Normal(1),
    Specular(2);
    //AmbientOcclusion(3),
    //Roughness(4),
    //Metallic(5),
    //Unknown(6)

    companion object {
        fun fromInt(value: Int) = entries.first { it.slot == value }
    }
}