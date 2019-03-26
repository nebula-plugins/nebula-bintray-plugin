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
    @PUT("/packages/{subject}/{repo}")
    fun updatePackage(
            @Path("subject") subject: String,
            @Path("repo") repo: String,
            @Body body: PackageRequest
    ): Call<ResponseBody>
}