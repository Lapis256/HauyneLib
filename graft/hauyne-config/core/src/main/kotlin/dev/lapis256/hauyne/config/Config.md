# hauyne-config/core/src/main/kotlin/dev/lapis256/hauyne/config/Config.kt

- Config · interface · L3-L3 — annotation class Config
- ConfigEntry · class · L5-L5 — data class ConfigEntry<T>(val key: String, val default: T)
- Backend · interface · L7-L11 — interface Backend
- read · method · L8-L8 — fun read(key: String): String?
- write · method · L10-L10 — fun write(key: String, value: String)
- Codec · interface · L13-L17 — interface Codec<T>
- encode · method · L14-L14 — fun encode(value: T): String
- decode · method · L16-L16 — fun decode(value: String): T
