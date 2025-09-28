package rune.core

import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.random.nextULong

@JvmInline
@Serializable
value class UUID(val value: ULong = Random.nextULong()) {
    fun get(): ULong = value
}