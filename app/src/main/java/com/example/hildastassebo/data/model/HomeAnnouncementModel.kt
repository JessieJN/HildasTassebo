package com.example.hildastassebo.data.model


import java.time.LocalDateTime

data class HomeAnnouncementModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime = LocalDateTime.now().plusMonths(1)
)