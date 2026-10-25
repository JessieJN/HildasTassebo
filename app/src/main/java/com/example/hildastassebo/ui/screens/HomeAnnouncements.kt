package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hildastassebo.data.model.HomeAnnouncementModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction

private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)


// Visar informationsmeddelanden på startsidan.
// Endast administratörer ser knapparna för att hantera meddelanden.
@Composable
fun HomeAnnouncements(
    announcements: List<HomeAnnouncementModel>,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (HomeAnnouncementModel) -> Unit,
    onDeleteClick: (HomeAnnouncementModel) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            if (isAdmin) {
                TextButton(onClick = onAddClick) {
                    Text(
                        text = "+ Nytt meddelande",
                        color = DarkSage
                    )
                }
            }
        }

        if (announcements.isEmpty()) {
            Text(
                text = "Inga nya meddelanden",
                fontSize = 14.sp,
                color = Muted
            )
        }

        announcements.forEach { announcement ->
            AnnouncementCard(
                announcement = announcement,
                isAdmin = isAdmin,
                onEditClick = { onEditClick(announcement) },
                onDeleteClick = { onDeleteClick(announcement) }
            )
        }
    }
}


// Visar ett meddelande med rubrik, text, författare och datum.
@Composable
private fun AnnouncementCard(
    announcement: HomeAnnouncementModel,
    isAdmin: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern(
        "d MMMM yyyy",
        Locale.forLanguageTag("sv-SE")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, FieldBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = announcement.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )

                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Redigera meddelande",
                                tint = DarkSage
                            )
                        }

                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Ta bort meddelande",
                                tint = Muted
                            )
                        }
                    }
                }
            }

            Text(
                text = announcement.message,
                fontSize = 14.sp,
                color = Ink
            )

            HorizontalDivider(color = PaleSage)

            Text(
                text = "${announcement.authorName} • " +
                        announcement.createdAt.format(dateFormatter),
                fontSize = 12.sp,
                color = Muted
            )

            announcement.updatedAt?.let { updatedAt ->
                Text(
                    text = "Redigerad ${updatedAt.format(dateFormatter)}",
                    fontSize = 12.sp,
                    color = Muted
                )
            }
        }
    }
}


// Dialogruta där administratören kan skapa ett nytt meddelande.
@Composable
fun AddAnnouncementDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    // Flyttar markören till meddelandefältet när användaren trycker Nästa.
    val messageFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),

        title = {
            Text(
                text = "Nytt informationsmeddelande",
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
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { messageFocusRequester.requestFocus() }
                    )
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Meddelande") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(messageFocusRequester),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title.trim(), message.trim())
                },
                enabled = title.isNotBlank() && message.isNotBlank()
            ) {
                Text("Publicera", color = DarkSage)
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt", color = Muted)
            }
        }
    )
}