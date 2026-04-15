package rune.asset

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import kool.contentToString
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.AI_MAX_NUMBER_OF_COLOR_SETS
import org.lwjgl.assimp.Assimp.AI_MAX_NUMBER_OF_TEXTURECOORDS
import org.lwjgl.system.MemoryUtil
import rune.project.ProjectManager
import rune.renderer.Renderer
import rune.renderer.TextureType
import rune.renderer.gpu.*
import rune.renderer.renderer3d.*
import rune.renderer.renderer3d.mesh.Vertex
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.min

object MeshImporter {

    private val meshPath = ProjectManager.current.meshes

    sealed class MeshData {
        abstract val vertices: List<Vertex>
        abstract val indices: List<Int>
        abstract val subMeshes: List<SubMesh>
        abstract val globalAABB: AABB

        data class Static(
            override val vertices: List<Vertex>,
            override val indices: List<Int>,
            override val subMeshes: List<SubMesh>,
            override val globalAABB: AABB
        ) : MeshData()

        data class Animated(
            override val vertices: List<Vertex>,
            override val indices: List<Int>,
            override val subMeshes: List<SubMesh>,
            override val globalAABB: AABB,
            val bones: List<Bone>
        ) : MeshData()
    }

    fun importStaticMesh(fileName: String, flipUVs: Boolean = false): Model =
        importMesh(fileName, flipUVs, MeshType.STATIC) { name, buffers, subMeshes, _ ->
            Mesh.Static(name, buffers, subMeshes)
        }

    fun importAnimatedMesh(fileName: String, flipUVs: Boolean = false): Model =
        importMesh(fileName, flipUVs, MeshType.ANIMATED) { name, buffers, subMeshes, meshData ->
            val animated = meshData as MeshData.Animated
            Mesh.Animated(name, buffers, subMeshes, animated.bones)
        }

    private fun importMesh(
        fileName: String,
        flipUVs: Boolean,
        meshType: MeshType,
        buildMesh: (name: String, buffers: MeshBuffers, subMeshes: List<SubMesh>, meshData: MeshData) -> Mesh
    ): Model {
        val scene = loadScene(fileName, flipUVs)
        val resourcePath = fileName.substringBefore('/')

        val materials = loadMaterials(scene, resourcePath)
        val meshData = loadMeshes(scene, materials, meshType)

        val buffers = MeshBuffers(
            VertexBuffer.create(meshData.vertices),
            IndexBuffer.create(meshData.indices),
            meshData.globalAABB
        )

        return Model(buildMesh(fileName.substringBefore('.'), buffers, meshData.subMeshes, meshData))
    }

    private fun loadScene(fileName: String, flipUVs: Boolean): AIScene {
        val flags = Assimp.aiProcess_Triangulate or
                Assimp.aiProcess_GenNormals or
                Assimp.aiProcess_OptimizeMeshes or
                Assimp.aiProcess_SortByPType or
                Assimp.aiProcess_CalcTangentSpace or
                (Assimp.aiProcess_FlipUVs.takeIf { flipUVs } ?: 0)

        return Assimp.aiImportFile("$meshPath/$fileName", flags)!!
    }

    private fun loadMaterials(scene: AIScene, resourcePath: String): List<Material> {
        val aiMaterials = scene.mMaterials() ?: return emptyList()
        val allTextureTypes = Assimp::class.java.fields
            .filter { it.name.startsWith("aiTextureType_") }
            .associate { it.name to it.getInt(null) }
        return (0 until scene.mNumMaterials()).mapNotNull { i ->
            AIMaterial.create(aiMaterials[i]).let { mat ->
                println('\n')
                val name = AIString.calloc()
                Assimp.aiGetMaterialString(mat, Assimp.AI_MATKEY_NAME, 0, 0, name)
                println("Material[$i]: ${name.dataString()}")
                name.free()

                for ((label, aiType) in allTextureTypes) {
                    val path = AIString.calloc()
                    val found = Assimp.aiGetMaterialTexture(
                        mat, aiType, 0, path,
                        null as IntBuffer?, null as IntBuffer?, null as FloatBuffer?,
                        null as IntBuffer?, null as IntBuffer?, null as IntBuffer?
                    ) == Assimp.aiReturn_SUCCESS && path.dataString().isNotEmpty()

                    if (found)
                        println("$label -> ${path.dataString()}")

                    path.free()
                }

                Material(
                    loadTextures(mat, resourcePath),
                    textureUvChannels(mat),
                    mat.getColor(Assimp.AI_MATKEY_COLOR_AMBIENT)  ?: Vec4(1f),
                    mat.getColor(Assimp.AI_MATKEY_COLOR_DIFFUSE)  ?: Vec4(1f),
                    mat.getColor(Assimp.AI_MATKEY_COLOR_SPECULAR) ?: Vec4(1f),
                    Renderer.getShader("Geometry")                 // TODO: MOVE/REMOVE
                )
            }
        }
    }

    private fun loadMeshes(scene: AIScene, materials: List<Material>, meshType: MeshType): MeshData {
        val vertices = mutableListOf<Vertex>()
        val indices = mutableListOf<Int>()
        val subMeshes = mutableListOf<SubMesh>()
        val globalAABB = AABB()
        val bones = mutableListOf<Bone>()

        repeat(scene.mNumMeshes()) { i ->
            val m = AIMesh.create(scene.mMeshes()!![i])
            debugMeshLayout(m)
            if (m.mNumFaces() == 0) return@repeat

            val base   = vertices.size
            val offset = indices.size

            // load vertices per submesh
            repeat(m.mNumVertices()) { v ->
                val p = m.mVertices()!![v]
                val normals = m.mNormals()!![v]
                val bitangents = m.mBitangents()!![v]
                val tangents = m.mTangents()!![v]
                val u0 = uvAt(m, v, 0)
                val u1 = if (m.mTextureCoords(1) != null) uvAt(m, v, 1) else u0
                vertices += Vertex(
                    Vec3(p.x(), p.y(), p.z()),
                    Vec3(normals.x(), normals.y(), normals.z()),
                    Vec3(bitangents.x(), bitangents.y(), bitangents.z()),
                    Vec3(tangents.x(), tangents.y(), tangents.z()),
                    u0,
                    u1,
                )
            }

            // load faces per submesh
            repeat(m.mNumFaces()) { f ->
                val face = m.mFaces()[f]
                repeat(face.mNumIndices()) { k ->
                    indices += base + face.mIndices()[k]
                }
            }

            // if the mesh has bones, load them
            if (meshType == MeshType.ANIMATED) {
                repeat(m.mNumBones()) { b ->
                    val bone = AIBone.create(m.mBones()!![b])

                    val weights = mutableListOf<BoneWeight>()
                    repeat(bone.mNumWeights()) { w ->
                        val weight = bone.mWeights()[w]
                        weights += BoneWeight(weight.mVertexId(), weight.mWeight())
                    }

                    bones += Bone(bone.mName().dataString(), bone.mOffsetMatrix().toMat4(), weights)
                }
            }

            val localAABB = computeLocalAABB(vertices, base, m.mNumVertices())
            subMeshes += SubMesh(offset, indices.size - offset, materials[m.mMaterialIndex()], localAABB)
            globalAABB.enclose(localAABB)
        }

        return when (meshType) {
            MeshType.STATIC -> MeshData.Static(vertices, indices, subMeshes, globalAABB)
            MeshType.ANIMATED -> MeshData.Animated(vertices, indices, subMeshes, globalAABB, bones)
            MeshType.PROCEDURAL -> TODO()
        }
    }

    private fun textureUvChannels(mat: AIMaterial): Array<Int> {
        val out = Array(TextureType.entries.size) { 0 }
        val map = mapOf(
            Assimp.aiTextureType_DIFFUSE  to TextureType.Albedo,
            Assimp.aiTextureType_NORMALS  to TextureType.Normal,
            Assimp.aiTextureType_SPECULAR to TextureType.Specular,
        )
        val uvBuf = MemoryUtil.memAllocInt(1)
        for ((aiType, texType) in map) {
            val path = AIString.calloc()
            uvBuf.put(0, 0)
            val ret = Assimp.aiGetMaterialTexture(
                mat, aiType, 0, path,
                null as IntBuffer?,
                uvBuf,
                null as FloatBuffer?,
                null as IntBuffer?,
                null as IntBuffer?,
                null as IntBuffer?
            )
            if (ret == Assimp.aiReturn_SUCCESS && path.dataString().isNotEmpty()) {
                out[texType.ordinal] = uvBuf.get(0).coerceIn(0, 1)
            }
            path.free()
        }
        MemoryUtil.memFree(uvBuf)
        return out
    }

    private fun uvAt(mesh: AIMesh, vert: Int, set: Int): Vec2 {
        val coords = mesh.mTextureCoords(set) ?: mesh.mTextureCoords(0)!!
        val p = coords[vert]
        val comps = mesh.mNumUVComponents().get(set)
        return Vec2(p.x(), if (comps >= 2) p.y() else 0f)
    }

    private fun loadTextures(mat: AIMaterial, resourcePath: String): Array<Texture2D?> {
        val map = mapOf(
            Assimp.aiTextureType_DIFFUSE to TextureType.Albedo,
            Assimp.aiTextureType_NORMALS to TextureType.Normal,
            Assimp.aiTextureType_SPECULAR to TextureType.Specular
        )

        val textures: Array<Texture2D?> = arrayOfNulls(TextureType.entries.size)

        for ((aiType, texType) in map) {
            val path = AIString.calloc()
            val hasTex = Assimp.aiGetMaterialTexture(
                mat, aiType, 0, path,
                null as IntBuffer?, null as IntBuffer?, null as FloatBuffer?,
                null as IntBuffer?, null as IntBuffer?, null as IntBuffer?
            ) == Assimp.aiReturn_SUCCESS && path.dataString().isNotEmpty()

            textures[texType.ordinal] = if (hasTex) {
                Texture2D.create("$meshPath/$resourcePath/${path.dataString()}")
            } else {
                if (aiType == Assimp.aiTextureType_NORMALS) {
                    Texture2D.defaultNormalTexture
                } else {
                    Texture2D.defaultWhiteTexture
                }
            }
            path.free()
        }
        return textures
    }

    private fun AIMaterial.getColor(key: String): Vec4? {
        val color = AIColor4D.create()
        return Assimp.aiGetMaterialColor(this, key, Assimp.aiTextureType_NONE, 0, color)
            .takeIf { it == 0 }
            ?.let { Vec4(color.r(), color.g(), color.b(), color.a()) }
    }

    fun debugMeshLayout(mesh: AIMesh) {
        println("=== Mesh: ${mesh.mName().dataString()} ===")
        println("Vertices:  ${mesh.mNumVertices()}")
        println("Faces:     ${mesh.mNumFaces()}")

        // Positions
        println("Positions: ${mesh.mVertices() != null}")

        // Normals / Tangents
        println("Normals:   ${mesh.mNormals() != null}")
        println("Tangents:  ${mesh.mTangents() != null}")
        println("Bitangents:${mesh.mBitangents() != null}")

        with(mesh.mNormals()!!) {
            println("Normals: [${x()}, ${y()}, ${z()}]")
        }

        // UV channels — this is the key one
        val uvChannelCount = mesh.mNumUVComponents().limit()
        println("UV channels: $uvChannelCount")
        for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
            val components = mesh.mNumUVComponents().get(i)
            val hasCoords = mesh.mTextureCoords(i) != null
            if (hasCoords) {
                println("  TEXCOORD$i — $components components")
            }
        }

        // Color channels
        for (i in 0 until AI_MAX_NUMBER_OF_COLOR_SETS) {
            if (mesh.mColors(i) != null) {
                println("  COLOR$i present")
            }
        }

        // Bone count
        println("Bones: ${mesh.mNumBones()}")

        // Sample first vertex
        mesh.mVertices()?.get(0)?.let { v ->
            println("First vertex pos: (${v.x()}, ${v.y()}, ${v.z()})")
        }
        mesh.mTextureCoords(0)?.get(0)?.let { uv ->
            println("First vertex UV0: (${uv.x()}, ${uv.y()})")
        }
        mesh.mTextureCoords(1)?.get(0)?.let { uv ->
            println("First vertex UV1: (${uv.x()}, ${uv.y()})")
        }
    }

}

private fun computeLocalAABB(verts: List<Vertex>, first: Int, count: Int): AABB =
    AABB().apply { (first until first + count).forEach { enclose(verts[it].position) } }

private fun AIMatrix4x4.toMat4(): Mat4 = Mat4(
    a1(), b1(), c1(), d1(),
    a2(), b2(), c2(), d2(),
    a3(), b3(), c3(), d3(),
    a4(), b4(), c4(), d4()
)

private fun Array<Texture2D?>.sizeNotNull(): Int {
    var count = 0
    this.forEach { if (it != null) count++ }
    return count
}
