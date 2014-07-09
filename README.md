nebula-bintray
==============

Additional Bintray tasks and defaults for nebula projects

## Usage

### Applying the Plugin

To include, add the following to your build.gradle

    buildscript {
      repositories { jcenter() }

      dependencies {
        classpath 'com.netflix.nebula:nebula-bintray:1.12.+'
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

Sets defaults on many of the fields from com.jfrog.bintray.gradle:gradle-bintray-plugin.  
