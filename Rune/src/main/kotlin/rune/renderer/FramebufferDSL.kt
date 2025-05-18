package rune.renderer

import org.lwjgl.opengl.GL45

class FramebufferSpecification {
    var width: Int = 0
    var height: Int = 0
    var samples: Int = 1
    var swapChainTarget: Boolean = false   // are we rendering to the screen or nah

    private val attachmentsBuilder: FramebufferAttachmentSpecification = FramebufferAttachmentSpecification()
    val attachments: List<FramebufferTextureSpecification>
        get() = attachmentsBuilder.list

    // DSL entry point
    fun attachments(init: FramebufferAttachmentSpecification.() -> Unit) {
        attachmentsBuilder.init()
    }

    override fun toString(): String {
        val out: StringBuilder = StringBuilder()

        out.append("Width:   $width\n")
        out.append("Height:  $height\n")
        out.append("Samples: $samples\n")
        out.append("Swap:    $swapChainTarget\n")
        out.append("Attachments:\n")
        for (attachment in attachments) {
            out.append("${attachment.format}\n")
        }

        return out.toString()
    }
}

fun framebuffer(init: FramebufferSpecification.() -> Unit) = FramebufferSpecification().apply(init)


enum class FramebufferTextureFormat(val glEnum: Int) {
    None(0),

    // color
    RGBA8(GL45.GL_RGBA8),

    // for entity id
    RED_INTEGER(GL45.GL_RED_INTEGER),

    // depth/stencil
    DEPTH24STENCIL8(GL45.GL_DEPTH24_STENCIL8),

    // default
    Depth(DEPTH24STENCIL8.glEnum)
}

data class FramebufferTextureSpecification(val format: FramebufferTextureFormat = FramebufferTextureFormat.None)

class FramebufferAttachmentSpecification {
    internal val list = mutableListOf<FramebufferTextureSpecification>()

    fun color(format: FramebufferTextureFormat) {
        require(format != FramebufferTextureFormat.None)       // TODO: make a Rune.assert()
        list += FramebufferTextureSpecification(format)
    }

    fun depth(format: FramebufferTextureFormat) {
        require(format != FramebufferTextureFormat.None)
        list += FramebufferTextureSpecification(format)
    }
}


