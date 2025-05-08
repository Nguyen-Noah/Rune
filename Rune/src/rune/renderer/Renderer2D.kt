package rune.renderer

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.lwjgl.system.MemoryUtil
import rune.components.SpriteRendererComponent

val FLOAT_MAT4_SIZE = 16 * 4

class Renderer2D {
    companion object {
        private val data: Renderer2DData = Renderer2DData()

        data class CameraData(var viewProjection: Mat4 = Mat4(1f))

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

            var quadLayout: VertexLayout? = null,
            var quadVertexWriter: VertexBufferWriter? = null,

            val textureSlots: Array<Texture2D?> = arrayOfNulls(maxTextureSlots),
            var textureSlotIndex: Int = 1,  // 0 = white texture

            val quadVertexPositions: Array<Vec4?> = arrayOfNulls(4),
        ) {
            val stats: Statistics = Statistics()
            val cameraBuffer: CameraData = CameraData()
            val cameraUniformBuffer: UniformBuffer = UniformBuffer.create(FLOAT_MAT4_SIZE, 0)

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
            // describing the vertex
            data.quadLayout = VertexLayout.build {
                attr(0, BufferType.Float3)  // position
                attr(1, BufferType.Float4)  // color
                attr(2, BufferType.Float2)  // texcoords
                attr(3, BufferType.Float1)  // texIndex
                attr(4, BufferType.Float1)  // tilingFactor
                attr(5, BufferType.Int1)    // entityID
            }
            data.quadVertexWriter = VertexBufferWriter(data.maxVertices, data.quadLayout!!)

            data.vbo = VertexBuffer.create(data.maxVertices * data.quadLayout!!.stride)

            // index buffer
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
            val ibo = IndexBuffer.create(indices, data.maxIndices)

            // TODO: maybe mix this bufferLayout stuff into the VertexBufferWriter
            data.vao = VertexArray.create(data.vbo!!, bufferLayout {
                attribute("a_Position", 3)
                attribute("a_Color", 4)
                attribute("a_TexCoord", 2)
                attribute("a_TexIndex", 1)
                attribute("a_TilingFactor", 1)
                attribute("a_EntityID", 1)
            }).apply { setIndexBuffer(ibo) }

            // creating and binding the default white texture
            data.whiteTex = Texture2D.create(1, 1).also {
                it.setData(0xffffffff.toInt(), 4)
                data.textureSlots[0] = it
            }

            data.texShader = Shader.create("assets/shaders/Texture.glsl")

            data.quadVertexPositions[0] = Vec4(-0.5f, -0.5f, 0f, 1f)
            data.quadVertexPositions[1] = Vec4( 0.5f, -0.5f, 0f, 1f)
            data.quadVertexPositions[2] = Vec4( 0.5f,  0.5f, 0f, 1f)
            data.quadVertexPositions[3] = Vec4(-0.5f,  0.5f, 0f, 1f)
        }

        fun shutdown() { }

        fun beginScene(camera: EditorCamera) {
            data.cameraBuffer.viewProjection = camera.getViewProjection()
            MemoryUtil.memAlloc(FLOAT_MAT4_SIZE).apply {
                data.cameraBuffer.viewProjection to this
                data.cameraUniformBuffer.setData(this)
                MemoryUtil.memFree(this)
            }

            startBatch()
        }

        fun beginScene(camera: RuneCamera, transform: Mat4) {
            data.cameraBuffer.viewProjection = camera.projection * glm.inverse(transform)
            MemoryUtil.memAlloc(FLOAT_MAT4_SIZE).apply {
                data.cameraBuffer.viewProjection to this
                data.cameraUniformBuffer.setData(this)
                MemoryUtil.memFree(this)
            }

            startBatch()
        }

        // TODO: remove -> this only breaks sandbox and particle thing
        fun beginScene(camera: OrthographicCamera) {
            // initializes an array with [0, 1, 2, ... data.maxTextureSlots - 1]
            val samplers = IntArray(data.maxTextureSlots) { it }

            data.texShader?.bind()
            data.texShader?.uploadUniform {
                uniform("u_ViewProjection", camera.getViewProjectionMatrix())
                uniform("u_Textures", samplers)
            }

            startBatch()
        }

        fun endScene() {
            // drawing the data
            data.vbo?.setData(data.quadVertexWriter!!.slice())
            flush()
        }

        private fun flush() {
            if (data.quadIndexCount == 0) return
            data.texShader!!.bind()

            for ((i, texture) in data.textureSlots.withIndex()) {
                texture?.bind(i)
            }

            RenderCommand.drawIndexed(data.vao!!, data.quadIndexCount)
            data.stats.drawCalls++
        }

        private fun startBatch() {
            data.quadVertexWriter?.reset()
            data.quadIndexCount = 0

            data.textureSlotIndex = 1
        }

        private fun nextBatch() {
            endScene()
            startBatch()
        }

        // primitives
        private fun drawQuadInternal(position: Vec3, size: Vec2, rotation: Float = 0f, color: Vec4 = Vec4(1f), texture: Texture2D? = null, tilingFactor: Float = 1f) {
            // 1. if we are out of space, flush
            if (data.quadIndexCount >= data.maxIndices) {
                nextBatch()
            }

            // 2. figure out tex index
            var textureIndex = 0f

            if (texture != null) {
                for (i in 1 until data.textureSlotIndex) {
                    if (data.textureSlots[i] == texture) {
                        textureIndex = i.toFloat()
                        break
                    }
                }

                // not found
                if (textureIndex == 0f) {
                    textureIndex = data.textureSlotIndex.toFloat()
                    data.textureSlots[data.textureSlotIndex] = texture
                    data.textureSlotIndex++
                }
            }

            // 3. build transform matrix (translate -> rotate -> scale)
            val transform = glm.translate(Mat4(1f), position) *
                    glm.rotate(Mat4(1f), rotation, Vec3(0f, 0f, 1f)) *
                    glm.scale(Mat4(1f), Vec3(size.x, size.y, 1f))

            val texCoords: Array<Vec2> = arrayOf(
                Vec2(0.0f, 0.0f),
                Vec2(1.0f, 0.0f),
                Vec2(1.0f, 1.0f),
                Vec2(0.0f, 1.0f),
            )

            // 4. write out 4 vertices
            for (i in 0 until 4) {
                pushVertex(
                    position = (transform * data.quadVertexPositions[i]!!).toVec3(),
                    color = color,
                    uv = texCoords[i],
                    texId = textureIndex,
                    tilingFactor = tilingFactor
                )
            }

            // 5) increment indexCount by 6
            data.quadIndexCount += 6
            data.stats.quadCount++
        }

        fun drawQuad(position: Vec2, size: Vec2, color: Vec4) {
            drawQuadInternal(
                position = Vec3(position, 0f),
                size = size,
                rotation = 0f,
                color = color,
                texture = null,
                tilingFactor = 1f
            )
        }
        fun drawQuad(position: Vec3, size: Vec2, color: Vec4) {
            drawQuadInternal(
                position = position,
                size = size,
                rotation = 0f,
                color = color,
                texture = null,
                tilingFactor = 1f
            )
        }
        fun drawQuad(position: Vec2, size: Vec2, texture: Texture2D, tilingFactor: Float = 1.0f, tintColor: Vec4 = Vec4(1.0f)) {
            drawQuadInternal(
                position = Vec3(position, 0f),
                size = size,
                rotation = 0f,
                color = tintColor,
                texture = texture,
                tilingFactor = tilingFactor
            )
        }
        fun drawQuad(position: Vec3, size: Vec2, texture: Texture2D, tilingFactor: Float = 1.0f, tintColor: Vec4 = Vec4(1.0f)) {
            drawQuadInternal(
                position = position,
                size = size,
                rotation = 0f,
                color = tintColor,
                texture = texture,
                tilingFactor = tilingFactor
            )
        }

        fun drawQuad(transform: Mat4, color: Vec4, entityId: Int = -1) {
            if (data.quadIndexCount >= data.maxIndices) {
                nextBatch()
            }

            val texCoords: Array<Vec2> = arrayOf(
                Vec2(0.0f, 0.0f),
                Vec2(1.0f, 0.0f),
                Vec2(1.0f, 1.0f),
                Vec2(0.0f, 1.0f),
            )

            val textureIndex = 0f
            val tilingFactor = 1f

            for (i in 0 until 4) {
                pushVertex(
                    position = (transform * data.quadVertexPositions[i]!!).toVec3(),
                    color = color,
                    uv = texCoords[i],
                    texId = textureIndex,
                    tilingFactor = tilingFactor,
                    entityId = entityId
                )
            }

            data.quadIndexCount += 6
            data.stats.quadCount++
        }
        fun drawQuad(transform: Mat4, texture: Texture2D, tilingFactor: Float = 1f, tintColor: Vec4 = Vec4(1f), entityId: Int = -1) {
            if (data.quadIndexCount >= data.maxIndices) {
                nextBatch()
            }

            val texCoords: Array<Vec2> = arrayOf(
                Vec2(0.0f, 0.0f),
                Vec2(1.0f, 0.0f),
                Vec2(1.0f, 1.0f),
                Vec2(0.0f, 1.0f),
            )

            var textureIndex = 0f
            for (i in 1 until data.textureSlotIndex) {
                if (data.textureSlots[i] == texture) {
                    textureIndex = i.toFloat()
                    break
                }
            }

            if (textureIndex == 0f) {
                if (data.textureSlotIndex >= data.maxTextureSlots) {
                    nextBatch()
                }

                textureIndex = data.textureSlotIndex.toFloat()
                data.textureSlots[data.textureSlotIndex] = texture
                data.textureSlotIndex++
            }

            for (i in 0 until 4) {
                pushVertex(
                    position = (transform * data.quadVertexPositions[i]!!).toVec3(),
                    color = tintColor,
                    uv = texCoords[i],
                    texId = textureIndex,
                    tilingFactor = tilingFactor,
                    entityId = entityId
                )
            }

            data.quadIndexCount += 6
            data.stats.quadCount++
        }

        // rotation in radians
        fun drawRotatedQuad(position: Vec2, size: Vec2, rotation: Float, color: Vec4) {
            drawQuadInternal(
                position = Vec3(position, 0f),
                size = size,
                rotation = rotation,
                color = color
            )
        }
        fun drawRotatedQuad(position: Vec3, size: Vec2, rotation: Float, color: Vec4) {
            drawQuadInternal(
                position = position,
                size = size,
                rotation = rotation,
                color = color
            )
        }
        fun drawRotatedQuad(position: Vec2, size: Vec2, rotation: Float, texture: Texture2D, tilingFactor: Float = 1.0f, tintColor: Vec4 = Vec4(1.0f)) {
            drawQuadInternal(
                position = Vec3(position, 0f),
                size = size,
                rotation = rotation,
                color = tintColor,
                texture = texture,
                tilingFactor = tilingFactor
            )
        }
        fun drawRotatedQuad(position: Vec3, size: Vec2, rotation: Float, texture: Texture2D, tilingFactor: Float = 1.0f, tintColor: Vec4 = Vec4(1.0f)) {
            drawQuadInternal(
                position = position,
                size = size,
                rotation = rotation,
                color = tintColor,
                texture = texture,
                tilingFactor = tilingFactor
            )
        }

        fun drawSprite(transform: Mat4, src: SpriteRendererComponent, entityId: Int) {
            src.texture?.let {
                drawQuad(transform, src.texture!!, src.tilingFactor, src.color, entityId)
            } ?: drawQuad(transform, src.color, entityId)
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

        private inline fun pushVertex(
            position: Vec3, color: Vec4, uv: Vec2,
            texId: Float, tilingFactor: Float, entityId: Int = -1
        ) = data.quadVertexWriter?.write {
            putFloat(position.x); putFloat(position.y); putFloat(position.z)
            putFloat(color.r); putFloat(color.g); putFloat(color.b); putFloat(color.a)
            putFloat(uv.x);  putFloat(uv.y)
            putFloat(texId)
            putFloat(tilingFactor)
            putInt(entityId)
        }
    }
}