package rune.platforms.opengl

internal object RenderCommandQueue {
    data class Task(val name: String, val exec: () -> Unit) {
        override fun toString(): String = name
    }
    private val pending = mutableListOf<Task>()

    fun enqueue(name: String = "Unnamed", exec: () -> Unit) {
        pending += Task(name, exec)
    }

    fun flush() {
        pending.forEach { it.exec() }
        pending.clear()
    }

    val size: Int get() = pending.size
}
