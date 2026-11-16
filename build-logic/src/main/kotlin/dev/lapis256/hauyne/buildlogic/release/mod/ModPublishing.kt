package dev.lapis256.hauyne.buildlogic.release.mod

import dev.lapis256.hauyne.buildlogic.release.staging.ReleaseArtifacts
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PublishContext
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class ModPublishing @Inject constructor(private val workers: WorkerExecutor, private val objects: ObjectFactory) {
    fun upload(platform: Platform, result: File) {
        val queue = workers.noIsolation()
        val resultFile = objects.fileProperty().fileValue(result).get()
        platform.publish(PublishContext(queue, resultFile))
        queue.await()
    }

    companion object {
        fun stagedJar(root: Project, publication: MavenPublication): File = root.layout.buildDirectory.get().asFile.resolve(
            "staging/maven/${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.version}/" +
                "${publication.artifactId}-${publication.version}.jar",
        )

        fun configure(project: Project, task: PublishModTask) {
            val platform = task.platform
            require(platform is Modrinth || platform is Curseforge) { "Unsupported Mod platform: ${platform.name}" }
            platform.maxRetries.set(1)
            task.description = "Publish a verified staged JAR; CurseForge requires CURSEFORGE_TOKEN and CURSEFORGE_API_KEY"
            // 標準アクションは dryRun でも書き込むため、同じ Platform の送信処理だけを再利用する。
            task.actions.clear()
            task.onlyIf("Mod publishing dryRun does not write files or access APIs") {
                validateConfiguration(platform)
                if (task.dryRun.get()) {
                    task.logger.lifecycle("Dry run ${task.path}: ${platform.file.get().asFile}")
                    false
                } else {
                    true
                }
            }
            val runner = project.objects.newInstance(ModPublishing::class.java)
            task.doLast {
                require(!platform.accessToken.orNull.isNullOrBlank()) { "Mod upload accessToken is required" }
                val release = release(project, task)
                release.run(false) { runner.upload(platform, release.uploadResult) }
            }
            val verification = project.tasks.register("verify${task.name.removePrefix("publish")}PublishedJar") {
                group = "verification"
                mustRunAfter(task)
                doLast { release(project, task).run(false) }
            }
            project.rootProject.tasks.matching { it.name == "verifyPublishedJars" }.configureEach { dependsOn(verification) }
        }

        private fun validateConfiguration(platform: Platform) {
            platform.validateInputs()
            val projectId = when (platform) {
                is Modrinth -> platform.projectId.get().also {
                    require(it.matches(Regex("[0-9a-zA-Z]{8}"))) { "Invalid Modrinth project ID" }
                    require(platform.minecraftVersions.get().isNotEmpty()) { "Missing Minecraft versions" }
                }

                is Curseforge -> platform.projectId.get().also {
                    require(it.toLongOrNull()?.let { value -> value > 0 } == true) { "Invalid CurseForge project ID" }
                    require(platform.minecraftVersions.get().isNotEmpty()) { "Missing Minecraft versions" }
                }

                else -> error("Unsupported Mod platform")
            }
            require(projectId.isNotBlank() && platform.version.get().isNotBlank() && platform.displayName.get().isNotBlank()) {
                "Missing Mod publishing identity"
            }
            platform.type.get()
            platform.changelog.get()
            require(platform.modLoaders.get().isNotEmpty()) { "Missing Mod loaders" }
            require(platform.file.get().asFile.isFile) { "Missing staged Mod JAR" }
            require(platform.additionalFiles.isEmpty) { "Additional Mod files are not part of the staged Maven JAR contract" }
        }

        private fun release(project: Project, task: PublishModTask): ModRelease {
            val rootBuild = project.rootProject.layout.buildDirectory.get().asFile
            val staging = rootBuild.resolve("staging")
            val platform = task.platform
            val jar = platform.file.get().asFile
            val kind: String
            val projectId: String
            val endpoint: String
            when (platform) {
                is Modrinth -> {
                    kind = "modrinth"
                    projectId = platform.projectId.get()
                    endpoint = platform.apiEndpoint.get()
                }

                is Curseforge -> {
                    kind = "curseforge"
                    projectId = platform.projectId.get()
                    endpoint = "https://api.curseforge.com/v1"
                }

                else -> error("Unsupported Mod platform")
            }
            return ModRelease(
                kind,
                projectId,
                platform.version.get(),
                platform.displayName.get(),
                jar,
                rootBuild.resolve("release/results"),
                expected = run {
                    val path = jar.relativeTo(staging.resolve("maven")).invariantSeparatorsPath
                    val hashes = ReleaseArtifacts.readManifest(staging)
                    checkNotNull(hashes[path]) { "Mod JAR is missing from verified staging: $path" }
                },
                client = {
                    val token = if (platform is Curseforge) {
                        project.providers.environmentVariable("CURSEFORGE_API_KEY").orNull
                    } else {
                        platform.accessToken.orNull
                    }
                    require(!token.isNullOrBlank()) { "${kind.uppercase()} query credential is required" }
                    ModReleaseClient(kind, projectId, token, endpoint)
                },
            )
        }
    }
}
