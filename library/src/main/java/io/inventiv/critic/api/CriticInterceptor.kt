package io.inventiv.critic.api

import okhttp3.Interceptor
import okhttp3.Response

internal class CriticInterceptor(private val userAgent: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        if (original.header("Accept").isNullOrEmpty()) {
            builder.addHeader("Accept", "application/json")
        }
        if (original.header("User-Agent").isNullOrEmpty()) {
            builder.addHeader("User-Agent", userAgent)
        }

        return chain.proceed(builder.build())
    }
}
