package runestone.panels

import rune.asset.AssetHandle
import java.nio.file.Path

data class DirectoryInfo(
    val handle: AssetHandle,
    val parent: DirectoryInfo?,
    val filepath: Path,
    val assets: List<AssetHandle>,
    val subDirectories: Map<AssetHandle, DirectoryInfo>
)