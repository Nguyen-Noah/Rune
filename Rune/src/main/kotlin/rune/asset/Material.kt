package rune.asset

import glm_.vec4.Vec4
import rune.renderer.gpu.Texture2D

data class Material(
    val texture: Texture2D,
    val ambient: Vec4,
    val diffuse: Vec4,
    val specular: Vec4
) {
    override fun toString(): String {
        return "Material(" +
                "texture=$texture," +
                " ambient=$ambient," +
                " diffuse=$diffuse," +
                " specular=$specular" +
                ")"
    }
}