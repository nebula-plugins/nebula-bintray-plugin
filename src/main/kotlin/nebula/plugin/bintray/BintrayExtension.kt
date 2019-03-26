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
    val autoPublish: Property<Boolean> = objects.property()
    val licenses: ListProperty<String> = objects.listProperty(String::class.java)
    val customLicenses: ListProperty<String> = objects.listProperty(String::class.java)
    val labels: ListProperty<String> = objects.listProperty(String::class.java)
    val websiteUrl: Property<String> =objects.property()
    val issueTrackerUrl: Property<String> = objects.property()
    val vcsUrl: Property<String> = objects.property()

    fun hasSubject(): Boolean = userOrg.isPresent || user.isPresent

    fun subject(): String {
        return userOrg.getOrElse(user.get())
    }
}