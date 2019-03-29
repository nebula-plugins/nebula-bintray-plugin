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

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface BintrayService {
    @Headers("Content-Type: application/json")
    @POST("/content/{subject}/{repo}/{pkg}/{version}/publish")
    fun publishVersion(
            @Path("subject") subject: String,
            @Path("repo") repo: String,
            @Path("pkg") pkg: String,
            @Path("version") version: String,
            @Body body: PublishRequest
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @GET("/packages/{subject}/{repo}/{pkg}")
    fun getPackage(
            @Path("subject") subject: String,
            @Path("repo") repo: String,
            @Path("pkg") pkg: String
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/packages/{subject}/{repo}")
    fun createPackage(
            @Path("subject") subject: String,
            @Path("repo") repo: String,
            @Body body: PackageRequest
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @PATCH("/packages/{subject}/{repo}")
    fun updatePackage(
            @Path("subject") subject: String,
            @Path("repo") repo: String,
            @Body body: PackageRequest
    ): Call<ResponseBody>
}