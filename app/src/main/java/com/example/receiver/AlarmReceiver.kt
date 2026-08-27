package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ITEM_ID = "EXTRA_ITEM_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_DESC = "EXTRA_DESC"
        const val EXTRA_TYPE = "EXTRA_TYPE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "LifeVault Reminder"
        val desc = intent.getStringExtra(EXTRA_DESC) ?: ""
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "TASK"

        NotificationHelper.showReminderNotification(
            context = context,
            itemId = itemId,
            title = title,
            description = desc,
            type = type
        )
    }
}
