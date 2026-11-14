package dev.lapis256.hauyne.config

annotation class Config

data class ConfigEntry<T>(val key: String, val default: T)

interface Backend {
    fun read(key: String): String?

    fun write(key: String, value: String)
}

interface Codec<T> {
    fun encode(value: T): String

    fun decode(value: String): T
}
