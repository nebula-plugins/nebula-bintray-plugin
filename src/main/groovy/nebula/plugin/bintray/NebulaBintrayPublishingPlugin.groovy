/*
 * Copyright 2014 Netflix, Inc.
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

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import com.jfrog.bintray.gradle.BintrayUploadTask
import nebula.plugin.publishing.maven.NebulaBaseMavenPublishingPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal

/**
 * Defaults for publishing the nebula-plugins on bintray
 */
class NebulaBintrayPublishingPlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(NebulaBintrayPublishingPlugin);

    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        def bintrayUpload = addBintray(project)

        project.plugins.withType(NebulaBaseMavenPublishingPlugin) { NebulaBaseMavenPublishingPlugin mavenPublishingPlugin ->
            mavenPublishingPlugin.withMavenPublication { MavenPublicationInternal mavenPublication ->
                // Ensure everything is built before uploading
                bintrayUpload.dependsOn(mavenPublication.publishableFiles)
            }
        }

        // Ensure our versions look like the project status before publishing
        def verifyStatus = project.tasks.create('verifyReleaseStatus')
        verifyStatus.doFirst {
            if(project.status != 'release') {
                throw new GradleException("Project should have a status of release when uploading to bintray")
            }

            def hasSnapshot = project.version.contains('-SNAPSHOT')
            if (hasSnapshot) {
                throw new GradleException("Version (${project.version}) can not have -SNAPSHOT if publishing release")
            }
        }
        bintrayUpload.dependsOn(verifyStatus)
    }

    BintrayUploadTask addBintray(Project project) {
        // Bintray Side
        project.plugins.apply(BintrayPlugin)

        BintrayExtension bintray = project.extensions.getByType(BintrayExtension)
        if (project.hasProperty('bintrayUser')) {
            bintray.user = project.property('bintrayUser')
            bintray.key = project.property('bintrayKey')
        }
        bintray.publish = true
        bintray.publications = ['mavenNebula'] // TODO Assuming this from the other plugin
        bintray.pkg.repo = 'gradle-plugins'
        bintray.pkg.desc = project.hasProperty('description') ? project.description : ''
        bintray.pkg.userOrg = 'nebula'
        bintray.pkg.name = project.name
        bintray.pkg.licenses = ['Apache-2.0']
        bintray.pkg.labels = ['gradle', 'nebula']
        bintray.pkg.websiteUrl = "https://github.com/nebula-plugins/${project.name}"
        bintray.pkg.issueTrackerUrl = "https://github.com/nebula-plugins/${project.name}/issues"
        bintray.pkg.vcsUrl = "https://github.com/nebula-plugins/${project.name}.git"

        BintrayUploadTask bintrayUpload = (BintrayUploadTask) project.tasks.find { it instanceof BintrayUploadTask }
        bintrayUpload.group = 'publishing'

        bintrayUpload
    }
}
