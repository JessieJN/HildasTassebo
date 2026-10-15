package com.example.hildastassebo.data.model

import java.time.LocalDate


// Datamodeller för arbetspass, passbokningar och identifiering av pass.


enum class ShiftType(val label: String) {
    MORNING("Förmiddag"),
    AFTERNOON("Eftermiddag")
}

data class ShiftBooking(
    val name: String,
    val isMe: Boolean = false,
    val wantsToWorkAlone: Boolean = false,
    val userId: String = ""
)

data class ShiftKey(
    val date: LocalDate,
    val type: ShiftType
)
