package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentRecordDao {

    @Query("SELECT * FROM payment_records ORDER BY paymentDateMillis DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payment_records WHERE billId = :billId ORDER BY paymentDateMillis DESC")
    fun getPaymentsForBill(billId: String): Flow<List<PaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(record: PaymentRecord)

    @Delete
    suspend fun deletePayment(record: PaymentRecord)

    @Query("DELETE FROM payment_records")
    suspend fun deleteAllPayments()
}
