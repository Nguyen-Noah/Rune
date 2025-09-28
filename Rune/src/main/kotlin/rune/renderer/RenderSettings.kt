package rune.renderer

object RenderSettings {
    enum class ToneMapper(val idx: Int, val label: String) {
        NONE(0, "None"),
        ACES(1, "ACES"),
        REINHARD(2, "Reinhard");

        companion object {
            private val ordered: List<ToneMapper> = entries.sortedBy { it.idx }

            @JvmStatic
            val labelsArray: Array<String> = ordered.map { it.label }.toTypedArray()

            @JvmStatic
            fun fromUiIndex(i: Int): ToneMapper = ordered.getOrElse(i) { NONE }

            @JvmStatic
            fun uiIndexOf(tm: ToneMapper): Int = ordered.indexOf(tm)

            @JvmStatic
            fun fromIdx(v: Int): ToneMapper = ordered.firstOrNull { it.idx == v } ?: NONE
        }
    }
}