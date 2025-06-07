package rune.platforms.opengl

import rune.rhi.IndexType

sealed interface GLBuffer {
    val rendererID: Int
    val target: Int
    val size: Int
}

interface GLIndexable : GLBuffer { val indexType: IndexType }