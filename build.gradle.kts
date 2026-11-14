plugins {
    base
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    id("hauyne.formatting")
    id("hauyne.release")
}
