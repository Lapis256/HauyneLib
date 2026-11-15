package dev.lapis256.hauyne.buildlogic.minecraft

import java.util.Properties

data class HauyneTarget(val series: String, val minecraft: String, val loader: String, val release: Boolean) {
    val modules: List<String>
        get() = listOf(":hauyne-config:minecraft", ":hauyne-config:$loader", ":hauyne-lib:$loader")
}

class HauyneTargets(text: String) {
    private val properties = Properties().apply { load(text.reader()) }
    val default: String = requireNotNull(properties.getProperty("default"))
    val all: List<HauyneTarget> = properties.stringPropertyNames().filter { it != "default" }.sortedDescending().map { series ->
        require(series.matches(Regex("[0-9]+\\.[0-9]+"))) { "Invalid series: $series" }
        val fields = properties.getProperty(series).split(',')
        require(fields.size == 3 && fields[1] in listOf("forge", "neoforge")) { "Invalid target: $series" }
        require(fields[0].matches(Regex("[0-9]+(\\.[0-9]+)+"))) { "Invalid Minecraft version: $series" }
        HauyneTarget(series, fields[0], fields[1], fields[2].toBooleanStrict())
    }

    fun isReleaseProject(path: String): Boolean = all.none { target ->
        !target.release && target.modules.any { "$it:${target.series}" == path }
    }

    init {
        require(all.any { it.series == default }) { "Unknown default target: $default" }
        require(all.any { it.release }) { "No release targets" }
    }
}
