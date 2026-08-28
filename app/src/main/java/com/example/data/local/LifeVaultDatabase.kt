package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.UserItem

@Database(entities = [UserItem::class], version = 2, exportSchema = false)
abstract class LifeVaultDatabase : RoomDatabase() {
    abstract fun userItemDao(): UserItemDao

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
