package com.example.mycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mycity.viewmodel.CityViewModel
import kotlinx.coroutines.launch // Import coroutine launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    navController: NavController,
    onCityAdded: () -> Unit = {}
) {
    val cityViewModel: CityViewModel = viewModel()
    val error by cityViewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var cityName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        error?.let { errorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    actionLabel = "DISMISS",
                    duration = SnackbarDuration.Long
                )
            }
            cityViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add City") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB13334),
                    titleContentColor = Color.White
                )
            )
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

            Button(
                onClick = {
                    if (cityName.isNotBlank()) {
                        cityViewModel.addCity(
                            name = cityName,
                            country = country,
                            onSuccess = onCityAdded
                        )
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