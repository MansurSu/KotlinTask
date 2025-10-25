package com.example.mycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycity.viewmodel.CityViewModel

@Composable
fun AddCityScreen(
    cityViewModel: CityViewModel = viewModel(),
    onCityAdded: () -> Unit = {}
) {
    var cityName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    val isLoading by cityViewModel.isLoading.collectAsState()
    val error by cityViewModel.error.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Add City", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = cityName,
            onValueChange = { cityName = it },
            label = { Text("City Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = country,
            onValueChange = { country = it },
            label = { Text("Country (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                cityViewModel.addCity(
                    name = cityName,
                    country = country,
                    onSuccess = {
                        cityName = ""
                        country = ""
                        onCityAdded()
                    },
                    onError = { /* je kan een snackbar tonen */ }
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add City")
        }

        if (isLoading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
