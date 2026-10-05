package com.example.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiServiceTruthfulnessTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun missingProviderConfigurationIsFailureWithoutNetworkRequest() = runBlocking {
        val service = GeminiService(apiKeyProvider = { "" })

        val result = service.generateContent("hello")

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun unauthorized401IsNotConvertedIntoSuccess() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = service().generateContent("hello")

        assertTrue(result.isFailure)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun httpFailureIsNotConvertedIntoSuccess() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = service().generateContent("hello")

        assertTrue(result.isFailure)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun networkFailureIsNotConvertedIntoSuccess() = runBlocking {
        val service = GeminiService(
            client = OkHttpClient.Builder()
                .connectTimeout(250, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(250, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(250, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build(),
            apiKeyProvider = { "test-key" },
            endpoint = "http://127.0.0.1:1/generateContent"
        )

        val result = service.generateContent("hello")

        assertTrue(result.isFailure)
    }

    @Test
    fun emptyProviderResponseIsNotConvertedIntoSuccess() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"candidates\":[]}")
        )

        val result = service().generateContent("hello")

        assertTrue(result.isFailure)
    }

    @Test
    fun usableProviderResponseIsReturned() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"usable\"}]}}]}")
        )

        val result = service().generateContent("hello")

        assertEquals(Result.success("usable"), result)
    }

    private fun service(): GeminiService = GeminiService(
        client = OkHttpClient.Builder().build(),
        apiKeyProvider = { "test-key" },
        endpoint = server.url("/generateContent").toString().removeSuffix("/")
    )
}
