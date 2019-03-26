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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
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

    @Internal
    val resolveSubject : Provider<String> = user.map {
        val resolvedSubject = userOrg.getOrElse(user.get())
        if (resolvedSubject.isNotSet()) {
            throw GradleException("userOrg or bintray.user must be set")
        }
         resolvedSubject
    }

    @Internal
    val resolveVersion : Provider<String> = version.map {
        val resolvedVersion = version.getOrElse(UNSET)
        if (resolvedVersion == "unspecified" || resolvedVersion.isNotSet()) {
            throw GradleException("version or project.version must be set")
        }
         resolvedVersion
    }

    private fun String.isNotSet() = this == UNSET

}