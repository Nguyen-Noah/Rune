package rune.asset

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.lwjgl.assimp.*
import rune.project.ProjectManager
import rune.renderer.Renderer
import rune.renderer.TextureType
import rune.renderer.gpu.*
import rune.renderer.renderer3d.*
import rune.renderer.renderer3d.mesh.Vertex
import java.nio.FloatBuffer
import java.nio.IntBuffer

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
                (Assimp.aiProcess_FlipUVs.takeIf { flipUVs } ?: 0)

        return Assimp.aiImportFile("$meshPath/$fileName", flags)!!
    }

    private fun loadMaterials(scene: AIScene, resourcePath: String): List<Material> {
        val aiMaterials = scene.mMaterials() ?: return emptyList()
        return (0 until scene.mNumMaterials()).mapNotNull { i ->
            AIMaterial.create(aiMaterials[i]).let { mat ->
                Material(
                    loadTextures(mat, resourcePath),
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
            if (m.mNumFaces() == 0) return@repeat

            val base   = vertices.size
            val offset = indices.size

            repeat(m.mNumVertices()) { v ->
                val p = m.mVertices()!![v]
                val n = m.mNormals()!![v]
                val t = m.mTextureCoords(0)!!.get(v)
                vertices += Vertex(
                    Vec3(p.x(), p.y(), p.z()),
                    Vec3(n.x(), n.y(), n.z()),
                    Vec2(t.x(), t.y())
                )
            }

            repeat(m.mNumFaces()) { f ->
                val face = m.mFaces()[f]
                repeat(face.mNumIndices()) { k ->
                    indices += base + face.mIndices()[k]
                }
            }

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

    private fun loadTextures(mat: AIMaterial, resourcePath: String): Array<Texture2D?> {
        val map = mapOf(
            Assimp.aiTextureType_DIFFUSE   to TextureType.Albedo,
            Assimp.aiTextureType_NORMALS   to TextureType.Normal,
            Assimp.aiTextureType_SHININESS to TextureType.Specular,
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
                Texture2D.create(1, 1).apply { setData(0xffffffff.toInt(), 4) }
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
}

private fun computeLocalAABB(verts: List<Vertex>, first: Int, count: Int): AABB =
    AABB().apply { (first until first + count).forEach { enclose(verts[it].position) } }

private fun AIMatrix4x4.toMat4(): Mat4 = Mat4(
    a1(), b1(), c1(), d1(),
    a2(), b2(), c2(), d2(),
    a3(), b3(), c3(), d3(),
    a4(), b4(), c4(), d4()
)
