package dev.lapis256.hauyne.buildlogic.server

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Initializes user-editable development server files")
abstract class SetupDevServer : DefaultTask() {
    @get:Internal
    abstract val serverDirectory: DirectoryProperty

    @TaskAction
    fun setup() {
        val directory = serverDirectory.get().asFile
        directory.mkdirs()
        for (name in listOf("eula.txt", "server.properties")) {
            val target = directory.resolve(name)
            if (!target.exists()) {
                val template = requireNotNull(javaClass.getResourceAsStream("/dev-server/$name"))
                template.use { input -> target.outputStream().use { input.copyTo(it) } }
            }
        }
    }
}
