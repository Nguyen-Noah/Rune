package runestone

import rune.core.Application

class Runestone : Application() {
    init {
        println("Runestone initialized.")
        pushLayer(EditorLayer())
    }
}

fun main() {
    val app: Application = Runestone()

    app.run()
}