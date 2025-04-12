package rune.core

import kotlin.random.Random

data class UUID(val value: Long = Random.nextLong()) {
    fun toLong() = value
}