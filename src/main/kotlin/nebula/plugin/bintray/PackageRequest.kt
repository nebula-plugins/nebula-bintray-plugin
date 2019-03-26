/*
 * Copyright 2019 Netflix, Inc.
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

data class PackageRequest(val name: String,
                          val desc: String,
                          val labels: List<String>,
                          val licenses: List<String>,
                          val custom_licenses: List<String>,
                          val vcs_url: String,
                          val website_url: String,
                          val issue_tracker_url: String,
                          val public_download_numbers: Boolean = true,
                          val public_stats: Boolean = true
)
