package nebula.plugin.bintray

import nebula.test.PluginProjectSpec

class NebulaOJOPublishingPluginSpec extends PluginProjectSpec {
    @Override
    String getPluginName() {
        'nebula.nebula-ojo-publishing'
    }

    def 'apply plugin'() {
        when:
        project.plugins.apply('nebula.nebula-ojo-publishing')

        then:
        project.tasks.getByName('artifactoryPublish') != null
    }
}

