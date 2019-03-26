package nebula.plugin.bintray

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class NebulaBintrayVersionTask : NebulaBintrayAbstractTask() {

    @TaskAction
    fun createVersion() {
        val resolvedSubject = resolveSubject()
        val resolvedVersion = resolveVersion()
        val resolvedRepoName = repo.get()
        val resolvedPkgName = pkgName.get()

        val bintrayClient = BintrayClient.Builder()
                .user(user.get())
                .apiUrl(apiUrl.get())
                .apiKey(apiKey.get())
                .build()


        val resolvedWait = autoPublishWaitForSeconds.getOrElse(0)
        val result = bintrayClient.publishVersion(resolvedSubject, resolvedRepoName, resolvedPkgName, resolvedVersion, PublishRequest(resolvedWait))
        if (result.isSuccessful) {
            logger.info("$resolvedPkgName version $resolvedVersion has been published")
        } else {
            throw GradleException("Received ${result.code()} attempting to publish $resolvedPkgName version $resolvedVersion")
        }
    }
}