package com.vaibhav.daystracker

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

private val Midnight = Color(0xFF0A0A0B)
private val CardTop = Color(0xFF1F2023)
private val CardBottom = Color(0xFF121214)
private val MainText = Color(0xFFF2F2F3)
private val SecondaryText = Color(0xFFB4B6BA)
private val MutedText = Color(0xFF8C8E93)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                RelationshipScreen()
            }
        }
    }
}

@Composable
fun RelationshipScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val preferences = remember {
        context.getSharedPreferences("relationship_tracker", Context.MODE_PRIVATE)
    }

    var startDate by remember {
        mutableStateOf(
            preferences.getString("start_date", null)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()
        )
    }

    var hasChosenDate by remember {
        mutableStateOf(preferences.contains("start_date"))
    }

    var currentTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    // Tick exactly on each second so the flip lands on the second.
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = now
            delay(1000 - now % 1000)
        }
    }

    val startInstant = startDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()

    val elapsedSeconds = (
            Instant.ofEpochMilli(currentTime).epochSecond -
                    startInstant.epochSecond
            ).coerceAtLeast(0)

    val days = elapsedSeconds / 86400
    val hours = (elapsedSeconds % 86400) / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60

    val period = Period.between(startDate, LocalDate.now())
    val periodText = "${period.years} years · ${period.months} months · ${period.days} days"

    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    val formattedDate = startDate.format(dateFormatter)

    fun chooseDate() {
        val calendar = Calendar.getInstance().apply {
            set(startDate.year, startDate.monthValue - 1, startDate.dayOfMonth)
        }

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)

                if (!selectedDate.isAfter(LocalDate.now())) {
                    startDate = selectedDate
                    hasChosenDate = true

                    preferences.edit()
                        .putString("start_date", selectedDate.toString())
                        .apply()

                    // Make the home-screen widget pick up the new date.
                    scope.launch { RelationshipWidget().updateAll(context) }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            setTitle("When did your relationship start?")
        }.show()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Midnight
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF202125),
                            Midnight
                        ),
                        radius = 1200f
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(CardTop, CardBottom)
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TOGETHER FOR",
                    color = MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = days.toString(),
                    color = MainText,
                    fontSize = 76.sp,
                    lineHeight = 82.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-2).sp
                )

                Text(
                    text = if (days == 1L) "day" else "days",
                    color = SecondaryText,
                    fontSize = 21.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = periodText,
                    color = SecondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(24.dp))

                FlipClock(hours, minutes, seconds)

                Spacer(Modifier.height(24.dp))

                Text(
                    text = if (hasChosenDate) {
                        "SINCE $formattedDate".uppercase()
                    } else {
                        "CHOOSE YOUR ANNIVERSARY DATE"
                    },
                    color = MutedText,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { chooseDate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34363A),
                        contentColor = MainText
                    )
                ) {
                    Text(
                        text = if (hasChosenDate) {
                            "Change start date"
                        } else {
                            "Set relationship date"
                        }
                    )
                }
            }
        }
    }
}