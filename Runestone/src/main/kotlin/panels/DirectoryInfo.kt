package runestone.panels

import rune.core.UUID
import java.nio.file.Path

data class DirectoryInfo(
    val handle: UUID,
    val parent: DirectoryInfo?,
    val filepath: Path,
    val assets: List<UUID>,
    val subDirectories: Map<UUID, DirectoryInfo>
)