package com.example.mycity.model

data class City(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val createdByUid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)