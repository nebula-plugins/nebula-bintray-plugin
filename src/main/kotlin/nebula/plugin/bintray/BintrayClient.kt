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

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.jodah.failsafe.RetryPolicy
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.GradleException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.time.Duration
import net.jodah.failsafe.Failsafe
import org.slf4j.LoggerFactory


class BintrayClient(var bintrayService: BintrayService, retryConfig: RetryConfig) {
    var retryPolicy : RetryPolicy<Any>

    val logger = LoggerFactory.getLogger(BintrayClient::class.java)

    init {
        this.retryPolicy = RetryPolicy()
                .handle(IOException::class.java)
                .withDelay(Duration.ofSeconds(retryConfig.retryDelayInSeconds))
                .withMaxRetries(retryConfig.maxRetries)
                .onFailedAttempt({ e -> logger.error("Trying to publish a new version to Bintray failed.", e.lastFailure) })
                .onRetry({ e -> logger.info("Retrying to publish a new version to Bintray.") })
    }


    data class Builder(
        var user: String? = null,
        var apiKey: String? = null,
        var apiUrl: String? = null,
        var maxRetries: Int = 3,
        var retryDelayInSeconds: Long = 15) {

        fun user(user: String) = apply { this.user = user }
        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun apiUrl(apiUrl: String) = apply { this.apiUrl = apiUrl }
        fun maxRetries(maxRetries: Int) = apply { this.maxRetries = maxRetries }
        fun retryDelayInSeconds(retryDelayInSeconds: Long) = apply { this.retryDelayInSeconds = retryDelayInSeconds }
        fun build() = BintrayClient(bintray(apiUrl!!, user!!, apiKey!!), RetryConfig(this.maxRetries, this.retryDelayInSeconds))
    }

    data class RetryConfig(val maxRetries: Int, val retryDelayInSeconds: Long)

    fun createOrUpdatePackage(subject: String, repo: String, pkg: String, packageRequest: PackageRequest) {
        val getPackageResult = Failsafe.with(retryPolicy).get( { ->  bintrayService.getPackage(subject, repo, pkg).execute() } )
        if(getPackageResult.isSuccessful) {
            return
        }

        if(getPackageResult.code() != 404) {
            throw GradleException("Could not obtain information for package $repo/$subject/$pkg - ${getPackageResult.errorBody()?.string()}")
        }

        val createPackageResult = Failsafe.with(retryPolicy).get( { ->  bintrayService.createPackage(subject, repo, packageRequest).execute() } )
        if(!createPackageResult.isSuccessful) {
            throw GradleException("Could not create or update information for package $repo/$subject/$pkg - ${getPackageResult.errorBody()?.string()}")
        }
    }

    fun publishVersion(subject: String, repo: String, pkg: String, version: String, publishRequest: PublishRequest) {
        val publishVersionResult = Failsafe.with(retryPolicy).get( { ->
            bintrayService.publishVersion(subject, repo, pkg, version, publishRequest).execute() } )
        if(!publishVersionResult.isSuccessful) {
            throw GradleException("Could not publish $version version for package $repo/$subject/$pkg - ${publishVersionResult.errorBody()?.string()}")
        }
    }

    fun syncVersionToMavenCentral(subject: String, repo: String, pkg: String, version: String, mavenCentralSyncRequest: MavenCentralSyncRequest) {
        val syncVersionToMavenCentralResult = Failsafe.with(retryPolicy).get( { ->
            bintrayService.syncVersionToMavenCentral(subject, repo, pkg, version, mavenCentralSyncRequest).execute() } )
        if(!syncVersionToMavenCentralResult.isSuccessful) {
            logger.error("Could not sync $version version for package $repo/$subject/$pkg to maven central - ${syncVersionToMavenCentralResult.errorBody()?.string()}")
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