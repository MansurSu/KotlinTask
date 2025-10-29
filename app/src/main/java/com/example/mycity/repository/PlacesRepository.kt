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

class PlacesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    /**
     * Get real-time stream of places for a specific city
     * @param cityId The ID of the city
     * @return Flow of list of places
     */
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
    
    /**
     * Add a new place to a city with optional photo
     * @param cityId The ID of the city
     * @param place The place data to add
     * @param photoUri Optional URI of the photo to upload
     * @param context Android context for image conversion
     * @return Result with place ID on success or error message on failure
     */
    suspend fun addPlace(
        cityId: String,
        place: Place,
        photoUri: Uri?,
        context: Context
    ): Result<String> {
        return try {
            // Convert photo URI to Base64 if provided
            val photoBase64 = if (photoUri != null) {
                ImageUtils.uriToBase64(context, photoUri) ?: ""
            } else {
                ""
            }
            
            // Create place data map
            val placeData = hashMapOf(
                "name" to place.name,
                "description" to place.description,
                "category" to place.category,
                "photoBase64" to photoBase64,
                "createdAt" to System.currentTimeMillis(),
                "cityId" to cityId
            )
            
            // Add to Firestore
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
