package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LifeVaultDatabase
import com.example.data.model.ItemStatus
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE = "com.example.lifevault.ACTION_DONE"
        const val ACTION_SNOOZE = "com.example.lifevault.ACTION_SNOOZE"
        const val EXTRA_ITEM_ID = "EXTRA_ITEM_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(itemId.hashCode())

        val db = LifeVaultDatabase.getDatabase(context)
        val dao = db.userItemDao()

        when (intent.action) {
            ACTION_DONE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val item = dao.getItemById(itemId)
                    if (item != null) {
                        dao.updateItem(
                            item.copy(
                                status = ItemStatus.COMPLETED.name,
                                completedAt = System.currentTimeMillis()
                            )
                        )
                        NotificationHelper.cancelReminder(context, itemId)
                    }
                }
            }
            ACTION_SNOOZE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val item = dao.getItemById(itemId)
                    if (item != null) {
                        val newReminder = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // +24 hours
                        val updated = item.copy(reminderTimestamp = newReminder)
                        dao.updateItem(updated)
                        NotificationHelper.scheduleReminder(context, updated)
                    }
                }
            }
        }
    }
}
