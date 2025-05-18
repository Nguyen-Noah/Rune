package rune.imgui

import imgui.ImColor

object Colors {
    object Theme {
        // IM_COL32(r,g,b,a) ⇒ (a<<24)|(b<<16)|(g<<8)|r
        private fun col32(r: Int, g: Int, b: Int, a: Int): Int =
            ((a and 0xFF) shl 24) or
                    ((b and 0xFF) shl 16) or
                    ((g and 0xFF) shl 8)  or
                    (r  and 0xFF)

        val accent           = col32(236, 158,  36, 255)
        val highlight        = col32( 39, 185, 242, 255)
        val niceBlue         = col32( 83, 232, 254, 255)
        val compliment       = col32( 78, 151, 166, 255)
        val background       = col32( 36,  36,  36, 255)
        val backgroundDark   = col32( 26,  26,  26, 255)
        val titlebar         = col32( 21,  21,  21, 255)
        val titlebarOrange   = col32(186,  66,  30, 255)
        val titlebarGreen    = col32( 18,  88,  30, 255)
        val titlebarRed      = col32(185,  30,  30, 255)
        val propertyField    = col32( 15,  15,  15, 255)
        val text             = col32(192, 192, 192, 255)
        val textBrighter     = col32(210, 210, 210, 255)
        val textDarker       = col32(128, 128, 128, 255)
        val textError        = col32(230,  51,  51, 255)
        val muted            = col32( 77,  77,  77, 255)
        val groupHeader      = col32( 47,  47,  47, 255)
        val selection        = col32(237, 192, 119, 255)
        val selectionMuted   = col32(237, 201, 142,  23)
        val backgroundPopup  = col32( 50,  50,  50, 255)
        val validPrefab      = col32( 82, 179, 222, 255)
        val invalidPrefab    = col32(222,  43,  43, 255)
        val missingMesh      = col32(230, 102,  76, 255)
        val meshNotSet       = col32(250, 101,  23, 255)
    }
}