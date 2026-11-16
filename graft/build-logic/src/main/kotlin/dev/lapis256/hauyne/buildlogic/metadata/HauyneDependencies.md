# build-logic/src/main/kotlin/dev/lapis256/hauyne/buildlogic/metadata/HauyneDependencies.kt

- DependencyKind · enum · L12-L12 — enum class DependencyKind
- Ordering · enum · L13-L13 — enum class Ordering
- Side · enum · L14-L14 — enum class Side
- HauyneDependency · class · L16-L71 — abstract class HauyneDependency @Inject constructor(@get:Input val modId: String, @get:Input val type: DependencyKind)
- relation · method · L48-L53 — internal fun relation(override: Property<DependencyType>): DependencyType?
- validate · method · L55-L70 — internal fun validate(owner: String, loader: HauyneFmlLoader)
- HauyneDependencies · class · L73-L83 — abstract class HauyneDependencies @Inject constructor(private val objects: ObjectFactory)
- require · method · L76-L76 — fun require(modId: String, action: Action<HauyneDependency> = Action {})
- optional · method · L77-L77 — fun optional(modId: String, action: Action<HauyneDependency> = Action {})
- add · method · L78-L82 — protected fun add(id: String, dependencyType: DependencyKind, action: Action<HauyneDependency>)
- HauyneNeoforgeDependencies · class · L85-L88 — abstract class HauyneNeoforgeDependencies @Inject constructor(objects: ObjectFactory) : HauyneDependencies(objects)
- incompatible · method · L86-L86 — fun incompatible(modId: String, action: Action<HauyneDependency> = Action {})
- discouraged · method · L87-L87 — fun discouraged(modId: String, action: Action<HauyneDependency> = Action {})
- validateDependencies · function · L90-L94 — internal fun validateDependencies(dependencies: List<HauyneDependency>, owner: String, loader: HauyneFmlLoader)
