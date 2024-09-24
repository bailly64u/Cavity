package com.louis.app.cavity.ui.notifications

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.louis.app.cavity.domain.repository.TastingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val repository =
                TastingRepository.getInstance(context.applicationContext as Application)

            CoroutineScope(SupervisorJob()).launch(IO) {
                repository.getAllTastingsNotLive().forEach {
                    TastingAlarmScheduler.scheduleTastingAlarm(context, it)
                }
            }
        }
    }
}
