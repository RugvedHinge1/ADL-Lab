package com.scholr.app.data.network

import android.util.Log
import com.scholr.app.data.model.ChatMessage
import com.scholr.app.data.model.Paper
import com.scholr.app.data.alerts.AiAlertDraft
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Real LLM answers for the per-paper AI chat, backed by Gemini. */
class GeminiRepository(private val api: GeminiApi = NetworkModule.geminiApi) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Asks Gemini for a few short research alerts based on [context] (the user's interests, saved
     * papers and upcoming deadlines). Returns an empty list on any failure — alerts are a nice-to-have.
     */
    suspend fun generateAlerts(context: String): List<AiAlertDraft> {
        val prompt = """
            You are Scholr's research alert assistant. Using ONLY the data below, write 2 or 3 short,
            specific, genuinely useful alerts for this researcher. Each alert is one of:
            - a conference deadline that fits their interests (refType "conference", refId = the id from the list),
            - a recent feed paper worth reading now (refType "paper", refId = the id from the list),
            - a brief tip tied to their saved papers or interests (refType "none", refId "").
            Never invent conferences, papers or dates, and only use ids that appear in the data.
            title: at most 60 characters. body: at most 140 characters, friendly and concrete, no emoji.
            Respond with a JSON array of objects with the keys title, body, refType, refId.

            $context
        """.trimIndent()

        return try {
            val response = api.generateContent(
                model = GeminiApi.MODEL,
                request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4,
                        maxOutputTokens = 900,
                        responseMimeType = "application/json"
                    )
                )
            )
            val text = response.candidates?.firstOrNull()?.content?.parts
                ?.joinToString("") { it.text.orEmpty() }.orEmpty()
                .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            json.decodeFromString(ListSerializer(AiAlertDraft.serializer()), text).take(3)
        } catch (e: Exception) {
            Log.w("GeminiRepository", "alert generation failed", e)
            emptyList()
        }
    }

    suspend fun ask(paper: Paper, history: List<ChatMessage>, question: String): String {
        val systemPrompt = """
            You are Scholr's research assistant. Answer questions about the paper below.
            Be precise, cite specifics from the abstract when relevant, and keep answers to
            2-4 sentences unless the user asks for more detail. If something isn't in the
            abstract, say so rather than inventing details.

            Title: ${paper.title}
            Authors: ${paper.authors}
            Venue: ${paper.venue} (${paper.year})
            Field: ${paper.field}
            Abstract: ${paper.abstractText}
        """.trimIndent()

        val contents = buildList {
            history.takeLast(10).forEach { msg ->
                add(
                    GeminiContent(
                        role = if (msg.fromUser) "user" else "model",
                        parts = listOf(GeminiPart(msg.text))
                    )
                )
            }
            add(GeminiContent(role = "user", parts = listOf(GeminiPart(question))))
        }

        return try {
            val response = api.generateContent(
                model = GeminiApi.MODEL,
                request = GeminiRequest(
                    contents = contents,
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt)))
                )
            )
            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.joinToString("") { it.text.orEmpty() }
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "I couldn't come up with an answer for that — try rephrasing the question."
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            Log.e("GeminiRepository", "Gemini request failed: ${e.code()} $body", e)
            if (e.code() == 429) {
                "The AI service is rate-limited right now. Please try again in a moment."
            } else {
                "The AI service returned an error (${e.code()}). Please try again."
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Gemini request failed", e)
            "I'm having trouble reaching the AI service right now. Please try again in a moment."
        }
    }
}
