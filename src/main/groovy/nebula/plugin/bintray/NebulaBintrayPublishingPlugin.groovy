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

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Defaults for publishing the nebula-plugins on bintray
 */
class NebulaBintrayPublishingPlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(NebulaBintrayPublishingPlugin)

    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        def bintrayUpload = addBintray(project)
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
        bintray.publications = ['nebula']
        bintray.pkg {
            version {
                name = project.getVersion()
                vcsTag = "v${project.getVersion()}"
                gpg {
                    sign = true
                }
                if (project.hasProperty('sonatypeUsername') && project.hasProperty('sonatypePassword')) {
                    def sonatypeUsername = project.property('sonatypeUsername')
                    def sonatypePassword = project.property('sonatypePassword')
                    mavenCentralSync {
                        user = sonatypeUsername
                        password = sonatypePassword
                    }
                }
            }
            publicDownloadNumbers = true

            desc = project.hasProperty('description') ? project.description : ''
            name = project.name

            repo = 'gradle-plugins'
            userOrg = 'nebula'
            licenses = ['Apache-2.0']
            labels = ['gradle', 'nebula']
            websiteUrl = "https://github.com/nebula-plugins/${project.name}"
            issueTrackerUrl = "https://github.com/nebula-plugins/${project.name}/issues"
            vcsUrl = "https://github.com/nebula-plugins/${project.name}.git"
        }

        BintrayUploadTask bintrayUpload = (BintrayUploadTask) project.tasks.find { it instanceof BintrayUploadTask }
        bintrayUpload.group = 'publishing'

        bintrayUpload
    }
}
