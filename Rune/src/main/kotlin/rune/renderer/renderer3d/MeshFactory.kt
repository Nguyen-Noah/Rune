package rune.renderer.renderer3d

import glm_.vec3.Vec3
import rune.renderer.renderer3d.mesh.Index
import rune.renderer.renderer3d.mesh.Vertex

object MeshFactory {

    fun box(size: Vec3): Pair<Array<Vertex>, Array<Index?>> {
        val vertices: Array<Vertex> = Array(8) { Vertex() }

        // positions
        vertices[0].position = Vec3(-size.x / 2f, -size.y / 2f,  size.z / 2f)
        vertices[1].position = Vec3( size.x / 2f, -size.y / 2f,  size.z / 2f)
        vertices[2].position = Vec3( size.x / 2f,  size.y / 2f,  size.z / 2f)
        vertices[3].position = Vec3(-size.x / 2f,  size.y / 2f,  size.z / 2f)
        vertices[4].position = Vec3(-size.x / 2f, -size.y / 2f, -size.z / 2f)
        vertices[5].position = Vec3( size.x / 2f, -size.y / 2f, -size.z / 2f)
        vertices[6].position = Vec3( size.x / 2f,  size.y / 2f, -size.z / 2f)
        vertices[7].position = Vec3(-size.x / 2f,  size.y / 2f, -size.z / 2f)

        // normals
        vertices[0].normal = Vec3(-1f, -1f,  1f)
        vertices[1].normal = Vec3( 1f, -1f,  1f)
        vertices[2].normal = Vec3( 1f,  1f,  1f)
        vertices[3].normal = Vec3(-1f,  1f,  1f)
        vertices[4].normal = Vec3(-1f, -1f, -1f)
        vertices[5].normal = Vec3( 1f, -1f, -1f)
        vertices[6].normal = Vec3( 1f,  1f, -1f)
        vertices[7].normal = Vec3(-1f,  1f, -1f)

        // indices
        val indices: Array<Index?> = arrayOfNulls(12)
        indices[0]  = Index(0, 1, 2)
        indices[1]  = Index(2, 3, 0)
        indices[2]  = Index(1, 5, 6)
        indices[3]  = Index(6, 2, 1)
        indices[4]  = Index(7, 6, 5)
        indices[5]  = Index(5, 4, 7)
        indices[6]  = Index(4, 0, 3)
        indices[7]  = Index(3, 7, 4)
        indices[8]  = Index(4, 5, 1)
        indices[9]  = Index(1, 0, 4)
        indices[10] = Index(3, 2, 6)
        indices[11] = Index(6, 7, 4)

        return Pair(vertices, indices)
    }

    fun createSphere(radius: Float) {

    }

    fun createCapsule(radius: Float, height: Float) {

    }
}

fun Pair<Array<Vertex>, Array<Index?>>.vbo() =
    this.first

fun Pair<Array<Vertex>, Array<Index?>>.ibo() =
    this.second