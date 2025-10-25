package com.example.mycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycity.viewmodel.CityViewModel

@Composable
fun CitiesListScreen(
    cityViewModel: CityViewModel = viewModel()
) {
    val cities by cityViewModel.cities.collectAsState()
    val isLoading by cityViewModel.isLoading.collectAsState()
    val error by cityViewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        cityViewModel.loadCities()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "All Cities", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn {
            items(cities) { city ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = city.name, style = MaterialTheme.typography.titleMedium)
                        if (city.country.isNotBlank()) {
                            Text(text = city.country)
                        }
                        Text(
                            text = "Added by: ${city.createdByUid.take(8)}…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
