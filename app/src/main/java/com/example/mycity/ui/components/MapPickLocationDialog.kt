package com.example.mycity.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun MapPickLocationDialog(
    initialLat: Double,
    initialLng: Double,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLng by remember { mutableStateOf(initialLng) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Pick Location",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)

                                controller.setZoom(18.5)
                                val startPoint = GeoPoint(initialLat, initialLng)
                                controller.setCenter(startPoint)

                                val marker = Marker(this)

                                fun updateMarkerAddress(point: GeoPoint) {
                                    marker.title = "Loading address..."
                                    marker.showInfoWindow()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val geocoder = GeocoderNominatim(ctx.packageName)
                                        try {
                                            val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
                                            withContext(Dispatchers.Main) {
                                                if (addresses.isNotEmpty()) {
                                                    marker.title = addresses[0].getAddressLine(0)
                                                } else {
                                                    marker.title = "Address not found"
                                                }
                                                marker.showInfoWindow()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                marker.title = "Could not find address"
                                                marker.showInfoWindow()
                                            }
                                        }
                                    }
                                }

                                marker.apply {
                                    position = startPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    isDraggable = true
                                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                        override fun onMarkerDrag(marker: Marker?) {}
                                        override fun onMarkerDragEnd(marker: Marker?) {
                                            marker?.let {
                                                selectedLat = it.position.latitude
                                                selectedLng = it.position.longitude
                                                updateMarkerAddress(it.position)
                                            }
                                        }
                                        override fun onMarkerDragStart(marker: Marker?) {}
                                    })
                                }

                                val mapEventsReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                        p?.let { point ->
                                            // Update de state variabelen
                                            selectedLat = point.latitude
                                            selectedLng = point.longitude

                                            marker.position = point

                                            invalidate()
                                            updateMarkerAddress(point)
                                        }
                                        return true
                                    }

                                    override fun longPressHelper(p: GeoPoint?): Boolean {
                                        return false
                                    }
                                }

                                val eventsOverlay = MapEventsOverlay(mapEventsReceiver)
                                overlays.add(eventsOverlay)

                                overlays.add(marker)

                                updateMarkerAddress(startPoint)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onLocationSelected(selectedLat, selectedLng) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
