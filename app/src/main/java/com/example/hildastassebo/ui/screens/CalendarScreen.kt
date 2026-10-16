package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import com.example.hildastassebo.data.model.Volunteer
import com.example.hildastassebo.data.repository.VolunteerRepository
import com.example.hildastassebo.ui.screens.calendar.AdminVolunteerPickerDialog
import com.example.hildastassebo.data.repository.ShiftRepository
import androidx.compose.runtime.DisposableEffect
import com.example.hildastassebo.data.repository.SavedShiftBooking
import com.example.hildastassebo.data.repository.NoteRepository
import com.example.hildastassebo.data.repository.EventRepository


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
    // ADMIN TILLÄGG AV PASS PÅ VOLONTÄR
    var showAdminVolunteerPicker by remember { mutableStateOf(false) }
    var adminSelectedShiftType by remember { mutableStateOf(ShiftType.MORNING) }
    var adminVolunteers by remember {
        mutableStateOf<List<Volunteer>>(emptyList())
    }
    var adminVolunteerError by remember {
        mutableStateOf<String?>(null)
    }

    val volunteerRepository = remember { VolunteerRepository() }
    val shiftRepository = remember { ShiftRepository() }
    val noteRepository = remember { NoteRepository() }
    val eventRepository = remember { EventRepository() }
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    val isPastDate = selectedDate.isBefore(
        LocalDate.now(java.time.ZoneId.of("Europe/Stockholm"))
    )
    var mode by remember { mutableStateOf(CalendarMode.WEEK) }
    var listFilter by remember { mutableStateOf(ShiftListFilter.ALL) }
    var bookingDialog by remember { mutableStateOf<ShiftType?>(null) }
    var shiftToCancel by remember { mutableStateOf<ShiftKey?>(null) }
    var bookingToCancel by remember {
        mutableStateOf<ShiftBooking?>(null)
    }
    var wantsToWorkAlone by remember { mutableStateOf(false) }

    // ANTECKNINGAR: hämtas från Firestore och uppdateras i realtid.
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<CalendarNote?>(null) }
    var noteToDelete by remember { mutableStateOf<CalendarNote?>(null) }
    val notes = remember { mutableStateMapOf<LocalDate, List<CalendarNote>>() }

    DisposableEffect(noteRepository) {
        val listener = noteRepository.observeNotes(
            onUpdate = { notesByDate ->
                notes.clear()
                notes.putAll(notesByDate)
            },
            onError = { exception ->
                android.util.Log.e(
                    "CalendarScreen",
                    "Kunde inte hämta anteckningar",
                    exception
                )
            }
        )

        onDispose {
            listener.remove()
        }
    }


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


    val bookings = remember {
        mutableStateMapOf<ShiftKey, List<ShiftBooking>>()
    }

    val userNames = remember {
        mutableStateMapOf<String, String>()
    }

    var savedBookings by remember {
        mutableStateOf(
            emptyList<SavedShiftBooking>()
        )
    }

    DisposableEffect(Unit) {
        val usersListener = FirebaseFirestore.getInstance()
            .collection("användare")
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    userNames.clear()

                    snapshot?.documents?.forEach { document ->
                        val firstName =
                            document.getString("förnamn").orEmpty()
                        val lastName =
                            document.getString("efternamn").orEmpty()

                        userNames[document.id] =
                            "$firstName $lastName".trim()
                    }
                }
            }


        val bookingsListener = shiftRepository.observeBookings(
            onUpdate = { savedBookings = it },
            onError = { exception ->
                adminVolunteerError =
                    exception.localizedMessage
                        ?: "Kunde inte hämta bokningar."
            }
        )

        onDispose {
            usersListener.remove()
            bookingsListener.remove()
        }
    }

    LaunchedEffect(savedBookings, userNames.toMap(), currentUserId) {
        bookings.clear()

        savedBookings
            .groupBy { ShiftKey(it.date, it.shiftType) }
            .forEach { (key, saved) ->
                bookings[key] = saved.map { booking ->
                    ShiftBooking(
                        name = userNames[booking.userId]
                            ?: "Okänd användare",
                        isMe = booking.userId == currentUserId,
                        wantsToWorkAlone = booking.wantsToWorkAlone,
                        userId = booking.userId
                    )
                }
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
                            if (booking.isMe || isAdmin) {
                                bookingToCancel = booking
                                shiftToCancel = ShiftKey(selectedDate, type)
                            }
                        }
                    )
                }

                // ADMIN: LÄGG TILL VOLONTÄR PÅ PASS
                if (isAdmin) {
                    item {
                        OutlinedButton(
                            onClick = {
                                adminVolunteerError = null

                                volunteerRepository.getOnlyVolunteers(
                                    onSuccess = { volunteers ->
                                        adminVolunteers = volunteers
                                        showAdminVolunteerPicker = true
                                    },
                                    onError = { exception ->
                                        adminVolunteerError =
                                            exception.localizedMessage
                                                ?: "Kunde inte hämta volontärer."
                                    }
                                )
                            },
                            enabled = !isPastDate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Lägg till volontär")
                        }
                    }
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
                            enabled = !isPastDate,
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
                                                // Endast skaparen eller admin får ändra evenemanget.
                                                val canManageEvent = currentUserId.isNotEmpty() &&
                                                        (event.creatorId == currentUserId || isAdmin)
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

                                                        if (canManageEvent) {
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

                        val dateNotes = notes[selectedDate]
                            .orEmpty()
                            .sortedWith(
                                compareByDescending<CalendarNote> { it.isAdminNote }
                                    .thenByDescending { it.createdAt }
                            )

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
    if (showAdminVolunteerPicker && isAdmin) {
        AdminVolunteerPickerDialog(
            volunteers = adminVolunteers,
            onDismiss = {
                showAdminVolunteerPicker = false
            },
            onVolunteerSelected = { volunteer, shiftType, wantsToWorkAlone ->

                if (isAdmin) {
                    shiftRepository.addVolunteerToShift(
                        volunteer = volunteer,
                        date = selectedDate,
                        shiftType = shiftType,
                        wantsToWorkAlone = wantsToWorkAlone,
                        onSuccess = {
                            showAdminVolunteerPicker = false
                        },
                        onError = { exception ->
                            showAdminVolunteerPicker = false
                            adminVolunteerError =
                                exception.localizedMessage
                                    ?: "Kunde inte spara bokningen."
                        }
                    )
                }
            }
        )
    }

    if (adminVolunteerError != null) {
        AlertDialog(
            onDismissRequest = { adminVolunteerError = null },
            title = { Text("Något gick fel") },
            text = { Text(adminVolunteerError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { adminVolunteerError = null }) {
                    Text("OK")
                }
            }
        )
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
        bookingToCancel = bookingToCancel,
        wantsToWorkAlone = wantsToWorkAlone,
        onWantsToWorkAloneChange = { wantsToWorkAlone = it },

        onBookShift = { shiftType, workAlone ->
            shiftRepository.bookOwnShift(
                date = selectedDate,
                shiftType = shiftType,
                wantsToWorkAlone = workAlone,
                onSuccess = {
                    bookingDialog = null
                    wantsToWorkAlone = false
                },
                onError = { exception ->
                    adminVolunteerError =
                        exception.localizedMessage
                            ?: "Kunde inte boka arbetspasset."
                }
            )
        },

        onCancelShift = {
            val selectedShift = shiftToCancel
            val selectedBooking = bookingToCancel

            if (selectedShift != null && selectedBooking != null) {
                val onSuccess = {
                    shiftToCancel = null
                    bookingToCancel = null
                }

                val onError: (Exception) -> Unit = { exception ->
                    adminVolunteerError =
                        exception.localizedMessage
                            ?: "Kunde inte avboka arbetspasset."
                }

                if (isAdmin) {
                    shiftRepository.adminCancelShift(
                        userId = selectedBooking.userId,
                        date = selectedShift.date,
                        shiftType = selectedShift.type,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                } else if (selectedBooking.isMe) {
                    shiftRepository.cancelOwnShift(
                        date = selectedShift.date,
                        shiftType = selectedShift.type,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                }
            }
        }
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
        onEventToDeleteChange = { eventToDelete = it },
        eventRepository = eventRepository,
        onEventError = { exception ->
            adminVolunteerError =
                exception.localizedMessage
                    ?: "Kunde inte hantera evenemanget."
        }
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
        isAdmin = isAdmin,
        noteRepository = noteRepository,
        onNoteError = { exception ->
            adminVolunteerError =
                exception.localizedMessage
                    ?: "Kunde inte hantera anteckningen."
        }
    )

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    CalendarScreen(
        events = remember { mutableStateMapOf<LocalDate, List<CalendarEvent>>() }
    )
}
