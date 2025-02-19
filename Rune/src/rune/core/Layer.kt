package rune.core

import rune.events.Event

abstract class Layer(val name: String = "Layer") {
    open fun onAttach() {

    }
    open fun onDetach() {

    }
    open fun onUpdate() {

    }
    open fun onEvent(event: Event) {

    }

    open fun onImGuiRender() {

    }
}