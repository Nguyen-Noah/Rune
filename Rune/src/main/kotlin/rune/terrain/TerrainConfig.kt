package rune.terrain

/**
 * Grid of height samples in model space: vertex [ix, iz] uses heights[iz * (gridX + 1) + ix].
 */
data class TerrainConfig(
    val gridX: Int,
    val gridZ: Int,
    val sizeX: Float,
    val sizeZ: Float,
    val heights: FloatArray,
    val meshName: String = "Terrain",
) {
    init {
        require(gridX >= 1 && gridZ >= 1) { "Terrain grid must be at least 1x1 cells" }
        require(heights.size == (gridX + 1) * (gridZ + 1)) {
            "heights size ${heights.size} != ${(gridX + 1) * (gridZ + 1)}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TerrainConfig
        if (gridX != other.gridX) return false
        if (gridZ != other.gridZ) return false
        if (sizeX != other.sizeX) return false
        if (sizeZ != other.sizeZ) return false
        if (meshName != other.meshName) return false
        if (!heights.contentEquals(other.heights)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = gridX
        result = 31 * result + gridZ
        result = 31 * result + sizeX.hashCode()
        result = 31 * result + sizeZ.hashCode()
        result = 31 * result + meshName.hashCode()
        result = 31 * result + heights.contentHashCode()
        return result
    }
}
