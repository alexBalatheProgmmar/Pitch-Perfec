package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<CardItem>>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getCardById(id: String): Flow<CardItem?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardByIdDirect(id: String): CardItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardItem)

    @Update
    suspend fun updateCard(card: CardItem)

    @Delete
    suspend fun deleteCard(card: CardItem)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
}
