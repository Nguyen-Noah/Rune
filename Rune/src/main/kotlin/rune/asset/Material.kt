package rune.asset

import glm_.vec4.Vec4
import rune.renderer.gpu.Shader
import rune.renderer.gpu.Texture2D

data class Material(
    val textures: Array<Texture2D?>,
    val ambient: Vec4,
    val diffuse: Vec4,
    val specular: Vec4,
    val shader: Shader
) {
    override fun toString(): String {
        return "Material(" +
                " texture=$textures," +
                " ambient=$ambient," +
                " diffuse=$diffuse," +
                " specular=$specular" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Material

        if (!textures.contentEquals(other.textures)) return false
        if (ambient != other.ambient) return false
        if (diffuse != other.diffuse) return false
        if (specular != other.specular) return false
        if (shader != other.shader) return false

        return true
    }

    override fun hashCode(): Int {
        var result = textures.contentHashCode()
        result = 31 * result + ambient.hashCode()
        result = 31 * result + diffuse.hashCode()
        result = 31 * result + specular.hashCode()
        result = 31 * result + shader.hashCode()
        return result
    }
}