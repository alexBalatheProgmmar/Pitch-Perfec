package com.example.data.remote

import android.graphics.Bitmap
import com.example.data.model.AIAnalysisResult
import com.example.data.model.Actionability
import com.example.data.model.ContentType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemPriority
import com.example.data.model.ItemType
import java.util.Locale

/**
 * Multi-Branch Image Classifier & Specialized Analyzers.
 *
 * Architecture:
 *   Image -> General Image Classifier -> Specific Analyzer Branch
 *
 * Directives:
 *   - RECEIPT ANALYSIS IS A SPECIALIZED BRANCH, NEVER THE DEFAULT.
 *   - Normal photos, landscapes, people, dogs, textbooks, news, chats, products, etc.
 *     are routed to their respective specialized branches.
 *   - Fallback for unrecognized or blurry content is UNKNOWN or GENERAL_INFORMATION (INFORMATIONAL),
 *     NEVER a receipt.
 */
object ImageContentClassifier {

    data class ClassificationResult(
        val contentType: ContentType,
        val confidence: Float,
        val titleCandidate: String,
        val visualDescription: String,
        val detectedTextSnippet: String? = null,
        val reasoning: String
    )

    /**
     * Rule-based / fast local image content classification when AI is offline,
     * or to pre-validate image content type.
     */
    fun classifyFromContextOrHint(hint: String?): ClassificationResult {
        val clean = hint?.trim().orEmpty()
        val lower = clean.lowercase(Locale.ROOT)

        if (clean.isBlank()) {
            return ClassificationResult(
                contentType = ContentType.UNKNOWN,
                confidence = 0.50f,
                titleCandidate = "Captured Image",
                visualDescription = "Image captured without additional text or labels.",
                reasoning = "No textual or classification hints available."
            )
        }

        // 1. Animals / Pets
        if (isDogOrPet(lower)) {
            val dogTitle = extractDogTitle(lower)
            return ClassificationResult(
                contentType = ContentType.PHOTOGRAPH,
                confidence = 0.96f,
                titleCandidate = dogTitle,
                visualDescription = "Photograph of a $dogTitle.",
                reasoning = "Visual elements indicate a domestic pet / animal photograph."
            )
        }

        // 2. Landscapes / Nature / Travel Photos
        if (isLandscapeOrNature(lower)) {
            val landscapeTitle = extractLandscapeTitle(lower)
            return ClassificationResult(
                contentType = ContentType.PHOTOGRAPH,
                confidence = 0.95f,
                titleCandidate = landscapeTitle,
                visualDescription = "Photograph depicting $landscapeTitle.",
                reasoning = "Visual scenery / nature / landscape detected."
            )
        }

        // 3. Person / Portrait / Selfie
        if (isPersonOrPortrait(lower)) {
            val personTitle = extractPersonTitle(lower)
            return ClassificationResult(
                contentType = ContentType.PHOTOGRAPH,
                confidence = 0.95f,
                titleCandidate = personTitle,
                visualDescription = "Portrait photograph.",
                reasoning = "Human subject / portrait photography detected."
            )
        }

        // 4. Blurry / Low Quality / Noise / Random
        if (isBlurryOrUnidentifiable(lower)) {
            return ClassificationResult(
                contentType = ContentType.UNKNOWN,
                confidence = 0.50f,
                titleCandidate = "Unidentified Image",
                visualDescription = "I couldn't identify this content or read clear details.",
                reasoning = "Image content is blurry, unreadable, or unidentifiable."
            )
        }

        // 5. Math / Equations / Physics Exercises
        if (isMathOrPhysics(lower)) {
            val mathTitle = extractMathTitle(clean, lower)
            return ClassificationResult(
                contentType = ContentType.EDUCATIONAL_PAGE,
                confidence = 0.96f,
                titleCandidate = mathTitle,
                visualDescription = "Mathematical equations and study problems.",
                detectedTextSnippet = clean.take(150),
                reasoning = "Academic mathematical formulas and academic problem sets."
            )
        }

        // 6. Educational Textbook / Grammar / Lecture Notes
        if (isEducationalContent(lower)) {
            val eduTitle = extractEducationalTitle(clean, lower)
            return ClassificationResult(
                contentType = ContentType.EDUCATIONAL_PAGE,
                confidence = 0.96f,
                titleCandidate = eduTitle,
                visualDescription = "Textbook page / educational study material.",
                detectedTextSnippet = clean.take(150),
                reasoning = "Textbook typography, pedagogical definitions, and academic concepts."
            )
        }

        // 7. News Articles / Journalism
        if (isNewsArticle(lower)) {
            val newsTitle = extractNewsTitle(clean, lower)
            return ClassificationResult(
                contentType = ContentType.NEWS,
                confidence = 0.95f,
                titleCandidate = newsTitle,
                visualDescription = "News article or journalistic press report.",
                detectedTextSnippet = clean.take(150),
                reasoning = "Editorial headlines and reporting style text."
            )
        }

        // 8. Chat / Messaging Screenshots
        if (isChatScreenshot(lower)) {
            val chatTitle = extractChatTitle(clean, lower)
            return ClassificationResult(
                contentType = ContentType.CHAT_SCREENSHOT,
                confidence = 0.95f,
                titleCandidate = chatTitle,
                visualDescription = "Screenshot of instant messaging conversation.",
                detectedTextSnippet = clean.take(150),
                reasoning = "Chat bubbles, sender timestamps, and conversational messaging UI."
            )
        }

        // 9. Product Photos / Gadgets / Merchandise (Non-Receipt)
        if (isProductPhoto(lower)) {
            val productTitle = extractProductPhotoTitle(clean, lower)
            return ClassificationResult(
                contentType = ContentType.PRODUCT_IMAGE,
                confidence = 0.94f,
                titleCandidate = productTitle,
                visualDescription = "Product photograph of $productTitle.",
                reasoning = "Commercial product / merchandise photography without transaction or receipt markers."
            )
        }

        // 10. Bills / Utility Invoices (Actionable with payment due)
        if (isUtilityBill(lower)) {
            val billTitle = extractBillTitle(clean, lower)
            return ClassificationResult(
                contentType = ContentType.BILL,
                confidence = 0.95f,
                titleCandidate = billTitle,
                visualDescription = "Utility bill statement with amount due.",
                detectedTextSnippet = clean.take(150),
                reasoning = "Utility billing statement with account/meter details and payment due date."
            )
        }

        // 11. Point-of-Sale Store Receipts (Requires strict evidence!)
        if (isStrictReceipt(lower, clean)) {
            val receiptTitle = extractStrictReceiptTitle(clean, lower)
            return ClassificationResult(
                contentType = ContentType.RECEIPT,
                confidence = 0.92f,
                titleCandidate = receiptTitle,
                visualDescription = "Itemized store sales receipt.",
                detectedTextSnippet = clean.take(150),
                reasoning = "Itemized point-of-sale receipt with store header, line items, and transaction totals."
            )
        }

        // 12. Generic Document / Record
        if (isGeneralDocument(lower)) {
            return ClassificationResult(
                contentType = ContentType.DOCUMENT,
                confidence = 0.90f,
                titleCandidate = clean.lines().firstOrNull()?.take(40) ?: "Document",
                visualDescription = "Printed or digital document page.",
                detectedTextSnippet = clean.take(150),
                reasoning = "Structured text document."
            )
        }

        // Default Safe Fallback: GENERAL_INFORMATION / INFORMATIONAL
        return ClassificationResult(
            contentType = ContentType.GENERAL_INFORMATION,
            confidence = 0.85f,
            titleCandidate = clean.lines().firstOrNull()?.take(40) ?: "General Record",
            visualDescription = clean.take(120),
            reasoning = "General information capture."
        )
    }

    /**
     * Executes the appropriate specialized analyzer branch according to the classified ContentType.
     */
    fun routeAndAnalyze(
        classification: ClassificationResult,
        rawText: String = "",
        bitmap: Bitmap? = null
    ): AIAnalysisResult {
        return when (classification.contentType) {
            ContentType.PHOTOGRAPH -> PhotographAnalyzer.analyze(classification, rawText)
            ContentType.EDUCATIONAL_PAGE, ContentType.EDUCATIONAL_CONTENT -> EducationalAnalyzer.analyze(classification, rawText)
            ContentType.NEWS, ContentType.NEWS_ARTICLE -> NewsAnalyzer.analyze(classification, rawText)
            ContentType.CHAT_SCREENSHOT, ContentType.CONVERSATION -> ConversationAnalyzer.analyze(classification, rawText)
            ContentType.PRODUCT_IMAGE -> ProductAnalyzer.analyze(classification, rawText)
            ContentType.BILL -> BillAnalyzer.analyze(classification, rawText)
            ContentType.RECEIPT, ContentType.INVOICE -> ReceiptAnalyzer.analyze(classification, rawText)
            ContentType.UNKNOWN -> UnknownAnalyzer.analyze(classification, rawText)
            ContentType.DOCUMENT, ContentType.FORM, ContentType.CERTIFICATE, ContentType.ID_DOCUMENT -> DocumentAnalyzer.analyze(classification, rawText)
            else -> GeneralAnalyzer.analyze(classification, rawText)
        }
    }

    // --- Classification Helpers ---

    private fun isDogOrPet(lower: String): Boolean {
        val petWords = listOf(
            "dog", "puppy", "golden retriever", "retriever", "german shepherd", "husky", "labrador",
            "bulldog", "poodle", "cat", "kitten", "pet", "animal", "canine", "feline", "bird", "parrot"
        )
        return petWords.any { lower.contains(it) } && !lower.contains("receipt") && !lower.contains("invoice")
    }

    private fun extractDogTitle(lower: String): String {
        return when {
            lower.contains("golden retriever") -> "Golden Retriever"
            lower.contains("german shepherd") -> "German Shepherd"
            lower.contains("husky") -> "Husky Dog"
            lower.contains("puppy") -> "Cute Puppy"
            lower.contains("dog") -> "Dog Photograph"
            lower.contains("cat") || lower.contains("kitten") -> "Cat Photograph"
            else -> "Pet Photograph"
        }
    }

    private fun isLandscapeOrNature(lower: String): Boolean {
        val sceneryWords = listOf(
            "landscape", "mountain", "mountains", "sunset", "sunrise", "beach", "ocean", "sea",
            "forest", "river", "lake", "scenery", "nature", "waterfall", "skyline", "clouds",
            "valley", "green hills", "horizon", "trees"
        )
        return sceneryWords.any { lower.contains(it) } && !lower.contains("receipt") && !lower.contains("bill")
    }

    private fun extractLandscapeTitle(lower: String): String {
        return when {
            lower.contains("mountain") -> "Mountain Landscape"
            lower.contains("sunset") -> "Sunset View"
            lower.contains("sunrise") -> "Sunrise View"
            lower.contains("beach") || lower.contains("ocean") -> "Ocean Beach Landscape"
            lower.contains("forest") -> "Forest Scenery"
            lower.contains("waterfall") -> "Waterfall Landscape"
            lower.contains("lake") || lower.contains("river") -> "Lakeside Scenery"
            else -> "Landscape Photograph"
        }
    }

    private fun isPersonOrPortrait(lower: String): Boolean {
        val personWords = listOf(
            "person", "portrait", "selfie", "man smiling", "woman smiling", "boy", "girl",
            "group of people", "friends photo", "headshot", "photograph of a person", "family photo"
        )
        return personWords.any { lower.contains(it) } && !lower.contains("receipt") && !lower.contains("bill")
    }

    private fun extractPersonTitle(lower: String): String {
        return when {
            lower.contains("selfie") -> "Selfie Photo"
            lower.contains("portrait") -> "Portrait Photo"
            lower.contains("friends") -> "Friends Group Photo"
            lower.contains("family") -> "Family Photo"
            else -> "Person Photograph"
        }
    }

    private fun isBlurryOrUnidentifiable(lower: String): Boolean {
        val blurryWords = listOf("blurry", "unfocused", "unreadable", "noise", "blank image", "cannot read", "unclear", "dark image", "random pixels")
        return blurryWords.any { lower.contains(it) }
    }

    private fun isMathOrPhysics(lower: String): Boolean {
        val mathWords = listOf(
            "math", "mathematics", "calculus", "algebra", "integral", "derivative", "equation",
            "solve for x", "quadratic formula", "physics problem", "velocity", "acceleration", "f = ma"
        )
        return mathWords.any { lower.contains(it) } && !lower.contains("receipt")
    }

    private fun extractMathTitle(clean: String, lower: String): String {
        return when {
            lower.contains("calculus") || lower.contains("integral") -> "Calculus Problem Sheet"
            lower.contains("algebra") || lower.contains("solve for x") -> "Algebra Equations"
            lower.contains("physics") -> "Physics Problem Set"
            else -> "Mathematics Notes"
        }
    }

    private fun isEducationalContent(lower: String): Boolean {
        val eduWords = listOf(
            "textbook", "chapter", "lesson", "grammar", "modifiers", "modifier", "definition",
            "syllabus", "lecture", "study guide", "biology notes", "chemistry", "literature"
        )
        return eduWords.any { lower.contains(it) } && !lower.contains("receipt") && !lower.contains("invoice")
    }

    private fun extractEducationalTitle(clean: String, lower: String): String {
        return when {
            lower.contains("modifier") -> "English Grammar: Modifiers"
            lower.contains("grammar") -> "English Grammar Lesson"
            lower.contains("textbook") -> "Textbook Page"
            clean.lines().firstOrNull { it.isNotBlank() } != null -> clean.lines().first { it.isNotBlank() }.take(40)
            else -> "Educational Material"
        }
    }

    private fun isNewsArticle(lower: String): Boolean {
        val newsWords = listOf(
            "breaking news", "news article", "journal", "reported by", "press release", "headline",
            "correspondent", "published on", "editorial", "newspaper", "daily news", "news screenshot"
        )
        return newsWords.any { lower.contains(it) } && !lower.contains("receipt")
    }

    private fun extractNewsTitle(clean: String, lower: String): String {
        return clean.lines().firstOrNull { it.isNotBlank() }?.take(50) ?: "News Article"
    }

    private fun isChatScreenshot(lower: String): Boolean {
        val chatWords = listOf(
            "chat screenshot", "whatsapp", "telegram", "messenger", "imessage", "conversation",
            "typing...", "online", "delivered", "read at", "yesterday at", "pm\n", "am\n"
        )
        return chatWords.any { lower.contains(it) } && !lower.contains("receipt")
    }

    private fun extractChatTitle(clean: String, lower: String): String {
        return "Chat Conversation"
    }

    private fun isProductPhoto(lower: String): Boolean {
        val productWords = listOf(
            "product photo", "product image", "sneakers", "nike", "adidas", "headphones",
            "sony", "iphone", "pixel", "samsung galaxy", "macbook", "camera lens", "watch",
            "coffee maker", "gadget", "smart watch"
        )
        val hasReceiptSigns = lower.contains("subtotal") || lower.contains("tax") || lower.contains("cashier") || lower.contains("pos terminal")
        return productWords.any { lower.contains(it) } && !hasReceiptSigns
    }

    private fun extractProductPhotoTitle(clean: String, lower: String): String {
        return when {
            lower.contains("sneakers") || lower.contains("nike") -> "Nike Sneakers"
            lower.contains("headphones") || lower.contains("sony") -> "Sony Headphones"
            lower.contains("iphone") -> "Apple iPhone"
            lower.contains("pixel") -> "Google Pixel"
            lower.contains("macbook") -> "Apple MacBook"
            lower.contains("watch") -> "Smart Watch"
            else -> "Product Photo"
        }
    }

    private fun isUtilityBill(lower: String): Boolean {
        val billWords = listOf("electricity bill", "desco", "dpdc", "gas bill", "water bill", "wasa", "internet bill", "utility statement", "total amount due", "due date:")
        val hasBillWord = billWords.any { lower.contains(it) }
        val hasAmount = lower.contains("৳") || lower.contains("$") || lower.contains("due") || lower.contains("payable")
        return hasBillWord && hasAmount && !lower.contains("receipt")
    }

    private fun extractBillTitle(clean: String, lower: String): String {
        return when {
            lower.contains("desco") -> "DESCO Electricity Bill"
            lower.contains("dpdc") -> "DPDC Electricity Bill"
            lower.contains("wasa") -> "WASA Water Bill"
            lower.contains("gas") -> "Gas Utility Bill"
            lower.contains("internet") -> "Internet Service Bill"
            else -> "Utility Bill"
        }
    }

    private fun isStrictReceipt(lower: String, clean: String): Boolean {
        // STRICT criteria for store purchase receipt:
        // Must contain explicit transaction/receipt tokens and cannot be just a random photo or note
        val receiptMarkers = listOf("receipt", "store #", "cashier", "pos terminal", "subtotal", "vat #", "sales slip", "tax invoice")
        val hasExplicitReceiptToken = receiptMarkers.any { lower.contains(it) }
        val hasPrices = clean.contains("$") || clean.contains("৳") || clean.contains("€") || clean.contains("£")
        return hasExplicitReceiptToken && hasPrices
    }

    private fun extractStrictReceiptTitle(clean: String, lower: String): String {
        val merchant = when {
            lower.contains("walmart") -> "Walmart"
            lower.contains("target") -> "Target"
            lower.contains("amazon") -> "Amazon"
            lower.contains("star tech") -> "Star Tech"
            lower.contains("apple store") -> "Apple Store"
            lower.contains("costco") -> "Costco"
            else -> null
        }
        return if (merchant != null) "$merchant Purchase Receipt" else "Purchase Receipt"
    }

    private fun isGeneralDocument(lower: String): Boolean {
        val docWords = listOf("document", "form", "certificate", "id card", "passport", "license", "agreement", "contract")
        return docWords.any { lower.contains(it) }
    }
}

// ==========================================
// SPECIALIZED ANALYZER BRANCHES
// ==========================================

object PhotographAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return AIAnalysisResult(
            contentType = ContentType.PHOTOGRAPH.name,
            actionability = Actionability.INFORMATIONAL.name,
            contentTypeConfidence = classification.confidence,
            actionabilityConfidence = 0.98f,
            extractionConfidence = 0.0f,
            title = classification.titleCandidate,
            summary = classification.visualDescription,
            description = classification.visualDescription,
            type = ItemType.NOTE.name,
            category = ItemCategory.GENERAL.name,
            action = "",
            priority = ItemPriority.LOW.name,
            confidence = classification.confidence,
            evidence = null,
            reason = "Visual photograph. No task, deadline, or financial obligation detected.",
            explanation = "Saved photo: ${classification.titleCandidate}.",
            isActionable = false,
            isUncertain = false
        )
    }
}

object EducationalAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return AIAnalysisResult(
            contentType = ContentType.EDUCATIONAL_PAGE.name,
            actionability = Actionability.INFORMATIONAL.name,
            contentTypeConfidence = classification.confidence,
            actionabilityConfidence = 0.98f,
            extractionConfidence = 0.0f,
            title = classification.titleCandidate,
            summary = "Educational material: ${classification.titleCandidate}",
            description = rawText.ifBlank { classification.visualDescription },
            type = ItemType.DOCUMENT.name,
            category = ItemCategory.EDUCATION.name,
            action = "",
            priority = ItemPriority.LOW.name,
            confidence = classification.confidence,
            evidence = null,
            reason = "Educational reference content. No personal task or deadline required.",
            explanation = "Saved educational study reference.",
            isActionable = false,
            isUncertain = false
        )
    }
}

object NewsAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return AIAnalysisResult(
            contentType = ContentType.NEWS.name,
            actionability = Actionability.INFORMATIONAL.name,
            contentTypeConfidence = classification.confidence,
            actionabilityConfidence = 0.98f,
            extractionConfidence = 0.0f,
            title = classification.titleCandidate,
            summary = "News article: ${classification.titleCandidate}",
            description = rawText.ifBlank { classification.visualDescription },
            type = ItemType.DOCUMENT.name,
            category = ItemCategory.DOCUMENTS.name,
            action = "",
            priority = ItemPriority.LOW.name,
            confidence = classification.confidence,
            evidence = null,
            reason = "News article / press publication with informational context.",
            explanation = "Saved news article reference.",
            isActionable = false,
            isUncertain = false
        )
    }
}

object ConversationAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return AIAnalysisResult(
            contentType = ContentType.CHAT_SCREENSHOT.name,
            actionability = Actionability.INFORMATIONAL.name,
            contentTypeConfidence = classification.confidence,
            actionabilityConfidence = 0.95f,
            extractionConfidence = 0.0f,
            title = classification.titleCandidate,
            summary = "Chat screenshot: ${classification.visualDescription}",
            description = rawText.ifBlank { classification.visualDescription },
            type = ItemType.NOTE.name,
            category = ItemCategory.GENERAL.name,
            action = "",
            priority = ItemPriority.LOW.name,
            confidence = classification.confidence,
            evidence = null,
            reason = "Chat messaging screenshot recorded for reference.",
            explanation = "Saved chat conversation.",
            isActionable = false,
            isUncertain = false
        )
    }
}

object ProductAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return AIAnalysisResult(
            contentType = ContentType.PRODUCT_IMAGE.name,
            actionability = Actionability.INFORMATIONAL.name,
            contentTypeConfidence = classification.confidence,
            actionabilityConfidence = 0.98f,
            extractionConfidence = 0.0f,
            title = classification.titleCandidate,
            summary = "Product image: ${classification.titleCandidate}",
            description = rawText.ifBlank { classification.visualDescription },
            type = ItemType.NOTE.name,
            category = ItemCategory.SHOPPING.name,
            product = classification.titleCandidate,
            action = "",
            priority = ItemPriority.LOW.name,
            confidence = classification.confidence,
            evidence = null,
            reason = "Product photograph without transaction receipt or pending purchase action.",
            explanation = "Saved product photo.",
            isActionable = false,
            isUncertain = false
        )
    }
}

object UnknownAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return AIAnalysisResult(
            contentType = ContentType.UNKNOWN.name,
            actionability = Actionability.INFORMATIONAL.name,
            contentTypeConfidence = 0.50f,
            actionabilityConfidence = 0.90f,
            extractionConfidence = 0.0f,
            title = "Unidentified Image",
            summary = "I couldn't identify this content or extract clear details.",
            description = rawText.ifBlank { "Unidentified image content." },
            type = ItemType.NOTE.name,
            category = ItemCategory.GENERAL.name,
            action = "",
            priority = ItemPriority.LOW.name,
            confidence = 0.50f,
            evidence = null,
            reason = "Image is unidentifiable, blurry, or lacks clear extractable content.",
            explanation = "I couldn't identify this content or read enough clear details.",
            isActionable = false,
            isUncertain = true
        )
    }
}

object DocumentAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return AIAnalysisResult(
            contentType = classification.contentType.name,
            actionability = Actionability.INFORMATIONAL.name,
            contentTypeConfidence = classification.confidence,
            actionabilityConfidence = 0.95f,
            extractionConfidence = 0.0f,
            title = classification.titleCandidate,
            summary = "Document: ${classification.titleCandidate}",
            description = rawText.ifBlank { classification.visualDescription },
            type = ItemType.DOCUMENT.name,
            category = ItemCategory.DOCUMENTS.name,
            action = "",
            priority = ItemPriority.LOW.name,
            confidence = classification.confidence,
            evidence = null,
            reason = "Document record preserved safely.",
            explanation = "Saved document.",
            isActionable = false,
            isUncertain = false
        )
    }
}

object BillAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return RuleBasedFallbackExtractor.extract(rawText.ifBlank { classification.titleCandidate })
    }
}

object ReceiptAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return RuleBasedFallbackExtractor.extract(rawText.ifBlank { classification.titleCandidate })
    }
}

object GeneralAnalyzer {
    fun analyze(classification: ImageContentClassifier.ClassificationResult, rawText: String): AIAnalysisResult {
        return RuleBasedFallbackExtractor.extract(rawText.ifBlank { classification.titleCandidate })
    }
}
