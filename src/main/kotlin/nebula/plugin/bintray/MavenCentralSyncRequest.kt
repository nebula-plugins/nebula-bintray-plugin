package nebula.plugin.bintray

data class MavenCentralSyncRequest(val username: String, val password: String, val close: String = "1")
