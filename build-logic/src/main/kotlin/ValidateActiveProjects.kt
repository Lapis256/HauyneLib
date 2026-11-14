import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ValidateActiveProjects : DefaultTask() {
    @get:Input
    abstract val activeProjects: MapProperty<String, String>

    @get:Input
    abstract val expectedProjects: MapProperty<String, String>

    @TaskAction
    fun validate() {
        val expected = expectedProjects.get()
        val mismatches = activeProjects.get().filter { (path, active) -> active != expected.getValue(path) }
        check(mismatches.isEmpty()) { "Active projects differ from expected selection: $mismatches. Use switchTo<series> or resetActiveProjects." }
    }
}
