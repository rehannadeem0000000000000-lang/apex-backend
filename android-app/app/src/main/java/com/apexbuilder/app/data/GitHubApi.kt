package com.apexbuilder.app.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface GitHubApi {
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContent(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repository: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: PutContentRequest,
    ): GitHubContentResponse

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow}/dispatches")
    suspend fun dispatchWorkflow(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repository: String,
        @Path(value = "workflow", encoded = true) workflow: String,
        @Body body: WorkflowDispatchRequest,
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/actions/workflows/{workflow}/runs")
    suspend fun workflowRuns(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repository: String,
        @Path(value = "workflow", encoded = true) workflow: String,
        @Query("branch") branch: String,
        @Query("event") event: String = "workflow_dispatch",
        @Query("per_page") perPage: Int = 10,
    ): WorkflowRunsResponse

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    suspend fun artifacts(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repository: String,
        @Path("runId") runId: Long,
    ): WorkflowArtifactsResponse

    @Streaming
    @GET("repos/{owner}/{repo}/actions/artifacts/{artifactId}/zip")
    suspend fun downloadArtifact(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repository: String,
        @Path("artifactId") artifactId: Long,
    ): Response<ResponseBody>
}

object GitHubApiFactory {
    fun create(): GitHubApi {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.MINUTES)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "ApexBuilder-Android/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubApi::class.java)
    }
}