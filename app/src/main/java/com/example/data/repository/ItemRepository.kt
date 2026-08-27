package com.example.data.repository

import android.content.Context
import com.example.data.local.UserItemDao
import com.example.data.model.ItemStatus
import com.example.data.model.UserItem
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class ItemRepository(
    private val context: Context,
    private val dao: UserItemDao
) {

    val allItems: Flow<List<UserItem>> = dao.getAllItems()
    val activeItems: Flow<List<UserItem>> = dao.getActiveItems()
    val inboxItems: Flow<List<UserItem>> = dao.getInboxItems()
    val completedItems: Flow<List<UserItem>> = dao.getCompletedItems()
    val archivedItems: Flow<List<UserItem>> = dao.getArchivedItems()

    fun getItemById(id: String): Flow<UserItem?> = dao.getItemByIdFlow(id)

    suspend fun getItemDirect(id: String): UserItem? = dao.getItemById(id)

    fun searchItems(query: String): Flow<List<UserItem>> = dao.searchItems(query)

    fun getItemsForDate(date: String): Flow<List<UserItem>> = dao.getItemsForDate(date)

    fun getItemsByCategory(category: String): Flow<List<UserItem>> = dao.getItemsByCategory(category)

    suspend fun checkForDuplicate(title: String, amount: Double?, dueDate: String?): UserItem? {
        return dao.findDuplicate(title, amount, dueDate)
    }

    suspend fun insertItem(item: UserItem) {
        dao.insertItem(item)
        if (item.status == ItemStatus.ACTIVE.name && item.reminderTimestamp != null) {
            NotificationHelper.scheduleReminder(context, item)
        }
    }

    suspend fun updateItem(item: UserItem) {
        dao.updateItem(item)
        if (item.status == ItemStatus.ACTIVE.name && item.reminderTimestamp != null) {
            NotificationHelper.scheduleReminder(context, item)
        } else {
            NotificationHelper.cancelReminder(context, item.id)
        }
    }

    suspend fun markCompleted(item: UserItem) {
        val updated = item.copy(
            status = ItemStatus.COMPLETED.name,
            completedAt = System.currentTimeMillis()
        )
        dao.updateItem(updated)
        NotificationHelper.cancelReminder(context, item.id)
    }

    suspend fun markIncomplete(item: UserItem) {
        val updated = item.copy(
            status = ItemStatus.ACTIVE.name,
            completedAt = null
        )
        dao.updateItem(updated)
        if (updated.reminderTimestamp != null) {
            NotificationHelper.scheduleReminder(context, updated)
        }
    }

    suspend fun snoozeItem(item: UserItem, hours: Int) {
        val newReminder = System.currentTimeMillis() + (hours * 3600 * 1000L)
        val updated = item.copy(reminderTimestamp = newReminder)
        dao.updateItem(updated)
        NotificationHelper.scheduleReminder(context, updated)
    }

    suspend fun archiveItem(item: UserItem) {
        val updated = item.copy(status = ItemStatus.ARCHIVED.name)
        dao.updateItem(updated)
        NotificationHelper.cancelReminder(context, item.id)
    }

    suspend fun dismissItem(item: UserItem) {
        val updated = item.copy(status = ItemStatus.DISMISSED.name)
        dao.updateItem(updated)
        NotificationHelper.cancelReminder(context, item.id)
    }

    suspend fun deleteItem(item: UserItem) {
        NotificationHelper.cancelReminder(context, item.id)
        dao.deleteItem(item)
    }

    suspend fun deleteAllData() {
        dao.deleteAllItems()
    }

    suspend fun exportDataAsJson(items: List<UserItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("description", item.description)
                put("type", item.type)
                put("category", item.category)
                put("action", item.action)
                put("dueDate", item.dueDate)
                put("dueTime", item.dueTime)
                put("amount", item.amount)
                put("currency", item.currency)
                put("person", item.person)
                put("organization", item.organization)
                put("location", item.location)
                put("priority", item.priority)
                put("status", item.status)
                put("confidence", item.confidence)
                put("explanation", item.explanation)
                put("warrantyExpiryDate", item.warrantyExpiryDate)
                put("returnWindowDays", item.returnWindowDays)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        return array.toString(2)
    }
}
