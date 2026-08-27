package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.UserItem
import kotlinx.coroutines.flow.Flow

@Dao
interface UserItemDao {
    @Query("SELECT * FROM user_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE status = 'ACTIVE' ORDER BY dueTimestamp ASC, createdAt DESC")
    fun getActiveItems(): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE status = 'INBOX' ORDER BY createdAt DESC")
    fun getInboxItems(): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedItems(): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE status = 'ARCHIVED' ORDER BY createdAt DESC")
    fun getArchivedItems(): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): UserItem?

    @Query("SELECT * FROM user_items WHERE id = :id LIMIT 1")
    fun getItemByIdFlow(id: String): Flow<UserItem?>

    @Query("SELECT * FROM user_items WHERE category = :category AND status != 'DISMISSED' ORDER BY createdAt DESC")
    fun getItemsByCategory(category: String): Flow<List<UserItem>>

    @Query("""
        SELECT * FROM user_items 
        WHERE status != 'DISMISSED' 
        AND (title LIKE '%' || :query || '%' 
             OR description LIKE '%' || :query || '%' 
             OR organization LIKE '%' || :query || '%' 
             OR person LIKE '%' || :query || '%' 
             OR category LIKE '%' || :query || '%'
             OR originalContent LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchItems(query: String): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE dueDate = :date AND status != 'DISMISSED' ORDER BY dueTimestamp ASC")
    fun getItemsForDate(date: String): Flow<List<UserItem>>

    @Query("SELECT * FROM user_items WHERE status = 'ACTIVE' AND reminderTimestamp IS NOT NULL AND reminderTimestamp > :now")
    suspend fun getUpcomingReminders(now: Long): List<UserItem>

    @Query("SELECT * FROM user_items WHERE (title = :title OR (amount = :amount AND dueDate = :dueDate)) AND status != 'DISMISSED' LIMIT 1")
    suspend fun findDuplicate(title: String, amount: Double?, dueDate: String?): UserItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: UserItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<UserItem>)

    @Update
    suspend fun updateItem(item: UserItem)

    @Delete
    suspend fun deleteItem(item: UserItem)

    @Query("DELETE FROM user_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM user_items")
    suspend fun deleteAllItems()
}
