package io.inventiv.critic.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {

    const val DEFAULT_BASE_URL = "https://critic.inventiv.io/api/v3/"
    private const val USER_AGENT = "Inventiv-Android-Client"

    private var baseUrl: String = DEFAULT_BASE_URL
    private var api: CriticApi? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun configure(baseUrl: String = DEFAULT_BASE_URL) {
        this.baseUrl = baseUrl
        this.api = null
    }

    @Synchronized
    fun api(): CriticApi {
        return api ?: createApi().also { api = it }
    }

    private fun createApi(): CriticApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(CriticInterceptor(USER_AGENT))
            .build()

        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(CriticApi::class.java)
    }
}
