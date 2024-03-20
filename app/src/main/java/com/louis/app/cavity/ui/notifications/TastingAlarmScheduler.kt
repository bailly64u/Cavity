package com.louis.app.cavity.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.louis.app.cavity.model.Tasting
import com.louis.app.cavity.util.DateFormatter
import java.util.*

object TastingAlarmScheduler {
    private const val NOTIFICATION_HOUR_MORNING = 9
    private const val NOTIFICATION_HOUR_AFTERNOON = 16

    fun scheduleTastingAlarm(context: Context, tasting: Tasting) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val alarmIntent = getTastingAlarmIntent(context, tasting)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = DateFormatter.roundToDay(tasting.date)
            set(
                Calendar.HOUR_OF_DAY,
                if (tasting.isMidday) NOTIFICATION_HOUR_MORNING else NOTIFICATION_HOUR_AFTERNOON
            )
        }

        alarmMgr?.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
    }

    fun cancelTastingAlarm(context: Context, tasting: Tasting) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val alarmIntent = getTastingAlarmIntent(context, tasting)
        alarmMgr?.cancel(alarmIntent)
    }

    fun setIsBootCompletedReceiverEnabled(context: Context, isEnabled: Boolean) {
        val receiver = ComponentName(context, BootCompletedReceiver::class.java)
        val state =
            if (isEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        context.packageManager.setComponentEnabledSetting(
            receiver,
            state,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun getTastingAlarmIntent(context: Context, tasting: Tasting): PendingIntent {
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        return Intent(context, TastingReceiver::class.java).let { intent ->
            intent.putExtra(TastingReceiver.EXTRA_TASTING_ID, tasting.id)
            PendingIntent.getBroadcast(context, tasting.id.hashCode(), intent, flags)
        }
    }
}
