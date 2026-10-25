
package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hildastassebo.R
import com.example.hildastassebo.data.model.CalendarEvent
import com.example.hildastassebo.data.model.HomeAnnouncementModel
import com.example.hildastassebo.data.repository.AnnouncementRepository
import com.example.hildastassebo.data.repository.ShiftRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Samma färger som i LoginScreen.
private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

// Representerar ett bokat arbetspass.
private data class HomeBookedShift(
    val date: LocalDate,
    val period: String
)

@Composable
fun HomeScreen(
    userName: String = "Volontär",
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNextShiftClick: () -> Unit = {},
    events: Map<LocalDate, List<CalendarEvent>> = emptyMap()
) {

    // Styr popup-fönstret för användarens pass.
    var showMyShiftsDialog by remember {
        mutableStateOf(false)
    }

    // Styr dialogen för nya informationsmeddelanden.
    var showAddAnnouncementDialog by remember {
        mutableStateOf(false)
    }

    // Hämtar information direkt från Firebase.
    val announcementRepository = remember {
        AnnouncementRepository()
    }

    val announcements = remember {
        mutableStateListOf<HomeAnnouncementModel>()
    }

    // Hämtar användarens administratörsbehörighet.
    val announcementUserId =
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    var isAnnouncementAdmin by remember(announcementUserId) {
        mutableStateOf(false)
    }



    // Styr redigering, borttagning och felmeddelanden.
    var announcementToEdit by remember {
        mutableStateOf<HomeAnnouncementModel?>(null)
    }

    var announcementToDelete by remember {
        mutableStateOf<HomeAnnouncementModel?>(null)
    }

    var announcementError by remember {
        mutableStateOf<String?>(null)
    }

    // Raderar utgångna meddelanden när en admin öppnar startsidan.
    LaunchedEffect(isAnnouncementAdmin, announcementUserId) {
        if (isAnnouncementAdmin && announcementUserId.isNotEmpty()) {
            announcementRepository.deleteExpiredAnnouncements(
                onError = {
                    announcementError = it.localizedMessage
                        ?: "Kunde inte radera gamla meddelanden."
                }
            )
        }
    }

    var announcementSaving by remember {
        mutableStateOf(false)
    }

    // Uppdaterar meddelandena automatiskt från Firebase.
    DisposableEffect(announcementRepository) {
        val listener = announcementRepository.observeAnnouncements(
            onUpdate = { updatedAnnouncements ->
                announcements.clear()
                announcements.addAll(updatedAnnouncements)
            },
            onError = {
                announcementError =
                    "Kunde inte hämta information. Försök igen senare."
            }
        )

        onDispose {
            listener.remove()
        }
    }

    // Endast användare med roll 1 är administratörer.
    DisposableEffect(announcementUserId) {
        isAnnouncementAdmin = false

        if (announcementUserId.isBlank()) {
            onDispose { }
        } else {
            val listener = FirebaseFirestore.getInstance()
                .collection("användare")
                .document(announcementUserId)
                .addSnapshotListener { document, error ->

                    if (error != null) {
                        isAnnouncementAdmin = false
                        announcementError =
                            "Kunde inte kontrollera behörigheten. Försök igen senare."
                    } else {
                        isAnnouncementAdmin =
                            document?.getLong("roll") == 1L
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }

    val today = LocalDate.now()

    // Hittar det närmast kommande evenemanget.
    val nextEvent = events.values
        .flatten()
        .filter { !it.date.isBefore(today) }
        .minWithOrNull(
            compareBy<CalendarEvent> { it.date }
                .thenBy { it.time }
        )

    val shiftRepository = remember {
        ShiftRepository()
    }

    val currentUserId =
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    var bookedShifts by remember(currentUserId) {
        mutableStateOf(emptyList<HomeBookedShift>())
    }

    // Hämtar användarens bokade arbetspass.
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            bookedShifts = emptyList()
            onDispose { }
        } else {
            val listener = shiftRepository.observeBookings(
                onUpdate = { allBookings ->
                    bookedShifts = allBookings
                        .filter {
                            it.userId == currentUserId &&
                                    !it.date.isBefore(today)
                        }
                        .map { booking ->
                            HomeBookedShift(
                                date = booking.date,
                                period = booking.shiftType.label
                            )
                        }
                        .sortedWith(
                            compareBy<HomeBookedShift> { it.date }
                                .thenBy { it.period }
                        )
                },
                onError = { exception ->
                    android.util.Log.e(
                        "HomeScreen",
                        "Kunde inte hämta arbetspass",
                        exception
                    )
                }
            )

            onDispose {
                listener.remove()
            }
        }
    }

    // Hämtar användarens närmast kommande pass.
    val nextShift = bookedShifts.firstOrNull {
        !it.date.isBefore(today)
    }

    // Skapar texten för nästa arbetspass.
    val nextShiftDescription = nextShift?.let { shift ->
        val formatter = DateTimeFormatter.ofPattern(
            "EEEE d MMMM",
            Locale.forLanguageTag("sv-SE")
        )

        val dateText = shift.date.format(formatter)
            .replaceFirstChar { it.uppercase() }

        "$dateText – ${shift.period}"
    } ?: "Inget kommande pass"

    // Startsidan behåller sin befintliga layout.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // Appens rubrik med tassavtryck.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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

                Spacer(modifier = Modifier.width(12.dp))

                PawPrint(
                    modifier = Modifier.size(32.dp),
                    color = Sage
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "För en ljusare framtid för hemlösa katter",
                fontSize = 12.sp,
                color = DarkSage
            )
        }

        // Stor kattbild.
        Image(
            painter = painterResource(id = R.drawable.home_cat),
            contentDescription = "Katt som vilar i en trädgård",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Personlig välkomsthälsning.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PaleSage
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PawPrint(
                        modifier = Modifier.size(44.dp),
                        color = Sage
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Hej $userName!",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Tack för att du hjälper Hildas Tassebo ♡",
                            fontSize = 14.sp,
                            color = Ink
                        )
                    }
                }
            }

            // Öppnar användarens bokade arbetspass.
            HomeInfoCard(
                icon = "▦",
                title = "Ditt nästa pass",
                description = nextShiftDescription,
                onClick = {
                    showMyShiftsDialog = true
                }
            )

            // Visar kommande evenemang.
            HomeInfoCard(
                icon = "campaign",
                title = "Kommande evenemang",
                description = nextEvent?.let { event ->
                    val dateText = event.date.format(
                        DateTimeFormatter.ofPattern(
                            "d MMMM",
                            Locale.forLanguageTag("sv-SE")
                        )
                    )

                    "${event.title} – $dateText" +
                            if (event.time.isNotBlank()) {
                                " kl. ${event.time}"
                            } else {
                                ""
                            }
                } ?: "Inga kommande evenemang"
            )

            // Information från Firebase.
            HomeAnnouncements(
                announcements = announcements,
                isAdmin = isAnnouncementAdmin,
                onAddClick = {
                    if (isAnnouncementAdmin) {
                        showAddAnnouncementDialog = true
                    }
                },
                onEditClick = { announcement ->
                    if (isAnnouncementAdmin) {
                        announcementToEdit = announcement
                    }
                },
                onDeleteClick = { announcement ->
                    if (isAnnouncementAdmin) {
                        announcementToDelete = announcement
                    }
                }
            )

            // Dekorativ bild längst ner på startsidan.
            Image(
                painter = painterResource(
                    id = R.drawable.bottom_cat_small
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp),
                contentScale = ContentScale.Fit
            )
        }
    }

    // Publicerar ett nytt informationsmeddelande.
    if (showAddAnnouncementDialog && isAnnouncementAdmin) {
        AddAnnouncementDialog(
            onDismiss = {
                if (!announcementSaving) {
                    showAddAnnouncementDialog = false
                }
            },
            onSave = { title, message ->
                if (!announcementSaving) {
                    announcementSaving = true

                    announcementRepository.addAnnouncement(
                        title = title,
                        message = message,
                        onSuccess = {
                            announcementSaving = false
                            showAddAnnouncementDialog = false
                        },
                        onError = {
                            announcementSaving = false
                            announcementError =
                                "Meddelandet kunde inte sparas. Försök igen senare."
                        }
                    )
                }
            }
        )
    }

    // Redigerar ett befintligt meddelande.
    announcementToEdit?.let { announcement ->
        if (isAnnouncementAdmin) {
            AnnouncementEditDialog(
                announcement = announcement,
                saving = announcementSaving,
                onDismiss = {
                    announcementToEdit = null
                },
                onSave = { title, message ->
                    if (!announcementSaving) {
                        announcementSaving = true

                        announcementRepository.updateAnnouncement(
                            announcement = announcement,
                            title = title,
                            message = message,
                            onSuccess = {
                                announcementSaving = false
                                announcementToEdit = null
                            },
                            onError = {
                                announcementSaving = false
                                announcementError =
                                    "Meddelandet kunde inte uppdateras. Försök igen senare."
                            }
                        )
                    }
                }
            )
        }
    }

    // Bekräftelse innan ett meddelande tas bort.
    announcementToDelete?.let { announcement ->
        if (isAnnouncementAdmin) {
            AlertDialog(
                onDismissRequest = {
                    if (!announcementSaving) {
                        announcementToDelete = null
                    }
                },
                title = {
                    Text("Ta bort meddelande?")
                },
                text = {
                    Text(
                        "Vill du ta bort \"${announcement.title}\"? " +
                                "Det går inte att ångra."
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !announcementSaving,
                        onClick = {
                            if (!announcementSaving) {
                                announcementSaving = true

                                announcementRepository.deleteAnnouncement(
                                    announcement = announcement,
                                    onSuccess = {
                                        announcementSaving = false
                                        announcementToDelete = null
                                    },
                                    onError = {
                                        announcementSaving = false
                                        announcementError =
                                            "Meddelandet kunde inte tas bort. Försök igen senare."
                                    }
                                )
                            }
                        }
                    ) {
                        Text(
                            "Ta bort",
                            color = DarkSage
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !announcementSaving,
                        onClick = {
                            announcementToDelete = null
                        }
                    ) {
                        Text(
                            "Avbryt",
                            color = Muted
                        )
                    }
                }
            )
        }
    }

    // Visar felmeddelanden i en vanlig popup.
    announcementError?.let { error ->
        AlertDialog(
            onDismissRequest = {
                announcementError = null
            },
            title = {
                Text("Något gick fel")
            },
            text = {
                Text(error)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        announcementError = null
                    }
                ) {
                    Text(
                        "OK",
                        color = DarkSage
                    )
                }
            }
        )
    }

    // Popup-fönster för användarens bokade pass.
    if (showMyShiftsDialog) {
        MyBookedShiftsDialog(
            shifts = bookedShifts,
            onDismiss = {
                showMyShiftsDialog = false
            }
        )
    }
}

// Dialog för att redigera information.
@Composable
private fun AnnouncementEditDialog(
    announcement: HomeAnnouncementModel,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(announcement.id) {
        mutableStateOf(announcement.title)
    }

    var message by remember(announcement.id) {
        mutableStateOf(announcement.message)
    }

    AlertDialog(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Redigera information",
                fontWeight = FontWeight.Bold,
                color = Ink
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Rubrik") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Meddelande") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving &&
                        title.isNotBlank() &&
                        message.isNotBlank(),
                onClick = {
                    onSave(title.trim(), message.trim())
                }
            ) {
                Text(
                    "Spara",
                    color = DarkSage
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss
            ) {
                Text(
                    "Avbryt",
                    color = Muted
                )
            }
        }
    )
}

// Popup-fönster med användarens kommande arbetspass.
@Composable
private fun MyBookedShiftsDialog(
    shifts: List<HomeBookedShift>,
    onDismiss: () -> Unit
) {

    // Svenska datum, exempelvis "måndag 12 oktober".
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern(
            "EEEE d MMMM",
            Locale.forLanguageTag("sv-SE")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),

        title = {
            Column {
                Text(
                    text = "Mina bokade pass",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Dina kommande arbetspass",
                    fontSize = 14.sp,
                    color = Muted
                )
            }
        },

        text = {
            // Begränsad höjd om användaren har många pass.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                if (shifts.isEmpty()) {

                    Text(
                        text = "Du har inga kommande arbetspass.",
                        fontSize = 14.sp,
                        color = Muted
                    )

                } else {

                    // Visar ett kort för varje bokat pass.
                    shifts.forEachIndexed { index, shift ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Datumruta med dag och månad.
                            Column(
                                modifier = Modifier
                                    .size(
                                        width = 62.dp,
                                        height = 64.dp
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaleSage),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = shift.date.dayOfMonth.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkSage
                                )

                                Text(
                                    text = shift.date.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMM",
                                            Locale.forLanguageTag("sv-SE")
                                        )
                                    ).uppercase(
                                        Locale.forLanguageTag("sv-SE")
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkSage
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = shift.date.format(dateFormatter)
                                        .replaceFirstChar {
                                            it.uppercase()
                                        },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = shift.period,
                                    fontSize = 14.sp,
                                    color = Muted
                                )
                            }
                        }

                        // Skiljelinje mellan passen.
                        if (index < shifts.lastIndex) {
                            HorizontalDivider(
                                color = FieldBorder,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        },

        // Knapp som stänger popup-fönstret.
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Stäng",
                    color = DarkSage,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

// Återanvändbart informationskort på startsidan.
@Composable
private fun HomeInfoCard(
    icon: String,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            FieldBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (icon == "campaign") {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = DarkSage,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

            } else {
                Text(
                    text = icon,
                    fontSize = 28.sp,
                    color = DarkSage,
                    modifier = Modifier.width(42.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Muted
                )
            }

            if (onClick != null) {
                Text(
                    text = "›",
                    fontSize = 28.sp,
                    color = DarkSage
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(userName = "Jessie")
}
