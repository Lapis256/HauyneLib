package dev.lapis256.hauyne.buildlogic.metadata

import me.modmuss50.mpp.PlatformDependency.DependencyType
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

enum class HauyneDependencyType { REQUIRED, OPTIONAL, INCOMPATIBLE, DISCOURAGED }
enum class HauyneDependencyOrdering { NONE, BEFORE, AFTER }
enum class HauyneDependencySide { BOTH, CLIENT, SERVER }

abstract class HauyneDependency @Inject constructor(@get:Input val modId: String, @get:Input val type: HauyneDependencyType) {

    @get:Input abstract val versionRange: Property<String>

    @get:Input abstract val ordering: Property<HauyneDependencyOrdering>

    @get:Input abstract val side: Property<HauyneDependencySide>

    @get:Input @get:Optional
    abstract val reason: Property<String>

    @get:Input @get:Optional
    abstract val curseForgeSlug: Property<String>

    @get:Input @get:Optional
    abstract val modrinthSlug: Property<String>

    @get:Input @get:Optional
    abstract val curseForgeRelation: Property<DependencyType>

    @get:Input @get:Optional
    abstract val modrinthRelation: Property<DependencyType>

    @get:Input @get:Optional
    abstract val modrinthVersionId: Property<String>

    init {
        versionRange.convention("(,)")
        ordering.convention(HauyneDependencyOrdering.NONE)
        side.convention(HauyneDependencySide.BOTH)
    }

    internal fun relation(override: Property<DependencyType>): DependencyType? = override.orNull ?: when (type) {
        HauyneDependencyType.REQUIRED -> DependencyType.REQUIRED
        HauyneDependencyType.OPTIONAL -> DependencyType.OPTIONAL
        HauyneDependencyType.INCOMPATIBLE -> DependencyType.INCOMPATIBLE
        HauyneDependencyType.DISCOURAGED -> null
    }

    internal fun validate(owner: String, loader: HauyneFmlLoader) {
        val id = modId
        require(id.matches(Regex("[a-z][a-z0-9_]{1,63}"))) { "Invalid FML dependency mod ID: $id" }
        require(id !in setOf(owner, "minecraft", loader.modId)) { "Reserved or self dependency: $id" }
        require(versionRange.get().isNotBlank()) { "Empty version range: $id" }
        require(loader != HauyneFmlLoader.FORGE || type in setOf(HauyneDependencyType.REQUIRED, HauyneDependencyType.OPTIONAL)) {
            "Forge does not support $type dependencies: $id"
        }
        for ((slug, relation) in listOf(curseForgeSlug to curseForgeRelation, modrinthSlug to modrinthRelation)) {
            require(slug.orNull == null || slug.get().isNotBlank()) { "Empty dependency slug: $id" }
            require(!relation.isPresent || slug.isPresent) { "Dependency relation requires a slug: $id" }
            require(!slug.isPresent || relation(relation) != null) { "Discouraged dependency requires an explicit publication relation: $id" }
        }
        require(!modrinthVersionId.isPresent || (modrinthSlug.isPresent && modrinthVersionId.get().isNotBlank())) {
            "Modrinth version ID requires a slug and must not be blank: $id"
        }
    }
}

abstract class HauyneDependencies @Inject constructor(private val objects: ObjectFactory) {
    internal val entries: ListProperty<HauyneDependency> = objects.listProperty(HauyneDependency::class.java).convention(emptyList())

    fun require(modId: String, action: Action<HauyneDependency> = Action {}) = add(modId, HauyneDependencyType.REQUIRED, action)
    fun optional(modId: String, action: Action<HauyneDependency> = Action {}) = add(modId, HauyneDependencyType.OPTIONAL, action)
    protected fun add(id: String, dependencyType: HauyneDependencyType, action: Action<HauyneDependency>) {
        val dependency = objects.newInstance(HauyneDependency::class.java, id, dependencyType)
        action.execute(dependency)
        entries.add(dependency)
    }
}

abstract class HauyneNeoforgeDependencies @Inject constructor(objects: ObjectFactory) : HauyneDependencies(objects) {
    fun incompatible(modId: String, action: Action<HauyneDependency> = Action {}) = add(modId, HauyneDependencyType.INCOMPATIBLE, action)
    fun discouraged(modId: String, action: Action<HauyneDependency> = Action {}) = add(modId, HauyneDependencyType.DISCOURAGED, action)
}

internal fun validateDependencies(dependencies: List<HauyneDependency>, owner: String, loader: HauyneFmlLoader) {
    val ids = dependencies.map { it.modId }
    require(ids.distinct().size == ids.size) { "Duplicate dependency mod IDs: $ids" }
    dependencies.forEach { it.validate(owner, loader) }
}
