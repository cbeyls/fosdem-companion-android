package be.digitalia.fosdem.parsers

import okio.BufferedSource

interface Parser<out T> {
    @Throws(Exception::class)
    fun parse(source: BufferedSource): T
}