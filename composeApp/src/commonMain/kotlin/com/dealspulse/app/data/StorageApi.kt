package com.dealspulse.app.data

import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageApi {
    
    private val bucketName = "deal-images"
    
    /**
     * Upload deal image and return public URL
     */
    suspend fun uploadDealImage(
        fileName: String,
        imageData: ByteArray,
        contentType: String = "image/jpeg"
    ): String? {
        return try {
            val user = Supa.auth.currentUserOrNull() ?: return null
            val uniqueFileName = "${user.id}/${System.currentTimeMillis()}_$fileName"
            
            // Upload file to Supabase Storage
            Supa.storage.from(bucketName).upload(
                path = uniqueFileName,
                data = imageData,
                upsert = false
            )
            
            // Get public URL
            val publicUrl = Supa.storage.from(bucketName).publicUrl(uniqueFileName)
            publicUrl
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Upload profile logo and return public URL
     */
    suspend fun uploadProfileLogo(
        fileName: String,
        imageData: ByteArray,
        contentType: String = "image/jpeg"
    ): String? {
        return try {
            val user = Supa.auth.currentUserOrNull() ?: return null
            val uniqueFileName = "logos/${user.id}/${System.currentTimeMillis()}_$fileName"
            
            // Upload file to Supabase Storage
            Supa.storage.from(bucketName).upload(
                path = uniqueFileName,
                data = imageData,
                upsert = false
            )
            
            // Get public URL
            val publicUrl = Supa.storage.from(bucketName).publicUrl(uniqueFileName)
            publicUrl
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Delete image from storage
     */
    suspend fun deleteImage(imagePath: String): Boolean {
        return try {
            // Extract path from URL if needed
            val path = if (imagePath.contains(bucketName)) {
                imagePath.substringAfter("$bucketName/")
            } else {
                imagePath
            }
            
            Supa.storage.from(bucketName).delete(path)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get signed URL for private images (if needed later)
     */
    suspend fun getSignedUrl(imagePath: String, expiresInSeconds: Int = 3600): String? {
        return try {
            val path = if (imagePath.contains(bucketName)) {
                imagePath.substringAfter("$bucketName/")
            } else {
                imagePath
            }
            
            Supa.storage.from(bucketName).createSignedUrl(path, expiresInSeconds)
        } catch (e: Exception) {
            null
        }
    }
}