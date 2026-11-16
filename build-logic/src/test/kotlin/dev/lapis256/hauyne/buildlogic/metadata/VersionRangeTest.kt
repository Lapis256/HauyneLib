package dev.lapis256.hauyne.buildlogic.metadata

import io.github.anschnapp.mutflow.MutFlow
import io.github.anschnapp.mutflow.junit.MutFlowTest
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@MutFlowTest
class VersionRangeTest {
    @Test
    fun operatorsPreserveInclusiveExclusiveAndUnionBoundaries() {
        val cases = MutFlow.underTest {
            listOf(
                "[1]" to "1".eq(),
                "(1,)" to "1".gt(),
                "[1,)" to "1".gte(),
                "(,1)" to "1".lt(),
                "(,1]" to "1".lte(),
                "(,1),(1,)" to "1".neq(),
                "[1,2]" to ("1".."2"),
                "[1,2)" to ("1"..<"2"),
                "(1,2)" to ("1" rangeToExclusive "2"),
                "(1,2]" to ("1" rangeToMax "2"),
                "(,1),[2,3]" to Or(LessThan("1"), RangeInclusive("2", "3")),
                "(,)" to AnyVersion,
            )
        }
        for ((expected, range) in cases) {
            assertEquals(expected, range.versionRange)
            assertEquals(expected, range.toString())
        }
    }

    @Test
    fun providerOperatorsRemainLazyAndFollowBothEndpoints() {
        val objects = ProjectBuilder.builder().build().objects
        val min = objects.property(String::class.java)
        val max = objects.property(String::class.java)
        val cases = listOf(
            "[1]" to min.eq(),
            "(1,)" to min.gt(),
            "[1,)" to min.gte(),
            "(,1)" to min.lt(),
            "(,1]" to min.lte(),
            "(,1),(1,)" to min.neq(),
            "[1,2]" to (min..max),
            "[1,2)" to (min..<max),
            "(1,2)" to (min rangeToExclusive max),
            "(1,2]" to (min rangeToMax max),
            "[1,2]" to (min.."2"),
            "[1,2)" to (min..<"2"),
            "(1,2)" to (min rangeToExclusive "2"),
            "(1,2]" to (min rangeToMax "2"),
        )
        min.set("1")
        max.set("2")
        for ((expected, range) in cases) {
            assertEquals(expected, MutFlow.underTest { range.get().versionRange })
        }
        min.set("3")
        max.set("4")
        assertEquals("[3]", MutFlow.underTest { cases.first().second.get().versionRange })
        assertEquals("[3,4]", MutFlow.underTest { cases[6].second.get().versionRange })
    }

    @Test
    fun blankVersionsRangeSyntaxInEndpointsAndEmptyUnionsAreRejected() {
        for (version in listOf("", " ", "[1]", "1,2", "1\n2")) {
            assertThrows(IllegalArgumentException::class.java) { MutFlow.underTest { Equal(version) } }
            assertThrows(IllegalArgumentException::class.java) { MutFlow.underTest { RangeInclusive("1", version) } }
        }
        assertThrows(IllegalArgumentException::class.java) { MutFlow.underTest { Or() } }
    }
}
