package com.example.mycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycity.viewmodel.CityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    onCityAdded: () -> Unit = {}
) {
    val cityViewModel: CityViewModel = viewModel()
    val error by cityViewModel.error.collectAsState()

    var cityName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add City") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = cityName,
                onValueChange = { cityName = it },
                label = { Text("City Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    if (cityName.isNotBlank()) {
                        cityViewModel.addCity(
                            name = cityName,
                            country = country
                        )
                        onCityAdded()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = cityName.isNotBlank()
            ) {
                Text("Add City")
            }
        }
    }
}
