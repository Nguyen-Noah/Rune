package rune.core

class LayerStack : Iterable<Layer> {
    private val layers = mutableListOf<Layer>()
    private var layerInsert: Int = 0

    val lastIndex: Int
        get() = layers.lastIndex

    fun destroy() {
        for (layer in layers) {
            layer.onDetach()
        }
        layers.clear()
    }

    fun pushLayer(layer: Layer) {
        layers.add(layerInsert, layer)
        layerInsert++
    }

    fun pushOverlay(overlay: Layer) {
        layers.add(overlay)
    }

    fun popLayer(layer: Layer) {
        val index = layers.indexOf(layer)
        if (index in 0 until layerInsert) {
            layer.onDetach()
            layers.removeAt(index)
            layerInsert--
        }
    }

    fun popOverlay(overlay: Layer) {
        val index = layers.indexOf(overlay)
        if (index >= layerInsert && index < layers.size) {
            overlay.onDetach()
            layers.removeAt(index)
        }
    }

    operator fun get(i: Int): Layer {
        return layers[i]
    }

    override fun iterator(): Iterator<Layer> {
        return layers.iterator()
    }
}