
package com.example.hildastassebo.data.model

data class Volunteer(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: Long = 0L
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}
