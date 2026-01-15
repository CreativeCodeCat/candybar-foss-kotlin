package candybar.lib.services

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.widget.RemoteViews
import candybar.lib.R

open class CandyBarWidgetService : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        val act = intent.action
        val flags: Int

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == act) {
            val clockView = RemoteViews(context.packageName, R.layout.analog_clock)

            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }

            clockView.setOnClickPendingIntent(
                R.id.analog_clock,
                PendingIntent.getActivity(context, 0, clockIntent, flags)
            )

            AppWidgetManager.getInstance(context).updateAppWidget(
                intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS),
                clockView
            )
        }
    }
}
