package com.example.data.remote

import com.example.data.model.AIAnalysisResult
import com.example.data.model.Actionability
import com.example.data.model.BillType
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

        // 2. Unidentifiable / Blurry / Random Content
        if (isBlurryOrUnidentifiable(lower)) {
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.UNKNOWN.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.50f,
                    actionabilityConfidence = 0.90f,
                    extractionConfidence = 0.0f,
                    title = "Unidentified Image",
                    summary = "I couldn't identify this content or read clear details.",
                    description = cleanText,
                    type = ItemType.NOTE.name,
                    category = ItemCategory.GENERAL.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.50f,
                    evidence = null,
                    reason = "Image/content is unreadable, blurry, or lacks clear extractable information.",
                    explanation = "Unidentified content.",
                    isActionable = false,
                    isUncertain = true
                ),
                cleanText
            ).validatedResult
        }

        // 3. Visual Photographs (Pets, Nature, Landscapes, People, Scenes)
        if (isPhotographOrVisualScene(lower)) {
            val photoTitle = extractPhotographTitle(lower, cleanText)
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.PHOTOGRAPH.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.96f,
                    actionabilityConfidence = 0.98f,
                    extractionConfidence = 0.0f,
                    title = photoTitle,
                    summary = "Photograph: $photoTitle",
                    description = cleanText,
                    type = ItemType.NOTE.name,
                    category = ItemCategory.GENERAL.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.95f,
                    evidence = null,
                    reason = "Visual photograph with no pending task, deadline, or financial obligation.",
                    explanation = "Saved photograph.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 4. Product Photographs / Gadgets / Merchandise (Non-Receipt)
        if (isProductPhoto(lower)) {
            val productTitle = extractProductPhotoTitle(cleanText, lower)
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.PRODUCT_IMAGE.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.94f,
                    actionabilityConfidence = 0.98f,
                    extractionConfidence = 0.0f,
                    title = productTitle,
                    summary = "Product image: $productTitle",
                    description = cleanText,
                    type = ItemType.NOTE.name,
                    category = ItemCategory.SHOPPING.name,
                    product = productTitle,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.94f,
                    evidence = null,
                    reason = "Product photograph without transaction receipt or pending obligation.",
                    explanation = "Saved product photo.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 5. Chat / Instant Messaging Screenshots
        if (isChatScreenshot(lower)) {
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.CHAT_SCREENSHOT.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.95f,
                    actionabilityConfidence = 0.95f,
                    extractionConfidence = 0.0f,
                    title = "Chat Conversation",
                    summary = "Instant messaging chat conversation record.",
                    description = cleanText,
                    type = ItemType.NOTE.name,
                    category = ItemCategory.GENERAL.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.95f,
                    evidence = null,
                    reason = "Messaging screenshot captured for information.",
                    explanation = "Saved chat conversation.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 6. Educational / Academic / Math / Definition Content (INFORMATIONAL)
        if (isEducationalOrDefinitional(lower, cleanText) || isMathOrPhysics(lower)) {
            val title = extractTitleFromEducational(lines, cleanText)
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.EDUCATIONAL_PAGE.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.96f,
                    actionabilityConfidence = 0.98f,
                    extractionConfidence = 0.0f,
                    title = title,
                    summary = "Educational material: $title.",
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

        // 3. Card / Payment Method Information (INFORMATIONAL / RECORD)
        if (isCardOrPaymentMethod(lower, cleanText)) {
            val cardTitle = extractCardTitle(cleanText, lower)
            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.CARD.name,
                    actionability = Actionability.INFORMATIONAL.name,
                    contentTypeConfidence = 0.94f,
                    actionabilityConfidence = 0.96f,
                    extractionConfidence = 0.90f,
                    title = cardTitle,
                    summary = "Payment card details for $cardTitle",
                    description = cleanText,
                    type = ItemType.DOCUMENT.name,
                    category = ItemCategory.FINANCE.name,
                    action = "",
                    priority = ItemPriority.LOW.name,
                    confidence = 0.94f,
                    evidence = cleanText.take(100),
                    reason = "Payment method / card information recorded safely.",
                    explanation = "Detected payment card info. No bill payment required.",
                    isActionable = false,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 4. News / Articles / Press Releases
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

        // Extract Monetary Amount (with special handling for Total Due vs Previous Balance)
        val (extractedAmount, extractedCurrency) = extractBillFinancials(cleanText, lower)

        // 4b. Commercial Invoice Detection (ACTIONABLE / PAYMENT PENDING)
        if (isCommercialInvoice(lower, cleanText)) {
            val invNum = extractInvoiceNumber(cleanText)
            val customer = extractCustomerName(cleanText)
            val provider = extractInvoiceProvider(cleanText, lines)
            val amountDue = extractedAmount
            val isPaid = lower.contains("status: paid") || lower.contains("paid in full")

            val title = if (provider != null) "$provider Invoice" else if (invNum != null) "Invoice $invNum" else "Commercial Invoice"
            val actionText = if (!isPaid && amountDue != null) "Pay invoice ${extractedCurrency ?: "৳"}${amountDue.toInt()}" else if (!isPaid) "Review invoice" else ""

            val evidence = cleanText.lines().firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("invoice") || ll.contains("total due") || ll.contains("amount due")
            } ?: cleanText.take(100)

            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.INVOICE.name,
                    actionability = if (isPaid) Actionability.INFORMATIONAL.name else Actionability.ACTIONABLE.name,
                    contentTypeConfidence = 0.96f,
                    actionabilityConfidence = 0.95f,
                    extractionConfidence = 0.94f,
                    title = title,
                    summary = "Invoice for $title with amount ${extractedCurrency ?: "৳"}${amountDue?.toInt() ?: ""}" + if (extractedDate != null) " due $extractedDate" else "",
                    description = cleanText,
                    type = ItemType.DOCUMENT.name,
                    category = ItemCategory.FINANCE.name,
                    action = actionText,
                    date = extractedDate,
                    time = extractedTime,
                    amount = amountDue,
                    amountDue = amountDue,
                    currency = extractedCurrency,
                    invoiceNumber = invNum,
                    customer = customer,
                    organization = provider,
                    paymentStatus = if (isPaid) "PAID" else "UNPAID",
                    priority = if (isPaid) ItemPriority.LOW.name else ItemPriority.HIGH.name,
                    confidence = 0.95f,
                    evidence = evidence,
                    reason = "Explicit commercial invoice with invoice number and amount due.",
                    explanation = "Identified invoice from ${provider ?: "vendor"}.",
                    isActionable = !isPaid,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 5. Active Bill / Utility Payment Detection (ACTIONABLE)
        if (isBillPaymentObligation(lower, extractedAmount, extractedDate)) {
            val detectedBillType = detectBillType(lower)
            val provider = extractBillProvider(lower)
            val title = if (provider != null) "$provider ${detectedBillType.displayName}" else detectedBillType.displayName

            val evidence = cleanText.lines().firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("bill") || ll.contains("due") || ll.contains("pay") || ll.contains("charges") || ll.contains("টাকা") || ll.contains("৳")
            } ?: cleanText.take(100)

            val actionText = if (extractedAmount != null && extractedCurrency != null) {
                if (extractedDate != null) "Pay $extractedCurrency${extractedAmount.toInt()} by $extractedDate"
                else "Pay $extractedCurrency${extractedAmount.toInt()}"
            } else "Pay bill"

            return AIResultValidator.validate(
                AIAnalysisResult(
                    contentType = ContentType.BILL.name,
                    actionability = Actionability.ACTIONABLE.name,
                    contentTypeConfidence = 0.96f,
                    actionabilityConfidence = 0.96f,
                    extractionConfidence = 0.94f,
                    title = title,
                    summary = "${detectedBillType.displayName} of ${extractedCurrency ?: ""}${extractedAmount?.toInt() ?: ""}" + if (extractedDate != null) " due $extractedDate" else "",
                    description = cleanText,
                    type = ItemType.PAYMENT.name,
                    category = ItemCategory.FINANCE.name,
                    action = actionText,
                    date = extractedDate,
                    time = extractedTime,
                    amount = extractedAmount,
                    amountDue = extractedAmount,
                    currency = extractedCurrency,
                    billType = detectedBillType.name,
                    billProvider = provider,
                    organization = provider,
                    paymentStatus = "UNPAID",
                    priority = ItemPriority.HIGH.name,
                    confidence = 0.95f,
                    evidence = evidence,
                    reason = "Explicit bill payment obligation with amount due.",
                    explanation = "Found bill payment obligation for $title.",
                    isActionable = true,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 6. Active Subscription Renewal (ACTIONABLE)
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
                    summary = "Subscription renewal" + if (extractedDate != null) " on $extractedDate" else "",
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
                    explanation = "Detected subscription renewal.",
                    isActionable = true,
                    isUncertain = false
                ),
                cleanText
            ).validatedResult
        }

        // 7. Active Appointment Detection (ACTIONABLE)
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

        // 8. Store Purchase / Receipt (INFORMATIONAL / RECORD)
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

        // 9. Explicit Task / Actionable Deadline (ACTIONABLE)
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

        // 10. Genuinely General Reference Document vs UNKNOWN Fallback
        val isGeneralWiki = lower.contains("wikipedia") || lower.contains("encyclopedia") || lower.contains("the capital of") || lower.contains("history of")
        val defaultContentType = if (isGeneralWiki) ContentType.GENERAL_INFORMATION.name else ContentType.UNKNOWN.name
        val defaultActionability = if (isGeneralWiki) Actionability.INFORMATIONAL.name else Actionability.UNCERTAIN.name
        val defaultTitle = firstLine.take(50)

        return AIResultValidator.validate(
            AIAnalysisResult(
                contentType = defaultContentType,
                actionability = defaultActionability,
                contentTypeConfidence = if (isGeneralWiki) 0.85f else 0.40f,
                actionabilityConfidence = if (isGeneralWiki) 0.90f else 0.50f,
                extractionConfidence = 0.0f,
                title = defaultTitle,
                summary = cleanText.take(120),
                description = cleanText,
                type = if (isGeneralWiki) ItemType.NOTE.name else ItemType.DOCUMENT.name,
                category = ItemCategory.GENERAL.name,
                action = "",
                date = extractedDate,
                time = extractedTime,
                amount = extractedAmount,
                currency = extractedCurrency,
                priority = ItemPriority.LOW.name,
                confidence = if (isGeneralWiki) 0.85f else 0.40f,
                evidence = null,
                reason = if (isGeneralWiki) "General reference knowledge content." else "No explicit actionable task, bill, or recognizable document pattern detected.",
                explanation = if (isGeneralWiki) "This is informational reference content." else "LifeVault is unable to confidently identify the document type.",
                isActionable = false,
                isUncertain = !isGeneralWiki
            ),
            cleanText
        ).validatedResult
    }

    // --- Helper Functions ---

    private fun isShortPersonalNoteOrGreeting(lower: String, clean: String): Boolean {
        if (clean.length > 50) return false
        val words = lower.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size <= 4) {
            val nonActionWords = listOf("this", "is", "alex", "john", "mary", "hello", "hi", "hey", "test", "my", "name", "good", "morning", "night", "thanks", "ok")
            if (words.all { nonActionWords.contains(it) || it.all { ch -> ch.isLetter() } }) {
                return !lower.contains("due") && !lower.contains("pay") && !lower.contains("submit") && !lower.contains("bill")
            }
        }
        return lower.startsWith("this is ") || lower == "hello" || lower == "hi" || lower.startsWith("my name is")
    }

    private fun isEducationalOrDefinitional(lower: String, text: String): Boolean {
        val educationalKeywords = listOf(
            "a modifier is", "modifiers", "grammar", "chapter ", "section ", "exercise ",
            "textbook", "formula", "definition", "means to", "is defined as", "the word '",
            "mathematical", "physics notes", "chemistry", "biology", "lecture notes", "syllabus",
            "what is a bill", "bills are defined", "understanding bills and utilities"
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

    private fun isCardOrPaymentMethod(lower: String, clean: String): Boolean {
        val hasCardWord = lower.contains("visa") || lower.contains("mastercard") || lower.contains("amex") ||
                lower.contains("credit card") || lower.contains("debit card") || lower.contains("ending in") ||
                lower.contains("ending ") || lower.contains("card number") || lower.contains("cardholder")
        val hasBillDue = lower.contains("due") || lower.contains("bill") || lower.contains("payable")
        // If it's pure card info like "My Visa ending 4821" or "Credit card info", classify as CARD
        return hasCardWord && !hasBillDue
    }

    private fun extractCardTitle(cleanText: String, lower: String): String {
        return when {
            lower.contains("visa") -> "Visa Card"
            lower.contains("mastercard") -> "Mastercard"
            lower.contains("amex") -> "American Express Card"
            lower.contains("debit") -> "Debit Card"
            lower.contains("credit") -> "Credit Card"
            else -> "Payment Method"
        }
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
        val billKeywords = listOf(
            "gas bill", "electricity bill", "water bill", "internet bill", "wifi bill", "mobile bill",
            "telephone bill", "tv bill", "cable bill", "rent", "tuition", "insurance premium",
            "credit card bill", "loan payment", "government fee", "bill", "বিল", "বিদ্যুৎ", "গ্যাস", "পানি", "ওয়াসা"
        )
        val hasBillKeyword = billKeywords.any { lower.contains(it) }
        val hasPaymentSignal = lower.contains("due") || lower.contains("pay") || lower.contains("payable") ||
                lower.contains("balance") || lower.contains("charges") || lower.contains("টাকা") || lower.contains("৳")
        return hasBillKeyword && (amount != null || hasPaymentSignal)
    }

    private fun detectBillType(lower: String): BillType {
        return when {
            lower.contains("gas") || lower.contains("গ্যাস") || lower.contains("titas") || lower.contains("bakhrabad") -> BillType.GAS
            lower.contains("electricity") || lower.contains("electric") || lower.contains("বিদ্যুৎ") || lower.contains("desco") || lower.contains("dpdc") || lower.contains("nesco") || lower.contains("reb") -> BillType.ELECTRICITY
            lower.contains("water") || lower.contains("পানি") || lower.contains("wasa") || lower.contains("ওয়াসা") -> BillType.WATER
            lower.contains("internet") || lower.contains("wifi") || lower.contains("broadband") || lower.contains("fiber") || lower.contains("link3") || lower.contains("carnival") -> BillType.INTERNET
            lower.contains("mobile bill") || lower.contains("postpaid") || lower.contains("grameenphone") || lower.contains("banglalink") || lower.contains("robi") || lower.contains("airtel") || lower.contains("teletalk") -> BillType.MOBILE
            lower.contains("telephone") || lower.contains("landline") || lower.contains("btcl") -> BillType.TELEPHONE
            lower.contains("tv") || lower.contains("cable") || lower.contains("dish") || lower.contains("akash") -> BillType.TV_CABLE
            lower.contains("rent") || lower.contains("house rent") || lower.contains("apartment rent") || lower.contains("ভাড়া") -> BillType.RENT
            lower.contains("tuition") || lower.contains("school fee") || lower.contains("college fee") || lower.contains("university fee") || lower.contains("semester fee") || lower.contains("বেতন") -> BillType.TUITION
            lower.contains("insurance") || lower.contains("premium") -> BillType.INSURANCE
            lower.contains("credit card bill") || lower.contains("card statement") || lower.contains("card bill") -> BillType.CREDIT_CARD
            lower.contains("loan") || lower.contains("emi") || lower.contains("installment") -> BillType.LOAN
            lower.contains("government") || lower.contains("tax bill") || lower.contains("holding tax") || lower.contains("municipal") -> BillType.GOVERNMENT
            lower.contains("subscription") -> BillType.SUBSCRIPTION
            else -> BillType.OTHER
        }
    }

    private fun extractBillProvider(lower: String): String? {
        val providers = listOf(
            "desco", "dpdc", "reb", "nesco", "bpdb", "titas", "bakhrabad", "jalalabad", "wasa",
            "btcl", "link3", "carnival", "amber it", "grameenphone", "robi", "banglalink", "teletalk",
            "comcast", "xfinity", "at&t", "verizon", "t-mobile", "con edison", "pge"
        )
        val matched = providers.firstOrNull { lower.contains(it) }
        return matched?.uppercase(Locale.ROOT)
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

    private fun isBlurryOrUnidentifiable(lower: String): Boolean {
        val blurryKeywords = listOf("blurry", "unfocused", "unreadable", "noise", "blank image", "cannot read", "unclear", "dark image", "random pixels", "empty image")
        return blurryKeywords.any { lower.contains(it) }
    }

    private fun isPhotographOrVisualScene(lower: String): Boolean {
        val visualKeywords = listOf(
            "dog", "puppy", "golden retriever", "retriever", "german shepherd", "husky", "labrador", "cat", "kitten", "pet",
            "landscape", "mountain", "mountains", "sunset", "sunrise", "beach", "ocean", "sea", "forest", "scenery", "waterfall",
            "person", "portrait", "selfie", "man smiling", "woman smiling", "boy", "girl", "friends photo", "family photo",
            "city skyline", "skyline", "nature", "trees", "lake", "river"
        )
        return visualKeywords.any { lower.contains(it) } && !lower.contains("receipt") && !lower.contains("tax invoice") && !lower.contains("subtotal")
    }

    private fun extractPhotographTitle(lower: String, clean: String): String {
        return when {
            lower.contains("golden retriever") -> "Golden Retriever"
            lower.contains("german shepherd") -> "German Shepherd"
            lower.contains("husky") -> "Husky Dog"
            lower.contains("puppy") -> "Cute Puppy"
            lower.contains("dog") -> "Dog Photograph"
            lower.contains("cat") || lower.contains("kitten") -> "Cat Photograph"
            lower.contains("mountain") -> "Mountain Landscape"
            lower.contains("sunset") -> "Sunset View"
            lower.contains("sunrise") -> "Sunrise View"
            lower.contains("beach") || lower.contains("ocean") -> "Ocean Beach Landscape"
            lower.contains("forest") -> "Forest Scenery"
            lower.contains("selfie") -> "Selfie Photo"
            lower.contains("portrait") -> "Portrait Photo"
            lower.contains("person") -> "Person Photograph"
            else -> "Photograph"
        }
    }

    private fun isProductPhoto(lower: String): Boolean {
        val productWords = listOf(
            "product photo", "product image", "sneakers", "nike", "adidas", "headphones",
            "sony", "iphone", "pixel", "samsung galaxy", "macbook", "camera lens", "watch",
            "coffee maker", "gadget", "smart watch", "samsung ssd", "ssd"
        )
        val hasReceiptSigns = lower.contains("subtotal") || lower.contains("tax") || lower.contains("cashier") || lower.contains("pos terminal") || lower.contains("receipt")
        return productWords.any { lower.contains(it) } && !hasReceiptSigns
    }

    private fun extractProductPhotoTitle(cleanText: String, lower: String): String {
        return when {
            lower.contains("samsung ssd") -> "Samsung SSD"
            lower.contains("ssd") -> "SSD Storage Drive"
            lower.contains("sneakers") || lower.contains("nike") -> "Nike Sneakers"
            lower.contains("headphones") || lower.contains("sony") -> "Sony Headphones"
            lower.contains("iphone") -> "Apple iPhone"
            lower.contains("pixel") -> "Google Pixel"
            lower.contains("macbook") -> "Apple MacBook"
            lower.contains("watch") -> "Smart Watch"
            else -> "Product Photo"
        }
    }

    private fun isChatScreenshot(lower: String): Boolean {
        val chatWords = listOf(
            "chat screenshot", "whatsapp", "telegram", "messenger", "imessage", "conversation",
            "typing...", "online", "delivered", "read at", "yesterday at", "pm\n", "am\n"
        )
        return chatWords.any { lower.contains(it) } && !lower.contains("receipt")
    }

    private fun isMathOrPhysics(lower: String): Boolean {
        val mathWords = listOf(
            "math", "mathematics", "calculus", "algebra", "integral", "derivative", "equation",
            "solve for x", "quadratic formula", "physics problem", "velocity", "acceleration", "f = ma"
        )
        return mathWords.any { lower.contains(it) } && !lower.contains("receipt")
    }

    private fun isReceiptOrPurchase(lower: String, amount: Double?, currency: String?): Boolean {
        val explicitReceiptTokens = listOf("receipt", "store #", "cashier", "pos terminal", "subtotal", "vat #", "sales slip", "tax invoice")
        val hasExplicitToken = explicitReceiptTokens.any { lower.contains(it) }
        val isWordCount = lower.contains("words") || lower.contains("total words") || lower.contains("pages")
        return hasExplicitToken && !isWordCount && (amount != null || currency != null || lower.contains("receipt"))
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
        if (lower.contains("means to") || lower.contains("is defined as") || lower.contains("the word '")) {
            return false
        }
        val taskImperatives = listOf(
            "submit your", "submit the", "must submit", "call ", "buy groceries",
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

    /**
     * Extracts the true amount due by prioritizing "total due", "amount due", "payable"
     * over previous balance or individual line item charges.
     */
    private fun extractBillFinancials(text: String, lower: String): Pair<Double?, String?> {
        // Guard against word counts (e.g. "Total 8,500 words")
        if ((lower.contains("words") || lower.contains("total words") || lower.contains("word count")) &&
            !text.contains("৳") && !text.contains("$") && !text.contains("€") && !text.contains("£") && !text.contains("bdt") && !text.contains("tk") && !text.contains("taka")
        ) {
            return Pair(null, null)
        }

        val totalDuePattern = Pattern.compile("(?i)(?:total\\s+due|amount\\s+due|balance\\s+due|total\\s+payable|payable|total\\s+amount|net\\s+payable)\\s*[:=]?\\s*(?:৳|\\$|€|£|bdt|tk|taka|টাকা)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:৳|\\$|€|£|bdt|tk|taka|টাকা)?")
        val totalDueMatcher = totalDuePattern.matcher(text)
        if (totalDueMatcher.find()) {
            val amountStr = totalDueMatcher.group(1)?.replace(",", "")
            val amount = amountStr?.toDoubleOrNull()
            val currency = extractCurrencySymbol(text)
            if (amount != null) {
                return Pair(amount, currency)
            }
        }

        // Generic currency extraction
        val moneyPattern = Pattern.compile("(?i)(৳|\\$|€|£|bdt|tk|টাকা|taka)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(৳|\\$|€|£|bdt|tk|টাকা|taka)")
        val moneyMatcher = moneyPattern.matcher(text)
        var lastAmount: Double? = null
        var lastCurrency = "৳"

        while (moneyMatcher.find()) {
            val currGroup = moneyMatcher.group(1) ?: moneyMatcher.group(4)
            val numGroup = moneyMatcher.group(2) ?: moneyMatcher.group(3)
            val amount = numGroup?.replace(",", "")?.toDoubleOrNull()
            if (amount != null) {
                lastAmount = amount
                lastCurrency = when (currGroup?.lowercase(Locale.ROOT)?.trim()) {
                    "৳", "bdt", "tk", "taka", "টাকা" -> "৳"
                    "$" -> "$"
                    "€" -> "€"
                    "£" -> "£"
                    else -> currGroup ?: "৳"
                }
            }
        }

        if (lastAmount != null) {
            return Pair(lastAmount, lastCurrency)
        }

        return Pair(null, null)
    }

    private fun extractCurrencySymbol(text: String): String {
        return when {
            text.contains("$") -> "$"
            text.contains("€") -> "€"
            text.contains("£") -> "£"
            text.contains("₹") -> "₹"
            else -> "৳"
        }
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

    private fun isCommercialInvoice(lower: String, text: String): Boolean {
        val hasInvoiceToken = lower.contains("invoice") || lower.contains("inv-") || lower.contains("inv #") || lower.contains("tax invoice")
        val hasFinancials = lower.contains("subtotal") || lower.contains("total amount due") || lower.contains("balance due") || lower.contains("bill to") || lower.contains("due date") || lower.contains("amount due")
        val isNotUtility = !lower.contains("kwh") && !lower.contains("meter reading") && !lower.contains("desco") && !lower.contains("dpdc")
        return hasInvoiceToken && (hasFinancials || isNotUtility)
    }

    private fun extractInvoiceNumber(text: String): String? {
        val pattern = Pattern.compile("(?i)(?:invoice|inv)[\\s#.:\\-_]+([A-Z0-9\\-_]{3,})")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun extractCustomerName(text: String): String? {
        val pattern = Pattern.compile("(?i)(?:bill to|customer|client|sold to)[\\s:]+([A-Za-z0-9 .,'-]+)")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(35)
        }
        return null
    }

    private fun extractInvoiceProvider(text: String, lines: List<String>): String? {
        val firstLine = lines.firstOrNull()
        if (!firstLine.isNullOrBlank() && !firstLine.lowercase(Locale.ROOT).contains("invoice") && firstLine.length <= 40) {
            return firstLine
        }
        val fromPattern = Pattern.compile("(?i)(?:from|vendor|provider|biller)[\\s:]+([A-Za-z0-9 .,'-]+)")
        val matcher = fromPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(40)
        }
        return lines.take(2).firstOrNull { it.length <= 40 && !it.contains(":") }
    }
}
