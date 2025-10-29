package com.example.mycity.model

data class Place(
    val id: String = "",
    val cityId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val photoBase64: String = "", // Base64 encoded image
    val createdAt: Long = System.currentTimeMillis()
)
