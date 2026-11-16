import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
    alias(libs.plugins.mutflow)
}

mutflow {
    targets = listOf(
        "dev.lapis256.hauyne.buildlogic.metadata.VersionRangeKt",
        "dev.lapis256.hauyne.buildlogic.metadata.HauyneDependency",
        "dev.lapis256.hauyne.buildlogic.metadata.HauyneDependenciesKt",
        "dev.lapis256.hauyne.buildlogic.metadata.GenerateFmlModMetadata",
        "dev.lapis256.hauyne.buildlogic.metadata.HauyneModMetadataExtension",
    )
    verificationMode = "ACCUMULATE"
    failOnSurvivors = false
}

// internal メソッドの名前を通常コンパイルと揃える。
tasks.withType<KotlinCompile>().matching { it.name == "compileMutatedMainKotlin" }.configureEach {
    dependsOn(
        tasks.named("generateExternalPluginSpecBuilders"),
        tasks.named("generatePrecompiledScriptPluginAccessors"),
        tasks.named("generateScriptPluginAdapters"),
    )
    compilerOptions.moduleName.set(tasks.named<KotlinCompile>("compileKotlin").flatMap { it.compilerOptions.moduleName })
}

tasks.withType<Test>().matching { it.name == "mutflowAccumulateTest" }.configureEach {
    filter.includeTestsMatching("dev.lapis256.hauyne.buildlogic.metadata.VersionRangeTest")
    filter.includeTestsMatching("dev.lapis256.hauyne.buildlogic.metadata.ModMetadataTest")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dokka.gradle)
    implementation(libs.spotless.gradle)
    implementation(libs.ec4j)
    implementation(libs.kotlin.gradle)
    implementation(libs.modpublish.gradle)
    implementation(libs.mdg.gradle)
    implementation(libs.stonecutter.gradle)
    implementation(libs.ktoml)
    implementation(libs.aws.s3)
    implementation(libs.aws.http)
    testImplementation(libs.nightconfig)
    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
