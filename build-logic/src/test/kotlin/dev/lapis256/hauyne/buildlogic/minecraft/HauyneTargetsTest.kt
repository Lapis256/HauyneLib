package dev.lapis256.hauyne.buildlogic.minecraft

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HauyneTargetsTest {
    @Test
    fun releaseSelectionExcludesEveryModuleOfDisabledSeries() {
        val targets = HauyneTargets("default=26.1\n26.1=26.1.2,neoforge,true\n20.1=1.20.1,forge,false")
        for (target in targets.all) {
            for (module in target.modules) {
                if (target.release) {
                    assertTrue(targets.isReleaseProject("$module:${target.series}"))
                } else {
                    assertFalse(targets.isReleaseProject("$module:${target.series}"))
                }
            }
        }
        assertTrue(targets.isReleaseProject(":hauyne-config:core"))
    }
}
