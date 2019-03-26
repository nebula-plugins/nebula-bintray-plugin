package nebula.plugin.bintray

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.property

open class NebulaBintrayAbstractTask : DefaultTask() {
    protected companion object {
        const val UNSET = "UNSET"
    }

    @Input
    val user: Property<String> = project.objects.property()
    @Input
    val apiKey: Property<String> = project.objects.property()
    @Input
    val apiUrl: Property<String> = project.objects.property()
    @Input
    val pkgName: Property<String> = project.objects.property()
    @Input
    val repo: Property<String> = project.objects.property()
    @Input
    @Optional
    val userOrg: Property<String> = project.objects.property()
    @Input
    @Optional
    val version: Property<String> = project.objects.property()

    fun resolveSubject() : String {
        val resolvedSubject = userOrg.getOrElse(user.get())
        if (resolvedSubject.isNotSet()) {
           throw GradleException("userOrg or bintray.user must be set")
        }
        return resolvedSubject
    }

    fun resolveVersion() : String {
        val resolvedVersion = version.getOrElse(UNSET)
        if (resolvedVersion == "unspecified" || resolvedVersion.isNotSet()) {
            throw GradleException("version or project.version must be set")
        }
        return resolvedVersion
    }


    private fun String.isNotSet() = this == UNSET

}