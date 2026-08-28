package com.example.data.remote

import android.util.Log
import com.example.data.model.AIAnalysisResult
import com.example.data.model.Actionability
import com.example.data.model.ContentType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemType
import java.util.Locale

/**
 * Validation layer for AI & Rule-Based extraction results.
 * Verifies grounding, internal consistency, and evidence requirements.
 * Conservative by design: Downgrades unsupported actionable claims to INFORMATIONAL or UNCERTAIN.
 */
object AIResultValidator {

    private const val TAG = "AIResultValidator"

    data class ValidationOutcome(
        val validatedResult: AIAnalysisResult,
        val isValid: Boolean,
        val validationNotes: List<String>
    )

    fun validate(raw: AIAnalysisResult, sourceText: String): ValidationOutcome {
        val notes = mutableListOf<String>()
        val normalizedSource = sourceText.lowercase(Locale.ROOT)

        var finalContentType = raw.contentType
        var finalActionability = raw.actionability
        var finalType = raw.type
        var finalCategory = raw.category
        var finalAction = raw.action
        var finalDate = raw.date
        var finalTime = raw.time
        var finalAmount = raw.amount
        var finalCurrency = raw.currency
        var finalMerchant = raw.merchant
        var finalProduct = raw.product
        var finalSubscription = raw.subscription
        var finalAppointment = raw.appointment
        var finalEvidence = raw.evidence
        var finalReason = raw.reason
        var finalExplanation = raw.explanation
        var finalContentTypeConfidence = raw.contentTypeConfidence.coerceIn(0.0f, 1.0f)
        var finalActionabilityConfidence = raw.actionabilityConfidence.coerceIn(0.0f, 1.0f)
        var finalExtractionConfidence = raw.extractionConfidence.coerceIn(0.0f, 1.0f)

        // 1. Check Date Grounding
        if (!finalDate.isNullOrBlank()) {
            val dateTokens = finalDate.lowercase(Locale.ROOT)
                .split("-", " ", "/", ",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length > 1 && it != "2026" && it != "2025" && it != "2027" }
            val hasDateInSource = if (dateTokens.isEmpty()) {
                normalizedSource.contains(finalDate.lowercase(Locale.ROOT))
            } else {
                dateTokens.any { normalizedSource.contains(it) }
            }

            if (!hasDateInSource) {
                notes.add("Date '$finalDate' not grounded in source text; removing date.")
                finalDate = null
                finalTime = null
            }
        }

        // 2. Check Amount Grounding
        if (finalAmount != null) {
            val intPart = finalAmount.toInt().toString()
            val sourceNoCommas = normalizedSource.replace(",", "")
            val hasAmountInSource = sourceNoCommas.contains(intPart) ||
                    normalizedSource.contains(String.format(Locale.ROOT, "%.2f", finalAmount))
            if (!hasAmountInSource) {
                notes.add("Amount '$finalAmount' not grounded in source text; removing amount.")
                finalAmount = null
                finalCurrency = null
            }
        }

        // 3. Check Merchant / Organization Grounding
        if (!finalMerchant.isNullOrBlank()) {
            val merchantLower = finalMerchant.lowercase(Locale.ROOT).trim()
            if (!normalizedSource.contains(merchantLower) && merchantLower.length > 3) {
                notes.add("Merchant '$finalMerchant' not grounded in source text; clearing merchant.")
                finalMerchant = null
            }
        }

        // 4. Verify Financial / Receipt Grounding
        if (finalContentType == ContentType.RECEIPT.name || finalContentType == ContentType.PURCHASE.name) {
            val hasPrice = finalAmount != null || containsCurrencySymbol(sourceText)
            val hasReceiptSignals = containsReceiptSignals(normalizedSource)
            if (!hasPrice && !hasReceiptSignals) {
                notes.add("Insufficient evidence for RECEIPT/PURCHASE; reclassifying to UNKNOWN.")
                finalContentType = ContentType.UNKNOWN.name
                finalActionability = Actionability.UNCERTAIN.name
                finalType = ItemType.DOCUMENT.name
                finalCategory = ItemCategory.GENERAL.name
            }
        }

        // 5. Verify Subscription Grounding
        if (finalContentType == ContentType.SUBSCRIPTION.name) {
            val hasSubSignals = containsSubscriptionSignals(normalizedSource)
            if (!hasSubSignals) {
                notes.add("Insufficient evidence for SUBSCRIPTION; reclassifying to UNKNOWN.")
                finalContentType = ContentType.UNKNOWN.name
                finalActionability = Actionability.INFORMATIONAL.name
                finalType = ItemType.NOTE.name
                finalCategory = ItemCategory.GENERAL.name
                finalSubscription = null
            }
        }

        // 6. Verify Task & Action Grounding
        if (finalActionability == Actionability.ACTIONABLE.name) {
            val isExplicitAction = hasExplicitActionObligation(normalizedSource)
            val isDefinitionalOrEducational = isDefinitionalOrEducational(normalizedSource)

            if (isDefinitionalOrEducational && !isExplicitAction) {
                notes.add("Source is educational/definitional; reclassifying actionable claim to INFORMATIONAL.")
                finalContentType = if (finalContentType == ContentType.TASK.name || finalContentType == ContentType.DEADLINE.name || finalContentType == ContentType.UNKNOWN.name) {
                    ContentType.EDUCATIONAL_DOCUMENT.name
                } else finalContentType
                finalActionability = Actionability.INFORMATIONAL.name
                finalType = ItemType.DOCUMENT.name
                finalCategory = ItemCategory.EDUCATION.name
                finalAction = ""
                finalEvidence = null
            } else if (!isExplicitAction && finalDate == null && finalAmount == null) {
                // If marked actionable but lacks action verb, date, or amount
                notes.add("No explicit actionable imperative found; marking as INFORMATIONAL.")
                finalActionability = Actionability.INFORMATIONAL.name
                finalType = ItemType.NOTE.name
                finalAction = ""
            }
        }

        // 7. Verify Short/Conversational Text
        if (sourceText.trim().length <= 30) {
            val shortText = sourceText.trim()
            if (isShortNameOrGreeting(shortText)) {
                finalContentType = ContentType.PERSONAL_NOTE.name
                finalActionability = Actionability.INFORMATIONAL.name
                finalType = ItemType.NOTE.name
                finalCategory = ItemCategory.GENERAL.name
                finalAction = ""
                finalDate = null
                finalAmount = null
                finalEvidence = null
                finalReason = "Short note or greeting with no actionable task or deadline."
                finalExplanation = "Saved as personal note."
            }
        }

        // 8. Confidence Threshold Enforcement
        if (finalActionability == Actionability.ACTIONABLE.name && finalActionabilityConfidence < 0.65f) {
            notes.add("Actionability confidence below safe threshold (0.65); marking as UNCERTAIN.")
            finalActionability = Actionability.UNCERTAIN.name
        }

        // 9. Consistency Check: Ensure Informational items do not carry fake actions
        if (finalActionability == Actionability.INFORMATIONAL.name) {
            finalAction = ""
            if (finalType == ItemType.TASK.name || finalType == ItemType.REMINDER.name) {
                finalType = ItemType.NOTE.name
            }
        }

        val isActionable = finalActionability == Actionability.ACTIONABLE.name
        val isUncertain = finalActionability == Actionability.UNCERTAIN.name
        val validationPassed = notes.isEmpty()

        val validated = raw.copy(
            contentType = finalContentType,
            actionability = finalActionability,
            contentTypeConfidence = finalContentTypeConfidence,
            actionabilityConfidence = finalActionabilityConfidence,
            extractionConfidence = finalExtractionConfidence,
            title = if (raw.title.isNotBlank()) raw.title else "Untitled Record",
            summary = raw.summary.ifBlank { raw.title },
            description = raw.description,
            type = finalType,
            category = finalCategory,
            action = finalAction,
            date = finalDate,
            time = finalTime,
            amount = finalAmount,
            amountDue = raw.amountDue ?: finalAmount,
            currency = finalCurrency,
            invoiceNumber = raw.invoiceNumber,
            customer = raw.customer,
            issueDate = raw.issueDate,
            billingPeriod = raw.billingPeriod,
            subtotal = raw.subtotal,
            tax = raw.tax,
            discount = raw.discount,
            amountPaid = raw.amountPaid,
            balance = raw.balance,
            paymentStatus = raw.paymentStatus,
            topic = raw.topic,
            subject = raw.subject,
            authors = raw.authors,
            abstractSnippet = raw.abstractSnippet,
            keyFindings = raw.keyFindings,
            keyConcepts = raw.keyConcepts,
            fileName = raw.fileName,
            fileSize = raw.fileSize,
            pageCount = raw.pageCount,
            sourcePageEvidence = raw.sourcePageEvidence,
            ocrConfidence = raw.ocrConfidence,
            isScannedPdf = raw.isScannedPdf,
            contentHash = raw.contentHash,
            merchant = finalMerchant,
            product = finalProduct,
            subscription = finalSubscription,
            appointment = finalAppointment,
            priority = raw.priority,
            confidence = (finalContentTypeConfidence * 0.4f + finalActionabilityConfidence * 0.6f).coerceIn(0.0f, 1.0f),
            evidence = finalEvidence,
            reason = finalReason ?: "Classified as $finalContentType with $finalActionability actionability.",
            explanation = finalExplanation ?: raw.explanation,
            isActionable = isActionable,
            isUncertain = isUncertain,
            validationPassed = validationPassed
        )

        // Diagnostic Logging
        try {
            Log.d(TAG, """
                AI_CLASSIFICATION
                contentType=${validated.contentType}
                actionability=${validated.actionability}
                contentConfidence=${validated.contentTypeConfidence}
                actionConfidence=${validated.actionabilityConfidence}
                validation=${if (validationPassed) "PASS" else "CORRECTED (${notes.joinToString("; ")})"}
            """.trimIndent())
        } catch (e: Throwable) {
            // Safe fallback during local JVM testing where android.util.Log is unmocked
        }

        return ValidationOutcome(
            validatedResult = validated,
            isValid = validationPassed,
            validationNotes = notes
        )
    }

    private fun containsTemporalReference(text: String): Boolean {
        val temporalKeywords = listOf(
            "today", "tomorrow", "tonight", "yesterday", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday", "jan", "feb", "mar", "apr",
            "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec", "pm", "am", "clock",
            "due", "deadline", "by "
        )
        return temporalKeywords.any { text.contains(it) }
    }

    private fun containsCurrencySymbol(text: String): Boolean {
        return text.contains("৳") || text.contains("$") || text.contains("€") ||
                text.contains("£") || text.contains("₹") || text.contains("tk", ignoreCase = true) ||
                text.contains("bdt", ignoreCase = true) || text.contains("usd", ignoreCase = true)
    }

    private fun containsReceiptSignals(text: String): Boolean {
        val signals = listOf("receipt", "invoice", "subtotal", "tax", "cash", "credit card", "pos terminal", "store #", "vat #")
        return signals.count { text.contains(it) } >= 1
    }

    private fun containsSubscriptionSignals(text: String): Boolean {
        val signals = listOf("subscription", "renews", "renewing", "recurring", "monthly plan", "yearly plan", "auto-renew", "membership")
        return signals.any { text.contains(it) }
    }

    private fun hasExplicitActionObligation(text: String): Boolean {
        val actionTriggers = listOf(
            "submit", "pay", "buy", "purchase", "call", "attend", "deliver", "return", "pick up",
            "due by", "must complete", "don't forget", "appointment with", "meeting at", "renews automatically",
            "bill due", "payment due"
        )
        return actionTriggers.any { text.contains(it) }
    }

    private fun isDefinitionalOrEducational(text: String): Boolean {
        val educationalTriggers = listOf(
            "means to", "is defined as", "refers to", "a modifier is", "chapter ", "exercise ",
            "textbook", "formula", "grammar rule", "vocabulary", "definition:", "explanation:"
        )
        return educationalTriggers.any { text.contains(it) }
    }

    private fun isShortNameOrGreeting(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val greetings = listOf("this is ", "hello", "hi", "hey", "good morning", "good evening", "my name is", "how are you")
        if (greetings.any { lower.startsWith(it) || lower == it }) return true
        val hasFinancialOrActionKeyword = lower.contains("bill") || lower.contains("due") || lower.contains("pay") ||
                lower.contains("meet") || lower.contains("appointment") || lower.contains("receipt") ||
                lower.contains("subscription") || lower.contains("card") || containsCurrencySymbol(text) || text.any { it.isDigit() }
        return !hasFinancialOrActionKeyword && lower.split("\\s+".toRegex()).size <= 3
    }
}
