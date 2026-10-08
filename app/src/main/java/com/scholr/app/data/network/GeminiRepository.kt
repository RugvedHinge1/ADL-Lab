package com.scholr.app.data.network

import android.util.Log
import com.scholr.app.data.model.ChatMessage
import com.scholr.app.data.model.Paper
import retrofit2.HttpException

/** Real LLM answers for the per-paper AI chat, backed by Gemini. */
class GeminiRepository(private val api: GeminiApi = NetworkModule.geminiApi) {

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
