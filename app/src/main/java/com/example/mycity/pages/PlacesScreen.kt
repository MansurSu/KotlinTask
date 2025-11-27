package com.example.mycity.pages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.mycity.model.Place
import com.example.mycity.utils.ImageUtils
import com.example.mycity.viewmodel.PlacesViewModel
import android.Manifest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.example.mycity.utils.LocationUtils
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.bonuspack.location.GeocoderNominatim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    cityId: String,
    cityName: String,
    modifier: Modifier = Modifier,
    viewModel: PlacesViewModel = viewModel(),
    navController: NavController? = null
) {
    val places by viewModel.filteredPlaces.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = remember(places) { viewModel.getCategories() }

    var showAddDialog by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(cityId) {
        viewModel.loadPlaces(cityId)
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Places in $cityName") },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB13334),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFEA897A)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Place", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Filter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory ?: "All Categories",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                viewModel.filterByCategory(null)
                                expandedDropdown = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    viewModel.filterByCategory(category)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Map showing all places
            if (places.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)

                                // Add markers for all places
                                places.forEach { place ->
                                    if (place.lat != 0.0 && place.lng != 0.0) {
                                        val marker = Marker(this).apply {
                                            position = GeoPoint(place.lat, place.lng)
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            title = place.name
                                            snippet = "${place.category} - ${place.rating}★"
                                        }
                                        overlays.add(marker)
                                    }
                                }

                                // Center map on first place or default
                                val firstPlace = places.firstOrNull { it.lat != 0.0 && it.lng != 0.0 }
                                if (firstPlace != null) {
                                    controller.setZoom(12.0)
                                    controller.setCenter(GeoPoint(firstPlace.lat, firstPlace.lng))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { mapView ->
                            mapView.overlays.clear()
                            places.forEach { place ->
                                if (place.lat != 0.0 && place.lng != 0.0) {
                                    val marker = Marker(mapView).apply {
                                        position = GeoPoint(place.lat, place.lng)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = place.name
                                        snippet = "${place.category} - ${place.rating}★"
                                    }
                                    mapView.overlays.add(marker)
                                }
                            }
                            mapView.invalidate()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Places List
            if (places.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedCategory != null)
                            "No places in this category"
                        else
                            "No places yet. Add your first place!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(places) { place ->
                        PlaceCard(place = place)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaceDialog(
            cityId = cityId,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}


@Composable
fun PlaceCard(place: Place) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row {
            // Photo - Using Base64
            val bitmap = remember(place.photoBase64) {
                if (place.photoBase64.isNotEmpty()) {
                    ImageUtils.base64ToBitmap(place.photoBase64)
                } else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = place.name,
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Photo", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = place.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB13334)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = place.category,
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (place.address.isNotEmpty()) {
                    Text(
                        text = place.address,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < place.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = " (${place.rating})",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = place.comment.ifEmpty { "No comment" },
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 3
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceDialog(
    cityId: String,
    viewModel: PlacesViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    val categories = listOf("Restaurant", "Monument", "Park", "Winkel", "Bar", "Anders")
    var category by remember { mutableStateOf(categories.first()) }
    var expandedCategory by remember { mutableStateOf(false) }

    var rating by remember { mutableStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }

    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> photoUri = uri }

    val cameraUri = remember {
        val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) photoUri = cameraUri }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) cameraLauncher.launch(cameraUri) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.viewModelScope.launch {
                val location = LocationUtils.getCurrentLatLng(context)
                location?.let { 
                    lat = it.latitude
                    lng = it.longitude
                    // Reverse geocode to get address
                    withContext(Dispatchers.IO) {
                        val geocoder = GeocoderNominatim(context.packageName)
                        try {
                            val addresses = geocoder.getFromLocation(lat, lng, 1)
                            if (addresses.isNotEmpty()) {
                                address = addresses[0].getAddressLine(0)
                            } else {
                                address = "Address not found"
                            }
                        } catch (e: Exception) {
                            address = "Could not find address"
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add New Place", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB13334))

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    category = selection
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Column {
                    Text("Rating: ${rating.toInt()}/5", fontSize = 14.sp)
                    Slider(value = rating, onValueChange = { rating = it }, valueRange = 0f..5f, steps = 4, enabled = !isLoading)
                }

                OutlinedTextField(
                    value = comment, onValueChange = { comment = it },
                    label = { Text("Comment") }, modifier = Modifier.fillMaxWidth(), maxLines = 3, enabled = !isLoading
                )

                // --- Location Section ---
                Text("Location", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                if (address.isNotEmpty()) {
                     OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                }

                Button(onClick = { 
                     if (LocationUtils.hasLocationPermission(context)) {
                        viewModel.viewModelScope.launch {
                            val location = LocationUtils.getCurrentLatLng(context)
                            location?.let { 
                                lat = it.latitude
                                lng = it.longitude
                                // Reverse geocode to get address
                                withContext(Dispatchers.IO) {
                                    val geocoder = GeocoderNominatim(context.packageName)
                                    try {
                                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                                        if (addresses.isNotEmpty()) {
                                            address = addresses[0].getAddressLine(0)
                                        } else {
                                            address = "Address not found"
                                        }
                                    } catch (e: Exception) {
                                        address = "Could not find address"
                                    }
                                }
                            }
                        }
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                    Text("Get Current Address")
                }

                // --- Photo Section ---
                Text("Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Gallery") }
                    Button(onClick = {
                        if (context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(cameraUri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Camera") }
                }

                if (photoUri != null) {
                    Text("✓ Photo selected", color = Color(0xFF4CAF50), fontSize = 14.sp)
                }

                // --- Action Buttons ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && lat != 0.0 && lng != 0.0) {
                                val place = Place(
                                    name = name,
                                    category = category,
                                    rating = rating,
                                    comment = comment,
                                    lat = lat,
                                    lng = lng,
                                    address = address
                                )

                                viewModel.addPlace(
                                    cityId = cityId,
                                    place = place,
                                    photoUri = photoUri,
                                    context = context,
                                    onSuccess = { onDismiss() },
                                    onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                                )
                            } else {
                                Toast.makeText(context, "Please fill name and get location", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Add")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlacesScreenPreview() {
    PlacesScreen(cityId = "1", cityName = "Preview City", navController = rememberNavController())
}
