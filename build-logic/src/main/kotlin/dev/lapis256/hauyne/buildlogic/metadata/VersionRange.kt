package dev.lapis256.hauyne.buildlogic.metadata

import org.gradle.api.provider.Provider
import java.io.Serializable

sealed class VersionRange(val versionRange: String) : Serializable {
    final override fun toString(): String = versionRange
}

data object AnyVersion : VersionRange("(,)")

class Equal(version: String) : VersionRange("[${checkedVersion(version)}]")
class GreaterThan(version: String) : VersionRange("(${checkedVersion(version)},)")
class GreaterThanOrEqual(version: String) : VersionRange("[${checkedVersion(version)},)")
class LessThan(version: String) : VersionRange("(,${checkedVersion(version)})")
class LessThanOrEqual(version: String) : VersionRange("(,${checkedVersion(version)}]")
class NotEqual(version: String) : VersionRange("(,${checkedVersion(version)}),($version,)")

class RangeExclusive(min: String, max: String) : VersionRange("(${checkedVersion(min)},${checkedVersion(max)})")
class RangeInclusive(min: String, max: String) : VersionRange("[${checkedVersion(min)},${checkedVersion(max)}]")
class RangeInclusiveMin(min: String, max: String) : VersionRange("[${checkedVersion(min)},${checkedVersion(max)})")
class RangeInclusiveMax(min: String, max: String) : VersionRange("(${checkedVersion(min)},${checkedVersion(max)}]")

class Or(vararg ranges: VersionRange) : VersionRange(ranges.joinToString(",") { it.versionRange }) {
    init {
        require(ranges.isNotEmpty()) { "A version union requires at least one range" }
    }
}

private fun checkedVersion(version: String): String {
    require(version.isNotBlank() && version.none { it.isWhitespace() || it in "[]()," }) { "Invalid version: $version" }
    return version
}

fun String.eq() = Equal(this)
fun String.gt() = GreaterThan(this)
fun String.gte() = GreaterThanOrEqual(this)
fun String.lt() = LessThan(this)
fun String.lte() = LessThanOrEqual(this)
fun String.neq() = NotEqual(this)

infix operator fun String.rangeTo(other: String) = RangeInclusive(this, other)
infix operator fun String.rangeUntil(other: String) = RangeInclusiveMin(this, other)
infix fun String.rangeToExclusive(other: String) = RangeExclusive(this, other)
infix fun String.rangeToMax(other: String) = RangeInclusiveMax(this, other)

fun Provider<String>.eq(): Provider<Equal> = map(::Equal)
fun Provider<String>.gt(): Provider<GreaterThan> = map(::GreaterThan)
fun Provider<String>.gte(): Provider<GreaterThanOrEqual> = map(::GreaterThanOrEqual)
fun Provider<String>.lt(): Provider<LessThan> = map(::LessThan)
fun Provider<String>.lte(): Provider<LessThanOrEqual> = map(::LessThanOrEqual)
fun Provider<String>.neq(): Provider<NotEqual> = map(::NotEqual)

infix operator fun Provider<String>.rangeTo(other: Provider<String>): Provider<RangeInclusive> = zip(other, ::RangeInclusive)
infix operator fun Provider<String>.rangeUntil(other: Provider<String>): Provider<RangeInclusiveMin> = zip(other, ::RangeInclusiveMin)
infix fun Provider<String>.rangeToExclusive(other: Provider<String>): Provider<RangeExclusive> = zip(other, ::RangeExclusive)
infix fun Provider<String>.rangeToMax(other: Provider<String>): Provider<RangeInclusiveMax> = zip(other, ::RangeInclusiveMax)

infix operator fun Provider<String>.rangeTo(other: String): Provider<RangeInclusive> = map { RangeInclusive(it, other) }
infix operator fun Provider<String>.rangeUntil(other: String): Provider<RangeInclusiveMin> = map { RangeInclusiveMin(it, other) }
infix fun Provider<String>.rangeToExclusive(other: String): Provider<RangeExclusive> = map { RangeExclusive(it, other) }
infix fun Provider<String>.rangeToMax(other: String): Provider<RangeInclusiveMax> = map { RangeInclusiveMax(it, other) }
