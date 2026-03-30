package io.inventiv.critic.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {

    const val DEFAULT_HOST = "https://critic.inventiv.io"
    private const val API_PATH = "/api/v3/"
    private const val USER_AGENT = "Inventiv-Android-Client"

    private var host: String = DEFAULT_HOST
    private var api: CriticApi? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun configure(host: String = DEFAULT_HOST) {
        this.host = host.trimEnd('/')
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
            .baseUrl("$host$API_PATH")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(CriticApi::class.java)
    }
}
