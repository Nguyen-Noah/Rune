package rune.components

import com.github.quillraven.fleks.Component

interface CopyableComponent<T : Component<T>> {
    fun copy(): T
}