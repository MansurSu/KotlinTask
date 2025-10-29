package com.example.mycity.repository

import android.content.Context
import android.net.Uri
import com.example.mycity.model.Place
import com.example.mycity.utils.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlacesRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getPlaces(cityId: String): Flow<List<Place>> = callbackFlow {
        val listenerRegistration = firestore
            .collection("cities")
            .document(cityId)
            .collection("places")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val places = snapshot?.documents?.map { doc ->
                    Place(
                        id = doc.id,
                        cityId = cityId,
                        name = doc.getString("name").orEmpty(),
                        description = doc.getString("description").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        photoBase64 = doc.getString("photoBase64").orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()

                trySend(places)
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addPlace(
        cityId: String,
        place: Place,
        photoUri: Uri?,
        context: Context
    ): Result<String> {
        return try {
            val photoBase64 = photoUri?.let {
                ImageUtils.uriToBase64(context, it)
            } ?: ""

            val placeData = hashMapOf(
                "cityId" to cityId,
                "name" to place.name,
                "description" to place.description,
                "category" to place.category,
                "photoBase64" to photoBase64,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = firestore
                .collection("cities")
                .document(cityId)
                .collection("places")
                .add(placeData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
