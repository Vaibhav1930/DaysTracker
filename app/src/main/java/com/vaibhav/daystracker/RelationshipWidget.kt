package com.vaibhav.daystracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val widgetWhite = ColorProvider(Color(0xFFF5F5F6))
private val widgetMuted = ColorProvider(Color(0xFF8C8E93))

class RelationshipWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val preferences = context.getSharedPreferences(
            "relationship_tracker",
            Context.MODE_PRIVATE
        )

        val startDate = preferences.getString("start_date", null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()

        val today = LocalDate.now()
        val period = Period.between(startDate, today)
        val months = period.toTotalMonths()
        val remainingDays = period.days

        val startInstant = startDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val elapsedSeconds = ChronoUnit.SECONDS
            .between(startInstant, Instant.now())
            .coerceAtLeast(0)

        val days = elapsedSeconds / 86400
        val secondsIntoDay = elapsedSeconds % 86400

        // Live clock: it starts at "time into today" and keeps counting by itself.
        val clock = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
            setChronometer(
                R.id.chrono,
                SystemClock.elapsedRealtime() - secondsIntoDay * 1000,
                null,
                true
            )
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .background(ImageProvider(R.drawable.widget_bg))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java)
                            )
                        ),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "TOGETHER FOR",
                        style = TextStyle(
                            color = widgetMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(GlanceModifier.height(4.dp))

                    Row(verticalAlignment = Alignment.Vertical.Bottom) {
                        Text(
                            text = days.toString(),
                            style = TextStyle(
                                color = widgetWhite,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            text = if (days == 1L) "day" else "days",
                            style = TextStyle(
                                color = widgetMuted,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(GlanceModifier.height(2.dp))

                    Text(
                        text = "$months months · $remainingDays days",
                        style = TextStyle(
                            color = widgetMuted,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(GlanceModifier.height(8.dp))

                    AndroidRemoteViews(
                        remoteViews = clock,
                        modifier = GlanceModifier.fillMaxWidth()
                    )




                    Spacer(GlanceModifier.height(6.dp))

                    Text(
                        text = "SINCE ${
                            startDate.dayOfMonth.toString().padStart(2, '0')
                        } ${startDate.month.name.take(3)} ${startDate.year}",
                        style = TextStyle(
                            color = widgetMuted,
                            fontSize = 9.sp
                        )
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
        scheduleNextTick(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleNextTick(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TICK) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    RelationshipWidget().updateAll(context)
                } finally {
                    pending.finish()
                }
            }
            scheduleNextTick(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java)
            .cancel(tickIntent(context))
    }

    private fun scheduleNextTick(context: Context) {
        val nextMinute = (System.currentTimeMillis() / 60_000 + 1) * 60_000
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, tickIntent(context))
    }

    private fun tickIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RelationshipWidgetReceiver::class.java)
                .setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_TICK = "com.vaibhav.daystracker.TICK"
    }
}