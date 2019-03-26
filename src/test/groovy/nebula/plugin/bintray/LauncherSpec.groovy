package nebula.plugin.bintray

import nebula.test.IntegrationSpec
import spock.lang.Ignore

class LauncherSpec extends IntegrationSpec {
    @Ignore
    File createSubProject(String name, String buildFile = null) {
        settingsFile << "\ninclude '${name}'"

        def sub = new File(projectDir, name)
        sub.mkdirs()

        if (buildFile) {
            new File(sub, 'build.gradle') << buildFile
        }

        return sub
    }

    void writeHelloWorld(String dottedPackage = 'netflix.hello') {
        writeHelloWorld(dottedPackage, getProjectDir())
    }
}
