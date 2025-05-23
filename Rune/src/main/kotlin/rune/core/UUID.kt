package rune.core

import kotlin.random.Random
import kotlin.random.nextULong

@JvmInline
value class UUID(val value: ULong = Random.nextULong()) {
    fun get(): ULong = value
}