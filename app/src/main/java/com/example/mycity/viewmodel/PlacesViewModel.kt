package com.example.mycity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycity.model.Place
import com.example.mycity.repository.PlacesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

class PlacesViewModel(
    private val repository: PlacesRepository = PlacesRepository()
) : ViewModel() {

    private val _allPlaces = MutableStateFlow<List<Place>>(emptyList())
    val allPlaces: StateFlow<List<Place>> = _allPlaces.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val filteredPlaces: StateFlow<List<Place>> =
        combine(_allPlaces, _selectedCategory) { places, category ->
        if (category == null) {
            places
        } else {
            places.filter { it.categories.contains(category) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPlaces(cityId: String) {
        viewModelScope.launch {
            repository.getPlaces(cityId).collect { placesList ->
                _allPlaces.value = placesList
            }
        }
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun getCategories(): List<String> {
        return _allPlaces.value.flatMap { it.categories }.distinct().sorted()
    }

    fun addPlace(
        cityId: String,
        place: Place,
        photoUri: Uri?,
        context: Context,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if(repository.doesPlaceNameExist(cityId, place.name)) {
                onError("Place with the same name already exists")
            }
            else{
                _isLoading.value = true
                repository.addPlace(cityId, place, photoUri, context)
                    .onSuccess {
                        onSuccess()
                    }
                    .onFailure { e ->
                        onError(e.message ?: "Failed to add place")
                    }
                _isLoading.value = false
            }
        }
    }
}