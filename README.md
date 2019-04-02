nebula-bintray-plugin
=====================

![Support Status](https://img.shields.io/badge/nebula-supported-brightgreen.svg)
[![Build Status](https://travis-ci.org/nebula-plugins/nebula-bintray-plugin.svg)](https://travis-ci.org/nebula-plugins/nebula-bintray-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/nebula-bintray-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/nebula-bintray-plugin?branch=master)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-bintray-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Additional Bintray tasks and defaults for nebula projects

Usages
------

To apply this plugin if using Gradle 2.1 or newer

    plugins {
      id 'nebula.nebula-bintray' version '7.0.0'
    }


### Applies the Following Plugins

* nebula.nebula-bintray-publishing
* nebula.nebula-ojo-publishing

nebula-bintray-publishing
-------------------------

## Usage

Sets defaults on many of the fields to push `MavenPublication` to bintray. This also covers the new Gradle metadata files.

LICENSE
=======

Copyright 2013-2019 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
