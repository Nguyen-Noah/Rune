package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.SubmitRender
import rune.rhi.Pipeline
import rune.rhi.PipelineSpec

class GLPipeline(desc: PipelineSpec) : Pipeline {
    private val shader = desc.shader
    private var vao = -1
    private val raster = desc.raster
    private val depth = desc.depth
    private val blends = desc.blends
    private val layout = desc.layout

    init {
        invalidate()
    }

    override fun bind() {
        SubmitRender("GLPipeline-bind") {
            glBindVertexArray(vao)

            with(depth) {
                if (test) {
                    glEnable(GL_DEPTH_TEST)
                } else {
                    glDisable(GL_DEPTH_TEST)
                }

                //glDepthMask(write)
                //glDepthFunc(compare.gl)
            }
        }
    }

    override fun unbind() {
        //shader.unbind()
        SubmitRender("GLPipeline-unbind") {
            glBindVertexArray(0)
        }
    }

    fun invalidate() {
        SubmitRender("GLPipeline-invalidate") {
            if (vao == -1)
                glDeleteVertexArrays(vao)

            vao = glGenVertexArrays()
            glBindVertexArray(vao)

            layout.attributes.forEach { attr ->
                glEnableVertexArrayAttrib(vao, attr.loc)
                val fmt = attr.component
                when(fmt.glType) {
                    // integer/boolean
                    GL_INT, GL_UNSIGNED_BYTE ->
                        glVertexArrayAttribIFormat(
                            vao,
                            attr.loc,
                            fmt.comps,
                            fmt.glType,
                            attr.offset
                        )
                    else ->
                        glVertexArrayAttribFormat(
                            vao,
                            attr.loc,
                            fmt.comps,
                            fmt.glType,
                            false,
                            attr.offset
                        )
                }
                glVertexArrayAttribBinding(vao, attr.loc, 0)
            }

            glBindVertexArray(0)
        }
    }

    internal fun attachVertexBuffer(vboId: Int) {
        SubmitRender("GLPipeline-attachVBO") { glVertexArrayVertexBuffer(vao, 0, vboId, 0L, layout.stride) }
    }

}