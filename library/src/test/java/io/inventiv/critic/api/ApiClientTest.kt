package io.inventiv.critic.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ApiClientTest {

    @Test
    fun `default host points to critic`() {
        assertEquals("https://critic.inventiv.io", ApiClient.DEFAULT_HOST)
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
        assertSame("Repeated calls should return same instance", api1, api2)
    }

    @Test
    fun `configure with custom host resets cached api`() {
        ApiClient.configure()
        val api1 = ApiClient.api()
        ApiClient.configure("https://custom.example.com")
        val api2 = ApiClient.api()
        assertNotSame("Reconfigure should create new instance", api1, api2)
    }
}
