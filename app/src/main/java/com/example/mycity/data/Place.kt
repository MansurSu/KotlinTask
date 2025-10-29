package com.example.mycity.model

data class Place(
    val id: String = "",
    val cityId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val photoBase64: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
