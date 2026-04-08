package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3

open class RuneCamera {
    var projection: Mat4 = Mat4(1f)
        protected set

    open var position: Vec3 = Vec3(1f)
        protected set

    constructor()

    constructor(projection: Mat4) {
        this.projection = projection
    }
}