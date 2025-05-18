package rune.utils

import kool.PointerBuffer
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.nfd.NFDFilterItem
import org.lwjgl.util.nfd.NFDFilterItem.Buffer
import org.lwjgl.util.nfd.NativeFileDialog.*
import rune.core.Logger
import java.io.File

class FileDialog {
    fun openFile(): String {
        // initialize the native file dialog library
        NFD_Init()

        MemoryStack.stackPush().use { stack ->
            // slot for out pointer
            val outPath: PointerBuffer = stack.PointerBuffer(1)
            // creating a stack filter list of length 1 (malloc works too)
            val filters: Buffer = NFDFilterItem.calloc(1, stack)
            filters.get(0)
                .name( stack.UTF8("Rune Scenes") )
                .spec( stack.UTF8("rune") )

            val defaultPath: CharSequence? = System.getProperty("user.dir")

            val result = NFD_OpenDialog(outPath, filters, defaultPath)

            when (result) {
                NFD_OKAY -> {
                    val path = outPath.getStringUTF8(0)
                    println("Selected: $path")
                    NFD_FreePath(outPath.get(0))

                    return path
                }
                NFD_CANCEL -> Logger.warn("User canceled")
                else -> Logger.error("Error: ${NFD_GetError()}")
            }
        }

        NFD_Quit()

        // String.isNotEmpty() can check this instead of making return type nullable
        return ""
    }

    fun saveAs(defaultName: String): String {
        NFD_Init()

        MemoryStack.stackPush().use { stack ->
            // slot for out pointer
            val outPath: PointerBuffer = stack.mallocPointer(1)
            // creating a stack filter list of length 1 (malloc works too)
            val filters: Buffer = NFDFilterItem.calloc(1, stack)
            filters.get(0)
                .name( stack.UTF8("Rune Scenes") )
                .spec( stack.UTF8("rune") )

            val defaultPath: CharSequence? = System.getProperty("user.dir")

            val result = NFD_SaveDialog(outPath, filters, defaultPath, defaultName)

            when (result) {
                NFD_OKAY -> {
                    val path = outPath.getStringUTF8(0)
                    NFD_FreePath(outPath.get(0))
                    return File(path).absolutePath

                }
                NFD_CANCEL -> Logger.warn("User canceled")
                else -> Logger.error("Error: ${NFD_GetError()}")
            }
        }

        NFD_Quit()

        return ""
    }
}