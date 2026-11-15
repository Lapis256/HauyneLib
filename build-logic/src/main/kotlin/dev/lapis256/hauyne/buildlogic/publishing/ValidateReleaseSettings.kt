package dev.lapis256.hauyne.buildlogic.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ValidateReleaseSettings : DefaultTask() {
    @get:Input
    abstract val required: Property<Boolean>

    @get:Input
    abstract val configurationPresent: MapProperty<String, Boolean>

    @TaskAction
    fun validate() {
        if (required.get()) {
            configurationPresent.get().forEach {
                    (key, present) ->
                require(present) { "$key is required" }
            }
        }
    }
}
