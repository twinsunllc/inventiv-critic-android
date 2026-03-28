package io.inventiv.critic.api

import io.inventiv.critic.model.BugReport
import io.inventiv.critic.model.PingRequest
import io.inventiv.critic.model.PingResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CriticApi {

    @POST("ping")
    suspend fun ping(@Body request: PingRequest): Response<PingResponse>

    @Multipart
    @POST("bug_reports")
    suspend fun createBugReport(
        @Part parts: List<MultipartBody.Part>,
    ): Response<BugReport>
}
