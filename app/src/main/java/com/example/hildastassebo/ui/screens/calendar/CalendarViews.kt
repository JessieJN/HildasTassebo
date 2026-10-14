package com.example.hildastassebo.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


import com.example.hildastassebo.data.model.ShiftBooking
import com.example.hildastassebo.data.model.ShiftKey
import com.example.hildastassebo.data.model.ShiftType

private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)
private val MissingRed = Color(0xFFE66A68)
private val NeedsMoreOrange = Color(0xFFE9A34A)
private val Swedish = Locale.forLanguageTag("sv-SE")


// Visning av vecka, månad, listfilter och statusmarkeringar.
internal enum class CalendarMode {
    WEEK, MONTH, LIST
}

internal enum class ShiftListFilter(val label: String) {
    ALL("Alla pass"), AVAILABLE("Lediga pass"), MINE("Mina pass")
}

@Composable
internal fun CalendarModeSelector(
    selected: CalendarMode,
    onSelect: (CalendarMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf(
            CalendarMode.WEEK to "Vecka",
            CalendarMode.MONTH to "Månad",
            CalendarMode.LIST to "Lista"
        ).forEach { (mode, label) ->
            val active = selected == mode

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (active) Sage else Color(0xFFF3F6F4),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (active) Color.White else Ink,
                    fontWeight = if (active) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
                )
            }
        }
    }
}



// ============================================================
// VECKOVY: DATUM OCH FM/EM-PRICKAR
// ============================================================
@Composable
internal fun WeekCalendar(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    bookings: Map<ShiftKey, List<ShiftBooking>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val days = (0..6).map {
        weekStart.plusDays(it.toLong())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // FM och EM till vänster om kalendern
        Column(
            modifier = Modifier.width(30.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Samma höjd som veckodag + datum
            Spacer(modifier = Modifier.height(65.dp))

            Box(
                modifier = Modifier.height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "FM",
                    fontSize = 12.sp,
                    color = Ink
                )
            }

            Box(
                modifier = Modifier.height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "EM",
                    fontSize = 12.sp,
                    color = Ink
                )
            }
        }

        // Veckans sju dagar
        days.forEach { date ->
            val isSelected = date == selectedDate

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) PaleSage
                        else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.format(
                        DateTimeFormatter.ofPattern("EEE", Swedish)
                    ).replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp,
                    color = Muted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = Ink
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Förmiddagens färgprick
                Box(
                    modifier = Modifier.height(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StatusDot(
                        bookings = bookings[
                            ShiftKey(date, ShiftType.MORNING)
                        ].orEmpty()
                    )
                }

                // Eftermiddagens färgprick
                Box(
                    modifier = Modifier.height(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StatusDot(
                        bookings = bookings[
                            ShiftKey(date, ShiftType.AFTERNOON)
                        ].orEmpty()
                    )
                }
            }
        }
    }
}



// ============================================================
// MÅNADSVY: DATUM OCH STATUSPRICKAR
// ============================================================
@Composable
internal fun MonthCalendar(
    selectedDate: LocalDate,
    bookings: Map<ShiftKey, List<ShiftBooking>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = selectedDate.withDayOfMonth(1)
    val offset = firstDay.dayOfWeek.value - 1
    val totalCells = ((offset + firstDay.lengthOfMonth() + 6) / 7) * 7

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row {
            listOf("Mån", "Tis", "Ons", "Tor", "Fre", "Lör", "Sön")
                .forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
        }

        repeat(totalCells / 7) { rowIndex ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(7) { columnIndex ->
                    val index = rowIndex * 7 + columnIndex
                    val date = firstDay.plusDays(
                        (index - offset).toLong()
                    )

                    val inMonth =
                        date.month == firstDay.month &&
                                date.year == firstDay.year

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(65.dp)
                            .background(
                                if (date == selectedDate) {
                                    PaleSage
                                } else {
                                    Color.White
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .then(
                                if (inMonth) {
                                    Modifier.clickable {
                                        onDateSelected(date)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontWeight = if (date == selectedDate) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            color = if (inMonth) Ink else FieldBorder,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(7.dp))

                        if (inMonth) {
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {
                                ShiftType.entries.forEach { type ->
                                    StatusDot(
                                        bookings = bookings[
                                            ShiftKey(date, type)
                                        ].orEmpty(),
                                        size = 7
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// STATUSPRICKAR: RÖD / ORANGE / GRÖN
// ============================================================
@Composable
internal fun StatusDot(
    bookings: List<ShiftBooking>,
    size: Int = 11
) {
    val color = when {
        bookings.isEmpty() -> MissingRed
        bookings.any { it.wantsToWorkAlone } -> DarkSage
        else -> NeedsMoreOrange
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, CircleShape)
    )
}

// ============================================================
// FÖRKLARING AV STATUSFÄRGERNA
// ============================================================
@Composable
internal fun ShiftLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF3F6F4),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(MissingRed, "Ingen bokad")
        LegendItem(NeedsMoreOrange, "Fler önskas")
        LegendItem(DarkSage, "Arbetar ensam")
    }
}

@Composable
internal fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .background(color, CircleShape)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Ink
        )
    }
}

// ============================================================
// ARBETSPASSKORT: FÖRMIDDAG / EFTERMIDDAG
// ============================================================
@Composable
internal fun ShiftListFilterSelector(
    selected: ShiftListFilter,
    onSelect: (ShiftListFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ShiftListFilter.entries.forEach { filter ->
            val active = selected == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (active) Sage else Color(0xFFF3F6F4),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(filter) }
                    .padding(vertical = 12.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    color = if (active) Color.White else Ink,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

