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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import nebula.test.IntegrationSpec
import org.junit.Rule
import static com.github.tomakehurst.wiremock.client.WireMock.*

class NebulaBintrayPublishingPluginIntegrationSpec extends IntegrationSpec {

    @Rule
    WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicPort())

    def 'apply plugin'() {
        given:
        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'https://api.bintray.com'
                pkgName = 'my-plugin'
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishPackageToBintray')

        then:
        result.standardOutput.contains('Task :publishPackageToBintray')
    }

    def 'apply plugin without any language plugin fails with clear message'() {
        given:
        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'https://api.bintray.com'
                pkgName = 'my-plugin'
                componentsForExport = ['java']
            }
            
        """

        when:
        def result = runTasksWithFailure('help')

        then:
        result.standardError.contains("You need to apply language plugin to have publishable component named 'java'. It will be most likely: `apply plugin: 'java'`")
    }

    def 'publishes package to bintray'() {
        given:
        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(patch(urlEqualTo("/packages/nebula/gradle-plugins"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))



        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishPackageToBintray')

        then:
        result.standardOutput.contains('ackage my-plugin has been created/updated')
    }

    def 'build fails if bad response from bintray when getting package information'() {
        given:
        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishPackageToBintray')

        then:
        result.standardError.contains('Could not obtain information for package gradle-plugins/nebula/my-plugin')
    }

    def 'build fails if bad response from bintray when creating package information'() {
        given:
        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/packages/nebula/gradle-plugins"))
                .withRequestBody(containing("\"name\":\"build-fails-if-bad-response-from-bintray-when-creating-package-information\""))
                .withRequestBody(containing("\"vcs_url\":\"https://github.com/nebula-plugins/build-fails-if-bad-response-from-bintray-when-creating-package-information.git\""))
                .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")))



        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishPackageToBintray')

        then:
        result.standardError.contains('Could not create or update information for package gradle-plugins/nebula/my-plugin ')
    }

    def 'publishes version to bintray'() {
        given:
        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/maven_central_sync/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .withRequestBody(equalToJson("{\"username\":\"my-mavencentral-username\",\"password\":\"my-mavencentral-password\", \"close\" : \"1\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'         
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishVersionToBintray')

        then:
        result.standardOutput.contains('my-plugin version 1.0.0 has been published')
    }

    def 'publishes version to bintray- no maven sync'() {
        given:
        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/gpg/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                syncToMavenCentral = false
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'         
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishVersionToBintray')

        then:
        result.standardOutput.contains('my-plugin version 1.0.0 has been published')
        result.standardOutput.contains('Task :syncVersionToMavenCentral SKIPPED')
    }

    def 'publishes version to bintray fails if version already exists'() {
        given:
        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/gpg/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/maven_central_sync/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .withRequestBody(equalToJson("{\"username\":\"my-mavencentral-username\",\"password\":\"my-mavencentral-password\", \"close\" : \"1\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'      
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishVersionToBintray')

        then:
        result.standardError.contains('Could not publish 1.0.0 version for package gradle-plugins/nebula/my-plugin')
    }

    def 'fails to publish version if no version'() {
        given:
        buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishVersionToBintray')

        then:
        !result.success
        result.standardError.contains("version or project.version must be set")
    }

    def 'publication fails if repository is not reachable'() {
        given:
        List<String> filePutUris = ["/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.jar",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.jar.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.jar.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.jar.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.jar.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.pom.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.pom.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.pom.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.pom.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/maven-metadata.xml",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/maven-metadata.xml.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/maven-metadata.xml.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/maven-metadata.xml.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/maven-metadata.xml.sha256"]

        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(patch(urlEqualTo("/packages/nebula/gradle-plugins"))
                .withRequestBody(containing("\"name\":\"publication-fails-if-repository-is-not-reachable\""))
                .withRequestBody(containing("\"vcs_url\":\"https://github.com/nebula-plugins/publication-fails-if-repository-is-not-reachable.git\""))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/gpg/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))


        filePutUris.each { fileUri ->
            stubFor(put(urlEqualTo(fileUri))
                    .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")))
        }

        stubFor(put(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publication-fails-if-repository-is-not-reachable/1.0.0/publication-fails-if-repository-is-not-reachable-1.0.0.pom"))
                .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                syncToMavenCentral = false
                componentsForExport = ['java']
            }
           
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishMavenPublicationToBintrayRepository')

        then:
        !result.success
        result.standardError.contains("Failed to publish publication 'maven' to repository 'Bintray'")
        result.standardError.contains("Could not PUT 'http://localhost:")
        result.standardError.contains("Received status code 500 from server: Server Error")

    }

    def 'publishes a plugin to bintray with gradle metadata'() {
        given:
        List<String> filePutUris = ["/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.jar",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.jar.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.jar.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.jar.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.jar.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.pom",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.pom.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.pom.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.pom.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.pom.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.module",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.module.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.module.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.module.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.module.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/maven-metadata.xml",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/maven-metadata.xml.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/maven-metadata.xml.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/maven-metadata.xml.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/maven-metadata.xml.sha512"]


        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(patch(urlEqualTo("/packages/nebula/gradle-plugins"))
                .withRequestBody(containing("\"name\":\"publishes-a-plugin-to-bintray-with-gradle-metadata\""))
                .withRequestBody(containing("\"vcs_url\":\"https://github.com/nebula-plugins/publishes-a-plugin-to-bintray-with-gradle-metadata.git\""))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/gpg/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))


        stubFor(post(urlEqualTo("/maven_central_sync/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .withRequestBody(equalToJson("{\"username\":\"my-mavencentral-username\",\"password\":\"my-mavencentral-password\", \"close\" : \"1\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        filePutUris.each { fileUri ->
            stubFor(put(urlEqualTo(fileUri))
                    .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")))
        }

        stubFor(get(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/maven-metadata.xml"))
                .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                componentsForExport = ['java']
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasksSuccessfully('publishMavenPublicationToBintrayRepository')

        then:
        result.standardOutput.findAll("Uploading").size() >= 3
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.jar")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.pom")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/1.0.0/publishes-a-plugin-to-bintray-with-gradle-metadata-1.0.0.module")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-with-gradle-metadata/maven-metadata.xml")
        result.standardOutput.contains("my-plugin version 1.0.0 has been synced to maven central")
        result.standardOutput.contains("my-plugin version 1.0.0 has been signed with GPG key")
    }

    def 'publishes a plugin to bintray - no maven sync if disabled'() {
        given:
        List<String> filePutUris = ["/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.jar",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.jar.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.jar.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.jar.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.jar.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.module",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.module.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.module.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.module.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.module.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.pom",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.pom.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.pom.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.pom.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.pom.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/maven-metadata.xml",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/maven-metadata.xml.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/maven-metadata.xml.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/maven-metadata.xml.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/maven-metadata.xml.sha1"]

        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(patch(urlEqualTo("/packages/nebula/gradle-plugins"))
                .withRequestBody(containing("\"name\":\"publishes-a-plugin-to-bintray-no-maven-sync-if-disabled\""))
                .withRequestBody(containing("\"vcs_url\":\"https://github.com/nebula-plugins/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled.git\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))


        stubFor(post(urlEqualTo("/gpg/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        filePutUris.each { fileUri ->
            stubFor(put(urlEqualTo(fileUri))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")))
        }

        stubFor(get(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/maven-metadata.xml"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'
                syncToMavenCentral = false
                componentsForExport = ['java']
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasksSuccessfully('publishMavenPublicationToBintrayRepository')

        then:
        result.standardOutput.findAll("Uploading").size() >= 3
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.jar")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled-1.0.0.pom")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-no-maven-sync-if-disabled/maven-metadata.xml")
        !result.standardOutput.contains("my-plugin version 1.0.0 has been synced to maven central")
    }


    def 'publishes a plugin to bintray - no gpg if disabled'() {
        given:
        List<String> filePutUris = ["/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.jar",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.jar.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.jar.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.jar.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.jar.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.module",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.module.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.module.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.module.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.module.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.pom",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.pom.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.pom.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.pom.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.pom.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/maven-metadata.xml",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/maven-metadata.xml.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/maven-metadata.xml.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/maven-metadata.xml.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/maven-metadata.xml.sha1"]


        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(patch(urlEqualTo("/packages/nebula/gradle-plugins"))
                .withRequestBody(containing("\"name\":\"publishes-a-plugin-to-bintray-no-gpg-if-disabled\""))
                .withRequestBody(containing("\"vcs_url\":\"https://github.com/nebula-plugins/publishes-a-plugin-to-bintray-no-gpg-if-disabled.git\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        filePutUris.each { fileUri ->
            stubFor(put(urlEqualTo(fileUri))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")))
        }

        stubFor(get(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/maven-metadata.xml"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'
                syncToMavenCentral = false
                gppSign = false
                componentsForExport = ['java']
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasksSuccessfully('publishMavenPublicationToBintrayRepository')

        then:
        result.standardOutput.findAll("Uploading").size() >= 3
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.jar")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/1.0.0/publishes-a-plugin-to-bintray-no-gpg-if-disabled-1.0.0.pom")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-no-gpg-if-disabled/maven-metadata.xml")
        !result.standardOutput.contains("my-plugin version 1.0.0 has been synced to maven central")
        result.standardOutput.contains("Task :gpgSignVersion SKIPPED")
        !result.standardOutput.contains("my-plugin version 1.0.0 has been signed with GPG key")
    }


    def 'publishes a plugin to bintray - custom gpg phrase'() {
        given:
        List<String> filePutUris = ["/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.jar",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.jar.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.jar.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.jar.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.jar.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.module",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.module.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.module.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.module.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.module.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.pom",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.pom.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.pom.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.pom.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.pom.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/maven-metadata.xml",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/maven-metadata.xml.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/maven-metadata.xml.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/maven-metadata.xml.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/maven-metadata.xml.sha1"]


        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(patch(urlEqualTo("/packages/nebula/gradle-plugins"))
                .withRequestBody(containing("\"name\":\"publishes-a-plugin-to-bintray-custom-gpg-phrase\""))
                .withRequestBody(containing("\"vcs_url\":\"https://github.com/nebula-plugins/publishes-a-plugin-to-bintray-custom-gpg-phrase.git\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))


        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/gpg/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("X-GPG-PASSPHRASE", "supersecret")))

        filePutUris.each { fileUri ->
            stubFor(put(urlEqualTo(fileUri))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")))
        }

        stubFor(get(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/maven-metadata.xml"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'
                syncToMavenCentral = false
                gpgPassphrase = 'supersecret'
                componentsForExport = ['java']
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasksSuccessfully('publishMavenPublicationToBintrayRepository')

        then:
        result.standardOutput.findAll("Uploading").size() >= 3
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.jar")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/1.0.0/publishes-a-plugin-to-bintray-custom-gpg-phrase-1.0.0.pom")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-custom-gpg-phrase/maven-metadata.xml")
        result.standardOutput.contains("my-plugin version 1.0.0 has been signed with GPG key")
    }


    def 'publishes a plugin to bintray - does not fail if gpg fails'() {
        given:
        List<String> filePutUris = ["/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.jar",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.jar.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.jar.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.jar.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.jar.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.module",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.module.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.module.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.module.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.module.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.pom",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.pom.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.pom.sha1",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.pom.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.pom.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/maven-metadata.xml",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/maven-metadata.xml.md5",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/maven-metadata.xml.sha256",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/maven-metadata.xml.sha512",
                                    "/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/maven-metadata.xml.sha1"]

        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(patch(urlEqualTo("/packages/nebula/gradle-plugins"))
                .withRequestBody(containing("\"name\":\"publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails\""))
                .withRequestBody(containing("\"vcs_url\":\"https://github.com/nebula-plugins/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails.git\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))


        stubFor(post(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/publish"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")))

        stubFor(post(urlEqualTo("/gpg/nebula/gradle-plugins/my-plugin/versions/1.0.0"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("X-GPG-PASSPHRASE", "supersecret")))

        filePutUris.each { fileUri ->
            stubFor(put(urlEqualTo(fileUri))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")))
        }

        stubFor(get(urlEqualTo("/content/nebula/gradle-plugins/my-plugin/1.0.0/test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/maven-metadata.xml"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                sonatypeUsername = 'my-mavencentral-username'
                sonatypePassword = 'my-mavencentral-password'
                syncToMavenCentral = false
                gpgPassphrase = 'supersecret'
                componentsForExport = ['java']
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasksSuccessfully('publishMavenPublicationToBintrayRepository')

        then:
        result.standardOutput.findAll("Uploading").size() >= 3
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.jar")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/1.0.0/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails-1.0.0.pom")
        result.standardOutput.contains("test/nebula/netflix/publishes-a-plugin-to-bintray-does-not-fail-if-gpg-fails/maven-metadata.xml")
        !result.standardOutput.contains("my-plugin version 1.0.0 has been signed with GPG key")
        result.standardError.contains("Could not sign 1.0.0 version with GPG key")
    }


    def 'should not publish components when set empty componentsForExport'() {
        given:
          buildFile << """ 
            apply plugin: 'java'
            apply plugin: 'nebula.nebula-bintray'
                
            group = 'test.nebula.netflix'
            version = '1.0.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                componentsForExport = []
            }
            
        """

          writeHelloWorld()

        when:
          def result = runTasksSuccessfully('publishAllPublicationsToBintrayRepository')

        then:
          result.wasUpToDate('publishAllPublicationsToBintrayRepository')
    }



    void writeHelloWorld(String dottedPackage = 'netflix.hello') {
        writeHelloWorld(dottedPackage, getProjectDir())
    }
}
