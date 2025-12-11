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

                        // ✅ FIX: Read categories correctly
                        categories = doc.get("categories") as? List<String> ?: emptyList(),

                        lat = doc.getDouble("lat") ?: 0.0,
                        lng = doc.getDouble("lng") ?: 0.0,
                        rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                        comment = doc.getString("comment").orEmpty(),
                        photoUrl = doc.getString("photoUrl").orEmpty(),
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
            // Convert image to Base64
            val photoBase64 = photoUri?.let {
                ImageUtils.uriToBase64(context, it)
            } ?: ""

            val placeData = hashMapOf(
                "cityId" to cityId,
                "name" to place.name,

                // ✅ FIX: Save MULTIPLE categories
                "categories" to place.categories,

                "lat" to place.lat,
                "lng" to place.lng,
                "rating" to place.rating,
                "comment" to place.comment,
                "photoBase64" to photoBase64,
                "photoUrl" to "",
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

    suspend fun doesPlaceNameExist(cityID: String, placeName: String): Boolean {
        return try {
            val snapshot = firestore
                .collection("cities")
                .document(cityID)
                .collection("places")
                .whereEqualTo("name", placeName)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            println("Error checking place name existence: ${e.message}")
            false
        }
    }

    suspend fun updatePlace(
        cityId: String,
        placeId: String,
        place: Place,
        photoUri: Uri?,
        context: Context
    ): Result<Unit> {
        return try {
            val photoBase64 = photoUri?.let {
                ImageUtils.uriToBase64(context, it)
            } ?: place.photoBase64

            val updates = hashMapOf(
                "name" to place.name,

                // ✅ FIX: Update categories list
                "categories" to place.categories,

                "lat" to place.lat,
                "lng" to place.lng,
                "rating" to place.rating,
                "comment" to place.comment,
                "photoBase64" to photoBase64
            )

            firestore
                .collection("cities")
                .document(cityId)
                .collection("places")
                .document(placeId)
                .update(updates as Map<String, Any>)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlace(cityId: String, placeId: String): Result<Unit> {
        return try {
            firestore
                .collection("cities")
                .document(cityId)
                .collection("places")
                .document(placeId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
