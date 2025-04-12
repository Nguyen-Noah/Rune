package rune.renderer

import glm_.mat4x4.Mat4

open class RuneCamera {
    var projection: Mat4 = Mat4(1f)
        protected set

    constructor()

    constructor(projection: Mat4) {
        this.projection = projection
    }

}