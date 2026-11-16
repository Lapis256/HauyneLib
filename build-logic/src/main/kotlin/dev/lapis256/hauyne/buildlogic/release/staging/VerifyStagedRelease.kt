package dev.lapis256.hauyne.buildlogic.release.staging

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class VerifyStagedRelease : DefaultTask() {
    @get:Internal
    abstract val stagingDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        ReleaseArtifacts.verifyStaging(stagingDirectory.get().asFile)
    }
}
