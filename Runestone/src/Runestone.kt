package runestone

import rune.core.Application
import rune.core.Logger

class Runestone : Application() {
    init {
        Logger.info("Runestone initialized.")
        pushLayer(EditorLayer())
    }
}

fun main() {
    val app: Application = Runestone()

    app.run()
}