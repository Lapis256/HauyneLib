# build-logic/src/main/kotlin/dev-server.gradle.kts

- modDev · variable · L6-L6 — val modDev = extensions.findByType<ModDevExtension>() ?: return@afterEvaluate
- setup · variable · L10-L14 — val setup = tasks.register<SetupDevServer>("setup${run.name.replaceFirstChar { it.uppercaseChar() }}DevServer") { group = "mod development" description = "Initialize local development server settings and EULA acceptance without overwriting existing files" serverDirectory = run.gameDirectory }
