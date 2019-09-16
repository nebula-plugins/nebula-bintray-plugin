package nebula.plugin.bintray

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin

class NebulaOJOPublishingPlugin implements Plugin<Project> {
    protected Project project

    @Override
    void apply(Project project) {
        this.project = project
        this.project.plugins.apply(ArtifactoryPlugin)

        if (this.project == this.project.rootProject) {
            configureArtifactory()
        }
    }

    def configureArtifactory() {
        def artifactoryConvention = project.convention.plugins.artifactory

        artifactoryConvention.contextUrl = 'https://oss.jfrog.org'
        artifactoryConvention.publish {
            repository {
                repoKey = 'oss-snapshot-local' //The Artifactory repository key to publish to
                //when using oss.jfrog.org the credentials are from Bintray. For local build we expect them to be found in
                //~/.gradle/gradle.properties, otherwise to be set in the build server
                // Conditionalize for the users who don't have bintray credentials setup
                if (project.hasProperty('bintrayUser')) {
                    username = project.property('bintrayUser')
                    password = project.property('bintrayKey')
                }
            }
            defaults {
                publications 'nebula'
            }
        }
    }
}