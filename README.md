nebula-bintray-plugin
=====================

[![Build Status](https://travis-ci.org/nebula-plugins/nebula-bintray-plugin.svg)](https://travis-ci.org/nebula-plugins/nebula-bintray-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/nebula-bintray-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/nebula-bintray-plugin?branch=master)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-bintray-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Additional Bintray tasks and defaults for nebula projects

Usages
------

To apply this plugin if using Gradle 2.1 or newer

    plugins {
      id 'nebula.nebula-bintray' version '3.0.0'
    }

If using an older version of Gradle

    buildscript {
      repositories { jcenter() }

      dependencies {
        classpath 'com.netflix.nebula:nebula-bintray-plugin:3.0.0'
      }
    }

    apply plugin: 'nebula.nebula-bintray'

### Applies the Following Plugins

* nebula.nebula-bintray-publishing
* nebula.nebula-ojo-publishing

Gradle Compatibility Tested
---------------------------

| Gradle Version | Works |
| :------------: | :---: |
| 2.2.1          | ??    |
| 2.3            | ??    |
| 2.4            | ??    |
| 2.5            | ??    |
| 2.6            | yes   |

nebula-bintray-publishing
=========================

## Usage

Sets defaults on many of the fields from com.jfrog.bintray.gradle:gradle-bintray-plugin. You can still configure the bintray extension as in the [gradle-bintray-plugin](https://github.com/bintray/gradle-bintray-plugin)

### Defaults

    bintray {
      user = <user from hidden properties file>
      key = <key from hidden properties file>

      publications = ['nebula']
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

nebula-ojo-publishing
=====================

Publish snapshots to oss.jfrog.org

LICENSE
=======

Copyright 2013-2015 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
