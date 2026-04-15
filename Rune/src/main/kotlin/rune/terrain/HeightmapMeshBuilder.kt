package rune.terrain

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.asset.Material
import rune.renderer.Renderer
import rune.renderer.TextureType
import rune.renderer.gpu.Texture2D
import rune.renderer.renderer3d.AABB
import rune.renderer.renderer3d.Mesh
import rune.renderer.renderer3d.MeshBuffers
import rune.renderer.renderer3d.Model
import rune.renderer.renderer3d.SubMesh
import rune.renderer.renderer3d.mesh.Vertex
import rune.renderer.gpu.IndexBuffer
import rune.renderer.gpu.VertexBuffer

object HeightmapMeshBuilder {

    fun buildModel(config: TerrainConfig): Model {
        val vertices = buildVertices(config)
        val indices = buildIndices(config.gridX, config.gridZ)
        val aabb = computeAabb(vertices)

        val buffers = MeshBuffers(
            VertexBuffer.create(vertices),
            IndexBuffer.create(indices),
            aabb,
        )

        val white = Texture2D.create(1, 1).apply { setData(0xffffffff.toInt(), 4) }
        val flatNormal = Texture2D.create(1, 1).apply { setData(0xff8080ff.toInt(), 4) }
        val blackSpec = Texture2D.create(1, 1).apply { setData(0xff000000.toInt(), 4) }

        val textures: Array<Texture2D?> = arrayOfNulls(TextureType.entries.size)
        textures[TextureType.Albedo.ordinal] = white
        textures[TextureType.Normal.ordinal] = flatNormal
        textures[TextureType.Specular.ordinal] = blackSpec

        val material = Material(
            textures,
            ambient = Vec4(1f),
            diffuse = Vec4(1f),
            specular = Vec4(0.2f),
            shader = Renderer.getShader("Terrain"),
        )

        val sub = SubMesh(
            indexOffset = 0,
            indexCount = indices.size,
            material = material,
            localAABB = aabb,
        )

        val mesh = Mesh.Static(config.meshName, buffers, listOf(sub))       // TODO: Mesh.Procedural
        return Model(mesh)
    }

    // TODO: add bitangents and tangents
    fun buildVertices(config: TerrainConfig): List<Vertex> {
        val width = config.gridX + 1
        val depth = config.gridZ + 1

        val out = ArrayList<Vertex>(width * depth)
        for (iz in 0 until depth) {
            for (ix in 0 until width) {
                val x = (ix.toFloat() / config.gridX - 0.5f) * config.sizeX
                val z = (iz.toFloat() / config.gridZ - 0.5f) * config.sizeZ
                val y = config.heights[iz * width + ix]

                val dhx = sampleDeltaX(config, ix, iz)
                val dhz = sampleDeltaZ(config, ix, iz)
                val normals = glm.normalize(Vec3(-dhx, 1f, -dhz))
                val bitangent = Vec3(0)
                val tangent = Vec3(0)

                val u = ix.toFloat() / config.gridX
                val v = iz.toFloat() / config.gridZ
                out += Vertex(Vec3(x, y, z), normals, bitangent, tangent, Vec2(u, v))
            }
        }
        return out
    }

    private fun sampleDeltaX(config: TerrainConfig, ix: Int, iz: Int): Float {
        val w = config.gridX + 1
        val stepX = config.sizeX / config.gridX
        val h = config.heights
        val ixL = (ix - 1).coerceAtLeast(0)
        val ixR = (ix + 1).coerceAtMost(config.gridX)
        val yL = h[iz * w + ixL]
        val yR = h[iz * w + ixR]
        return (yR - yL) / ((ixR - ixL) * stepX).coerceAtLeast(1e-6f)
    }

    private fun sampleDeltaZ(config: TerrainConfig, ix: Int, iz: Int): Float {
        val w = config.gridX + 1
        val stepZ = config.sizeZ / config.gridZ
        val h = config.heights
        val izD = (iz - 1).coerceAtLeast(0)
        val izU = (iz + 1).coerceAtMost(config.gridZ)
        val yD = h[izD * w + ix]
        val yU = h[izU * w + ix]
        return (yU - yD) / ((izU - izD) * stepZ).coerceAtLeast(1e-6f)
    }

    fun buildIndices(gridX: Int, gridZ: Int): List<Int> {
        val w = gridX + 1
        val faces = gridX * gridZ * 6
        val out = ArrayList<Int>(faces)
        for (iz in 0 until gridZ) {
            for (ix in 0 until gridX) {
                val a = iz * w + ix
                val b = a + 1
                val c = a + w
                val d = c + 1
                out += a
                out += b
                out += c
                out += b
                out += d
                out += c
            }
        }
        return out
    }

    private fun computeAabb(vertices: List<Vertex>): AABB {
        val box = AABB()
        vertices.forEach { box.enclose(it.position) }
        return box
    }
}
