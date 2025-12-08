package com.example.mycity.model

data class Place(
    val id: String = "",
    val cityId: String = "",
    val name: String = "",
    val categories: List<String> = emptyList(),
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val photoUrl: String = "",
    val photoBase64: String = "", // Behouden voor backwards compatibility
    val createdAt: Long = System.currentTimeMillis(),
    val category: String = "" // Verouderd, voor migratie
)
