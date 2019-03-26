package nebula.plugin.bintray

import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class NebulaBintrayPackageTask : NebulaBintrayAbstractTask() {
    @Input
    val name: Property<String> = project.objects.property()
    @Input
    val desc: Property<String> = project.objects.property()
    @Input
    @Optional
    val licenses: Property<List<String>> = project.objects.property()
    @Input
    @Optional
    val customLicenses: Property<List<String>> = project.objects.property()
    @Input
    @Optional
    val labels: Property<List<String>> = project.objects.property()
    @Input
    @Optional
    val websiteUrl: Property<String> = project.objects.property()
    @Input
    @Optional
    val issueTrackerUrl: Property<String> = project.objects.property()
    @Input
    @Optional
    val vcsUrl: Property<String> = project.objects.property()


    @TaskAction
    fun createOrUpdatePackage() {
        val resolvedSubject = resolveSubject()
        val bintrayClient = BintrayClient.Builder()
                .user(user.get())
                .apiUrl(apiUrl.get())
                .apiKey(apiKey.get())
                .build()

        val packageRequest = PackageRequest(
                name = name.get(),
                desc = desc.get(),
                labels = labels.get(),
                licenses = licenses.get(),
                custom_licenses = customLicenses.get(),
                vcs_url = vcsUrl.get(),
                website_url = websiteUrl.get(),
                issue_tracker_url = issueTrackerUrl.get(),
                public_download_numbers = true,
                public_stats = true
        )

        bintrayClient.createOrUpdatePackage(resolvedSubject,  repo.get(), pkgName.get(), packageRequest)
    }
}