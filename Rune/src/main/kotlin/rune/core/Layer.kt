package rune.core

import rune.events.Event

abstract class Layer(val name: String = "Layer") {
    open fun onAttach() {

    }
    open fun onDetach() {

    }
    open fun onUpdate(dt: Float) {

    }
    open fun onEvent(e: Event) {

    }

    open fun onImGuiRender() {

    }
}