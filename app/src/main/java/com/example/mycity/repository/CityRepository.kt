package com.example.mycity.repository

import com.example.mycity.model.City
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CityRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("cities")

    suspend fun addCity(city: City): Result<String> = try {
        val doc = col.document()
        val toSave = city.copy(id = doc.id)
        doc.set(toSave).await()
        Result.success(doc.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCities(): Result<List<City>> = try {
        val snap = col.orderBy("name", Query.Direction.ASCENDING).get().await()
        val list = snap.documents.mapNotNull { it.toObject(City::class.java) }
        Result.success(list)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
