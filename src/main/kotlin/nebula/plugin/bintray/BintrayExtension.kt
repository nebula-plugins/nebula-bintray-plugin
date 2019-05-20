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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class BintrayExtension(objects: ObjectFactory) {
    val user: Property<String> = objects.property()
    val apiKey: Property<String> = objects.property()
    val apiUrl: Property<String> = objects.property()
    val pkgName: Property<String> = objects.property()
    val repo: Property<String> = objects.property()
    val userOrg: Property<String> = objects.property()
    val licenses: ListProperty<String> = objects.listProperty(String::class.java)
    val customLicenses: ListProperty<String> = objects.listProperty(String::class.java)
    val labels: ListProperty<String> = objects.listProperty(String::class.java)
    val websiteUrl: Property<String> = objects.property()
    val issueTrackerUrl: Property<String> = objects.property()
    val vcsUrl: Property<String> = objects.property()
    val componentsForExport: ListProperty<String> = objects.listProperty(String::class.java)
    val syncToMavenCentral: Property<Boolean> = objects.property()
    val sonatypeUsername: Property<String> = objects.property()
    val sonatypePassword: Property<String> = objects.property()
    val gppSign:  Property<Boolean> = objects.property()
    val gpgPassphrase:  Property<String?> = objects.property()
    fun hasSubject(): Boolean = userOrg.isPresent || user.isPresent

    fun subject(): String {
        return userOrg.getOrElse(user.get())
    }
}
