# build-logic/src/main/kotlin/hauyne.minecraft-library.gradle.kts

- minecraft · variable · L5-L5 — val minecraft = extensions.create<HauyneMinecraftExtension>("hauyneMinecraft")
- series · variable · L8-L8 — val series = minecraft.minecraftVersion.get().removePrefix("1.").split('.').take(2).joinToString(".")
- artifact · variable · L10-L10 — val artifact = base.archivesName.get()
