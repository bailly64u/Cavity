package com.louis.app.cavity.ui.notifications

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.louis.app.cavity.domain.repository.TastingRepository
import com.louis.app.cavity.util.toInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TastingActionDoneReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASTING_ACTION_ID = "com.louis.app.cavity.EXTRA_TASTING_ACTION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val repository = TastingRepository.getInstance(context.applicationContext as Application)
        val tastingActionId = intent.getLongExtra(EXTRA_TASTING_ACTION_ID, -1)

        if (tastingActionId == -1L) {
            return
        }

        CoroutineScope(SupervisorJob()).launch(IO) {
            val tastingAction = repository.getTastingActionById(tastingActionId)
            tastingAction.done = true.toInt()
            repository.updateTastingAction(tastingAction)

            NotificationBuilder.cancelNotification(context, tastingActionId.toInt())
        }
    }
}
