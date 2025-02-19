package runestone

import rune.*
import rune.events.Event

class Test : Layer("Test") {
    override fun onUpdate() {
        println("Testlayer.Update")
    }

    override fun onEvent(event: Event) {
        println(event)
    }
}

class Runestone : Application() {
    init {
        println("Runestone initialized.")
    }
}

fun main() {
    val app: Application = Runestone()
    app.run()
}