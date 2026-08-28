package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Bill
import com.example.data.model.CardItem
import com.example.data.model.PaymentRecord
import com.example.data.model.UserItem

@Database(
    entities = [
        UserItem::class,
        Bill::class,
        CardItem::class,
        PaymentRecord::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LifeVaultDatabase : RoomDatabase() {
    abstract fun userItemDao(): UserItemDao
    abstract fun billDao(): BillDao
    abstract fun cardDao(): CardDao
    abstract fun paymentRecordDao(): PaymentRecordDao

    companion object {
        @Volatile
        private var INSTANCE: LifeVaultDatabase? = null

        fun getDatabase(context: Context): LifeVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeVaultDatabase::class.java,
                    "lifevault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
