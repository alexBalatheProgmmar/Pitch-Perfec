package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "payment_records")
data class PaymentRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billId: String,
    val billTitle: String,
    val billType: String = BillType.OTHER.name,
    val amountPaid: Double,
    val currency: String = "BDT",
    val paymentDate: String,
    val paymentDateMillis: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Cash / Manual",
    val cardId: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val paidAt: String get() = paymentDate
}
