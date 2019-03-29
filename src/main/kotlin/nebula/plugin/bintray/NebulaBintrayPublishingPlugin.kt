/*
 * Copyright 2019 Netflix, Inc.
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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

open class NebulaBintrayPublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply<MavenPublishPlugin>()
        val bintray = project.extensions.create("bintray", BintrayExtension::class)
        configureBintrayExtensionConventions(bintray, project)
        setBintrayCredentials(bintray, project)
        val description = if (project.hasProperty("description") && project.description != null) project.description else project.name
        val publishPackageToBintray = project.tasks.register<NebulaBintrayPackageTask>("publishPackageToBintray") {
            user.set(bintray.user)
            apiKey.set(bintray.apiKey)
            apiUrl.set(bintray.apiUrl)
            pkgName.set(bintray.pkgName)
            repo.set(bintray.repo)
            userOrg.set(bintray.userOrg)
            version.set(project.version.toString())
            desc.set(description)
            name.set(project.name)
            licenses.set(bintray.licenses)
            customLicenses.set(bintray.customLicenses)
            labels.set(bintray.labels)
            websiteUrl.set(bintray.websiteUrl)
            issueTrackerUrl.set(bintray.issueTrackerUrl)
            vcsUrl.set(bintray.vcsUrl)
        }

        val publishVersionToBintray = project.tasks.register<NebulaBintrayVersionTask>("publishVersionToBintray") {
            user.set(bintray.user)
            apiKey.set(bintray.apiKey)
            apiUrl.set(bintray.apiUrl)
            pkgName.set(bintray.pkgName)
            repo.set(bintray.repo)
            userOrg.set(bintray.userOrg)
            version.set(project.version.toString())
        }

        project.afterEvaluate {
            project.extensions.configure<PublishingExtension> {
                publications {
                    register("maven", MavenPublication::class) {
                        from(components.getByName("java"))
                    }
                }
                repositories {
                    maven {
                        if (!bintray.hasSubject()) {
                            project.logger.warn("Skipping adding Bintray repository - Neither bintray.user or bintray.userOrg defined")
                        } else {
                            name = "Bintray"
                            url = project.uri("${bintray.apiUrl.get()}/maven/${bintray.subject()}/${bintray.repo.get()}/${bintray.pkgName.get()}/${project.version.toString()}")
                            credentials {
                                username = bintray.user.get()
                                password = bintray.apiKey.get()
                            }
                        }
                    }
                }
            }

            project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
                if (!bintray.hasSubject()) {
                    project.logger.warn("Skipping task dependencies setup - Neither bintray.user or bintray.userOrg defined")
                } else {
                    val subject = bintray.subject()
                    val repoUrl = "${bintray.apiUrl.get()}/maven/$subject/${bintray.repo.get()}/${bintray.pkgName.get()}/${project.version.toString()}"
                    if (repository.url == project.uri(repoUrl)) {
                        dependsOn(publishPackageToBintray)
                        finalizedBy(publishVersionToBintray)
                    }
                }
            }
        }
    }

    private fun configureBintrayExtensionConventions(bintray: BintrayExtension, project: Project) {
        bintray.user.convention("MY_USER")
        bintray.apiKey.convention("MY_API_KEY")
        bintray.apiUrl.convention("https://api.bintray.com")
        bintray.pkgName.convention(project.name)
        bintray.repo.convention("gradle-plugins")
        bintray.userOrg.convention("nebula")
        bintray.licenses.convention(listOf("Apache-2.0"))
        bintray.customLicenses.convention(emptyList())
        bintray.labels.convention(listOf("gradle", "nebula"))
        bintray.websiteUrl.convention("https://github.com/nebula-plugins/${project.name}")
        bintray.issueTrackerUrl.convention("https://github.com/nebula-plugins/${project.name}/issues")
        bintray.vcsUrl.convention("https://github.com/nebula-plugins/${project.name}.git")
    }

    private fun setBintrayCredentials(bintray: BintrayExtension, project: Project) {
        if(project.hasProperty("bintrayUser")) {
            bintray.user.set(project.prop("bintrayUser"))
        } else if(project.hasProperty("bintray.user")) {
            bintray.user.set(project.prop("bintray.user"))
        }

        if(project.hasProperty("bintrayKey")) {
            bintray.apiKey.set(project.prop("bintrayKey"))
        } else if(project.hasProperty("bintray.apiKey")) {
            bintray.apiKey.set(project.prop("bintray.apiKey"))
        }
    }

    private fun Project.prop(s: String): String? = project.findProperty(s) as String?
}
