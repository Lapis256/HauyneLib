package dev.lapis256.hauyne.buildlogic.release.prepare

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class PrepareRelease : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    abstract val bump: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    private fun run(vararg args: String): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            workingDir(rootDirectory.get().asFile)
            commandLine(*args)
            standardOutput = output
        }
        return output.toString(Charsets.UTF_8).trim()
    }

    @TaskAction
    fun prepare() {
        val root = rootDirectory.get().asFile
        val source = root.resolve("gradle.properties").readText()
        val tag = nextTag(currentVersion(source), bump.get())
        require(tag.matches(Regex("v\\d+\\.\\d+\\.\\d+"))) { "Invalid release tag: $tag" }
        generateChangelogs(root, tag)
        writeReleaseVersion(root, source, tag)
        logger.lifecycle("Release prepared: $tag")
    }

    private fun currentVersion(source: String): String {
        val versions = Regex("^modVersion=(\\d+\\.\\d+\\.\\d+)$", RegexOption.MULTILINE).findAll(source).toList()
        require(versions.size == 1) { "modVersion must contain one x.y.z version" }
        return versions.single().groupValues[1]
    }

    private fun nextTag(current: String, mode: String): String {
        require(mode in listOf("auto", "major", "minor", "patch")) { "Invalid release bump: $mode" }
        val tags = run("git", "tag", "--merged", "HEAD", "--list", "v*", "--sort=-version:refname")
            .lines().filter { it.matches(Regex("v\\d+\\.\\d+\\.\\d+")) }
        if (tags.isEmpty()) return initialTag(current, mode)
        require(tags.first() == "v$current") { "Latest release tag does not match modVersion" }
        return gitCliff("--bump", mode, "--bumped-version").also {
            require(it != tags.first()) { "No changes to release" }
        }
    }

    private fun initialTag(current: String, mode: String): String {
        // 初回の自動リリースでは Gradle に宣言済みのバージョンを使う。
        val version = current.split('.').map(String::toInt).toMutableList()
        if (mode != "auto") {
            val index = listOf("major", "minor", "patch").indexOf(mode)
            version[index]++
            for (i in index + 1..2) version[i] = 0
        }
        return "v${version.joinToString(".")}"
    }

    private fun generateChangelogs(root: File, tag: String) {
        root.resolve("build/release").mkdirs()
        gitCliff("--tag", tag, "--output", "CHANGELOG.md")
        gitCliff("--tag", tag, "--unreleased", "--strip", "header", "--output", "build/release/notes.md")
    }

    private fun writeReleaseVersion(root: File, source: String, tag: String) {
        root.resolve("gradle.properties").writeText(
            source.replace(Regex("^modVersion=.*$", RegexOption.MULTILINE), "modVersion=${tag.drop(1)}"),
        )
        root.resolve("build/release/tag").writeText("$tag\n")
    }

    private fun gitCliff(vararg args: String): String = run("git-cliff", "--offline", "--use-branch-tags", "--config", "cliff.toml", *args)
}
