package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ContentType(val displayName: String, val iconEmoji: String) {
    GENERAL_INFORMATION("General Information", "ℹ️"),
    NEWS_ARTICLE("News Article", "📰"),
    EDUCATIONAL_CONTENT("Educational Content", "📚"),
    PERSONAL_NOTE("Personal Note", "📝"),
    CONVERSATION("Conversation", "💬"),
    TASK("Task", "✅"),
    DEADLINE("Deadline", "⏰"),
    APPOINTMENT("Appointment", "🗓️"),
    EVENT("Event", "🎉"),
    PAYMENT("Payment", "💳"),
    BILL("Bill", "🧾"),
    RECEIPT("Receipt", "🧾"),
    PURCHASE("Purchase", "🛍️"),
    SUBSCRIPTION("Subscription", "🔄"),
    WARRANTY("Warranty", "🛡️"),
    RETURN("Return", "👟"),
    DELIVERY("Delivery", "📦"),
    TRAVEL("Travel", "✈️"),
    DOCUMENT("Document", "📄"),
    CARD("Card / Payment Method", "💳"),
    OTHER("Other", "🗂️")
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
    val type: String = ItemType.TASK.name,
    val category: String = ItemCategory.GENERAL.name,
    val contentType: String = ContentType.GENERAL_INFORMATION.name,
    val actionability: String = Actionability.INFORMATIONAL.name,
    val action: String = "",
    val dueDate: String? = null,           // YYYY-MM-DD
    val dueTime: String? = null,           // HH:mm
    val dueTimestamp: Long? = null,        // Unix timestamp in millis
    val amount: Double? = null,
    val currency: String? = null,          // e.g., ৳, $, €, £
    val person: String? = null,
    val organization: String? = null,
    val location: String? = null,
    val source: String = "Text",          // Text, Screenshot, Camera, Document, Link, Voice, ShareSheet
    val originalContent: String? = null,
    val imageUri: String? = null,
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
    val contentType: String = ContentType.GENERAL_INFORMATION.name,
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
    val currency: String? = null,
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

