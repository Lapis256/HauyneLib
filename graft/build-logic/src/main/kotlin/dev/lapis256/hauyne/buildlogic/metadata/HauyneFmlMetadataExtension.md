# build-logic/src/main/kotlin/dev/lapis256/hauyne/buildlogic/metadata/HauyneFmlMetadataExtension.kt

- HauyneFmlLoader · enum · L9-L12 — enum class HauyneFmlLoader(val modId: String, val metadataFileName: String)
- HauyneFmlMetadataExtension · class · L14-L20 — abstract class HauyneFmlMetadataExtension
- HauyneForgeMetadataExtension · class · L22-L27 — abstract class HauyneForgeMetadataExtension @Inject constructor(objects: ObjectFactory) : HauyneFmlMetadataExtension()
- dependencies · method · L26-L26 — fun dependencies(action: Action<HauyneDependencies>)
- HauyneNeoforgeMetadataExtension · class · L29-L34 — abstract class HauyneNeoforgeMetadataExtension @Inject constructor(objects: ObjectFactory) : HauyneFmlMetadataExtension()
- dependencies · method · L33-L33 — fun dependencies(action: Action<HauyneNeoforgeDependencies>)
