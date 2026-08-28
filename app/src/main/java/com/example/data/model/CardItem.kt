package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class CardType(val displayName: String) {
    DEBIT("Debit Card"),
    CREDIT("Credit Card"),
    PREPAID("Prepaid Card")
}

enum class CardNetwork(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    DISCOVER("Discover"),
    OTHER("Other")
}

@Entity(tableName = "cards")
data class CardItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cardName: String,
    val cardType: String = CardType.DEBIT.name,
    val network: String = CardNetwork.VISA.name,
    val last4Digits: String,
    val cardholderName: String? = null,
    val bankIssuer: String? = null,
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,
    val colorHex: String = "#1E293B",
    val colorGradientIndex: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val nickname: String get() = cardName
}
