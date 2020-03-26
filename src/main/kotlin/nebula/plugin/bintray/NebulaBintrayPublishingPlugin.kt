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

import org.gradle.api.BuildCancelledException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import java.io.File
import java.net.URI

private const val defaultReadTimeoutInSeconds : Long = 900L // 15 minutes
private const val defaultConnectionTimeoutInSeconds : Long = 5L  // 5 seconds

open class NebulaBintrayPublishingPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<MavenPublishPlugin>()
        val bintray = project.rootProject.extensions.findByType(BintrayExtension::class.java) ?: project.rootProject.extensions.create("bintray", BintrayExtension::class)
        configureBintrayExtensionConventions(bintray, project)
        setBintrayCredentials(bintray, project)
        setMavenCentralCredentials(bintray, project)
        setGpgPassphrase(bintray, project)

        val description = if (project.hasProperty("description") && project.description != null) project.description else project.name

        if(project.rootProject == project) {
            project.tasks.register<NebulaBintrayPackageTask>("publishPackageToBintray") {
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
                readTimeoutInSeconds.set(bintray.readTimeoutInSeconds)
                connectionTimeoutInSeconds.set(bintray.connectionTimeoutInSeconds)
            }


            val syncVersionToMavenCentralTask = project.tasks.register<NebulaMavenCentralVersionSyncTask>("syncVersionToMavenCentral") {
                user.set(bintray.user)
                apiKey.set(bintray.apiKey)
                apiUrl.set(bintray.apiUrl)
                pkgName.set(bintray.pkgName)
                repo.set(bintray.repo)
                userOrg.set(bintray.userOrg)
                version.set(project.version.toString())
                sonatypeUsername.set(bintray.sonatypeUsername)
                sonatypePassword.set(bintray.sonatypePassword)
                readTimeoutInSeconds.set(bintray.readTimeoutInSeconds)
                connectionTimeoutInSeconds.set(bintray.connectionTimeoutInSeconds)
                onlyIf { bintray.syncToMavenCentral.get() }
            }


            val publishVersionToBintrayTask = project.tasks.register<NebulaBintrayVersionTask>("publishVersionToBintray") {
                user.set(bintray.user)
                apiKey.set(bintray.apiKey)
                apiUrl.set(bintray.apiUrl)
                pkgName.set(bintray.pkgName)
                repo.set(bintray.repo)
                userOrg.set(bintray.userOrg)
                version.set(project.version.toString())
                readTimeoutInSeconds.set(bintray.readTimeoutInSeconds)
                connectionTimeoutInSeconds.set(bintray.connectionTimeoutInSeconds)
                finalizedBy(syncVersionToMavenCentralTask)
            }

            val gpgSignVersionTask = project.tasks.register<NebulaGpgSignVersionTask>("gpgSignVersion") {
                user.set(bintray.user)
                apiKey.set(bintray.apiKey)
                apiUrl.set(bintray.apiUrl)
                pkgName.set(bintray.pkgName)
                repo.set(bintray.repo)
                userOrg.set(bintray.userOrg)
                version.set(project.version.toString())
                readTimeoutInSeconds.set(bintray.readTimeoutInSeconds)
                connectionTimeoutInSeconds.set(bintray.connectionTimeoutInSeconds)
                if (bintray.gpgPassphrase.isPresent) {
                    passphrase.set(bintray.gpgPassphrase)
                }
                onlyIf { bintray.gppSign.get() }
                finalizedBy(publishVersionToBintrayTask)
            }

            project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
                if (!bintray.hasSubject()) {
                    project.logger.warn("Skipping task dependencies setup - Neither bintray.user or bintray.userOrg defined")
                } else {
                    finalizedBy(gpgSignVersionTask)
                }
            }
        }


        project.plugins.withType(JavaBasePlugin::class.java) {
            project.afterEvaluate {
                project.extensions.configure<PublishingExtension> {
                    publications {
                        bintray.componentsForExport.getOrElse(emptyList())
                                .forEach { componentName: String ->
                                    if (components.names.contains(componentName)) {
                                        val publicationName = if (componentName == "java") "maven" else "maven${componentName.capitalize()}"

                                        if (!publicationName.contains("Marker")) {
                                            register(publicationName, MavenPublication::class) {
                                                from(components.getByName(componentName))
                                            }
                                        }
                                    } else {
                                        throw BuildCancelledException("You need to apply language plugin to have publishable component named '$componentName'. It will be most likely: `apply plugin: '$componentName'`")
                                    }
                                }
                    }

                    repositories {
                        maven {
                            if (!bintray.hasSubject()) {
                                project.logger.warn("Skipping adding Bintray repository - Neither bintray.user or bintray.userOrg defined")
                            } else {
                                name = "Bintray"
                                url = getRepoUrl(project, bintray)
                                credentials {
                                    username = bintray.user.get()
                                    password = bintray.apiKey.get()
                                }

                                //Gradle 6.x emits warnings when repos are not HTTP and not set to allow non-secure protocol. Using reflection to maintain backwards compatibility
                                if (GradleVersion.current().baseVersion >= GradleVersion.version("6.0") && bintray.apiUrl.get().startsWith("http://")) {
                                    (this::class.java).getDeclaredMethod("setAllowInsecureProtocol", Boolean::class.java).invoke(this, true)
                                }
                            }
                        }
                    }
                }

                project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
                    if (!bintray.hasSubject()) {
                        project.logger.warn("Skipping task dependencies setup - Neither bintray.user or bintray.userOrg defined")
                    } else {
                        val repoUrl = getRepoUrl(project, bintray)
                        if (repository.url == project.uri(repoUrl)) {
                            dependsOn(project.rootProject.tasks.named("publishPackageToBintray"))
                            finalizedBy(project.rootProject.tasks.named("gpgSignVersion"))
                        }
                    }
                }
            }
        }
    }

    private fun getRepoUrl(project: Project, bintray: BintrayExtension): URI {
        return if (shouldUseSnapshotRepo(project)) {
            project.uri(bintray.apiUrl.get() + File.separator + bintray.repo.get())
        } else {
            project.uri("${bintray.apiUrl.get()}/content/${bintray.subject()}/${bintray.repo.get()}/${bintray.pkgName.get()}/${project.version.toString()}")
        }
    }

    private fun shouldUseSnapshotRepo(project: Project): Boolean {
        return project.gradle.startParameter.taskNames.contains("snapshot") || project.gradle.startParameter.taskNames.contains(":snapshot")
    }

    private fun configureBintrayExtensionConventions(bintray: BintrayExtension, project: Project) {
        bintray.user.convention("MY_USER")
        bintray.apiKey.convention("MY_API_KEY")
        bintray.apiUrl.convention("https://api.bintray.com")
        bintray.pkgName.convention(project.rootProject.name.toLowerCase())
        bintray.repo.convention("gradle-plugins")
        bintray.userOrg.convention("nebula")
        bintray.licenses.convention(listOf("Apache-2.0"))
        bintray.customLicenses.convention(emptyList())
        bintray.labels.convention(listOf("gradle", "nebula"))
        bintray.websiteUrl.convention("https://github.com/nebula-plugins/${project.rootProject.name}")
        bintray.issueTrackerUrl.convention("https://github.com/nebula-plugins/${project.rootProject.name}/issues")
        bintray.vcsUrl.convention("https://github.com/nebula-plugins/${project.rootProject.name}.git")
        bintray.componentsForExport.convention(emptyList())
        bintray.syncToMavenCentral.convention(true)
        bintray.gppSign.convention(true)
        bintray.gpgPassphrase.convention("")
        bintray.readTimeoutInSeconds.convention(defaultReadTimeoutInSeconds)
        bintray.connectionTimeoutInSeconds.convention(defaultConnectionTimeoutInSeconds)
    }

    private fun setBintrayCredentials(bintray: BintrayExtension, project: Project) {
        if (System.getenv("bintrayUser") != null) {
            bintray.user.set(System.getenv("bintrayUser"))
        } else if (project.hasProperty("bintrayUser")) {
            bintray.user.set(project.prop("bintrayUser"))
        } else if (project.hasProperty("bintray.user")) {
            bintray.user.set(project.prop("bintray.user"))
        }

        if (System.getenv("bintrayKey") != null) {
            bintray.apiKey.set(System.getenv("bintrayKey"))
        } else if (project.hasProperty("bintrayKey")) {
            bintray.apiKey.set(project.prop("bintrayKey"))
        } else if (project.hasProperty("bintray.apiKey")) {
            bintray.apiKey.set(project.prop("bintray.apiKey"))
        }
    }

    private fun setMavenCentralCredentials(bintray: BintrayExtension, project: Project) {
        if (System.getenv("sonatypeUsername") != null) {
            bintray.sonatypeUsername.set(System.getenv("sonatypeUsername"))
        } else if (project.hasProperty("sonatypeUsername")) {
            bintray.sonatypeUsername.set(project.prop("sonatypeUsername"))
        } else if (project.hasProperty("sonatype.username")) {
            bintray.sonatypeUsername.set(project.prop("sonatype.username"))
        }

        if (System.getenv("sonatypePassword") != null) {
            bintray.sonatypePassword.set(System.getenv("sonatypePassword"))
        } else if (project.hasProperty("sonatypePassword")) {
            bintray.sonatypePassword.set(project.prop("sonatypePassword"))
        } else if (project.hasProperty("sonatype.password")) {
            bintray.sonatypePassword.set(project.prop("sonatype.password"))
        }
    }

    private fun setGpgPassphrase(bintray: BintrayExtension, project: Project) {
        if (System.getenv("gpgPassphrase") != null) {
            bintray.gpgPassphrase.set(System.getenv("gpgPassphrase"))
        } else if (project.hasProperty("gpgPassphrase")) {
            bintray.gpgPassphrase.set(project.prop("gpgPassphrase"))
        } else if (project.hasProperty("bintray.version.gpgPassphrase")) {
            bintray.gpgPassphrase.set(project.prop("bintray.version.gpgPassphrase"))
        }
    }

    private fun Project.prop(s: String): String? = project.findProperty(s) as String?
}
