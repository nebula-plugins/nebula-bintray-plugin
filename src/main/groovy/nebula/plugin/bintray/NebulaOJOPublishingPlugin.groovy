/*
 * Copyright 2014-2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.bintray

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPublicationsPlugin

/**
 * Instructions for publishing snapshots oss.jfrog.org
 */
class NebulaOJOPublishingPlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(NebulaBintrayPublishingPlugin);

    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        applyArtifactory()
    }

    def applyArtifactory() {
        def artifactoryPublicationsPlugin = project.plugins.apply(ArtifactoryPublicationsPlugin)

        def convention = artifactoryPublicationsPlugin.getArtifactoryPluginConvention(project) // Will peak into rootProject

        convention.contextUrl = 'https://oss.jfrog.org'

        convention.publish {
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
