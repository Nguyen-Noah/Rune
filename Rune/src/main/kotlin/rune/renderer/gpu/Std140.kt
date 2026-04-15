package rune.renderer.gpu

/**
 * OpenGL std140 uniform block layout. Field order and packing must match the GLSL `layout(std140)` block.
 *
 * Use [Std140Layout.build] for ad-hoc layouts, or [Std140Layouts] for engine UBOs that mirror [common.glsl].
 */
enum class Std140Type(val size: Int, val align: Int) {
    Int32(4, 4),
    /** GLSL `bool` in std140: 32-bit 0 / non-zero, same size/align as [Int32]. */
    Bool(4, 4),
    Float32(4, 4),
    Vec2(8, 8),
    /** Occupies one vec4 slot (xyz + implicit padding). */
    Vec3(16, 16),
    Vec4(16, 16),
    /** Column-major; three vec4 columns. */
    Mat3(48, 16),
    Mat4(64, 16),
}

class Std140Layout private constructor(
    val size: Int,
    private val offsets: Map<String, Int>,
) {
    operator fun get(name: String): Int =
        offsets[name] ?: error("Unknown std140 member '$name'. Declared: ${offsets.keys}")

    companion object {
        fun build(block: Builder.() -> Unit): Std140Layout = Builder().apply(block).finish()
    }

    class Builder internal constructor() {
        private var cursor = 0
        private val named = linkedMapOf<String, Int>()

        private fun alignTo(alignment: Int) {
            require(alignment > 0 && (alignment and (alignment - 1)) == 0) {
                "alignment must be a power of two"
            }
            cursor = (cursor + alignment - 1) and (alignment - 1).inv()
        }

        /**
         * One vec4 slot: `vec3` + following `float` (std140 packs these in 16 bytes).
         * Matches e.g. `vec3 color; float diffuseIntensity;`.
         */
        fun vec3PlusFloat(name: String? = null): Int {
            alignTo(16)
            val o = cursor
            if (name != null) named[name] = o
            cursor += 16
            return o
        }

        fun field(type: Std140Type, name: String? = null): Int {
            alignTo(type.align)
            val o = cursor
            if (name != null) named[name] = o
            cursor += type.size
            return o
        }

        fun int(name: String? = null) = field(Std140Type.Int32, name)
        fun bool(name: String? = null) = field(Std140Type.Bool, name)
        fun float(name: String? = null) = field(Std140Type.Float32, name)
        fun vec2(name: String? = null) = field(Std140Type.Vec2, name)
        fun vec3(name: String? = null) = field(Std140Type.Vec3, name)
        fun vec4(name: String? = null) = field(Std140Type.Vec4, name)
        fun mat3(name: String? = null) = field(Std140Type.Mat3, name)
        fun mat4(name: String? = null) = field(Std140Type.Mat4, name)

        internal fun finish(): Std140Layout {
            alignTo(16)
            return Std140Layout(cursor, named.toMap())
        }
    }
}

/**
 * Layouts for UBOs declared in [rune.shaders.include.common] and related shaders.
 */
object Std140Layouts {
    /** [common.glsl] `Camera` */
    val Camera = Std140Layout.build {
        mat4("u_ViewProjection")
        mat4("u_SkyProjection")
        vec3("u_CameraPos")
    }

    /** Single `mat4 u_ModelTransform` transform UBO. */
    val Transform = Std140Layout.build {
        mat4("u_ModelTransform")
    }

    /** [Geometry.glsl] UV channel 0/1 per map (std140). */
    val GeometryUvSets = Std140Layout.build {
        int("u_AlbedoUvSet")
        int("u_NormalUvSet")
        int("u_SpecularUvSet")
    }

    /** [common.glsl] `PBRMaterial` / `Material` struct (three vec4). */
    val PbrMaterial = Std140Layout.build {
        vec4("Albedo")
        vec4("Diffuse")
        vec4("Specular")
    }

    /**
     * [common.glsl] `DirectionalLight` inside `DirectionalLights`:
     * `vec3 color; float diffuseIntensity; vec3 direction;`
     */
    val DirectionalLights = Std140Layout.build {
        vec3PlusFloat("colorAndIntensity")
        vec3("direction")
    }

    /** [common.glsl] `RendererSettings` */
    val RendererSettings = Std140Layout.build {
        int("aaMethod")
        int("toneMapper")
        float("exposure")
        float("gamma")
        float("bloomIntensity")
        float("vignetteStrength")
        bool("enableCelShading")
        float("specularBandWidth")
        float("rimWidth")
        float("rimIntensity")
        float("specularColorIntensity")
        float("rimColorIntensity")
    }
}
