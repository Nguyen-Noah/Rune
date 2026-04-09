package sandbox

import rune.core.Application
import rune.core.Logger
import rune.core.WindowProps
import rune.imgui.ImguiLayer

val windowProps = WindowProps(
    title = "Rune Sandbox",
    width = 1920,
    height = 1080
)

class Sandbox : Application(windowProps) {
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
