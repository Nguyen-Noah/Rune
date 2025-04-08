package rune.renderer

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4

class Renderer2D {
    companion object {
        private val data: Renderer2DData = Renderer2DData()

        data class Renderer2DData(
            var vao: VertexArray? = null,
            var vbo: VertexBuffer? = null,
            var texShader: Shader? = null,
            var whiteTex: Texture2D? = null,

            val maxQuads: Int = 10000,
            val maxVertices: Int = maxQuads * 4,
            val maxIndices: Int = maxQuads * 6,
            val maxTextureSlots: Int = 32,      // TODO: RenderCaps

            var quadIndexCount: Int = 0,

            var quadVertexWriter: QuadVertexBufferWriter? = null,

            val textureSlots: Array<Texture2D?> = arrayOfNulls(maxTextureSlots),
            var textureSlotIndex: Int = 1,  // 0 = white texture

            val quadVertexPositions: Array<Vec4?> = arrayOfNulls(4),
        ) {
            val stats: Statistics = Statistics()

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Renderer2DData

                if (vao != other.vao) return false
                if (texShader != other.texShader) return false
                if (whiteTex != other.whiteTex) return false
                if (maxQuads != other.maxQuads) return false
                if (maxVertices != other.maxVertices) return false
                if (maxIndices != other.maxIndices) return false
                if (quadIndexCount != other.quadIndexCount) return false

                return true
            }

            override fun hashCode(): Int {
                var result = vao?.hashCode() ?: 0
                result = 31 * result + (texShader?.hashCode() ?: 0)
                result = 31 * result + (whiteTex?.hashCode() ?: 0)
                result = 31 * result + maxQuads
                result = 31 * result + maxVertices
                result = 31 * result + maxIndices
                result = 31 * result + quadIndexCount
                return result
            }
        }

        fun init() {
            data.quadVertexWriter = QuadVertexBufferWriter(data.maxVertices)

            val vertices = floatArrayOf(
                -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
                 0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
                 0.5f,  0.5f, 0.0f, 1.0f, 1.0f,
                -0.5f,  0.5f, 0.0f, 0.0f, 1.0f,
            )

            val indices = IntArray(data.maxIndices)

            var offset = 0
            for (i in 0 until data.maxIndices step 6) {
                indices[i + 0] = offset + 0
                indices[i + 1] = offset + 1
                indices[i + 2] = offset + 2

                indices[i + 3] = offset + 2
                indices[i + 4] = offset + 3
                indices[i + 5] = offset + 0

                offset += 4
            }

            data.vbo = VertexBuffer.create(data.maxVertices * QuadVertex.BYTE_SIZE)
            val ibo = IndexBuffer.create(indices, data.maxIndices)

            // creating and binding the default white texture
            data.whiteTex = Texture2D.create(1, 1)
            data.whiteTex?.setData(0xffffffff.toInt(), 4)
            data.textureSlots[0] = data.whiteTex

            data.vao = VertexArray.create(data.vbo!!, bufferLayout {
                attribute("a_Position", 3)
                attribute("a_Color", 4)
                attribute("a_TexCoord", 2)
                attribute("a_TexIndex", 1)
                attribute("a_TilingFactor", 1)
            })
            data.vao!!.setIndexBuffer(ibo)

            data.texShader = Shader.create("assets/shaders/Texture.glsl")

            data.quadVertexPositions[0] = Vec4(-0.5f, -0.5f, 0f, 1f)
            data.quadVertexPositions[1] = Vec4( 0.5f, -0.5f, 0f, 1f)
            data.quadVertexPositions[2] = Vec4( 0.5f,  0.5f, 0f, 1f)
            data.quadVertexPositions[3] = Vec4(-0.5f,  0.5f, 0f, 1f)
        }

        fun shutdown() {

        }

        fun beginScene(camera: OrthographicCamera) {
            // initializes an array with [0, 1, 2, ... data.maxTextureSlots - 1]
            val samplers = IntArray(data.maxTextureSlots) { it }

            data.texShader?.bind()
            data.texShader?.uploadUniform {
                uniform("u_ViewProjection", camera.getViewProjectionMatrix())
                uniform("u_Textures", samplers)
            }


            data.quadVertexWriter?.reset()
            data.quadIndexCount = 0

            data.textureSlotIndex = 1
        }

        fun endScene() {
            data.vbo!!.setData(data.quadVertexWriter!!.getBuffer(), data.quadVertexWriter!!.getSizeInBytes())

            flush()
        }

        private fun flush() {
            // bind textures
            for ((i, texture) in data.textureSlots.withIndex()) {
                texture?.bind(i)
            }

            RenderCommand.drawIndexed(data.vao!!, data.quadIndexCount)
            data.stats.drawCalls++
        }

        private fun flushAndReset() {
            endScene()

            data.quadVertexWriter?.reset()
            data.quadIndexCount = 0

            data.textureSlotIndex = 1
        }

        // primitives
        fun drawQuad(position: Vec2, size: Vec2, color: Vec4) {
            drawQuad(Vec3(position.x, position.y, 0.0f), size, color)
        }

        fun drawQuad(position: Vec3, size: Vec2, color: Vec4) {
            if (data.quadIndexCount >= data.maxIndices) {
                flushAndReset()
            }

            val textureIndex = 0f
            val tilingFactor = 1f

            val transform: Mat4 = glm.translate(Mat4(1f), position) *
                    glm.scale(Mat4(1f), Vec3(size.x, size.y, 1f))

            // Bottom-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[0]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Bottom-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[1]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[2]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[3]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            data.quadIndexCount += 6

            // stats
            data.stats.quadCount++
        }

        fun drawQuad(
            position: Vec2,
            size: Vec2,
            texture: Texture2D,
            tilingFactor: Float = 1.0f,
            tintColor: Vec4 = Vec4(1.0f)
        ) {
            drawQuad(Vec3(position.x, position.y, 0.0f), size, texture, tilingFactor, tintColor)
        }

        fun drawQuad(
            position: Vec3,
            size: Vec2,
            texture: Texture2D,
            tilingFactor: Float = 1.0f,
            tintColor: Vec4 = Vec4(1.0f)
        ) {
            if (data.quadIndexCount >= data.maxIndices) {
                flushAndReset()
            }

            val color = Vec4(1f)

            var textureIndex = 0f

            for (i in 1..data.textureSlotIndex) {
                if (data.textureSlots[i] == texture) {
                    textureIndex = i.toFloat()
                    break
                }
            }

            if (textureIndex == 0f) {
                textureIndex = data.textureSlotIndex.toFloat()
                data.textureSlots[data.textureSlotIndex] = texture
                data.textureSlotIndex++
            }

            val transform: Mat4 = glm.translate(Mat4(1f), position) *
                    glm.scale(Mat4(1f), Vec3(size.x, size.y, 1f))

            // Bottom-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[0]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Bottom-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[1]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[2]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[3]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            data.quadIndexCount += 6

            // stats
            data.stats.quadCount++
        }

        fun drawRotatedQuad(position: Vec2, size: Vec2, rotation: Float, color: Vec4) {
            drawRotatedQuad(Vec3(position.x, position.y, 0.0f), size, rotation, color)
        }

        fun drawRotatedQuad(position: Vec3, size: Vec2, rotation: Float, color: Vec4) {
            if (data.quadIndexCount >= data.maxIndices) {
                flushAndReset()
            }

            val textureIndex = 0f
            val tilingFactor = 1f

            val transform: Mat4 = glm.translate(Mat4(1f), position) *
                    glm.rotate(Mat4(1f), glm.radians(rotation), Vec3(0f, 0f, 1f)) *
                    glm.scale(Mat4(1f), Vec3(size.x, size.y, 1f))

            // Bottom-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[0]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Bottom-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[1]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[2]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[3]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            data.quadIndexCount += 6

            // stats
            data.stats.quadCount++
        }

        fun drawRotatedQuad(
            position: Vec2,
            size: Vec2,
            rotation: Float,
            texture: Texture2D,
            tilingFactor: Float = 1.0f,
            tintColor: Vec4 = Vec4(1.0f)
        ) {
            drawRotatedQuad(Vec3(position.x, position.y, 0.0f), size, rotation, texture, tilingFactor, tintColor)
        }

        fun drawRotatedQuad(
            position: Vec3,
            size: Vec2,
            rotation: Float,
            texture: Texture2D,
            tilingFactor: Float = 1.0f,
            tintColor: Vec4 = Vec4(1.0f)
        ) {
            if (data.quadIndexCount >= data.maxIndices) {
                flushAndReset()
            }

            val color = Vec4(1f)

            var textureIndex = 0f

            for (i in 1..data.textureSlotIndex) {
                if (data.textureSlots[i] == texture) {
                    textureIndex = i.toFloat()
                    break
                }
            }

            if (textureIndex == 0f) {
                textureIndex = data.textureSlotIndex.toFloat()
                data.textureSlots[data.textureSlotIndex] = texture
                data.textureSlotIndex++
            }

            val transform: Mat4 = glm.translate(Mat4(1f), position) *
                    glm.rotate(Mat4(1f), glm.radians(rotation), Vec3(0f, 0f, 1f)) *
                    glm.scale(Mat4(1f), Vec3(size.x, size.y, 1f))

            // Bottom-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[0]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Bottom-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[1]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 0.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-right
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[2]!!).toVec3(),
                color = color,
                texCoords = Vec2(1.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            // Top-left
            data.quadVertexWriter!!.write(
                position = (transform * data.quadVertexPositions[3]!!).toVec3(),
                color = color,
                texCoords = Vec2(0.0f, 1.0f),
                texIndex = textureIndex,
                tilingFactor = tilingFactor
            )

            data.quadIndexCount += 6

            // stats
            data.stats.quadCount++
        }


        // STATISTICS
        data class Statistics(var drawCalls: Int = 0, var quadCount: Int = 0) {
            fun getTotalVertexCount(): Int = quadCount * 4
            fun getTotalIndexCount(): Int = quadCount * 6
        }

        fun resetStats() {
            data.stats.quadCount = 0
            data.stats.drawCalls = 0
        }
        fun getStats(): Statistics = data.stats
    }
}