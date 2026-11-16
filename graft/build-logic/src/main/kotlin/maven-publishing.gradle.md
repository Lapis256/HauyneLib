# build-logic/src/main/kotlin/maven-publishing.gradle.kts

- publication · variable · L20-L20 — val publication = publishing.publications.getByName("maven") as MavenPublication
- coordinate · variable · L21-L21 — val coordinate = "${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.version}"
- base · variable · L22-L22 — val base = "${publication.artifactId}-${publication.version}"
- files · variable · L23-L31 — val files = publication.artifacts.associate { val classifier = it.classifier?.takeIf(String::isNotBlank)?.let { value -> "-$value" }.orEmpty() it.file.relativeTo(rootDir).invariantSeparatorsPath to "$coordinate/$base$classifier.${it.extension}" } + mapOf( layout.buildDirectory.file("publications/maven/pom-default.xml").get().asFile.relativeTo(rootDir).invariantSeparatorsPath to "$coordinate/$base.pom", layout.buildDirectory.file("publications/maven/module.json").get().asFile.relativeTo(rootDir).invariantSeparatorsPath to "$coordinate/$base.module", )
- releaseManifest · variable · L87-L87 — val releaseManifest = providers.gradleProperty("releaseManifest")
- releaseRoot · variable · L88-L88 — val releaseRoot = rootProject.layout.projectDirectory.asFile
