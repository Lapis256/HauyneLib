package dev.lapis256.hauyne.buildlogic.release.mod

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import java.io.File

object ModPublishing {
    fun stagedJar(root: Project, publication: MavenPublication): File = root.layout.buildDirectory.get().asFile.resolve(
        "staging/maven/${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.version}/" +
            "${publication.artifactId}-${publication.version}.jar",
    )
}
