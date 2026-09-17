package com.aura.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-flash-lite:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest,
    ): GeminiResponse
}

// ── Request models ──
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig = GenerationConfig(),
    val safetySettings: List<SafetySetting> = defaultSafetySettings(),
)

data class GeminiContent(
    val role: String, // "user" or "model"
    val parts: List<GeminiPart>,
)

data class GeminiPart(
    val text: String,
)

data class GenerationConfig(
    val temperature: Float = 0.9f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 1024,
)

data class SafetySetting(
    val category: String,
    val threshold: String,
)

fun defaultSafetySettings() = listOf(
    SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
    SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
    SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH"),
    SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH"),
)

// ── Response models ──
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
)

data class GeminiCandidate(
    val content: GeminiContent?,
)

// Helper to extract text from response
fun GeminiResponse.getText(): String {
    return candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
}
