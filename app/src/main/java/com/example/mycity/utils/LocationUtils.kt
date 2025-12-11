package com.example.mycity.utils
import android.Manifest

import android.content.Context

import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission

import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient

import com.google.android.gms.location.LocationServices

import com.google.android.gms.location.Priority

import com.google.android.gms.tasks.CancellationTokenSource

import kotlinx.coroutines.tasks.await

data class LatLng(val latitude: Double, val longitude: Double)


object LocationUtils {


    fun hasLocationPermission(context: Context): Boolean {

        return ContextCompat.checkSelfPermission(

            context,

            Manifest.permission.ACCESS_FINE_LOCATION

        ) == PackageManager.PERMISSION_GRANTED

    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLatLng(context: Context): LatLng? {

        if (!hasLocationPermission(context)) {

            return null

        }


        return try {

            val fusedLocationClient: FusedLocationProviderClient =

                LocationServices.getFusedLocationProviderClient(context)


            val cancellationTokenSource = CancellationTokenSource()


            val location = fusedLocationClient.getCurrentLocation(

                Priority.PRIORITY_HIGH_ACCURACY,

                cancellationTokenSource.token

            ).await()


            location?.let {

                LatLng(it.latitude, it.longitude)

            }

        } catch (e: Exception) {

            e.printStackTrace()

            null

        }

    }

} 