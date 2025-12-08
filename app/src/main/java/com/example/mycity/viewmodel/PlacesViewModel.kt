package com.example.mycity.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycity.model.Place
import com.example.mycity.repository.PlacesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlacesViewModel(
    private val repository: PlacesRepository = PlacesRepository()
) : ViewModel() {

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places

    private val _filteredPlaces = MutableStateFlow<List<Place>>(emptyList())
    val filteredPlaces: StateFlow<List<Place>> = _filteredPlaces

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadPlaces(cityId: String) {
        viewModelScope.launch {
            repository.getPlaces(cityId).collect { placesList ->
                _places.value = placesList
                applyFilter()
            }
        }
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        applyFilter()
    }

    private fun applyFilter() {
        _filteredPlaces.value = if (_selectedCategory.value == null) {
            _places.value
        } else {
            _places.value.filter { it.categories.contains(_selectedCategory.value) }
        }
    }

    fun getCategories(): List<String> {
        return _places.value.flatMap { it.categories }.distinct().sorted()
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
