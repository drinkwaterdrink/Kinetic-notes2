package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.NoteEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

private const val GEMINI_ENDPOINT =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

private fun defaultGeminiClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

class GeminiService(
    private val client: OkHttpClient = defaultGeminiClient(),
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
    private val endpoint: String = GEMINI_ENDPOINT
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    /**
     * A provider call is successful only when the configured provider returns usable content.
     * Missing configuration, transport errors, empty responses and HTTP failures are explicit
     * failures; the app must never turn them into plausible-looking demo prose.
     */
    suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            apiKeyProvider()
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("AI provider is not configured")
            )
        }

        try {
            val geminiReq = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                )
            )
            val jsonBody = requestAdapter.toJson(geminiReq)
            val request = Request.Builder()
                .url("$endpoint?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            executeCancellable(request).use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("AI provider request failed (${response.code})")
                    )
                }

                val bodyString = response.body?.string().orEmpty()
                val parsed = responseAdapter.fromJson(bodyString)
                val text = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(IllegalStateException("AI provider returned no usable content"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException("AI provider is unavailable", e))
        }
    }

    /**
     * OkHttp's blocking execute() is not automatically coupled to coroutine cancellation.
     * Enqueue the call instead so leaving the screen or cancelling the ViewModel interrupts
     * the provider request and does not leave an in-flight network operation behind.
     */
    private suspend fun executeCancellable(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    suspend fun beautifyAndFormatNote(title: String, content: String): Result<String> {
        val prompt = """
You are an executive knowledge formatting assistant for Kinetic Canvas.
Reformat and elevate this note into a clean, structured, and neat layout:
- Retain all core ideas.
- Add clear Markdown headers (#, ##).
- Organize key takeaways with clean bullet points.
- Highlight actionable milestones with - [ ] task checkboxes.
- Add relevant [[WikiLinks]] where applicable.

Title: $title
Raw Content:
$content
""".trimIndent()
        return generateContent(prompt)
    }

    suspend fun synthesizeSpace(spaceName: String, notes: List<NoteEntity>): Result<String> {
        val notesSummary = notes.joinToString("\n---\n") { "${it.title} (${it.tag}):\n${it.content.take(150)}" }
        val prompt = """
You are the Gemini Knowledge Architect for Kinetic Canvas space "$spaceName".
Synthesize these notes into:
1. Executive Space Overview (2-3 concise sentences)
2. Core Themes & Inter-card Relationships
3. Top 3 Immediate Action Items (- [ ])

Notes:
$notesSummary
""".trimIndent()
        return generateContent(prompt)
    }
}
