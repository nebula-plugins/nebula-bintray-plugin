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

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.lang.Exception

open class NebulaGpgSignVersionTask : NebulaBintrayAbstractTask() {
    @Input
    val passphrase: Property<String> = project.objects.property()

    @TaskAction
    fun createVersion() {
        val resolvedVersion = resolveVersion.get()
        val resolvedPkgName = pkgName.get()

        val bintrayClient = BintrayClient.Builder()
                .user(user.get())
                .apiUrl(apiUrl.get())
                .apiKey(apiKey.get())
                .retryDelayInSeconds(15)
                .maxRetries(3)
                .readTimeoutInSeconds(readTimeoutInSeconds.get())
                .connectionTimeoutInSeconds(connectionTimeoutInSeconds.get())
                .build()

        try {
            bintrayClient.gpgSignVersion(resolveSubject.get().toLowerCase(), repo.get().toLowerCase(), resolvedPkgName.toLowerCase(), resolvedVersion, passphrase.orNull)
            logger.info("$resolvedPkgName version $resolvedVersion has been signed with GPG key")
        } catch (e: Exception) {
            logger.error("Could not sign ${version.get()} version with GPG key")
        }

    }
}