package rune.renderer.renderer2d

internal interface Batch {
    /* resets writers & counters so the next scene can start */
    fun begin()

    /* true when pushing *vertexCount / indexCount* would overflow this batch */
    fun isFull(vertexCount: Int = 0, indexCount: Int = 0): Boolean

    /* upload GPU buffers and issue draw call */
    fun flush()
}