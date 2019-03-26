package nebula.plugin.bintray

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

open class BintrayPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply<NebulaBintrayPublishingPlugin>()
    }
}
