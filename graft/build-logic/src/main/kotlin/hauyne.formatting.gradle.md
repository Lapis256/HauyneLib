# build-logic/src/main/kotlin/hauyne.formatting.gradle.kts

- catalog · variable · L13-L13 — val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
- editorConfig · variable · L21-L23 — val editorConfig = ResourcePropertiesService.builder().build().queryProperties( Resource.Resources.ofPath(file("src/main/java/Formatting.java").toPath(), Charsets.UTF_8), ).properties
