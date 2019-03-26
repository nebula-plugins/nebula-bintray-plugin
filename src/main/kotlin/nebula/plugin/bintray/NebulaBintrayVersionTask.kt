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

import org.gradle.api.tasks.TaskAction

open class NebulaBintrayVersionTask : NebulaBintrayAbstractTask() {

    @TaskAction
    fun createVersion() {
        val resolvedVersion = resolveVersion.get()
        val resolvedPkgName = pkgName.get()

        val bintrayClient = BintrayClient.Builder()
                .user(user.get())
                .apiUrl(apiUrl.get())
                .apiKey(apiKey.get())
                .build()


        bintrayClient.publishVersion(resolveSubject.get(), repo.get(), resolvedPkgName, resolvedVersion, PublishRequest())
        logger.info("$resolvedPkgName version $resolvedVersion has been published")
    }
}