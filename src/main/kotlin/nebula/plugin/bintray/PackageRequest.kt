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
