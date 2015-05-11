nebula-bintray-plugin
=====================

Additional Bintray tasks and defaults for nebula projects

## Usage

### Applying the Plugin

To include, add the following to your build.gradle

    buildscript {
      repositories { jcenter() }

      dependencies {
        classpath 'com.netflix.nebula:nebula-bintray-plugin:2.4.+'
      }
    }

    apply plugin: 'nebula-bintray'

### Applies the Following Plugins

* nebula-bintray-publishing
* nebula-bintray-sync-publishing
* nebula-ojo-publishing

nebula-bintray-publishing
=========================

## Usage

Sets defaults on many of the fields from com.jfrog.bintray.gradle:gradle-bintray-plugin. You can still configure the bintray extension as in the [gradle-bintray-plugin](https://github.com/bintray/gradle-bintray-plugin)

### Defaults

    bintray {
      user = <user from hidden properties file>
      key = <key from hidden properties file>

      publications = ['mavenNebula']
      dryRun = false
      publish = true
      pkg {
        repo = 'gradle-plugins'
        userOrg = 'nebula'
        name = 'project.name'
        desc = project.description
        websiteUrl = 'https://github.com/nebula-plugins/${project.name}'
        issueTrackerUrl = 'https://github.com/nebula-plugins/${project.name}/issues'
        vcsUrl = 'https://github.com/nebula-plugins/${project.name}.git'
        licenses = ['Apache-2.0']
        labels = ['gradle', 'nebula']
        publicDownloadNumbers = true
        attributes = [:]
        version {
          name = project.version
          vcsTag = project.version
          attributes = [:]
        }
      }
    }

nebula-bintray-sync-publishing
==============================

Syncs the bintray publish to a mavenCentral publish.

nebula-ojo-publishing
=====================

Publish snapshots to oss.jfrog.org
