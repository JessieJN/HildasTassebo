package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hildastassebo.PawPrint
import com.example.hildastassebo.data.model.CalendarEvent
import com.example.hildastassebo.data.model.CalendarNote
import com.example.hildastassebo.data.model.ShiftBooking
import com.example.hildastassebo.data.model.ShiftKey
import com.example.hildastassebo.data.model.ShiftType
import com.example.hildastassebo.ui.screens.calendar.CalendarMode
import com.example.hildastassebo.ui.screens.calendar.ShiftListFilter
import com.example.hildastassebo.ui.screens.calendar.CalendarModeSelector
import com.example.hildastassebo.ui.screens.calendar.WeekCalendar
import com.example.hildastassebo.ui.screens.calendar.MonthCalendar
import com.example.hildastassebo.ui.screens.calendar.ShiftLegend
import com.example.hildastassebo.ui.screens.calendar.ShiftCard
import com.example.hildastassebo.ui.screens.calendar.ShiftListFilterSelector
import com.example.hildastassebo.ui.screens.calendar.UpcomingShiftCard
import com.example.hildastassebo.ui.screens.calendar.CalendarShiftDialogs
import com.example.hildastassebo.ui.screens.calendar.CalendarEventDialogs
import com.example.hildastassebo.ui.screens.calendar.CalendarNoteDialogs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

// ============================================================
// FÄRGER OCH GRUNDINSTÄLLNINGAR
// ============================================================
private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)
private val MissingRed = Color(0xFFE66A68)
private val NeedsMoreOrange = Color(0xFFE9A34A)

private val Swedish = Locale.forLanguageTag("sv-SE")


// ============================================================
// HUVUDSKÄRM: TILLSTÅND, KALENDER OCH DIALOGRUTOR
// ============================================================
@Composable
fun CalendarScreen(
    events: MutableMap<LocalDate, List<CalendarEvent>>
) {
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    var mode by remember { mutableStateOf(CalendarMode.WEEK) }
    var listFilter by remember { mutableStateOf(ShiftListFilter.ALL) }
    var bookingDialog by remember { mutableStateOf<ShiftType?>(null) }
    var shiftToCancel by remember { mutableStateOf<ShiftKey?>(null) }
    var wantsToWorkAlone by remember { mutableStateOf(false) }

    // ANTECKNINGAR: tillfälligt i minnet, ännu inte sparade i Firestore.
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<CalendarNote?>(null) }
    var noteToDelete by remember { mutableStateOf<CalendarNote?>(null) }
    val notes = remember { mutableStateMapOf<LocalDate, List<CalendarNote>>() }


    var showEventDialog by remember { mutableStateOf(false) }
    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }

    // Inloggad användare signerar automatiskt nya anteckningar.
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid.orEmpty()
    val currentUserName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "Okänd användare"

    // Adminroll hämtas från användardokumentets fält "roll" (1 = admin).
    // Om rollen inte kan läsas ges ingen extra adminbehörighet.
    var isAdmin by remember(currentUserId) { mutableStateOf(false) }
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("användare")
                .document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    isAdmin = document.getLong("roll") == 1L
                }
        }
    }

    // Används under författarnamnet i anteckningsrutan.
    val noteTimeFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy 'kl.' HH:mm", Swedish)
    }

    // Tillfälliga bokningar för att kunna testa designen.
    // Dessa ersätts senare med data från Firebase.
    val bookings = remember {
        mutableStateMapOf<ShiftKey, List<ShiftBooking>>().apply {
            val monday = today.minusDays(
                (today.dayOfWeek.value - 1).toLong()
            )

            this[ShiftKey(monday, ShiftType.MORNING)] =
                listOf(ShiftBooking("Anna"))

            this[ShiftKey(monday.plusDays(1), ShiftType.MORNING)] =
                listOf(ShiftBooking("Maria"), ShiftBooking("Erik"))

            this[ShiftKey(monday.plusDays(2), ShiftType.AFTERNOON)] =
                listOf(
                    ShiftBooking(
                        "Sofia",
                        wantsToWorkAlone = true
                    )
                )

            this[ShiftKey(monday.plusDays(4), ShiftType.MORNING)] =
                listOf(ShiftBooking("Linda"))
        }
    }

    val weekStart = selectedDate.minusDays(
        (selectedDate.dayOfWeek.value - 1).toLong()
    )

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("LLLL yyyy", Swedish)
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE d MMMM", Swedish)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {


        // ------------------------------------------------------------
        // SIDHUVUD
        // ------------------------------------------------------------
        // Samma sidhuvud som på startsidan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "HILDAS TASSEBO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )

                Spacer(Modifier.width(12.dp))

                PawPrint(
                    modifier = Modifier.size(32.dp),
                    color = Sage
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "För en ljusare framtid för hemlösa katter",
                fontSize = 12.sp,
                color = DarkSage
            )
        }

        // ------------------------------------------------------------
        // KALENDER: lägesval, veckor/månader och arbetspass
        // ------------------------------------------------------------
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kalender",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = null,
                        tint = DarkSage,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            item {
                CalendarModeSelector(
                    selected = mode,
                    onSelect = { mode = it }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedDate = when (mode) {
                                CalendarMode.WEEK ->
                                    selectedDate.minusWeeks(1)
                                CalendarMode.MONTH ->
                                    selectedDate.minusMonths(1)
                                CalendarMode.LIST ->
                                    selectedDate.minusWeeks(1)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Föregående"
                        )
                    }

                    Text(
                        text = when (mode) {
                            CalendarMode.WEEK -> {
                                val weekNumber = weekStart.get(
                                    WeekFields.ISO.weekOfWeekBasedYear()
                                )
                                "Vecka $weekNumber"
                            }

                            CalendarMode.MONTH ->
                                selectedDate.format(monthFormatter)
                                    .replaceFirstChar { it.uppercase() }

                            CalendarMode.LIST -> "Kommande pass"
                        },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )

                    IconButton(
                        onClick = {
                            selectedDate = when (mode) {
                                CalendarMode.WEEK ->
                                    selectedDate.plusWeeks(1)
                                CalendarMode.MONTH ->
                                    selectedDate.plusMonths(1)
                                CalendarMode.LIST ->
                                    selectedDate.plusWeeks(1)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Nästa"
                        )
                    }
                }
            }

            when (mode) {
                CalendarMode.WEEK -> {
                    item {
                        WeekCalendar(
                            weekStart = weekStart,
                            selectedDate = selectedDate,
                            bookings = bookings,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                }

                CalendarMode.MONTH -> {
                    item {
                        MonthCalendar(
                            selectedDate = selectedDate,
                            bookings = bookings,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                }

                CalendarMode.LIST -> {
                    // ----------------------------------------------------
                    // LISTVY: kommande pass, filter och snabbnavigering
                    // Visar två veckor från valt datum (inte tidigare än idag).
                    // Pilarna ovan flyttar listans startdatum en vecka.
                    // ----------------------------------------------------
                    item {
                        ShiftListFilterSelector(
                            selected = listFilter,
                            onSelect = { listFilter = it }
                        )
                    }

                    val listStart = if (selectedDate.isBefore(today)) today else selectedDate
                    val upcomingShifts = (0L..13L).flatMap { dayOffset ->
                        val date = listStart.plusDays(dayOffset)
                        ShiftType.entries.map { type ->
                            ShiftKey(date, type)
                        }
                    }.filter { key ->
                        val shiftBookings = bookings[key].orEmpty()
                        when (listFilter) {
                            ShiftListFilter.ALL -> true
                            ShiftListFilter.AVAILABLE ->
                                shiftBookings.none { it.wantsToWorkAlone }
                            ShiftListFilter.MINE -> shiftBookings.any { it.isMe }
                        }
                    }

                    if (upcomingShifts.isEmpty()) {
                        item {
                            Text(
                                text = when (listFilter) {
                                    ShiftListFilter.MINE -> "Du har inga bokade pass under perioden."
                                    ShiftListFilter.AVAILABLE -> "Inga lediga pass under perioden."
                                    ShiftListFilter.ALL -> "Inga pass att visa."
                                },
                                color = Muted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        items(upcomingShifts) { key ->
                            UpcomingShiftCard(
                                date = key.date,
                                type = key.type,
                                bookings = bookings[key].orEmpty(),
                                onClick = {
                                    selectedDate = key.date
                                    mode = CalendarMode.WEEK
                                }
                            )
                        }
                    }
                }
            }

            if (mode != CalendarMode.LIST) {
                item {
                    ShiftLegend()
                }

                item {
                    Text(
                        text = selectedDate.format(dateFormatter)
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }

                items(ShiftType.entries.toList()) { type ->
                    val shiftBookings =
                        bookings[ShiftKey(selectedDate, type)]
                            .orEmpty()

                    ShiftCard(
                        type = type,
                        bookings = shiftBookings,
                        date = selectedDate,
                        isAdmin = isAdmin,
                        onCancelClick = { booking ->
                            if (booking.isMe) {
                                shiftToCancel = ShiftKey(selectedDate, type)
                            }
                        }
                    )
                }


                // ----------------------------------------------------
                //  BEFINTLIG BOKNINGSKNAPP
                // ----------------------------------------------------
                item {
                    Button(
                        onClick = {
                            wantsToWorkAlone = false
                            bookingDialog = ShiftType.MORNING
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSage
                        )
                    ) {
                        Icon(
                            Icons.Outlined.EventAvailable,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            "Boka pass",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }


                // ----------------------------------------------------
                // EVENEMANG FÖR VALT DATUM
                // ----------------------------------------------------
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFFF3F6F4),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Evenemang",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Ink
                                        )

                                        val dateEvents = events[selectedDate].orEmpty()

                                        if (dateEvents.isEmpty()) {
                                            Text(
                                                text = "Inga evenemang för detta datum.",
                                                fontSize = 14.sp,
                                                color = Muted
                                            )
                                        } else {
                                            dateEvents.forEach { event ->
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = event.title,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Ink,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        // Redigera evenemang
                                                        IconButton(
                                                            onClick = {
                                                                editingEvent = event
                                                                eventTitle = event.title
                                                                eventDescription = event.description
                                                                eventTime = event.time
                                                                showEventDialog = true
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Edit,
                                                                contentDescription = "Redigera evenemang",
                                                                tint = DarkSage,
                                                                modifier = Modifier.size(19.dp)
                                                            )
                                                        }

                                                        // Ta bort evenemang
                                                        IconButton(
                                                            onClick = {
                                                                eventToDelete = event
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Delete,
                                                                contentDescription = "Ta bort evenemang",
                                                                tint = Muted,
                                                                modifier = Modifier.size(19.dp)
                                                            )
                                                        }
                                                    }

                                                    if (event.time.isNotBlank()) {
                                                        Text(
                                                            text = event.time,
                                                            fontSize = 13.sp,
                                                            color = DarkSage
                                                        )
                                                    }

                                                    if (event.description.isNotBlank()) {
                                                        Text(
                                                            text = event.description,
                                                            fontSize = 14.sp,
                                                            color = Muted
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                editingEvent = null
                                                eventTitle = ""
                                                eventDescription = ""
                                                eventTime = ""
                                                showEventDialog = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, DarkSage),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = DarkSage
                                            )
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Lägg till evenemang")
                                        }
                                    }
                                }


                // ----------------------------------------------------
                // ANTECKNINGAR FÖR VALT DATUM
                // ----------------------------------------------------
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF3F6F4),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Anteckningar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )

                        val dateNotes = notes[selectedDate].orEmpty()

                        if (dateNotes.isEmpty()) {
                            Text(
                                text = "Inga anteckningar för detta datum.",
                                fontSize = 14.sp,
                                color = Muted
                            )
                        } else {
                            dateNotes.forEach { note ->
                                val canManageNote = currentUserId.isNotEmpty() &&
                                        (note.authorId == currentUserId || isAdmin)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.text,
                                            fontSize = 14.sp,
                                            color = Ink
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${note.authorName} · ${note.createdAt.format(noteTimeFormatter)}",
                                            fontSize = 12.sp,
                                            color = Muted
                                        )
                                    }

                                    // Endast författaren eller admin ser dessa ikoner.
                                    if (canManageNote) {
                                        IconButton(
                                            onClick = {
                                                editingNote = note
                                                noteText = note.text
                                                showNoteDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "Redigera anteckning",
                                                tint = DarkSage,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { noteToDelete = note },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Ta bort anteckning",
                                                tint = Muted,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // ----------------------------------------------------
                // KNAPP FÖR ANTECKNINGAR
                // ----------------------------------------------------
                item {
                    OutlinedButton(
                        onClick = {
                            editingNote = null
                            noteText = ""
                            showNoteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DarkSage),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = DarkSage
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Lägg till anteckning", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Dialogernas originalkod ligger nu i respektive kalenderfil.
    // Tillståndet ägs fortfarande av CalendarScreen, så beteendet är oförändrat.
    CalendarShiftDialogs(
        selectedDate = selectedDate,
        dateFormatter = dateFormatter,
        bookings = bookings,
        bookingDialog = bookingDialog,
        onBookingDialogChange = { bookingDialog = it },
        shiftToCancel = shiftToCancel,
        onShiftToCancelChange = { shiftToCancel = it },
        wantsToWorkAlone = wantsToWorkAlone,
        onWantsToWorkAloneChange = { wantsToWorkAlone = it }
    )
    CalendarEventDialogs(
        selectedDate = selectedDate,
        dateFormatter = dateFormatter,
        events = events,
        showEventDialog = showEventDialog,
        onShowEventDialogChange = { showEventDialog = it },
        eventTitle = eventTitle,
        onEventTitleChange = { eventTitle = it },
        eventDescription = eventDescription,
        onEventDescriptionChange = { eventDescription = it },
        eventTime = eventTime,
        onEventTimeChange = { eventTime = it },
        editingEvent = editingEvent,
        onEditingEventChange = { editingEvent = it },
        eventToDelete = eventToDelete,
        onEventToDeleteChange = { eventToDelete = it }
    )
    CalendarNoteDialogs(
        selectedDate = selectedDate,
        dateFormatter = dateFormatter,
        notes = notes,
        showNoteDialog = showNoteDialog,
        onShowNoteDialogChange = { showNoteDialog = it },
        noteText = noteText,
        onNoteTextChange = { noteText = it },
        editingNote = editingNote,
        onEditingNoteChange = { editingNote = it },
        noteToDelete = noteToDelete,
        onNoteToDeleteChange = { noteToDelete = it },
        currentUserId = currentUserId,
        currentUserName = currentUserName,
        isAdmin = isAdmin
    )

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    CalendarScreen(
        events = remember { mutableStateMapOf<LocalDate, List<CalendarEvent>>() }
    )
}
