package com.example.mycity.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * Convert URI to Base64 string with compression
     * @param context Android context for content resolver
     * @param uri Image URI to convert
     * @param maxWidth Maximum width for image compression (default 800px)
     * @return Base64 encoded string or null if conversion fails
     */
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 800): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) return null
            
            // Scale bitmap to reduce size
            val scaledBitmap = scaleBitmap(bitmap, maxWidth)
            
            // Compress to JPEG with 70% quality
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            
            // Convert to Base64
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Scale bitmap to fit within maxWidth while maintaining aspect ratio
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) {
            return bitmap
        }
        
        val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
        val newHeight = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }
    
    /**
     * Convert Base64 string back to Bitmap
     * @param base64String Base64 encoded image string
     * @return Bitmap or null if conversion fails
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            if (base64String.isEmpty()) return null
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
