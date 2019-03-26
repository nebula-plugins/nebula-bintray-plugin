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
package nebula.plugin.bintray

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Rule
import static com.github.tomakehurst.wiremock.client.WireMock.*

class NebulaBintrayPublishingPluginIntegrationSpec extends LauncherSpec {

    @Rule
    WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicPort())

    def 'apply plugin'() {
        given:
        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0'
            
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

    def 'build fails if bad response from bintray'() {
        given:
        stubFor(get(urlEqualTo("/packages/nebula/gradle-plugins/my-plugin"))
                .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")))


        buildFile << """ 
            apply plugin: 'nebula.nebula-bintray'
            apply plugin: 'java'
                
            group = 'test.nebula.netflix'
            version = '1.0'
            description = 'my plugin'
            
            bintray {
                user = 'nebula-plugins'
                apiKey = 'mykey'
                apiUrl = 'http://localhost:${wireMockRule.port()}'
                pkgName = 'my-plugin'
                autoPublish = true
            }
            
        """

        writeHelloWorld()

        when:
        def result = runTasks('publishPackageToBintray')

        then:
        result.standardError.contains('Could not obtain information for package gradle-plugins/nebula/my-plugin')
    }
}
