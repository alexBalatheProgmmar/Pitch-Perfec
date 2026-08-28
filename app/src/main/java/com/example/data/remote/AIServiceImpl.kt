package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.model.AIAnalysisResult
import com.example.data.model.Actionability
import com.example.data.model.ContentType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemPriority
import com.example.data.model.ItemType
import com.example.data.model.UserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class AIServiceImpl(
    private val geminiService: GeminiApiService = GeminiClient.service
) : AIService {

    private val systemPrompt = """
        You are the LifeVault AI Understanding & Extraction Engine.
        
        PRIMARY RULE:
        LifeVault must NEVER assume that every input contains a task, reminder, payment, deadline, receipt, subscription, or appointment.
        Many inputs are simply:
        - News articles & reports
        - Educational content, textbooks, grammar guides, modifier explanations, formulas
        - Personal notes & self-introductions (e.g., 'This is Alex')
        - Conversations & greetings
        - General reference documents & facts
        
        Never force content into an actionable category. If the input does not clearly contain an actionable event or explicit user obligation, classify it as INFORMATIONAL.
        
        PIPELINE RULES:
        1. CONTENT TYPE CLASSIFICATION:
           Allowed values: INVOICE, BILL, RECEIPT, PURCHASE, SUBSCRIPTION, APPOINTMENT, TASK, DEADLINE, EDUCATIONAL_DOCUMENT, EDUCATIONAL_CONTENT, EDUCATIONAL_PAGE, RESEARCH_PAPER, CONTRACT, CERTIFICATE, RESUME, PRODUCT_MANUAL, BANK_STATEMENT, NEWS_ARTICLE, NEWS, PERSONAL_NOTE, CONVERSATION, PHOTOGRAPH, PRODUCT_IMAGE, CHAT_SCREENSHOT, GENERAL_INFORMATION, UNKNOWN.
           CRITICAL: Do NOT classify every document as GENERAL_INFORMATION. Classify invoices as INVOICE, grammar/math/textbooks as EDUCATIONAL_DOCUMENT, papers as RESEARCH_PAPER, agreements as CONTRACT, etc. If genuinely uncertain, use UNKNOWN.
        
        2. ACTIONABILITY CLASSIFICATION:
           Allowed values: ACTIONABLE, INFORMATIONAL, UNCERTAIN.
           - "This is Alex." -> INFORMATIONAL
           - "The capital of France is Paris." -> INFORMATIONAL
           - "English Modifiers — A modifier is a word that modifies..." -> INFORMATIONAL (EDUCATIONAL_DOCUMENT)
           - "Chapter 5 — Total 8,500 words." -> INFORMATIONAL (EDUCATIONAL_DOCUMENT)
           - "Submit the assignment by Friday." -> ACTIONABLE (TASK)
           - "Invoice #INV-2024-001 from Acme Corp for ৳5,000 due Sept 15." -> ACTIONABLE (INVOICE, amount_due=5000, invoice_number="INV-2024-001")
           - "Your electricity bill of ৳1,850 is due September 2." -> ACTIONABLE (BILL, amount_due=1850)
           - "Gas bill ৳750." -> ACTIONABLE (BILL, amount_due=750, due_date=null)
           - "My Visa ending 4821" -> INFORMATIONAL (CARD)
           - "Your subscription renews September 15 for $49.99." -> ACTIONABLE (SUBSCRIPTION)
           - "Your dentist appointment is September 4 at 3 PM." -> ACTIONABLE (APPOINTMENT)
        
        3. FINANCIAL & BILL RULES:
           - Separate the amount due from other numbers (previous balance, charges, tax, account numbers).
           - Do not invent due dates if none is mentioned.
           - Allowed bill_type values: GAS, ELECTRICITY, WATER, INTERNET, MOBILE, TELEPHONE, TV_CABLE, RENT, TUITION, INSURANCE, CREDIT_CARD, LOAN, GOVERNMENT, SUBSCRIPTION, OTHER.
        
        4. EVIDENCE & REASONING:
           Provide evidence quoting directly from the user's text for any actionable item.
           If informational, set action=null, due_date=null, amount=null, evidence=null, and explain in reason.
        
        Return ONLY valid JSON matching this schema:
        {
          "content_type": "INVOICE",
          "actionability": "ACTIONABLE",
          "confidence": {
            "content_type": 0.98,
            "actionability": 0.99,
            "extraction": 0.95
          },
          "title": "Acme Corp Invoice",
          "summary": "Invoice INV-102 for Acme Corp due Sept 15",
          "bill_type": null,
          "provider": "Acme Corp",
          "invoice_number": "INV-102",
          "customer": "John Doe",
          "issue_date": "2026-09-01",
          "action": "Pay ৳5000",
          "due_date": "September 15, 2026",
          "due_time": null,
          "amount": 5000.0,
          "amount_due": 5000.0,
          "subtotal": 4500.0,
          "tax": 500.0,
          "currency": "৳",
          "payment_status": "UNPAID",
          "topic": null,
          "subject": null,
          "authors": null,
          "merchant": null,
          "product": null,
          "subscription": null,
          "appointment": null,
          "evidence": "Total Amount Due: ৳5,000",
          "reason": "Commercial invoice with pending payment obligation."
        }
    """.trimIndent()

    private val imageClassifierSystemPrompt = """
        You are the LifeVault General Image Classifier & Multimodal Understanding Engine.
        
        MANDATORY FIRST-STAGE DIRECTIVE:
        RECEIPT ANALYSIS MUST BE A SPECIALIZED BRANCH, NEVER THE DEFAULT.
        Never classify an image as a Store Purchase Receipt unless there is unmistakable visual/textual proof of a point-of-sale checkout slip (containing store name, itemized list of purchased items, prices, and transaction total).
        
        Classify the image into ONE of these content_types:
        - PHOTOGRAPH: A normal photograph of a person (portrait, selfie), pet/animal (dog, cat), landscape (mountains, ocean, sunset, forest), nature, food, vehicle, room, or everyday scene. Set content_type="PHOTOGRAPH", actionability="INFORMATIONAL", action=null, due_date=null, amount=null, merchant=null. Provide a natural descriptive title (e.g., 'Golden Retriever', 'Mountain Sunset', 'Portrait Photo').
        - EDUCATIONAL_PAGE: Textbook page, grammar explanation, math problem/equation, physics notes, definition, lecture notes. Actionability="INFORMATIONAL".
        - NEWS: News website screenshot, newspaper article, journalistic reporting. Actionability="INFORMATIONAL".
        - CHAT_SCREENSHOT: Chat app conversation (WhatsApp, Telegram, Messenger, iMessage, SMS). Actionability="INFORMATIONAL".
        - PRODUCT_IMAGE: Commercial photo of a standalone gadget, sneaker, electronics, watch, merchandise (NOT a sales receipt). Actionability="INFORMATIONAL".
        - BILL: Active utility billing statement (electricity, gas, water, internet) with a pending amount due and due date. Actionability="ACTIONABLE".
        - RECEIPT: Point-of-sale store checkout slip ONLY if it contains merchant name, itemized products purchased, taxes, and transaction total. Actionability="INFORMATIONAL".
        - DOCUMENT: Official form, certificate, letter, ID document. Actionability="INFORMATIONAL".
        - UNKNOWN: Blurry, unreadable, dark, or abstract image. Actionability="INFORMATIONAL".
        
        If the input is blurry, unreadable, or not recognizable, return content_type="UNKNOWN", actionability="INFORMATIONAL", title="Unidentified Image".
        
        Return ONLY valid JSON matching this schema:
        {
          "content_type": "PHOTOGRAPH",
          "actionability": "INFORMATIONAL",
          "confidence": {
            "content_type": 0.98,
            "actionability": 0.99,
            "extraction": 0.0
          },
          "title": "Golden Retriever",
          "summary": "Photograph of a golden retriever dog outdoors",
          "bill_type": null,
          "provider": null,
          "action": null,
          "due_date": null,
          "due_time": null,
          "amount": null,
          "currency": null,
          "merchant": null,
          "product": null,
          "subscription": null,
          "appointment": null,
          "evidence": null,
          "reason": "Visual photograph of a pet animal."
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
                        parts = listOf(GeminiPart(text = "Analyze and extract structured LifeVault data from this input:\n\n$text"))
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
            val classification = ImageContentClassifier.classifyFromContextOrHint(promptHint)
            return@withContext ImageContentClassifier.routeAndAnalyze(classification, promptHint.orEmpty(), bitmap)
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val promptText = if (!promptHint.isNullOrBlank()) {
                "Classify and understand this image. Optional context: $promptHint"
            } else {
                "Classify and understand this image accurately using the multi-branch image classifier."
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
                    parts = listOf(GeminiPart(text = imageClassifierSystemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.1f,
                    responseMimeType = "application/json"
                )
            )

            val response = geminiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!jsonText.isNullOrBlank()) {
                parseJsonResult(jsonText, promptHint ?: "Image Content")
            } else {
                val classification = ImageContentClassifier.classifyFromContextOrHint(promptHint)
                ImageContentClassifier.routeAndAnalyze(classification, promptHint.orEmpty(), bitmap)
            }
        } catch (e: Exception) {
            val classification = ImageContentClassifier.classifyFromContextOrHint(promptHint)
            ImageContentClassifier.routeAndAnalyze(classification, promptHint.orEmpty(), bitmap)
        }
    }

    override suspend fun answerQuestion(query: String, contextItems: List<UserItem>): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiClient.getApiKey()
        val itemsSummary = if (contextItems.isEmpty()) {
            "No saved items in LifeVault yet."
        } else {
            contextItems.joinToString("\n---\n") { item ->
                "Title: ${item.title}, Type: ${item.type}, Category: ${item.category}, ContentType: ${item.contentType}, Actionability: ${item.actionability}, Due: ${item.dueDate ?: "None"} ${item.dueTime ?: ""}, Amount: ${item.currency ?: ""}${item.amount ?: ""}, Status: ${item.status}, Action: ${item.action}, Notes: ${item.description}"
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
                val bills = active.filter { it.type == "PAYMENT" || it.category == "FINANCE" || it.contentType == "BILL" }
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
                val warranties = contextItems.filter { it.type == "WARRANTY" || it.warrantyExpiryDate != null || it.contentType == "WARRANTY" }
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

            val contentType = obj.optString("content_type", ContentType.UNKNOWN.name).uppercase()
            val actionability = obj.optString("actionability", Actionability.INFORMATIONAL.name).uppercase()

            val confidenceObj = obj.optJSONObject("confidence")
            val contentTypeConfidence = confidenceObj?.optDouble("content_type", 0.90)?.toFloat() ?: 0.90f
            val actionabilityConfidence = confidenceObj?.optDouble("actionability", 0.90)?.toFloat() ?: 0.90f
            val extractionConfidence = confidenceObj?.optDouble("extraction", 0.0)?.toFloat() ?: 0.0f

            val title = obj.optString("title", rawText.take(30))
            val summary = obj.optString("summary", rawText.take(100))
            val action = if (obj.isNull("action") || obj.optString("action").isBlank() || obj.optString("action") == "null") "" else obj.optString("action")
            val date = if (obj.isNull("due_date") || obj.optString("due_date").isBlank() || obj.optString("due_date") == "null") null else obj.optString("due_date")
            val time = if (obj.isNull("due_time") || obj.optString("due_time").isBlank() || obj.optString("due_time") == "null") null else obj.optString("due_time")
            val amount = if (obj.has("amount") && !obj.isNull("amount")) obj.optDouble("amount") else null
            val amountDue = if (obj.has("amount_due") && !obj.isNull("amount_due")) obj.optDouble("amount_due") else amount
            val currency = if (obj.isNull("currency") || obj.optString("currency").isBlank() || obj.optString("currency") == "null") null else obj.optString("currency")
            val merchant = if (obj.isNull("merchant") || obj.optString("merchant").isBlank() || obj.optString("merchant") == "null") null else obj.optString("merchant")
            val product = if (obj.isNull("product") || obj.optString("product").isBlank() || obj.optString("product") == "null") null else obj.optString("product")
            val subscription = if (obj.isNull("subscription") || obj.optString("subscription").isBlank() || obj.optString("subscription") == "null") null else obj.optString("subscription")
            val appointment = if (obj.isNull("appointment") || obj.optString("appointment").isBlank() || obj.optString("appointment") == "null") null else obj.optString("appointment")
            val billType = if (obj.isNull("bill_type") || obj.optString("bill_type").isBlank() || obj.optString("bill_type") == "null") null else obj.optString("bill_type")
            val billProvider = if (obj.isNull("provider") || obj.optString("provider").isBlank() || obj.optString("provider") == "null") null else obj.optString("provider")
            val invoiceNumber = if (obj.isNull("invoice_number") || obj.optString("invoice_number").isBlank() || obj.optString("invoice_number") == "null") null else obj.optString("invoice_number")
            val customer = if (obj.isNull("customer") || obj.optString("customer").isBlank() || obj.optString("customer") == "null") null else obj.optString("customer")
            val issueDate = if (obj.isNull("issue_date") || obj.optString("issue_date").isBlank() || obj.optString("issue_date") == "null") null else obj.optString("issue_date")
            val subtotal = if (obj.has("subtotal") && !obj.isNull("subtotal")) obj.optDouble("subtotal") else null
            val tax = if (obj.has("tax") && !obj.isNull("tax")) obj.optDouble("tax") else null
            val paymentStatus = if (obj.isNull("payment_status") || obj.optString("payment_status").isBlank() || obj.optString("payment_status") == "null") null else obj.optString("payment_status")
            val topic = if (obj.isNull("topic") || obj.optString("topic").isBlank() || obj.optString("topic") == "null") null else obj.optString("topic")
            val subject = if (obj.isNull("subject") || obj.optString("subject").isBlank() || obj.optString("subject") == "null") null else obj.optString("subject")
            val authors = if (obj.isNull("authors") || obj.optString("authors").isBlank() || obj.optString("authors") == "null") null else obj.optString("authors")
            val evidence = if (obj.isNull("evidence") || obj.optString("evidence").isBlank() || obj.optString("evidence") == "null") null else obj.optString("evidence")
            val reason = if (obj.isNull("reason") || obj.optString("reason").isBlank() || obj.optString("reason") == "null") null else obj.optString("reason")

            // Map to UI legacy fields
            val (itemType, itemCategory) = mapToTypeAndCategory(contentType, actionability)

            val rawResult = AIAnalysisResult(
                contentType = contentType,
                actionability = actionability,
                contentTypeConfidence = contentTypeConfidence,
                actionabilityConfidence = actionabilityConfidence,
                extractionConfidence = extractionConfidence,
                title = title,
                summary = summary,
                description = rawText,
                type = itemType.name,
                category = itemCategory.name,
                action = action,
                date = date,
                time = time,
                amount = if (amount?.isNaN() == true) null else amount,
                amountDue = if (amountDue?.isNaN() == true) null else amountDue,
                currency = currency,
                merchant = merchant,
                product = product,
                subscription = subscription,
                appointment = appointment,
                billType = billType,
                billProvider = billProvider,
                invoiceNumber = invoiceNumber,
                customer = customer,
                issueDate = issueDate,
                subtotal = subtotal,
                tax = tax,
                paymentStatus = paymentStatus,
                topic = topic,
                subject = subject,
                authors = authors,
                priority = if (actionability == Actionability.ACTIONABLE.name) ItemPriority.HIGH.name else ItemPriority.LOW.name,
                confidence = (contentTypeConfidence * 0.4f + actionabilityConfidence * 0.6f).coerceIn(0.0f, 1.0f),
                evidence = evidence,
                reason = reason,
                explanation = reason ?: "Extracted by LifeVault AI",
                isActionable = actionability == Actionability.ACTIONABLE.name,
                isUncertain = actionability == Actionability.UNCERTAIN.name
            )

            // Pass through validation layer
            AIResultValidator.validate(rawResult, rawText).validatedResult
        } catch (e: Exception) {
            RuleBasedFallbackExtractor.extract(rawText)
        }
    }

    private fun mapToTypeAndCategory(contentType: String, actionability: String): Pair<ItemType, ItemCategory> {
        if (actionability == Actionability.INFORMATIONAL.name || actionability == Actionability.UNCERTAIN.name) {
            return when (contentType) {
                ContentType.PHOTOGRAPH.name -> ItemType.NOTE to ItemCategory.GENERAL
                ContentType.EDUCATIONAL_PAGE.name, ContentType.EDUCATIONAL_CONTENT.name -> ItemType.DOCUMENT to ItemCategory.EDUCATION
                ContentType.NEWS.name, ContentType.NEWS_ARTICLE.name -> ItemType.DOCUMENT to ItemCategory.DOCUMENTS
                ContentType.CHAT_SCREENSHOT.name, ContentType.CONVERSATION.name -> ItemType.NOTE to ItemCategory.GENERAL
                ContentType.PRODUCT_IMAGE.name -> ItemType.NOTE to ItemCategory.SHOPPING
                ContentType.DOCUMENT.name, ContentType.FORM.name, ContentType.CERTIFICATE.name, ContentType.ID_DOCUMENT.name -> ItemType.DOCUMENT to ItemCategory.DOCUMENTS
                ContentType.RECEIPT.name, ContentType.PURCHASE.name, ContentType.INVOICE.name -> ItemType.DOCUMENT to ItemCategory.SHOPPING
                ContentType.CARD.name -> ItemType.DOCUMENT to ItemCategory.FINANCE
                ContentType.PERSONAL_NOTE.name -> ItemType.NOTE to ItemCategory.GENERAL
                ContentType.UNKNOWN.name -> ItemType.NOTE to ItemCategory.GENERAL
                else -> ItemType.NOTE to ItemCategory.GENERAL
            }
        }
        return when (contentType) {
            ContentType.BILL.name, ContentType.PAYMENT.name -> ItemType.PAYMENT to ItemCategory.FINANCE
            ContentType.SUBSCRIPTION.name -> ItemType.SUBSCRIPTION to ItemCategory.FINANCE
            ContentType.APPOINTMENT.name -> ItemType.APPOINTMENT to ItemCategory.HEALTH
            ContentType.DEADLINE.name -> ItemType.DEADLINE to ItemCategory.GENERAL
            ContentType.TASK.name -> ItemType.TASK to ItemCategory.GENERAL
            ContentType.RECEIPT.name, ContentType.PURCHASE.name, ContentType.INVOICE.name -> ItemType.DOCUMENT to ItemCategory.SHOPPING
            ContentType.WARRANTY.name -> ItemType.WARRANTY to ItemCategory.WARRANTY
            ContentType.RETURN.name -> ItemType.RETURN to ItemCategory.SHOPPING
            ContentType.DELIVERY.name -> ItemType.DELIVERY to ItemCategory.DELIVERY
            ContentType.PRODUCT_IMAGE.name -> ItemType.NOTE to ItemCategory.SHOPPING
            ContentType.PHOTOGRAPH.name -> ItemType.NOTE to ItemCategory.GENERAL
            else -> ItemType.TASK to ItemCategory.GENERAL
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
