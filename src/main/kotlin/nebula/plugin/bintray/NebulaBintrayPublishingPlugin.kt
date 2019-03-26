package nebula.plugin.bintray

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
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
        setExtensionDefaults(bintray, project)
        setBintrayCredentials(bintray, project)
        val description = if (project.hasProperty("description")) project.description else ""
        val publishPackageToBintray = project.tasks.register<NebulaBintrayPackageTask>("publishPackageToBintray") {
            user.set(bintray.user)
            apiKey.set(bintray.apiKey)
            apiUrl.set(bintray.apiUrl)
            pkgName.set(bintray.pkgName)
            repo.set(bintray.repo)
            userOrg.set(bintray.userOrg)
            version.set(project.version as String)
            desc.set(description)
            name.set(project.name)
            licenses.set(bintray.licenses)
            customLicenses.set(bintray.customLicenses)
            labels.set(bintray.labels)
            websiteUrl.set(bintray.websiteUrl)
            issueTrackerUrl.set(bintray.issueTrackerUrl)
            vcsUrl.set(bintray.vcsUrl)
            autoPublishWaitForSeconds.set(bintray.autoPublishWaitForSeconds)
            onlyIf { bintray.autoPublish.getOrElse(false) }
        }

        val publishVersionToBintray = project.tasks.register<NebulaBintrayVersionTask>("publishVersionToBintray") {
            user.set(bintray.user)
            apiKey.set(bintray.apiKey)
            apiUrl.set(bintray.apiUrl)
            pkgName.set(bintray.pkgName)
            repo.set(bintray.repo)
            userOrg.set(bintray.userOrg)
            version.set(project.version as String)
            autoPublishWaitForSeconds.set(bintray.autoPublishWaitForSeconds)
            onlyIf { bintray.autoPublish.getOrElse(false) }
        }

        project.afterEvaluate {
            project.extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        if (!bintray.hasSubject()) {
                            project.logger.warn("Skipping adding Bintray repository - Neither bintray.user or bintray.userOrg defined")
                        } else {
                            val subject = bintray.subject()
                            name = "Bintray"
                            url = project.uri("${bintray.apiUrl}/content/$subject/${bintray.repo.get()}/${bintray.pkgName.get()}/${project.version}")
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
                    val repoUrl = "${bintray.apiUrl}/content/$subject/${bintray.repo.get()}/${bintray.pkgName.get()}/${project.version}"
                    if (repository.url == project.uri(repoUrl)) {
                        finalizedBy(publishVersionToBintray)
                    }
                }
            }
        }
    }

    private fun setExtensionDefaults(bintray: BintrayExtension, project: Project) {
        if(!bintray.repo.isPresent) {
            bintray.repo.set("gradle-plugins")
        }
        if(!bintray.userOrg.isPresent) {
            bintray.userOrg.set("nebula")
        }
        if(!bintray.websiteUrl.isPresent) {
            bintray.websiteUrl.set("https://github.com/nebula-plugins/${project.name}")
        }
        if(!bintray.issueTrackerUrl.isPresent) {
            bintray.issueTrackerUrl.set("https://github.com/nebula-plugins/${project.name}/issues")
        }
        if(!bintray.vcsUrl.isPresent) {
            bintray.vcsUrl.set("https://github.com/nebula-plugins/${project.name}.git")
        }
        if(!bintray.licenses.isPresent) {
            bintray.licenses.set(listOf("Apache-2.0"))
        }
        if(!bintray.labels.isPresent) {
            bintray.labels.set(listOf("gradle", "nebula"))
        }
    }
    private fun setBintrayCredentials(bintray: BintrayExtension, project: Project) {
        if(project.hasProperty("bintray.user")) {
            bintray.user.set(project.prop("bintray.user"))
        } else if(project.hasProperty("bintrayUser")) {
            bintray.user.set(project.prop("bintray.user"))
        }

        if(project.hasProperty("bintray.apiKey")) {
            bintray.apiKey.set(project.prop("bintray.apiKey"))
        } else if(project.hasProperty("bintrayKey")) {
            bintray.apiKey.set(project.prop("bintrayKey"))
        }
    }

    private fun Project.prop(s: String): String? = project.findProperty(s) as String?
}
