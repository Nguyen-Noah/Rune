package rune.core

private typealias MouseCode = UInt

enum class MouseButton(val code: MouseCode) {
    Button0(0u),
    Button1(1u),
    Button2(2u),
    Button3(3u),
    Button4(4u),
    Button5(5u),
    Button6(6u),
    Button7(7u),

    // Reference existing codes for "aliases"
    ButtonLast(Button7.code),
    ButtonLeft(Button0.code),
    ButtonRight(Button1.code),
    ButtonMiddle(Button2.code);

    companion object {
        fun fromCode(value: Int): MouseButton? {
            return MouseButton.entries.find { it.code.toInt() == value }
        }
    }
}