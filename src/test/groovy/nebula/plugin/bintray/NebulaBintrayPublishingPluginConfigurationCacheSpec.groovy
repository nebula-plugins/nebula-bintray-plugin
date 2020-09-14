package nebula.plugin.bintray

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import nebula.test.IntegrationTestKitSpec
import org.junit.Rule
import static com.github.tomakehurst.wiremock.client.WireMock.*

class NebulaBintrayPublishingPluginConfigurationCacheSpec extends IntegrationTestKitSpec {

    @Rule
    WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicPort())

    def 'allows to publish when enabling configuration cache'() {
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
            plugins {
                id 'nebula.nebula-bintray'
                id 'java'
            }
                
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
        runTasks('--configuration-cache', 'publishPackageToBintray')
        def result = runTasks('--configuration-cache', 'publishPackageToBintray')

        then:
        result.output.contains('Reusing configuration cache')
        result.output.contains('my-plugin has been created/updated')
    }

}
