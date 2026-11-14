package dev.lapis256.hauyne.example

import dev.lapis256.hauyne.config.Config
import dev.lapis256.hauyne.config.generated.ConfigMarker

@Config
class Example {
    fun generated() = ConfigMarker.generated
}
