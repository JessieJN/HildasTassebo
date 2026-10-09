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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

// ============================================================
// 1. FÄRGER OCH GRUNDINSTÄLLNINGAR (befintlig design)
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
// 2. DATAMODELLER FÖR KALENDER, PASS OCH ANTECKNINGAR
// ============================================================
private enum class CalendarMode {
    WEEK, MONTH, LIST
}

// Filter används bara i listvyn och påverkar inte vecka/månad.
private enum class ShiftListFilter(val label: String) {
    ALL("Alla pass"), AVAILABLE("Lediga pass"), MINE("Mina pass")
}

private enum class ShiftType(val label: String) {
    MORNING("Förmiddag"),
    AFTERNOON("Eftermiddag")
}

private data class ShiftBooking(
    val name: String,
    val isMe: Boolean = false,
    val wantsToWorkAlone: Boolean = false
)

private data class ShiftKey(
    val date: LocalDate,
    val type: ShiftType
)

// En anteckning har ett unikt ID, författare och skapandetid.
// authorId används för att avgöra vem som får redigera/radera.
private data class CalendarNote(
    val id: String,
    val text: String,
    val authorId: String,
    val authorName: String,
    val createdAt: LocalDateTime
)

// ============================================================
// 3. HUVUDSKÄRM: TILLSTÅND, KALENDER OCH DIALOGRUTOR
// ============================================================
@Composable
fun CalendarScreen() {
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    var mode by remember { mutableStateOf(CalendarMode.WEEK) }
    var listFilter by remember { mutableStateOf(ShiftListFilter.ALL) }
    var bookingDialog by remember { mutableStateOf<ShiftType?>(null) }
    var wantsToWorkAlone by remember { mutableStateOf(false) }

    // ANTECKNINGAR: tillfälligt i minnet, ännu inte sparade i Firestore.
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<CalendarNote?>(null) }
    var noteToDelete by remember { mutableStateOf<CalendarNote?>(null) }
    val notes = remember { mutableStateMapOf<LocalDate, List<CalendarNote>>() }

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
        // 3A. SIDHUVUD (oförändrat)
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
        // 3B. KALENDER: lägesval, veckor/månader och arbetspass
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
                    // 3C. LISTVY: kommande pass, filter och snabbnavigering
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
                        bookings = shiftBookings
                    )
                }

                // ----------------------------------------------------
                // 3C. ANTECKNINGAR FÖR VALT DATUM
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
                // 3D. BEFINTLIG BOKNINGSKNAPP (oförändrad)
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
                // 3E. NY SEKUNDÄR KNAPP FÖR ANTECKNINGAR
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

    // ------------------------------------------------------------
    // 3F. BEFINTLIG BOKNINGSDIALOG (oförändrad)
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
            onDismissRequest = { bookingDialog = null },
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
                                wantsToWorkAlone = it
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
                        val key = ShiftKey(selectedDate, chosenType)

                        bookings[key] = currentBookings +
                                ShiftBooking(
                                    name = "Jessie",
                                    isMe = true,
                                    wantsToWorkAlone =
                                        wantsToWorkAlone &&
                                                currentBookings.isEmpty()
                                )

                        bookingDialog = null
                    }
                ) {
                    Text("Bekräfta")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { bookingDialog = null }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }

    // ------------------------------------------------------------
    // 3G. DIALOG: SKAPA ELLER REDIGERA EN ANTECKNING
    // ------------------------------------------------------------
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = {
                Text(if (editingNote == null) "Lägg till anteckning" else "Redigera anteckning")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        selectedDate.format(dateFormatter)
                            .replaceFirstChar { it.uppercase() },
                        color = Muted
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Anteckning") },
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = noteText.isNotBlank() && currentUserId.isNotEmpty(),
                    onClick = {
                        val existing = notes[selectedDate].orEmpty()
                        val edit = editingNote
                        if (edit == null) {
                            notes[selectedDate] = existing + CalendarNote(
                                id = UUID.randomUUID().toString(),
                                text = noteText.trim(),
                                authorId = currentUserId,
                                authorName = currentUserName,
                                createdAt = LocalDateTime.now()
                            )
                        } else if (edit.authorId == currentUserId || isAdmin) {
                            // Behåll ursprunglig signatur och tid vid redigering.
                            notes[selectedDate] = existing.map { note ->
                                if (note.id == edit.id) note.copy(text = noteText.trim())
                                else note
                            }
                        }
                        showNoteDialog = false
                        editingNote = null
                        noteText = ""
                    }
                ) {
                    Text("Spara")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Avbryt")
                }
            }
        )
    }

    // ------------------------------------------------------------
    // 3H. BEKRÄFTELSE INNAN EN ANTECKNING RADERAS
    // ------------------------------------------------------------
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Ta bort anteckning?") },
            text = { Text("Vill du verkligen ta bort den här anteckningen?") },
            confirmButton = {
                TextButton(onClick = {
                    if (note.authorId == currentUserId || isAdmin) {
                        notes[selectedDate] = notes[selectedDate].orEmpty()
                            .filterNot { it.id == note.id }
                    }
                    noteToDelete = null
                }) {
                    Text("Ta bort", color = MissingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Avbryt")
                }
            }
        )
    }
}

// ============================================================
// 4. VÄLJARE FÖR VECKA / MÅNAD / LISTA
// ============================================================
@Composable
private fun CalendarModeSelector(
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
// 5. VECKOVY: DATUM OCH FM/EM-PRICKAR
// ============================================================
@Composable
private fun WeekCalendar(
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
// 6. MÅNADSVY: DATUM OCH STATUSPRICKAR
// ============================================================
@Composable
private fun MonthCalendar(
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
// 7. STATUSPRICKAR: RÖD / ORANGE / GRÖN
// ============================================================
@Composable
private fun StatusDot(
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
// 8. FÖRKLARING AV STATUSFÄRGERNA
// ============================================================
@Composable
private fun ShiftLegend() {
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
private fun LegendItem(
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
// 9. ARBETSPASSKORT: FÖRMIDDAG / EFTERMIDDAG
// ============================================================
@Composable
private fun ShiftCard(
    type: ShiftType,
    bookings: List<ShiftBooking>
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
                    }
                }
            }
        }
    }
}

// ============================================================
// 10. LISTVY: FILTER OCH KORT FÖR KOMMANDE ARBETSPASS
// Påverkar inte de befintliga kalender- eller bokningskorten.
// ============================================================
@Composable
private fun ShiftListFilterSelector(
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

@Composable
private fun UpcomingShiftCard(
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
// 11. PREVIEW I ANDROID STUDIO
// ============================================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    CalendarScreen()
}
