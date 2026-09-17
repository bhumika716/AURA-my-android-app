package com.aura.app.data.repository

import com.aura.app.data.local.JournalDao
import com.aura.app.data.local.entities.JournalEntry
import com.aura.app.data.remote.GeminiApiService
import com.aura.app.data.remote.GeminiContent
import com.aura.app.data.remote.GeminiPart
import com.aura.app.data.remote.GeminiRequest
import com.aura.app.data.remote.GenerationConfig
import com.aura.app.data.remote.getText
import com.aura.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val journalDao: JournalDao,
    private val geminiApi: GeminiApiService,
) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()
    val latestEntry: Flow<JournalEntry?> = journalDao.getLatestEntry()

    fun getRecentEntries(limit: Int = 30): Flow<List<JournalEntry>> =
        journalDao.getRecentEntries(limit)

    fun getEntriesSince(since: Long): Flow<List<JournalEntry>> =
        journalDao.getEntriesSince(since)

    suspend fun getEntryCount(): Int = journalDao.getEntryCount()

    suspend fun getAverageMoodSince(since: Long): Float? =
        journalDao.getAverageMoodSince(since)

    /**
     * Saves a new journal entry and runs Gemini sentiment analysis on the text.
     * First saves immediately with defaults, then updates with AI results.
     */
    suspend fun saveAndAnalyze(
        rawText: String,
        selectedMood: String,
        selectedEmoji: String,
    ): JournalEntry {
        // 1. Save immediately so user sees their entry right away
        val entry = JournalEntry(rawText = rawText)

        // Get the actual ID of the inserted entry
        val insertedId = journalDao.insertEntry(entry).toInt()

        // Fetch the exact inserted entry
        val savedEntry = journalDao.getEntryById(insertedId)
            ?: return entry

        // 3. Run Gemini sentiment analysis
        return try {
            val analysisResult = analyzeSentiment(rawText)
            val updatedEntry = savedEntry.copy(
                primaryMood = selectedMood,
                moodEmoji = selectedEmoji,
                moodScore = analysisResult.score,
                energyLevel = analysisResult.energy,
                burnoutScore = analysisResult.burnout,
                aiInsight = analysisResult.insight,
                tags = analysisResult.tags,
            )
            journalDao.updateEntry(updatedEntry)
            updatedEntry
        } catch (e: Exception) {
            // Use local analysis when Gemini API is unavailable
            val localResult = analyzeLocally(rawText)

            val updatedEntry = savedEntry.copy(
                primaryMood = selectedMood,
                moodEmoji = selectedEmoji,
                moodScore = localResult.score,
                energyLevel = localResult.energy,
                burnoutScore = localResult.burnout,
                aiInsight = localResult.insight,
                tags = localResult.tags,
            )

            journalDao.updateEntry(updatedEntry)

            updatedEntry
        }
    }

    /**
     * Calls Gemini API to analyze sentiment, mood, and generate an empathetic insight.
     */
    private suspend fun analyzeSentiment(text: String): SentimentResult {
        val prompt = """
            You are an empathetic AI wellness coach. Analyze the following journal entry and respond ONLY with a valid JSON object (no markdown, no extra text).

            Journal entry: "$text"

            Respond in this exact JSON format:
            {
              "mood": "happy",
              "emoji": "😊",
              "score": 75,
              "energy": "high",
              "burnout": 10,
              "insight": "A short, warm, empathetic 1-2 sentence reflection for the user.",
              "tags": "gratitude,growth"
            }

            Rules:
            - "mood" must be one of: happy, sad, anxious, excited, calm, angry, neutral
            - "emoji" should match the mood
            - "score" is 0 (very negative) to 100 (very positive)
            - "energy" must be one of: low, medium, high
            - "burnout" is 0 (no burnout) to 100 (extreme burnout)
            - "insight" should be empathetic and encouraging (max 2 sentences)
            - "tags" should be 2-4 comma-separated keywords extracted from the text
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt)),
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.3f,  // Low temperature for consistent structured output
                maxOutputTokens = 256,
            ),
        )

        val response = geminiApi.generateContent(
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request,
        )

        val rawJson = response.getText()
            .replace("```json", "")
            .replace("```", "")
            .trim()

        return try {
            val json = JSONObject(rawJson)
            SentimentResult(
                mood = json.optString("mood", "neutral"),
                emoji = json.optString("emoji", "😐"),
                score = json.optInt("score", 50).coerceIn(0, 100),
                energy = json.optString("energy", "medium"),
                burnout = json.optInt("burnout", 0).coerceIn(0, 100),
                insight = json.optString("insight", "Thank you for sharing your thoughts today."),
                tags = json.optString("tags", ""),
            )
        } catch (e: Exception) {
            SentimentResult() // Return safe defaults
        }
    }

    private fun analyzeLocally(text: String): SentimentResult {
        val lowerText = text.lowercase()

        return when {
            listOf("happy", "great", "excited", "good", "joy", "khush")
                .any { lowerText.contains(it) } -> {
                SentimentResult(
                    mood = "happy",
                    emoji = "😊",
                    score = 80,
                    energy = "high",
                    insight = "Your journal reflects positivity. Notice what made you happy today and try to create more moments like this."
                )
            }

            listOf("sad", "upset", "lonely", "bad", "cry", "dukhi")
                .any { lowerText.contains(it) } -> {
                SentimentResult(
                    mood = "sad",
                    emoji = "😔",
                    score = 30,
                    energy = "low",
                    insight = "You seem to be going through a difficult moment. Be gentle with yourself and take one small step toward feeling better."
                )
            }

            listOf("stress", "stressed", "anxious", "worried", "tension", "pareshan")
                .any { lowerText.contains(it) } -> {
                SentimentResult(
                    mood = "anxious",
                    emoji = "😟",
                    score = 40,
                    energy = "medium",
                    burnout = 50,
                    insight = "You may be feeling overwhelmed. Try taking a short break and focus on one manageable task at a time."
                )
            }

            listOf("angry", "anger", "frustrated", "gussa", "irritated")
                .any { lowerText.contains(it) } -> {
                SentimentResult(
                    mood = "angry",
                    emoji = "😡",
                    score = 35,
                    energy = "high",
                    insight = "Strong emotions can feel intense. Take a pause, breathe slowly, and reflect on what you need right now."
                )
            }

            listOf("calm", "peaceful", "relaxed", "shant")
                .any { lowerText.contains(it) } -> {
                SentimentResult(
                    mood = "calm",
                    emoji = "😌",
                    score = 75,
                    energy = "medium",
                    insight = "Your thoughts show a sense of calm. Take a moment to appreciate this peaceful feeling."
                )
            }

            listOf("excited", "thrilled", "utsahit")
                .any { lowerText.contains(it) } -> {
                SentimentResult(
                    mood = "excited",
                    emoji = "🤩",
                    score = 85,
                    energy = "high",
                    insight = "Your excitement can be a source of motivation. Use this positive energy toward something meaningful."
                )
            }

            else -> {
                SentimentResult(
                    mood = "neutral",
                    emoji = "😐",
                    score = 50,
                    energy = "medium",
                    insight = "Your journal gives you space to understand your thoughts. Notice one feeling or thought that stood out today."
                )
            }
        }
    }
    suspend fun deleteEntry(entry: JournalEntry) = journalDao.deleteEntry(entry)
}

data class SentimentResult(
    val mood: String = "neutral",
    val emoji: String = "😐",
    val score: Int = 50,
    val energy: String = "medium",
    val burnout: Int = 0,
    val insight: String = "Thank you for sharing your thoughts today.",
    val tags: String = "",
)
