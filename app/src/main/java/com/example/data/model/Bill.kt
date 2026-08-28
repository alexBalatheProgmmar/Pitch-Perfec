package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class BillType(val displayName: String, val iconEmoji: String) {
    GAS("Gas Bill", "🔥"),
    ELECTRICITY("Electricity Bill", "⚡"),
    WATER("Water Bill", "💧"),
    INTERNET("Internet Bill", "🌐"),
    MOBILE("Mobile Bill", "📱"),
    TELEPHONE("Telephone Bill", "☎️"),
    TV_CABLE("TV / Cable Bill", "📺"),
    RENT("Rent", "🏠"),
    TUITION("Tuition / Education Fee", "🎓"),
    INSURANCE("Insurance", "🛡️"),
    CREDIT_CARD("Credit Card Bill", "💳"),
    LOAN("Loan Payment", "🏦"),
    GOVERNMENT("Government Fee", "🏛️"),
    SUBSCRIPTION("Subscription Payment", "🔄"),
    OTHER("Other Bill", "🧾")
}

enum class BillStatus(val displayName: String) {
    UNPAID("Unpaid"),
    DUE_SOON("Due Soon"),
    OVERDUE("Overdue"),
    PAID("Paid"),
    PARTIALLY_PAID("Partially Paid"),
    CANCELLED("Cancelled")
}

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billType: String = BillType.ELECTRICITY.name,
    val customTypeName: String? = null,
    val provider: String? = null,
    val amountDue: Double,
    val currency: String = "BDT",
    val dueDate: String? = null,
    val dueTimestamp: Long? = null,
    val billingPeriod: String? = null,
    val billNumber: String? = null,
    val accountNumber: String? = null,
    val status: String = BillStatus.UNPAID.name,
    val paymentDate: String? = null,
    val source: String = "Manual",
    val notes: String? = null,
    val cardId: String? = null,
    val reminderTiming: String = "ONE_DAY_BEFORE",
    val reminderTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
