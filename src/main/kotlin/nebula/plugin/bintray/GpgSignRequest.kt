package nebula.plugin.bintray

data class GpgSignRequest(val subject: String, val passphrase: String, val private_key: String)
