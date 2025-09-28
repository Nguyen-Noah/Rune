package rune.asset

import kotlinx.serialization.Serializable
import rune.core.UUID

@Serializable
data class RuneMesh(
    val id: UUID,
    val submesh: Iterable<Int>
)

class Mesh {
}