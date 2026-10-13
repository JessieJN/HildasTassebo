package com.example.hildastassebo.data.model

import java.time.LocalDate
import java.time.LocalDateTime


// Datamodeller för kalenderns evenemang och anteckningar.


data class CalendarEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val description: String,
    val time: String
)


data class CalendarNote(
    val id: String,
    val text: String,
    val authorId: String,
    val authorName: String,
    val createdAt: LocalDateTime
)
