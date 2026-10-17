package com.vaibhav.daystracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val widgetWhite = ColorProvider(Color(0xFFF5F5F6))
private val widgetSoft = ColorProvider(Color(0xFFC9CBCF))
private val widgetMuted = ColorProvider(Color(0xFF8C8E93))

private fun plural(n: Int, unit: String) = "$n ${if (n == 1) unit else unit + "s"}"

class RelationshipWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val preferences = context.getSharedPreferences(
            "relationship_tracker",
            Context.MODE_PRIVATE
        )

        val today = LocalDate.now()

        // A start date in the future would give negative numbers, so clamp it to today.
        val startDate = preferences.getString("start_date", null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { if (it.isAfter(today)) today else it }
            ?: today

        val days = ChronoUnit.DAYS.between(startDate, today)
        val period = Period.between(startDate, today)
        val summary = "${plural(period.years, "year")} · " +
                "${plural(period.months, "month")} · " +
                "${plural(period.days, "day")}"

        // Opens the app through the system launcher, so no class name is needed here.
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        provideContent {
            val rootModifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .background(ImageProvider(R.drawable.widget_bg))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .let { base ->
                    if (launchIntent != null) {
                        base.clickable(actionStartActivity(launchIntent))
                    } else {
                        base
                    }
                }

            GlanceTheme {
                Column(
                    modifier = rootModifier,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "TOGETHER FOR",
                        style = TextStyle(
                            color = widgetMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Row(verticalAlignment = Alignment.Vertical.Bottom) {
                        Text(
                            text = days.toString(),
                            style = TextStyle(
                                color = widgetWhite,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = if (days == 1L) "day" else "days",
                            style = TextStyle(
                                color = widgetMuted,
                                fontSize = 18.sp
                            )
                        )
                    }

                    Text(
                        text = summary,
                        style = TextStyle(
                            color = widgetSoft,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(GlanceModifier.height(10.dp))

                    // HH : MM : SS are TextClocks, so the system keeps them live.
                    AndroidRemoteViews(
                        remoteViews = RemoteViews(
                            context.packageName,
                            R.layout.widget_clock_row
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

class RelationshipWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = RelationshipWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMidnightRefresh(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleMidnightRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_MIDNIGHT) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    RelationshipWidget().updateAll(context)
                } finally {
                    pending.finish()
                }
            }
            scheduleMidnightRefresh(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java)
            .cancel(refreshIntent(context))
    }

    /** The day count and the year/month/day line only change at midnight. */
    private fun scheduleMidnightRefresh(context: Context) {
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + 1_000

        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, refreshIntent(context))
    }

    private fun refreshIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RelationshipWidgetReceiver::class.java)
                .setAction(ACTION_MIDNIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_MIDNIGHT = "com.vaibhav.daystracker.MIDNIGHT"
    }
}