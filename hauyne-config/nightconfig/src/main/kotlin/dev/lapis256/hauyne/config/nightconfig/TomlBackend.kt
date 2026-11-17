package dev.lapis256.hauyne.config.nightconfig

import com.electronwill.nightconfig.core.file.FileConfig
import dev.lapis256.hauyne.config.Backend
import java.nio.file.Path

class TomlBackend(path: Path) :
    Backend,
    AutoCloseable {
    private val config = FileConfig.builder(path).sync().build().also {
        it.load()
    }

    override fun read(key: String): String? = config.get<String>(key)

    override fun write(key: String, value: String) {
        config.set<String>(key, value)
        config.save()
    }

    override fun close() = config.close()
}
