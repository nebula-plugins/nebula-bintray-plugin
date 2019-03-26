package nebula.plugin.bintray

import groovy.lang.GroovyObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.delegateClosureOf
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

class NebulaOJOPublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ArtifactoryPlugin::class.java)

        if (project == project.rootProject) {
            configureArtifactory(project)
        }
    }

    private fun configureArtifactory(project: Project) {
        val artifactoryConvention = project.convention.plugins.get("artifactory") as ArtifactoryPluginConvention

        artifactoryConvention.setContextUrl("https://oss.jfrog.org")
        artifactoryConvention.publish(delegateClosureOf<PublisherConfig> {
            repository(delegateClosureOf<GroovyObject> {
                setProperty("repoKey", "oss-snapshot-local")
                if (project.hasProperty("bintrayUser")) {
                    setProperty("username", project.property("bintrayUser"))
                    setProperty("password", project.property("bintrayKey"))
                }
            })
        })
    }
}