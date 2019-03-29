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

import org.gradle.api.provider.ListProperty
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
    val licenses: ListProperty<String> = project.objects.listProperty(String::class.java)
    @Input
    @Optional
    val customLicenses: ListProperty<String> = project.objects.listProperty(String::class.java)
    @Input
    @Optional
    val labels:ListProperty<String> = project.objects.listProperty(String::class.java)
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
        val bintrayClient = BintrayClient.Builder()
                .user(user.get())
                .apiUrl(apiUrl.get())
                .apiKey(apiKey.get())
                .retryDelayInSeconds(15)
                .maxRetries(3)
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

        bintrayClient.createOrUpdatePackage(resolveSubject.get(),  repo.get(), pkgName.get(), packageRequest)
        logger.info("Package ${pkgName.get()} has been created/updated")
    }
}