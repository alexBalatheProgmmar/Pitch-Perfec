package com.example.data.remote

import com.example.data.model.AIAnalysisResult
import com.example.data.model.Actionability
import com.example.data.model.ContentType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemPriority
import com.example.data.model.ItemType
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust, conservative rule-based classification and extraction engine.
 * Adheres strictly to the principle:
 * NEVER assume an input is actionable unless explicit, unambiguous evidence exists.
 */
object RuleBasedFallbackExtractor {

    fun extract(text: String): AIAnalysisResult {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.GENERAL_INFORMATION.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.95f,
                    actionabilityConfidence = 0.99f,
                    extractionConfidence = 0.0f,
                    title = "Empty Note",
                    summary = "No content provided.",
                    type = ItemType.NOTE.name,
                    category = ItemCategory.GENERAL.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.95f,
                    evidence = null,
                    reason = "No content provided.",
                    explanation = "Empty note.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        val lower = cleanText.lowercase(Locale.ROOT)
        val lines = cleanText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val firstLine = lines.firstOrNull() ?: cleanText

        // 1. Short Text / Personal Intro / Casual Greetings
        if (isShortPersonalNoteOrGreeting(lower, cleanText)) {
            val title = if (cleanText.length <= 40) cleanText else firstLine.take(40)
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.PERSONAL_NOTE.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.95f,
                    actionabilityConfidence = 0.98f,
                    extractionConfidence = 0.0f,
                    title = title,
                    summary = cleanText,
                    description = cleanText,
                    type = ItemType.NOTE.name,
                    category = ItemCategory.GENERAL.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.95f,
                    evidence = null,
                    reason = "Personal note or greeting with no actionable task or deadline.",
                    explanation = "This is informational content. No action needed.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 2. Educational / Academic / Definition Content
        if (isEducationalOrDefinitional(lower, cleanText)) {
            val title = extractTitleFromEducational(lines, cleanText)
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.EDUCATIONAL_CONTENT.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.96f,
                    actionabilityConfidence = 0.98f,
                    extractionConfidence = 0.0f,
                    title = title,
                    summary = "Educational material about $title.",
                    description = cleanText,
                    type = ItemType.DOCUMENT.name,
                    category = ItemCategory.EDUCATION.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.95f,
                    evidence = null,
                    reason = "The document contains educational/reference material and no explicit user action or deadline.",
                    explanation = "This is educational reference material. No action required.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 3. News / Articles / Press Releases
        if (isNewsArticle(lower, cleanText)) {
            val title = firstLine.take(60)
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.NEWS_ARTICLE.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.95f,
                    actionabilityConfidence = 0.98f,
                    extractionConfidence = 0.0f,
                    title = title,
                    summary = "News article: $title",
                    description = cleanText,
                    type = ItemType.DOCUMENT.name,
                    category = ItemCategory.DOCUMENTS.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.95f,
                    evidence = null,
                    reason = "News article or press report with no direct personal action.",
                    explanation = "This appears to be informational news content. No task or deadline was detected.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // Extract Temporal Tokens
        val (extractedDate, isDateUncertain) = extractDate(cleanText, lower)
        val extractedTime = extractTime(cleanText)

        // Extract Monetary Amount
        val (extractedAmount, extractedCurrency) = extractMoney(cleanText)

        // 4. Active Bill / Utility Payment Detection (ACTIONABLE)
        if (isBillPaymentObligation(lower, extractedAmount, extractedDate)) {
            val title = if (lower.contains("electricity") || lower.contains("বিদ্যুৎ")) "Electricity Bill"
            else if (lower.contains("water") || lower.contains("পানি")) "Water Bill"
            else if (lower.contains("gas") || lower.contains("গ্যাস")) "Gas Bill"
            else if (lower.contains("internet") || lower.contains("wifi")) "Internet Bill"
            else "Bill Payment"

            val evidence = cleanText.lines().firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("bill") || ll.contains("due") || ll.contains("pay")
            } ?: cleanText.take(100)

            val actionText = if (extractedAmount != null && extractedCurrency != null) {
                "Pay $extractedCurrency${extractedAmount.toInt()} by ${extractedDate ?: "due date"}"
            } else "Pay bill"

            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.BILL.name,
                    actionability = Actionability.ACTIONABLE.name,
                    contentTypeConfidence = 0.94f,
                    actionabilityConfidence = 0.96f,
                    extractionConfidence = 0.92f,
                    title = title,
                    summary = "Bill payment obligation of ${extractedCurrency ?: ""}${extractedAmount?.toInt() ?: ""} due $extractedDate",
                    description = cleanText,
                    type = ItemType.PAYMENT.name,
                    category = ItemCategory.FINANCE.name,
                    action = actionText,
                    date = extractedDate,
                    time = extractedTime,
                    amount = extractedAmount,
                    currency = extractedCurrency,
                    priority = ItemPriority.HIGH.name,
                    confidence = 0.95f,
                    evidence = evidence,
                    reason = "Explicit bill payment obligation with due date.",
                    explanation = "Found bill payment deadline for $title.",
                    isActionable = true,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 5. Active Subscription Renewal (ACTIONABLE)
        if (isSubscriptionRenewal(lower, extractedAmount, extractedDate)) {
            val title = extractSubscriptionTitle(lower, firstLine)
            val interval = if (lower.contains("annual") || lower.contains("yearly") || lower.contains("year")) "YEARLY" else "MONTHLY"
            val evidence = cleanText.lines().firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("subscription") || ll.contains("renew") || ll.contains("auto-renew")
            } ?: cleanText.take(100)

            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.SUBSCRIPTION.name,
                    actionability = Actionability.ACTIONABLE.name,
                    contentTypeConfidence = 0.95f,
                    actionabilityConfidence = 0.94f,
                    extractionConfidence = 0.90f,
                    title = title,
                    summary = "Subscription renewal on $extractedDate",
                    description = cleanText,
                    type = ItemType.SUBSCRIPTION.name,
                    category = ItemCategory.FINANCE.name,
                    subscription = title,
                    action = "Review subscription renewal",
                    date = extractedDate,
                    time = extractedTime,
                    amount = extractedAmount,
                    currency = extractedCurrency,
                    subscriptionInterval = interval,
                    priority = ItemPriority.MEDIUM.name,
                    confidence = 0.93f,
                    evidence = evidence,
                    reason = "Explicit subscription renewal detected.",
                    explanation = "Detected subscription renewal on $extractedDate.",
                    isActionable = true,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 6. Active Appointment Detection (ACTIONABLE)
        if (isAppointmentSchedule(lower, extractedDate, extractedTime)) {
            val title = extractAppointmentTitle(lower)
            val evidence = cleanText.lines().firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("appointment") || ll.contains("doctor") || ll.contains("dentist") || ll.contains("clinic")
            } ?: cleanText.take(100)

            val actionText = "Attend $title on ${extractedDate ?: "scheduled date"}" + if (extractedTime != null) " at $extractedTime" else ""

            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.APPOINTMENT.name,
                    actionability = Actionability.ACTIONABLE.name,
                    contentTypeConfidence = 0.95f,
                    actionabilityConfidence = 0.95f,
                    extractionConfidence = 0.92f,
                    title = title,
                    summary = "Appointment scheduled for $extractedDate at ${extractedTime ?: ""}",
                    description = cleanText,
                    type = ItemType.APPOINTMENT.name,
                    category = ItemCategory.HEALTH.name,
                    appointment = title,
                    action = actionText,
                    date = extractedDate,
                    time = extractedTime,
                    priority = ItemPriority.HIGH.name,
                    confidence = 0.95f,
                    evidence = evidence,
                    reason = "Scheduled personal appointment with date/time.",
                    explanation = "Scheduled appointment on $extractedDate.",
                    isActionable = true,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 7. Store Purchase / Receipt (INFORMATIONAL / RECORD)
        if (isReceiptOrPurchase(lower, extractedAmount, extractedCurrency)) {
            val productName = extractProductName(cleanText, lower)
            val title = if (productName != null) "$productName Purchase" else "Store Purchase Receipt"
            val evidence = cleanText.lines().firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("total") || ll.contains("receipt") || ll.contains("purchased")
            } ?: cleanText.take(100)

            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.RECEIPT.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.92f,
                    actionabilityConfidence = 0.90f,
                    extractionConfidence = 0.88f,
                    title = title,
                    summary = "Purchase receipt for $title (${extractedCurrency ?: ""}${extractedAmount?.toInt() ?: ""})",
                    description = cleanText,
                    type = ItemType.DOCUMENT.name,
                    category = ItemCategory.SHOPPING.name,
                    product = productName,
                    merchant = extractMerchantName(lower),
                    action = "",
                    date = extractedDate,
                    amount = extractedAmount,
                    currency = extractedCurrency,
                    returnWindowDays = if (lower.contains("return window")) 14 else null,
                    priority = ItemPriority.LOW.name,
                    confidence = 0.90f,
                    evidence = evidence,
                    reason = "Purchase record / receipt without pending payment.",
                    explanation = "Extracted receipt record.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 8. Explicit Task / Actionable Deadline (ACTIONABLE)
        if (isExplicitUserTask(lower, cleanText)) {
            val taskTitle = extractTaskTitle(cleanText, lines)
            val evidence = cleanText.lines().firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("submit") || ll.contains("must") || ll.contains("deadline") || ll.contains("by friday") || ll.contains("don't forget")
            } ?: cleanText.take(100)

            val actionText = if (extractedDate != null) "Complete by $extractedDate" else "Complete task"

            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.TASK.name,
                    actionability = Actionability.ACTIONABLE.name,
                    contentTypeConfidence = 0.92f,
                    actionabilityConfidence = 0.93f,
                    extractionConfidence = 0.88f,
                    title = taskTitle,
                    summary = "Action item: $taskTitle due $extractedDate",
                    description = cleanText,
                    type = if (extractedDate != null) ItemType.DEADLINE.name else ItemType.TASK.name,
                    category = if (lower.contains("assignment") || lower.contains("homework") || lower.contains("exam") || lower.contains("physics") || lower.contains("project") || lower.contains("students")) ItemCategory.EDUCATION.name else ItemCategory.GENERAL.name,
                    action = actionText,
                    date = extractedDate,
                    time = extractedTime,
                    priority = if (extractedDate != null) ItemPriority.HIGH.name else ItemPriority.MEDIUM.name,
                    confidence = 0.90f,
                    evidence = evidence,
                    reason = "Explicit actionable directive or deadline for user.",
                    explanation = "Detected action item due $extractedDate.",
                    isActionable = true,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 9. Default Fallback: GENERAL_INFORMATION / INFORMATIONAL
        // NEVER assume actionability if evidence is missing!
        val defaultTitle = firstLine.take(50)
        return AIResultValidator.validate(
            AIAnalysisResult(
                contentType = ContentType.GENERAL_INFORMATION.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.85f,
                actionabilityConfidence = 0.90f,
                extractionConfidence = 0.0f,
                title = defaultTitle,
                summary = cleanText.take(120),
                description = cleanText,
                type = ItemType.NOTE.name,
                category = ItemCategory.GENERAL.name,
                action = "",
                date = extractedDate,
                time = extractedTime,
                amount = extractedAmount,
                currency = extractedCurrency,
                priority = ItemPriority.LOW.name,
                confidence = 0.85f,
                evidence = null,
                reason = "No explicit actionable task, bill, or deadline detected.",
                explanation = "This is informational content. No action needed.",
                isActionable = false,
                isUncertain = false
            ),
            cleanText
        ).validatedResult
    }

    // Helper functions

    private fun isShortPersonalNoteOrGreeting(lower: String, clean: String): Boolean {
        if (clean.length > 50) return false
        val words = lower.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size <= 4) {
            val nonActionWords = listOf("this", "is", "alex", "john", "mary", "hello", "hi", "hey", "test", "my", "name", "good", "morning", "night", "thanks", "ok")
            if (words.all { nonActionWords.contains(it) || it.all { ch -> ch.isLetter() } }) {
                return !lower.contains("due") && !lower.contains("pay") && !lower.contains("submit")
            }
        }
        return lower.startsWith("this is ") || lower == "hello" || lower == "hi" || lower.startsWith("my name is")
    }

    private fun isEducationalOrDefinitional(lower: String, text: String): Boolean {
        val educationalKeywords = listOf(
            "a modifier is", "modifiers", "grammar", "chapter ", "section ", "exercise ",
            "textbook", "formula", "definition", "means to", "is defined as", "the word '",
            "mathematical", "physics notes", "chemistry", "biology", "lecture notes", "syllabus"
        )
        val hasDefinitionalStructure = lower.contains("means to") || lower.contains("refers to") || lower.contains("is defined as")
        val isModifierDoc = lower.contains("modifier") || lower.contains("pre-modifier") || lower.contains("post-modifier")
        val hasChapter = lower.contains("chapter ") && !lower.contains("submit chapter")

        return (educationalKeywords.any { lower.contains(it) } || hasDefinitionalStructure || isModifierDoc || hasChapter) &&
                !hasExplicitStudentAction(lower)
    }

    private fun hasExplicitStudentAction(lower: String): Boolean {
        val actionTriggers = listOf(
            "students must submit", "submit your assignment", "assignment due by", "homework due",
            "submit the assignment by", "exam on"
        )
        return actionTriggers.any { lower.contains(it) }
    }

    private fun extractTitleFromEducational(lines: List<String>, cleanText: String): String {
        for (line in lines) {
            val l = line.lowercase(Locale.ROOT)
            if (l.contains("modifier")) return "English Modifiers"
            if (l.startsWith("chapter ")) return line.take(40)
            if (l.contains("definition")) return line.take(40)
        }
        return lines.firstOrNull()?.take(45) ?: "Educational Reference"
    }

    private fun isNewsArticle(lower: String, text: String): Boolean {
        val newsTriggers = listOf(
            "scientists discovered", "researchers found", "according to reports", "breaking news",
            "reported today", "new study finds", "government announced", "officials stated",
            "spokesperson said", "press release", "in an interview", "published in the journal"
        )
        return newsTriggers.any { lower.contains(it) }
    }

    private fun isBillPaymentObligation(lower: String, amount: Double?, date: String?): Boolean {
        val hasBillWord = lower.contains("bill") || lower.contains("electricity") || lower.contains("utility") ||
                lower.contains("gas bill") || lower.contains("water bill") || lower.contains("internet bill") || lower.contains("বিল")
        val hasDueOrPay = lower.contains("due") || lower.contains("pay") || lower.contains("payable") || lower.contains("amount due")
        return hasBillWord && (hasDueOrPay || amount != null)
    }

    private fun isSubscriptionRenewal(lower: String, amount: Double?, date: String?): Boolean {
        val hasSubWord = lower.contains("subscription") || lower.contains("membership") || lower.contains("netflix") || lower.contains("spotify")
        val hasRenewWord = lower.contains("renew") || lower.contains("renews") || lower.contains("auto-renew") || lower.contains("recurring")
        return (hasSubWord && hasRenewWord) || (hasSubWord && date != null) || (hasRenewWord && amount != null)
    }

    private fun extractSubscriptionTitle(lower: String, firstLine: String): String {
        return when {
            lower.contains("netflix") -> "Netflix Subscription"
            lower.contains("spotify") -> "Spotify Subscription"
            lower.contains("youtube") -> "YouTube Premium"
            lower.contains("prime") -> "Amazon Prime"
            lower.contains("gym") -> "Gym Membership"
            else -> "Subscription Renewal"
        }
    }

    private fun isAppointmentSchedule(lower: String, date: String?, time: String?): Boolean {
        val appointmentWords = listOf("dentist", "doctor", "appointment", "clinic", "consultation", "checkup", "meeting with")
        val hasApptWord = appointmentWords.any { lower.contains(it) }
        return hasApptWord && (date != null || time != null || lower.contains("scheduled") || lower.contains("visit"))
    }

    private fun extractAppointmentTitle(lower: String): String {
        return when {
            lower.contains("dentist") -> "Dentist Appointment"
            lower.contains("doctor") -> "Doctor Appointment"
            lower.contains("eye") -> "Eye Clinic Appointment"
            lower.contains("meeting") -> "Scheduled Meeting"
            else -> "Appointment"
        }
    }

    private fun isReceiptOrPurchase(lower: String, amount: Double?, currency: String?): Boolean {
        val purchaseWords = listOf("receipt", "invoice", "purchased", "purchase", "order #", "store #", "ssd", "samsung ssd", "pos terminal")
        val hasPurchaseWord = purchaseWords.any { lower.contains(it) }
        // Ensure not word count like "8,500 words"
        val isWordCount = lower.contains("words") || lower.contains("total words") || lower.contains("pages")
        return hasPurchaseWord && !isWordCount && (amount != null || currency != null || lower.contains("receipt"))
    }

    private fun extractProductName(cleanText: String, lower: String): String? {
        return when {
            lower.contains("samsung ssd") -> "Samsung SSD"
            lower.contains("ssd") -> "SSD Drive"
            lower.contains("shoes") -> "Shoes"
            lower.contains("laptop") -> "Laptop"
            else -> null
        }
    }

    private fun extractMerchantName(lower: String): String? {
        val merchants = listOf("amazon", "walmart", "target", "daraz", "star tech", "ryans", "best buy", "apple store")
        return merchants.firstOrNull { lower.contains(it) }
    }

    private fun isExplicitUserTask(lower: String, clean: String): Boolean {
        // Exclude educational definitions
        if (lower.contains("means to") || lower.contains("is defined as") || lower.contains("the word '")) {
            return false
        }
        val taskImperatives = listOf(
            "submit your", "submit the", "must submit", "call ", "buy groceries", "pay ",
            "finish the", "complete the", "don't forget to", "remember to", "deliver to", "pick up"
        )
        return taskImperatives.any { lower.contains(it) }
    }

    private fun extractTaskTitle(cleanText: String, lines: List<String>): String {
        val lower = cleanText.lowercase(Locale.ROOT)
        return when {
            lower.contains("physics assignment") -> "Submit Physics Assignment"
            lower.contains("assignment") -> "Submit Assignment"
            lower.contains("project") -> "Project Submission"
            else -> lines.firstOrNull()?.take(45) ?: "Task"
        }
    }

    private fun extractMoney(text: String): Pair<Double?, String?> {
        val lower = text.lowercase(Locale.ROOT)
        // Guard against word counts (e.g. "Total 8,500 words")
        if (lower.contains("words") || lower.contains("total words") || lower.contains("word count")) {
            // Check if there is also an explicit currency symbol
            if (!text.contains("৳") && !text.contains("$") && !text.contains("€") && !text.contains("£") && !text.contains("bdt") && !text.contains("tk")) {
                return Pair(null, null)
            }
        }

        val moneyPattern = Pattern.compile("(?i)(৳|\\$|€|£|bdt|tk|টাকা)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(৳|\\$|€|£|bdt|tk|টাকা)")
        val moneyMatcher = moneyPattern.matcher(text)
        if (moneyMatcher.find()) {
            val currGroup = moneyMatcher.group(1) ?: moneyMatcher.group(4)
            val numGroup = moneyMatcher.group(2) ?: moneyMatcher.group(3)
            val amount = numGroup?.replace(",", "")?.toDoubleOrNull()
            val currency = when (currGroup?.lowercase(Locale.ROOT)?.trim()) {
                "৳", "bdt", "tk", "টাকা" -> "৳"
                "$" -> "$"
                "€" -> "€"
                "£" -> "£"
                else -> currGroup ?: "৳"
            }
            return Pair(amount, currency)
        }
        return Pair(null, null)
    }

    private fun extractDate(text: String, lower: String): Pair<String?, Boolean> {
        val datePattern = Pattern.compile("(?i)(?:due|on|by|before|date:)?\\s*(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?")
        val dateMatcher = datePattern.matcher(text)
        if (dateMatcher.find()) {
            val month = dateMatcher.group(1)?.replaceFirstChar { it.uppercase() }
            val day = dateMatcher.group(2)
            val year = dateMatcher.group(3) ?: "2026"
            return Pair("$month $day, $year", false)
        }

        val dayOfWeekPattern = Pattern.compile("(?i)(?:by|on|due)?\\s*(monday|tuesday|wednesday|thursday|friday|saturday|sunday|tomorrow|today)")
        val dowMatcher = dayOfWeekPattern.matcher(text)
        if (dowMatcher.find()) {
            val dow = dowMatcher.group(1)?.replaceFirstChar { it.uppercase() }
            return Pair(dow, false)
        }

        if (lower.contains("next week") || lower.contains("sometime next week")) {
            return Pair("Next week", true)
        }

        return Pair(null, false)
    }

    private fun extractTime(text: String): String? {
        val timePattern = Pattern.compile("(?i)(\\d{1,2}(?::\\d{2})?)\\s*(am|pm)")
        val timeMatcher = timePattern.matcher(text)
        if (timeMatcher.find()) {
            return "${timeMatcher.group(1)} ${timeMatcher.group(2)?.uppercase(Locale.ROOT)}"
        }
        return null
    }
}
