package io.inventiv.critic.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ApiClientTest {

    @Test
    fun `default base URL points to v3 API`() {
        assertEquals("https://critic.inventiv.io/api/v3/", ApiClient.DEFAULT_BASE_URL)
    }

    @Test
    fun `api returns non-null CriticApi instance`() {
        ApiClient.configure()
        val api = ApiClient.api()
        assertNotNull(api)
    }

    @Test
    fun `api returns same instance on repeated calls`() {
        ApiClient.configure()
        val api1 = ApiClient.api()
        val api2 = ApiClient.api()
        assert(api1 === api2)
    }

    @Test
    fun `configure with custom URL resets cached api`() {
        ApiClient.configure()
        val api1 = ApiClient.api()
        ApiClient.configure("https://custom.example.com/api/v3/")
        val api2 = ApiClient.api()
        assert(api1 !== api2)
    }
}
