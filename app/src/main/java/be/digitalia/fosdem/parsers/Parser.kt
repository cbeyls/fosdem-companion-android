package be.digitalia.fosdem.parsers

import okio.BufferedSource

interface Parser<T> {
    @Throws(Exception::class)
    fun parse(source: BufferedSource): T
}