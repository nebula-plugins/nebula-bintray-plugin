package nebula.plugin.bintray

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
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

    fun createOrUpdatePackage(subject: String, repo: String, pkg: String, packageRequest: PackageRequest) : Call<Void> {
        if(bintrayService.getPackage(subject, repo, pkg).execute().isSuccessful) {
            return bintrayService.updatePackage(subject, repo, packageRequest)
        } else {
            return bintrayService.createPackage(subject, repo, packageRequest)
        }
    }

    fun publishVersion(subject: String, repo: String, pkg: String, version: String, publishRequest: PublishRequest) : Response<Void> {
        return bintrayService.publishVersion(subject, repo, pkg, version, publishRequest).execute()
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