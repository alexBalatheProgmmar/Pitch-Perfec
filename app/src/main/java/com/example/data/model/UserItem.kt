package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ContentType(val displayName: String, val iconEmoji: String) {
    DOCUMENT("Document", "📄"),
    INVOICE("Invoice", "🧾"),
    RECEIPT("Purchase Receipt", "🧾"),
    BILL("Bill", "💳"),
    FINANCIAL_STATEMENT("Financial Statement", "📊"),
    BANK_STATEMENT("Bank Statement", "🏦"),
    EDUCATIONAL_DOCUMENT("Educational Document", "📚"),
    EDUCATIONAL_PAGE("Educational Material", "📚"),
    EDUCATIONAL_CONTENT("Educational Content", "📚"),
    TEXTBOOK("Textbook", "📖"),
    RESEARCH_PAPER("Research Paper", "🔬"),
    NEWS_ARTICLE("News Article", "📰"),
    NEWS("News", "📰"),
    CONTRACT("Contract", "📄"),
    LEGAL_DOCUMENT("Legal Document", "⚖️"),
    FORM("Form", "📋"),
    CERTIFICATE("Certificate", "🏆"),
    RESUME("Resume / CV", "👤"),
    COVER_LETTER("Cover Letter", "✉️"),
    PRODUCT_MANUAL("Product Manual", "📘"),
    REPORT("Report", "📑"),
    PRESENTATION("Presentation", "📊"),
    PERSONAL_DOCUMENT("Personal Document", "🗂️"),
    LETTER("Letter", "✉️"),
    APPLICATION("Application", "📝"),
    PHOTOGRAPH("Photograph", "🖼️"),
    CHAT_SCREENSHOT("Chat Conversation", "💬"),
    PRODUCT_IMAGE("Product Image", "📱"),
    ID_DOCUMENT("ID Document", "🪪"),
    POSTER("Poster / Notice", "📢"),
    SCREENSHOT("Screenshot", "📱"),
    DIAGRAM("Diagram / Chart", "📊"),
    CHART("Chart / Graph", "📈"),
    PERSONAL_NOTE("Personal Note", "📝"),
    CONVERSATION("Conversation", "💬"),
    TASK("Task", "✅"),
    DEADLINE("Deadline", "⏰"),
    APPOINTMENT("Appointment", "🗓️"),
    EVENT("Event", "🎉"),
    PAYMENT("Payment", "💳"),
    PURCHASE("Purchase", "🛍️"),
    SUBSCRIPTION("Subscription", "🔄"),
    WARRANTY("Warranty", "🛡️"),
    RETURN("Return", "👟"),
    DELIVERY("Delivery", "📦"),
    TRAVEL("Travel", "✈️"),
    CARD("Card / Payment Method", "💳"),
    GENERAL_INFORMATION("General Information", "ℹ️"),
    OTHER("Other", "🗂️"),
    UNKNOWN("Unidentified Document", "❓")
}

enum class Actionability(val displayName: String) {
    ACTIONABLE("Actionable"),
    INFORMATIONAL("Informational"),
    UNCERTAIN("Uncertain")
}

enum class ItemType(val displayName: String, val iconEmoji: String) {
    TASK("Task", "✅"),
    DEADLINE("Deadline", "⏰"),
    APPOINTMENT("Appointment", "🗓️"),
    PAYMENT("Payment", "💳"),
    SUBSCRIPTION("Subscription", "🔄"),
    RETURN("Return", "👟"),
    WARRANTY("Warranty", "🛡️"),
    DELIVERY("Delivery", "📦"),
    EVENT("Event", "🎉"),
    REMINDER("Reminder", "🔔"),
    IMPORTANT("Important Info", "📌"),
    DOCUMENT("Document", "📄"),
    NOTE("Note", "📝")
}

enum class ItemCategory(val displayName: String, val iconEmoji: String) {
    EDUCATION("Education", "📚"),
    FINANCE("Finance", "💰"),
    SHOPPING("Shopping", "🛒"),
    HEALTH("Health", "🏥"),
    TRAVEL("Travel", "✈️"),
    WORK("Work", "💼"),
    HOME("Home", "🏠"),
    TECHNOLOGY("Technology", "📱"),
    EVENTS("Events", "🎉"),
    DOCUMENTS("Documents", "📄"),
    DELIVERY("Delivery", "📦"),
    WARRANTY("Warranty", "🔧"),
    GENERAL("General", "🗂️")
}

enum class ItemPriority {
    HIGH,
    MEDIUM,
    LOW
}

enum class ItemStatus {
    INBOX,
    ACTIVE,
    COMPLETED,
    ARCHIVED,
    DISMISSED
}

enum class ReminderTiming(val displayName: String) {
    SAME_DAY("On the day"),
    ONE_DAY_BEFORE("1 day before"),
    THREE_DAYS_BEFORE("3 days before"),
    SEVEN_DAYS_BEFORE("7 days before"),
    CUSTOM("Custom time"),
    NONE("No reminder")
}

@Entity(tableName = "user_items")
data class UserItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: String = ItemType.NOTE.name,
    val category: String = ItemCategory.GENERAL.name,
    val contentType: String = ContentType.UNKNOWN.name,
    val actionability: String = Actionability.INFORMATIONAL.name,
    val action: String = "",
    val dueDate: String? = null,           // YYYY-MM-DD
    val dueTime: String? = null,           // HH:mm
    val dueTimestamp: Long? = null,        // Unix timestamp in millis
    val amount: Double? = null,
    val amountDue: Double? = null,         // Primary payable amount
    val currency: String? = null,          // e.g., ৳, $, €, £
    val invoiceNumber: String? = null,
    val customer: String? = null,
    val issueDate: String? = null,
    val billingPeriod: String? = null,
    val subtotal: Double? = null,
    val tax: Double? = null,
    val discount: Double? = null,
    val amountPaid: Double? = null,
    val balance: Double? = null,
    val paymentStatus: String? = null,     // UNPAID, PAID, PARTIAL, OVERDUE
    val topic: String? = null,
    val subject: String? = null,
    val authors: String? = null,
    val abstractSnippet: String? = null,
    val keyFindings: String? = null,
    val keyConcepts: String? = null,
    val person: String? = null,
    val organization: String? = null,
    val location: String? = null,
    val source: String = "Text",          // Text, Screenshot, Camera, Document, Link, Voice, ShareSheet
    val originalContent: String? = null,
    val imageUri: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val pageCount: Int? = null,
    val sourcePageEvidence: String? = null,
    val ocrConfidence: Float? = null,
    val isScannedPdf: Boolean = false,
    val contentHash: String? = null,
    val priority: String = ItemPriority.MEDIUM.name,
    val status: String = ItemStatus.ACTIVE.name,
    val confidence: Float = 0.85f,         // 0.0 to 1.0
    val contentTypeConfidence: Float = 0.85f,
    val actionabilityConfidence: Float = 0.85f,
    val extractionConfidence: Float = 0.0f,
    val evidence: String? = null,
    val reason: String? = null,
    val explanation: String? = null,
    val reminderTiming: String = ReminderTiming.ONE_DAY_BEFORE.name,
    val reminderTimestamp: Long? = null,
    val returnWindowDays: Int? = null,
    val warrantyExpiryDate: String? = null,
    val subscriptionInterval: String? = null, // MONTHLY, YEARLY
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class AIAnalysisResult(
    val contentType: String = ContentType.UNKNOWN.name,
    val actionability: String = Actionability.INFORMATIONAL.name,
    val contentTypeConfidence: Float = 0.90f,
    val actionabilityConfidence: Float = 0.90f,
    val extractionConfidence: Float = 0.0f,
    val title: String,
    val summary: String = "",
    val description: String = "",
    val type: String = ItemType.NOTE.name,
    val category: String = ItemCategory.GENERAL.name,
    val action: String = "",
    val date: String? = null,             // YYYY-MM-DD or readable
    val time: String? = null,             // HH:mm or readable
    val amount: Double? = null,
    val amountDue: Double? = null,
    val currency: String? = null,
    val invoiceNumber: String? = null,
    val customer: String? = null,
    val issueDate: String? = null,
    val billingPeriod: String? = null,
    val subtotal: Double? = null,
    val tax: Double? = null,
    val discount: Double? = null,
    val amountPaid: Double? = null,
    val balance: Double? = null,
    val paymentStatus: String? = null,    // UNPAID, PAID, PARTIAL
    val topic: String? = null,
    val subject: String? = null,
    val authors: String? = null,
    val abstractSnippet: String? = null,
    val keyFindings: String? = null,
    val keyConcepts: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val pageCount: Int? = null,
    val sourcePageEvidence: String? = null,
    val ocrConfidence: Float? = null,
    val isScannedPdf: Boolean = false,
    val contentHash: String? = null,
    val person: String? = null,
    val organization: String? = null,
    val location: String? = null,
    val merchant: String? = null,
    val product: String? = null,
    val subscription: String? = null,
    val appointment: String? = null,
    val billType: String? = null,
    val billProvider: String? = null,
    val priority: String = ItemPriority.MEDIUM.name,
    val confidence: Float = 0.85f,
    val evidence: String? = null,
    val reason: String? = null,
    val explanation: String? = null,
    val returnWindowDays: Int? = null,
    val warrantyExpiryDate: String? = null,
    val subscriptionInterval: String? = null,
    val isActionable: Boolean = false,
    val isUncertain: Boolean = false,
    val validationPassed: Boolean = true
)

