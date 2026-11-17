import dev.lapis256.hauyne.buildlogic.server.SetupDevServer
import net.neoforged.moddevgradle.dsl.InternalModelHelper
import net.neoforged.moddevgradle.dsl.ModDevExtension

afterEvaluate {
    val modDev = extensions.findByType<ModDevExtension>() ?: return@afterEvaluate
    modDev.runs.forEach { run ->
        if (run.type.get().startsWith("server")) {
            run.programArgument("--nogui")
            val setup = tasks.register<SetupDevServer>("setup${run.name.replaceFirstChar { it.uppercaseChar() }}DevServer") {
                group = "mod development"
                description = "Initialize local development server settings and EULA acceptance without overwriting existing files"
                serverDirectory = run.gameDirectory
            }
            @Suppress("UnstableApiUsage")
            tasks.named(InternalModelHelper.nameOfRun(run, "prepare", "run")) {
                dependsOn(setup)
            }
        }
    }
}
