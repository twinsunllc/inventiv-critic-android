package io.inventiv.critic.api

import io.inventiv.critic.model.BugReportListResponse
import io.inventiv.critic.model.BugReportResponse
import io.inventiv.critic.model.DeviceListResponse
import io.inventiv.critic.model.PingRequest
import io.inventiv.critic.model.PingResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface CriticApi {

    @POST("ping")
    suspend fun ping(@Body request: PingRequest): Response<PingResponse>

    @Multipart
    @POST("bug_reports")
    suspend fun createBugReport(
        @Part parts: List<MultipartBody.Part>,
    ): Response<BugReportResponse>

    @GET("bug_reports")
    suspend fun listBugReports(
        @Query("app_api_token") appApiToken: String,
        @Query("archived") archived: Boolean? = null,
        @Query("device_id") deviceId: String? = null,
        @Query("since") since: String? = null,
    ): Response<BugReportListResponse>

    @GET("bug_reports/{id}")
    suspend fun getBugReport(
        @Path("id") id: String,
        @Query("app_api_token") appApiToken: String,
    ): Response<BugReportResponse>

    @GET("devices")
    suspend fun listDevices(
        @Query("app_api_token") appApiToken: String,
    ): Response<DeviceListResponse>
}
