package rune.asset

import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.lwjgl.assimp.AIColor3D
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.Assimp
import rune.core.Logger
import rune.renderer.Renderer
import rune.renderer.gpu.*
import rune.renderer.renderer3d.*
import rune.renderer.renderer3d.mesh.Vertex
import java.nio.FloatBuffer
import java.nio.IntBuffer

object MeshImporter {

    private val assetPath = "assets/meshes/Zelda"

    fun importStaticMesh(fileName: String): Model {
        val flags = Assimp.aiProcess_Triangulate or
                    //Assimp.aiProcess_FlipUVs or
                    Assimp.aiProcess_GenNormals or
                    Assimp.aiProcess_OptimizeMeshes or
                    Assimp.aiProcess_SortByPType

        // variables
        val scene = Assimp.aiImportFile("$assetPath/$fileName", flags)!!
        val vertices = mutableListOf<Vertex>()
        val indices  = mutableListOf<Int>()
        val subs     = mutableListOf<SubMesh>()
        val globalAABB = AABB()

        val materials = mutableListOf<Material>()

        // * materials
        val aiMaterials = scene.mMaterials()
        repeat(scene.mNumMaterials()) { i ->
            val material = aiMaterials?.get(i)?.let { AIMaterial.create(it) }

            material?.let {
                // * processing the material
                val color: AIColor4D = AIColor4D.create()
                val path: AIString = AIString.calloc()

                // this will always run, but material is AIMaterial?
                Assimp.aiGetMaterialTexture(
                    material,
                    Assimp.aiTextureType_DIFFUSE,
                    0,
                    path,
                    null as IntBuffer?,     // mapping
                    null as IntBuffer?,     // uvindex
                    null as FloatBuffer?,   // blend
                    null as IntBuffer?,     // op
                    null as IntBuffer?,     // mapmode
                    null as IntBuffer?
                )

                val textPath = path.dataString()
                // white texture
                var texture: Texture2D = Texture2D.create(1, 1).apply { setData(0xffffffff.toInt(), 4) }
                if (textPath.isNotEmpty()) {
                    texture = Texture2D.create("$assetPath/$textPath")
                    Logger.warn("Loaded texture: $textPath.")
                }

                val ambient: Vec4 = Assimp.aiGetMaterialColor(
                    material,
                    Assimp.AI_MATKEY_COLOR_AMBIENT,
                    Assimp.aiTextureType_NONE,
                    0,
                    color
                ).takeIf { it == 0 }?.let {
                    Vec4(color.r(), color.g(), color.b(), color.a())
                } ?: Vec4(1f)

                val diffuse: Vec4 = Assimp.aiGetMaterialColor(
                    material,
                    Assimp.AI_MATKEY_COLOR_DIFFUSE,
                    Assimp.aiTextureType_NONE,
                    0,
                    color
                ).takeIf { it == 0 }?.let {
                    Vec4(color.r(), color.g(), color.b(), color.a())
                } ?: Vec4(1f)

                val specular: Vec4 = Assimp.aiGetMaterialColor(
                    material,
                    Assimp.AI_MATKEY_COLOR_SPECULAR,
                    Assimp.aiTextureType_NONE,
                    0,
                    color
                ).takeIf { it == 0 }?.let {
                    Vec4(color.r(), color.g(), color.b(), color.a())
                } ?: Vec4(1f)

                materials += Material(
                    texture,
                    ambient,
                    diffuse,
                    specular,
                    Renderer.getShader("StaticMesh")
                )
            }
        }

        // * Meshes
        // looping through each mesh and pulling the vertices and indices
        repeat(scene.mNumMeshes()) { i ->
            val m = AIMesh.create(scene.mMeshes()!![i])

            if (m.mNumFaces() == 0)
                return@repeat

            val base = vertices.size
            repeat(m.mNumVertices()) { v ->
                val p = m.mVertices()!![v]; val n = m.mNormals()!![v]
                val t = m.mTextureCoords(0)!!.get(v)
                vertices += Vertex(Vec3(p.x(), p.y(), p.z()),
                                   Vec3(n.x(), n.y(), n.z()),
                                   Vec2(t.x(), t.y()))
            }

            val offset = indices.size
            repeat(m.mNumFaces()) { f ->
                val face = m.mFaces()[f]
                repeat(face.mNumIndices()) { k ->
                    indices += base + face.mIndices()[k]
                }
            }

            // saving the submesh
            val count = indices.size - offset
            val local = computeLocalAABB(vertices, base, m.mNumVertices())
            subs += SubMesh(offset, count, materials[m.mMaterialIndex()], local)//m.mMaterialIndex()
            globalAABB.enclose(local)
        }

        // creating the buffer for the entire mesh
        val buffers = MeshBuffers(
            VertexBuffer.create(vertices),
            IndexBuffer.create(indices),
            globalAABB
        )

        val mesh = Mesh(fileName.substringBefore('.'), buffers, subs)

        // TEMP: MOVE THIS ELSEWHERE
        val vao = VertexArray.create(buffers.vbo, bufferLayout {
            attribute("a_Position", 3)
            attribute("a_Normal", 3)
            attribute("a_TexCoords", 2)
            //attribute("a_EntityID", 1)
        }).apply { setIndexBuffer(buffers.ibo) }

        return Model(mesh, vao)
    }
}

private fun computeAABB(verts: List<Vertex>): AABB =
    AABB().apply { verts.forEach { enclose(it.position) } }

private fun computeLocalAABB(verts: List<Vertex>, first: Int, count: Int): AABB =
    AABB().apply { (first until first + count).forEach { enclose(verts[it].position) } }