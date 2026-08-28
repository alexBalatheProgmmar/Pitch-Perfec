package com.example.data.repository

import android.content.Context
import com.example.data.local.BillDao
import com.example.data.local.CardDao
import com.example.data.local.PaymentRecordDao
import com.example.data.model.Bill
import com.example.data.model.BillStatus
import com.example.data.model.CardItem
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BillRepository(
    private val context: Context,
    private val billDao: BillDao,
    private val cardDao: CardDao,
    private val paymentRecordDao: PaymentRecordDao
) {

    val allBills: Flow<List<Bill>> = billDao.getAllBills()
    val unpaidBills: Flow<List<Bill>> = billDao.getUnpaidBills()
    val paidBills: Flow<List<Bill>> = billDao.getPaidBills()
    val allCards: Flow<List<CardItem>> = cardDao.getAllCards()
    val allPayments: Flow<List<PaymentRecord>> = paymentRecordDao.getAllPayments()

    fun getBillById(id: String): Flow<Bill?> = billDao.getBillById(id)

    suspend fun getBillByIdDirect(id: String): Bill? = billDao.getBillByIdDirect(id)

    fun getCardById(id: String): Flow<CardItem?> = cardDao.getCardById(id)

    suspend fun getCardByIdDirect(id: String): CardItem? = cardDao.getCardByIdDirect(id)

    fun getPaymentsForBill(billId: String): Flow<List<PaymentRecord>> =
        paymentRecordDao.getPaymentsForBill(billId)

    suspend fun insertBill(bill: Bill) {
        billDao.insertBill(bill)
    }

    suspend fun updateBill(bill: Bill) {
        billDao.updateBill(bill)
    }

    suspend fun deleteBill(bill: Bill) {
        billDao.deleteBill(bill)
    }

    suspend fun markBillAsPaid(
        bill: Bill,
        paymentMethod: String? = null,
        cardId: String? = null,
        notes: String? = null
    ) {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        val todayStr = dateFormat.format(Date())

        val updatedBill = bill.copy(
            status = BillStatus.PAID.name,
            paymentDate = todayStr,
            cardId = cardId ?: bill.cardId
        )
        billDao.updateBill(updatedBill)

        val paymentRecord = PaymentRecord(
            id = UUID.randomUUID().toString(),
            billId = bill.id,
            billTitle = bill.provider?.let { "$it ${bill.billType}" } ?: bill.billType,
            billType = bill.billType,
            amountPaid = bill.amountDue,
            currency = bill.currency,
            paymentDate = todayStr,
            paymentDateMillis = System.currentTimeMillis(),
            paymentMethod = paymentMethod ?: "Cash / Manual",
            cardId = cardId ?: bill.cardId,
            notes = notes ?: bill.notes
        )
        paymentRecordDao.insertPayment(paymentRecord)
    }

    suspend fun insertCard(card: CardItem) {
        cardDao.insertCard(card)
    }

    suspend fun updateCard(card: CardItem) {
        cardDao.updateCard(card)
    }

    suspend fun deleteCard(card: CardItem) {
        cardDao.deleteCard(card)
    }

    suspend fun deleteAll() {
        billDao.deleteAllBills()
        cardDao.deleteAllCards()
        paymentRecordDao.deleteAllPayments()
    }
}
