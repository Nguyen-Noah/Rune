package rune.asset

import rune.core.UUID

@JvmInline
value class AssetHandle(private val uuid: UUID = UUID()) {
    constructor(value: ULong): this(UUID(value))

    val value: UUID get() = uuid
}