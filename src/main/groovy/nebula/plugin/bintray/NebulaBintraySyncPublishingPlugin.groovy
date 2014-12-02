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

import com.jfrog.bintray.gradle.BintrayHttpClientFactory
import com.jfrog.bintray.gradle.BintrayUploadTask
import groovyx.net.http.HttpResponseDecorator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST

/**
 * If provided with a sonatypeUsername and sonatypePassword, this plugin will enhance the NebulaBintrayPublishingPlugin
 * to sync to Maven Central.
 */
class NebulaBintraySyncPublishingPlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(NebulaBintraySyncPublishingPlugin)

    protected Project project

    @Override
    void apply(Project project) {
        this.project = project

        NebulaBintrayPublishingPlugin bintrayPublishingPlugin = project.plugins.apply(NebulaBintrayPublishingPlugin)

        BintrayUploadTask bintrayUpload = (BintrayUploadTask) project.tasks.find { it instanceof BintrayUploadTask }

        if (project.hasProperty('sonatypeUsername') && project.hasProperty('sonatypePassword')) {
            def sonatypeUsername = project.properties['sonatypeUsername']
            def sonatypePassword = project.properties['sonatypePassword']
            bintrayUpload.doLast {
                if (project != project.rootProject) { // Subproject
                    def rootBintrayUpload = project.rootProject.tasks.find { it instanceof BintrayUploadTask }
                    def hasRootUploadInGraph = project.gradle.taskGraph.hasTask(rootBintrayUpload)
                    // If we're uploading a single project, we want to let the sign
                    if (hasRootUploadInGraph) {
                        return
                    }
                }

                bintrayUpload.with {
                    def http = BintrayHttpClientFactory.create(bintrayUpload.apiUrl, bintrayUpload.user, bintrayUpload.apiKey)
                    def repoPath = "${userOrg ?: user}/$repoName"
                    // /maven_central_sync/:subject/:repo/:package/versions/:version?username=:username&password=:password[&close=0/1]
                    def uploadUri = "/maven_central_sync/$repoPath/$packageName/versions/${project.version}"
                    logger.info("Package Sync: $uploadUri")
                    def successful = false
                    def retries = 0
                    while (!successful && retries < 3) {
                        http.request(POST, JSON) {
                            uri.path = uploadUri
                            uri.query = [username: sonatypeUsername, password: sonatypePassword, close: 1]

                            response.success = { resp ->
                                successful = true
                                logger.info("Synced package $packageName.")
                            }
                            response.failure = { HttpResponseDecorator resp ->
                                logger.error("Retrying sync to Maven Central")
                                logger.debug(new StringBuilder()
                                        .append(resp.getStatusLine().toString())
                                        .append("\n")
                                        .append(resp.getHeaders().iterator().join(","))
                                        .append("\n")
                                        .append(resp.getData().toString())
                                        .append("\n")
                                        .toString())
                                retries++
                            }
                        }
                    }
                    if (!successful) {
                        logger.error("Unable to sync to Maven Central, please do manually at https://bintray.com/$userOrg/$repoName/${project.name}/${project.version}/view/central")
                    }
                }
            }
        }
    }
}