package rune.renderer.gpu

import rune.rhi.AttachmentFormat

data class AttachmentSpec(val format: AttachmentFormat)

class AttachmentList {
    internal val list = mutableListOf<AttachmentSpec>()

    fun color(format: AttachmentFormat) {
        require(format != AttachmentFormat.DEPTH24STENCIL8)
        list += AttachmentSpec(format)
    }

    fun depth(format: AttachmentFormat = AttachmentFormat.DEPTH24STENCIL8) {
        require(format == AttachmentFormat.DEPTH24STENCIL8)
        list += AttachmentSpec(format)
    }
}

class FramebufferSpecification {
    var width: Int = 0
    var height: Int = 0
    var samples: Int = 1
    var swapChainTarget: Boolean = false   // are we rendering to the screen or nah

    private val attachmentsBuilder = AttachmentList()
    val attachments: List<AttachmentSpec>
        get() = attachmentsBuilder.list

    // DSL entry point
    fun attachments(init: AttachmentList.() -> Unit) {
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

// TODO: make this return a Framebuffer
fun framebuffer(init: FramebufferSpecification.() -> Unit) =
    FramebufferSpecification().apply(init)
