package sandbox

import rune.core.Application
import rune.core.Logger
import rune.imgui.ImguiLayer

class Sandbox : Application() {
    init {
        Logger.info("Sandbox initialized.")
        getImGuiLayer().showDemoWindow = false
        pushLayer(SceneLayer())
    }
}

fun main() {
    val app: Application = Sandbox()
    app.run()
}
