package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LifeVaultDatabase
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = LifeVaultDatabase.getDatabase(context)
            val dao = db.userItemDao()

            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()
                val upcoming = dao.getUpcomingReminders(now)
                upcoming.forEach { item ->
                    NotificationHelper.scheduleReminder(context, item)
                }
            }
        }
    }
}
