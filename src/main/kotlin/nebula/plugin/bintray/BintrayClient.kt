package nebula.plugin.bintray

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.http.HttpStatus
import org.gradle.api.GradleException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class BintrayClient private constructor(val bintrayService: BintrayService) {

    data class Builder(
        var user: String? = null,
        var apiKey: String? = null,
        var apiUrl: String? = null) {

        fun user(user: String) = apply { this.user = user }
        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun apiUrl(apiUrl: String) = apply { this.apiUrl = apiUrl }
        fun build() = BintrayClient(bintray(apiUrl!!, user!!, apiKey!!))
    }

    fun createOrUpdatePackage(subject: String, repo: String, pkg: String, packageRequest: PackageRequest) {
        val getPackageResult = bintrayService.getPackage(subject, repo, pkg).execute()
        if(!getPackageResult.isSuccessful && getPackageResult.code() != HttpStatus.SC_NOT_FOUND) {
            throw GradleException("Could not obtain information for package $repo/$subject/$pkg - ${getPackageResult.errorBody()?.string()}")
        }

        val createOrUpdatePackageResult = if(getPackageResult.isSuccessful)  bintrayService.updatePackage(subject, repo, packageRequest).execute() else bintrayService.createPackage(subject, repo, packageRequest).execute()
        if(!createOrUpdatePackageResult.isSuccessful) {
            throw GradleException("Could not create or update information for package $repo/$subject/$pkg - ${getPackageResult.errorBody()?.string()}")
        }
    }

    fun publishVersion(subject: String, repo: String, pkg: String, version: String, publishRequest: PublishRequest) {
        val publishVersionResult = bintrayService.publishVersion(subject, repo, pkg, version, publishRequest).execute()
        if(!publishVersionResult.isSuccessful) {
            throw GradleException("Could not publish $version version for package $repo/$subject/$pkg - ${publishVersionResult.errorBody()?.string()}")
        }
    }
}

fun bintray(apiUrl: String, user: String, apiKey: String): BintrayService = Retrofit.Builder()
        .baseUrl(apiUrl)
        .client(OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .addInterceptor({ chain ->
                    chain.proceed(chain.request().newBuilder()
                            .header("Authorization", Credentials.basic(user, apiKey))
                            .build())
                })
                .build())
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build()
        .create(BintrayService::class.java)