package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Bill
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE status != 'PAID' AND status != 'CANCELLED' ORDER BY createdAt DESC")
    fun getUnpaidBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE status = 'PAID' ORDER BY paymentDate DESC, createdAt DESC")
    fun getPaidBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    fun getBillById(id: String): Flow<Bill?>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillByIdDirect(id: String): Bill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}
