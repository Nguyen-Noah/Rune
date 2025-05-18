package rune.imgui

private val local = object : ThreadLocal<Buffers>() {
    override fun initialValue() = Buffers()
}

internal class Buffers {
    val f1 = FloatArray(1)
    val f2 = FloatArray(2)
    val f3 = FloatArray(3)
    val f4 = FloatArray(4)
    val b1 = BooleanArray(1)
}

internal fun buffers(): Buffers = local.get()