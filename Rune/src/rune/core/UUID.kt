package rune.core

import kotlin.random.Random
import kotlin.random.nextULong

data class UUID(val value: ULong = Random.nextULong()) {
    fun toLong() = value
}