package rune.utils

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos

data class DecomposedTransform(
    val translation: Vec3,
    val rotation: Vec3,
    val scale: Vec3
)

fun decomposeTransform(transform: Mat4, epsilon: Float = glm.epsilonF): DecomposedTransform? {
    // working on a copy
    val m = Mat4(transform)

    // 1. make sure w != 0
    if (abs(m[3][3]) < epsilon) return null

    // 2 strip off any perspective skew (rare in TRS)
    if (abs(m[0][3]) > epsilon ||
        abs(m[1][3]) > epsilon ||
        abs(m[2][3]) > epsilon) {
        // zero out the 3 perspective elements in col 0..2, keep m[3][3]=1
        (0..2).forEach { col ->
            m[col] = m[col].also { it[3] = 0f }
        }
        m[3] = Vec4(0f, 0f, 0f, 1f)
    }

    // 3. pull translation from col 3, then zero it out
    val translation = Vec3(m[3].x, m[3].y, m[3].z)
    m[3] = m[3].also { it.x = 0f; it.y = 0f; it.z = 0f }

    // 4. extract the upper 3x3 rows for S/R
    var row0 = Vec3(m[0].x, m[0].y, m[0].z)
    var row1 = Vec3(m[1].x, m[1].y, m[1].z)
    var row2 = Vec3(m[2].x, m[2].y, m[2].z)

    // 5. scale = lengths of those rows, then normalize them
    val scaleX = glm.length(row0); row0 = row0 / scaleX
    val scaleY = glm.length(row1); row1 = row1 / scaleY
    val scaleZ = glm.length(row2); row2 = row2 / scaleZ

    // 6. rotation: extract Euler angles from the orthogonal matrix
    val rotY = asin(-row0.z)
    val cosY = cos(rotY)

    val rotX: Float
    val rotZ: Float
    if (abs(cosY) > epsilon) {
        // normal case
        rotX = atan2(row1.z, row2.z)
        rotZ = atan2(row0.y, row0.x)
    } else {
        // gimbal-lock fallback
        rotX = atan2(-row2.x, row1.y)
        rotZ = 0f
    }

    return DecomposedTransform(
        translation = translation,
        rotation = Vec3(rotX, rotY, rotZ),
        scale = Vec3(scaleX, scaleY, scaleZ)
    )
}