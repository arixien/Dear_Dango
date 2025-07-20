package com.example.deardango

import java.util.Date

data class DiaryEntry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val timestamp: Date = Date(),
    val userId: String = ""
)