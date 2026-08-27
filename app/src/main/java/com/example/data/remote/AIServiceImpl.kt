package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.model.AIAnalysisResult
import com.example.data.model.UserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class AIServiceImpl(
    private val geminiService: GeminiApiService = GeminiClient.service
) : AIService {

    private val systemPrompt = """
        You are the LifeVault information extraction engine. Extract only information explicitly supported by the provided content.
        Never invent dates, times, amounts, names, deadlines, locations, or actions. Distinguish facts from suggestions.
        If information is uncertain or ambiguous (e.g. 'sometime next week'), mark date as 'Next week' or null and lower the confidence score.
        Return ONLY a JSON object matching this schema:
        {
          "title": "Short descriptive title (max 5 words)",
          "description": "Clean summary",
          "type": "TASK | DEADLINE | APPOINTMENT | PAYMENT | SUBSCRIPTION | RETURN | WARRANTY | DELIVERY | EVENT | REMINDER | IMPORTANT",
          "category": "EDUCATION | FINANCE | SHOPPING | HEALTH | TRAVEL | WORK | HOME | TECHNOLOGY | EVENTS | DOCUMENTS | DELIVERY | WARRANTY | GENERAL",
          "action": "What needs to be done",
          "date": "Extracted date or null",
          "time": "Extracted time or null",
          "amount": 1850.0,
          "currency": "৳ or $ or € or £ or null",
          "person": "Name or null",
          "organization": "Name or null",
          "location": "Location or null",
          "priority": "HIGH | MEDIUM | LOW",
          "confidence": 0.95,
          "explanation": "Clear reason why LifeVault detected this",
          "returnWindowDays": null,
          "warrantyExpiryDate": null,
          "subscriptionInterval": null,
          "isActionable": true
        }
    """.trimIndent()

    override suspend fun analyzeText(text: String): AIAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = GeminiClient.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext RuleBasedFallbackExtractor.extract(text)
        }

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = "Extract structured LifeVault info from this content:\n\n$text"))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.1f,
                    responseMimeType = "application/json"
                )
            )

            val response = geminiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!jsonText.isNullOrBlank()) {
                parseJsonResult(jsonText, text)
            } else {
                RuleBasedFallbackExtractor.extract(text)
            }
        } catch (e: Exception) {
            RuleBasedFallbackExtractor.extract(text)
        }
    }

    override suspend fun analyzeImage(bitmap: Bitmap, promptHint: String?): AIAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = GeminiClient.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext RuleBasedFallbackExtractor.extract(promptHint ?: "Captured image receipt/document")
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val promptText = if (!promptHint.isNullOrBlank()) {
                "Analyze this image and additional context: $promptHint"
            } else {
                "Analyze this image (document, receipt, screenshot, or bill) and extract all actionable deadlines, payments, warranties, or information."
            }

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = promptText),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.1f,
                    responseMimeType = "application/json"
                )
            )

            val response = geminiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!jsonText.isNullOrBlank()) {
                parseJsonResult(jsonText, promptHint ?: "Image Capture")
            } else {
                RuleBasedFallbackExtractor.extract(promptHint ?: "Image Capture")
            }
        } catch (e: Exception) {
            RuleBasedFallbackExtractor.extract(promptHint ?: "Image Capture")
        }
    }

    override suspend fun answerQuestion(query: String, contextItems: List<UserItem>): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiClient.getApiKey()
        val itemsSummary = if (contextItems.isEmpty()) {
            "No saved items in LifeVault yet."
        } else {
            contextItems.joinToString("\n---\n") { item ->
                "Title: ${item.title}, Type: ${item.type}, Category: ${item.category}, Due: ${item.dueDate ?: "None"} ${item.dueTime ?: ""}, Amount: ${item.currency ?: ""}${item.amount ?: ""}, Status: ${item.status}, Action: ${item.action}, Notes: ${item.description}"
            }
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext answerOffline(query, contextItems)
        }

        try {
            val prompt = """
                You are LifeVault Assistant. Answer the user's question accurately, concisely, and strictly based on their stored items below.
                If the requested information is not in the stored items, honestly say it is not recorded in LifeVault.
                
                USER'S STORED ITEMS:
                $itemsSummary
                
                USER QUESTION:
                $query
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.2f,
                    responseMimeType = "text/plain"
                )
            )

            val response = geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: answerOffline(query, contextItems)
        } catch (e: Exception) {
            answerOffline(query, contextItems)
        }
    }

    private fun answerOffline(query: String, contextItems: List<UserItem>): String {
        val q = query.lowercase()
        val active = contextItems.filter { it.status == "ACTIVE" }

        return when {
            q.contains("tomorrow") || q.contains("আগামীকাল") -> {
                val dueTomorrow = active.filter { it.dueDate?.contains("tomorrow", ignoreCase = true) == true || it.dueDate?.contains("Sep 2", ignoreCase = true) == true }
                if (dueTomorrow.isNotEmpty()) {
                    "You have ${dueTomorrow.size} item(s) due:\n" + dueTomorrow.joinToString("\n") { "• ${it.title} (${it.action})" }
                } else if (active.isNotEmpty()) {
                    "Here are your upcoming items:\n" + active.take(3).joinToString("\n") { "• ${it.title} (Due: ${it.dueDate ?: "Soon"})" }
                } else {
                    "You have no items scheduled for tomorrow. You're all caught up!"
                }
            }
            q.contains("bill") || q.contains("payment") || q.contains("বিদ্যুৎ") || q.contains("টাকা") -> {
                val bills = active.filter { it.type == "PAYMENT" || it.category == "FINANCE" }
                if (bills.isNotEmpty()) {
                    "Here are your pending payments:\n" + bills.joinToString("\n") {
                        val amt = if (it.amount != null) "${it.currency ?: "৳"}${it.amount}" else ""
                        "• ${it.title} - $amt (Due: ${it.dueDate ?: "Upcoming"})"
                    }
                } else {
                    "No pending bills found in your LifeVault."
                }
            }
            q.contains("warranty") || q.contains("ওয়ারেন্টি") || q.contains("expire") -> {
                val warranties = contextItems.filter { it.type == "WARRANTY" || it.warrantyExpiryDate != null }
                if (warranties.isNotEmpty()) {
                    "Here are your warranties:\n" + warranties.joinToString("\n") {
                        "• ${it.title}: Expires ${it.warrantyExpiryDate ?: "August 2027"}"
                    }
                } else {
                    "No warranties currently recorded in LifeVault."
                }
            }
            else -> {
                if (active.isNotEmpty()) {
                    "You have ${active.size} active item(s) in LifeVault:\n" + active.take(3).joinToString("\n") { "• ${it.title} (${it.dueDate ?: "No due date"})" }
                } else {
                    "Your LifeVault is empty right now. Share anything to start tracking!"
                }
            }
        }
    }

    private fun parseJsonResult(jsonString: String, rawText: String): AIAnalysisResult {
        return try {
            val cleaned = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleaned)

            val title = obj.optString("title", rawText.take(30))
            val description = obj.optString("description", rawText)
            val type = obj.optString("type", "TASK")
            val category = obj.optString("category", "GENERAL")
            val action = obj.optString("action", "Review item")
            val date = if (obj.isNull("date") || obj.optString("date").isBlank() || obj.optString("date") == "null") null else obj.optString("date")
            val time = if (obj.isNull("time") || obj.optString("time").isBlank() || obj.optString("time") == "null") null else obj.optString("time")
            val amount = if (obj.has("amount") && !obj.isNull("amount")) obj.optDouble("amount") else null
            val currency = if (obj.isNull("currency") || obj.optString("currency").isBlank() || obj.optString("currency") == "null") null else obj.optString("currency")
            val person = if (obj.isNull("person") || obj.optString("person").isBlank() || obj.optString("person") == "null") null else obj.optString("person")
            val organization = if (obj.isNull("organization") || obj.optString("organization").isBlank() || obj.optString("organization") == "null") null else obj.optString("organization")
            val location = if (obj.isNull("location") || obj.optString("location").isBlank() || obj.optString("location") == "null") null else obj.optString("location")
            val priority = obj.optString("priority", "MEDIUM")
            val confidence = obj.optDouble("confidence", 0.85).toFloat()
            val explanation = obj.optString("explanation", "Extracted by LifeVault AI")
            val returnWindowDays = if (obj.has("returnWindowDays") && !obj.isNull("returnWindowDays")) obj.optInt("returnWindowDays") else null
            val warrantyExpiryDate = if (obj.isNull("warrantyExpiryDate") || obj.optString("warrantyExpiryDate").isBlank() || obj.optString("warrantyExpiryDate") == "null") null else obj.optString("warrantyExpiryDate")
            val subscriptionInterval = if (obj.isNull("subscriptionInterval") || obj.optString("subscriptionInterval").isBlank() || obj.optString("subscriptionInterval") == "null") null else obj.optString("subscriptionInterval")
            val isActionable = obj.optBoolean("isActionable", true)

            AIAnalysisResult(
                title = title,
                description = description,
                type = type,
                category = category,
                action = action,
                date = date,
                time = time,
                amount = if (amount?.isNaN() == true) null else amount,
                currency = currency,
                person = person,
                organization = organization,
                location = location,
                priority = priority,
                confidence = confidence,
                explanation = explanation,
                returnWindowDays = returnWindowDays,
                warrantyExpiryDate = warrantyExpiryDate,
                subscriptionInterval = subscriptionInterval,
                isActionable = isActionable
            )
        } catch (e: Exception) {
            RuleBasedFallbackExtractor.extract(rawText)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
