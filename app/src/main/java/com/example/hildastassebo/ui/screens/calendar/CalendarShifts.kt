package com.example.hildastassebo.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.combinedClickable


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


// Passkort och kort för kommande pass. Bokningsdialoger ligger i CalendarScreen.
@Composable
internal fun ShiftCard(
    type: ShiftType,
    bookings: List<ShiftBooking>,
    date: LocalDate,
    isAdmin: Boolean,
    onCancelClick: (ShiftBooking) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, FieldBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (type == ShiftType.MORNING) {
                        Icons.Outlined.WbSunny
                    } else {
                        Icons.Outlined.NightsStay
                    },
                    contentDescription = null,
                    tint = DarkSage,
                    modifier = Modifier.size(27.dp)
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = type.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )

                StatusDot(bookings)
            }

            Spacer(Modifier.height(12.dp))

            if (bookings.isEmpty()) {
                Text(
                    text = "Ingen är bokad på detta pass",
                    fontSize = 14.sp,
                    color = Muted
                )
            } else {
                bookings.forEach { booking ->
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PaleSage, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = booking.name.take(1),
                                color = DarkSage,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = booking.name +
                                    if (booking.isMe) " (Du)" else "",
                            color = Ink,
                            fontSize = 14.sp
                        )

                        if (booking.wantsToWorkAlone) {
                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Önskar arbeta ensam",
                                color = DarkSage,
                                fontSize = 11.sp
                            )
                        }
                        if (booking.isMe || isAdmin) {
                            Spacer(Modifier.weight(1f))

                            val canCancel = isAdmin ||
                                    date.isAfter(LocalDate.now().plusDays(7))

                            var showLockMessage by remember { mutableStateOf(false) }

                            Box {
                                Icon(
                                    imageVector = if (canCancel) {
                                        Icons.Outlined.Delete
                                    } else {
                                        Icons.Outlined.Lock
                                    },
                                    contentDescription = if (canCancel) {
                                        "Avboka pass"
                                    } else {
                                        "Passet är låst"
                                    },
                                    tint = if (canCancel) MissingRed else Muted,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (canCancel) {
                                                    onCancelClick(booking)
                                                }
                                            },
                                            onLongClick = {
                                                if (!canCancel) {
                                                    showLockMessage = true
                                                }
                                            }
                                        )
                                        .padding(5.dp)
                                )

                                DropdownMenu(
                                    expanded = showLockMessage,
                                    onDismissRequest = { showLockMessage = false }
                                ) {
                                    Text(
                                        text = "Kontakta administratör för att avboka passet.",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 13.sp,
                                        color = Ink
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
// LISTVY: FILTER OCH KORT FÖR KOMMANDE ARBETSPASS
// Påverkar inte de befintliga kalender- eller bokningskorten.
// ============================================================
@Composable
internal fun UpcomingShiftCard(
    date: LocalDate,
    type: ShiftType,
    bookings: List<ShiftBooking>,
    onClick: () -> Unit
) {
    val dateLabel = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Swedish))
            .replaceFirstChar { it.uppercase() }
    }
    val statusText = when {
        bookings.isEmpty() -> "Ingen bokad"
        bookings.any { it.wantsToWorkAlone } -> "Arbetar ensam"
        else -> "Fler önskas"
    }
    val names = if (bookings.isEmpty()) {
        "Ingen bokad"
    } else {
        bookings.joinToString(", ") { booking ->
            booking.name + if (booking.isMe) " (Du)" else ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, FieldBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )
                StatusDot(bookings)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (type == ShiftType.MORNING) {
                        Icons.Outlined.WbSunny
                    } else {
                        Icons.Outlined.NightsStay
                    },
                    contentDescription = null,
                    tint = DarkSage,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(type.label, color = Ink, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(statusText, color = Muted, fontSize = 12.sp)
            }
            Text(text = names, fontSize = 14.sp, color = Muted)
        }
    }
}

// ============================================================
// PREVIEW I ANDROID STUDIO
// ============================================================

// Boknings- och avbokningsdialoger (samma regler och texter som originalet).
@Composable
internal fun CalendarShiftDialogs(
    selectedDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    bookings: MutableMap<ShiftKey, List<ShiftBooking>>,
    bookingDialog: ShiftType?,
    onBookingDialogChange: (ShiftType?) -> Unit,
    shiftToCancel: ShiftKey?,
    onShiftToCancelChange: (ShiftKey?) -> Unit,
    bookingToCancel: ShiftBooking?,
    wantsToWorkAlone: Boolean,
    onWantsToWorkAloneChange: (Boolean) -> Unit,
    onBookShift: (ShiftType, Boolean) -> Unit,
    onCancelShift: () -> Unit
) {


    // ------------------------------------------------------------
    // BEFINTLIG BOKNINGSDIALOG
    // ------------------------------------------------------------
    bookingDialog?.let { initialType ->
        var chosenType by remember(initialType) {
            mutableStateOf(initialType)
        }

        val currentBookings =
            bookings[ShiftKey(selectedDate, chosenType)].orEmpty()

        val alreadyBooked = currentBookings.any { it.isMe }
        val someoneWantsAlone =
            currentBookings.any { it.wantsToWorkAlone }

        AlertDialog(
            onDismissRequest = { onBookingDialogChange(null) },
            containerColor = Color.White,
            title = { Text("Boka arbetspass") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        selectedDate.format(dateFormatter)
                            .replaceFirstChar { it.uppercase() }
                    )

                    Text(
                        "Välj pass",
                        fontWeight = FontWeight.SemiBold
                    )

                    ShiftType.entries.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenType = type }
                        ) {
                            RadioButton(
                                selected = chosenType == type,
                                onClick = { chosenType = type }
                            )

                            Text(type.label)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = wantsToWorkAlone,
                            onCheckedChange = {
                                onWantsToWorkAloneChange(it)
                            },
                            enabled = currentBookings.isEmpty()
                        )

                        Text(
                            text = "Jag önskar arbeta ensam",
                            fontSize = 14.sp
                        )
                    }

                    if (currentBookings.isNotEmpty()) {
                        Text(
                            text = "Önskemål om att arbeta ensam kan bara väljas när ingen annan är bokad.",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }

                    if (someoneWantsAlone) {
                        Text(
                            text = "En volontär har önskat att arbeta ensam på detta pass.",
                            color = DarkSage,
                            fontSize = 13.sp
                        )
                    }

                    if (alreadyBooked) {
                        Text(
                            text = "Du är redan bokad på detta pass.",
                            color = MissingRed,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !alreadyBooked && !someoneWantsAlone,
                    onClick = {
                        onBookShift(
                            chosenType,
                            wantsToWorkAlone && currentBookings.isEmpty()
                        )
                    }
                ) {
                    Text("Bekräfta")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onBookingDialogChange(null) }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }

    shiftToCancel?.let { key ->
        AlertDialog(
            onDismissRequest = { onShiftToCancelChange(null) },
            containerColor = Color.White,
            title = { Text("Avboka arbetspass?") },
            text = {
                Text(
                    if (bookingToCancel?.isMe == true) {
                        "Är du säker på att du vill avboka ditt arbetspass?"
                    } else {
                        "Är du säker på att du vill avboka " +
                                "${bookingToCancel?.name ?: "volontären"} från detta pass?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelShift()
                    }
                ) {
                    Text("Avboka pass", color = MissingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { onShiftToCancelChange(null) }) {
                    Text("Behåll passet")
                }
            }
        )
    }
}
