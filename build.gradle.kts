import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

/*
 * Copyright 2014-2019 Netflix, Inc.
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

plugins {
    `kotlin-dsl`
    id("nebula.plugin-plugin") version "9.3.7"
}

description = "Additional Bintray tasks"

group = "com.netflix.nebula"

val contact = Contact("robertoperezalcolea.info")
contact.moniker = "Roberto Perez"
contact.github = "rpalcolea"
contacts {
    people.set("robertoperezalcolea.info", contact)
}


dependencies {
    implementation("net.jodah:failsafe:2.0.1")
    implementation("com.squareup.retrofit2:retrofit:2.5.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.5.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.8.0")
    implementation("com.squareup.okhttp3:logging-interceptor:3.12.2")
    implementation("org.jfrog.buildinfo:build-info-extractor-gradle:latest.release") {
        exclude(module = "groovy-all")
    }
    testImplementation("com.github.tomakehurst:wiremock:2.22.0")
}

pluginBundle {
    website = "https://github.com/nebula-plugins/nebula-bintray-plugin"
    vcsUrl = "https://github.com/nebula-plugins/nebula-bintray-plugin.git"
    description = "Plugins to configure common configuration"
    tags = listOf("nebula")

    mavenCoordinates {
        groupId = "com.netflix.nebula"
        artifactId = "nebula-bintray-plugin"
    }
}

gradlePlugin {
    plugins {
        create("nebulaBintray") {
            id = "nebula.nebula-bintray"
            displayName = "Nebula Bintray plugin"
            description = "Applies nebula.nebula-bintray-publishing and nebula.nebula-ojo-publishing"
            implementationClass = "nebula.plugin.bintray.BintrayPlugin"
        }
        create("nebulaBintrayPublishing") {
            id = "nebula.nebula-bintray-publishing"
            displayName = "Nebula Bintray Publishing plugin"
            description = "Uploads candidate and final artifacts to bintray with Nebula defaults"
            implementationClass = "nebula.plugin.bintray.NebulaBintrayPublishingPlugin"
        }
    }
}

