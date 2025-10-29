package com.example.mycity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycity.model.City
import com.example.mycity.model.Place
import com.example.mycity.repository.PlacesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CityViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val placesRepository: PlacesRepository = PlacesRepository()
) : ViewModel() {

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var listenerAttached = false

    fun loadCities() {
        if (listenerAttached) return
        listenerAttached = true

        _isLoading.value = true
        firestore.collection("cities")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = e.message
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    City(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        country = doc.getString("country").orEmpty(),
                        createdByUid = doc.getString("createdByUid").orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.orEmpty()

                _cities.value = list
                _error.value = null
                _isLoading.value = false
            }
    }

    fun addCity(
        name: String,
        country: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (name.isBlank()) {
            val msg = "City name is required"
            _error.value = msg
            onError(msg)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = auth.currentUser?.uid.orEmpty()
                val data = hashMapOf(
                    "name" to name.trim(),
                    "country" to country?.trim().orEmpty(),
                    "createdByUid" to uid,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("cities").add(data).await()
                _error.value = null
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to add city"
                _error.value = msg
                onError(msg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load places for a specific city
     * Subscribes to real-time updates from Firestore
     * @param cityId The ID of the city to load places for
     */
    fun loadPlaces(cityId: String) {
        viewModelScope.launch {
            placesRepository.getPlaces(cityId).collect { placesList ->
                _places.value = placesList
            }
        }
    }

    /**
     * Add a new place to a city with Base64 encoded photo
     * @param cityId The ID of the city
     * @param place The place data to add
     * @param photoUri Optional URI of the photo to upload (will be converted to Base64)
     * @param context Android context for image conversion
     */
    fun addPlace(
        cityId: String,
        place: Place,
        photoUri: Uri?,
        context: Context,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (place.name.isBlank()) {
            val msg = "Place name is required"
            _error.value = msg
            onError(msg)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = placesRepository.addPlace(cityId, place, photoUri, context)
                if (result.isSuccess) {
                    _error.value = null
                    onSuccess()
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Failed to add place"
                    _error.value = msg
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to add place"
                _error.value = msg
                onError(msg)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
