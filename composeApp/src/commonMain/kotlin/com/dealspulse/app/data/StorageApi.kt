package com.dealspulse.app.data

import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.github.jan.supabase.storage.download
import io.github.jan.supabase.storage.delete
import io.github.jan.supabase.storage.list
import kotlinx.coroutines.flow.Flow

class StorageApi {
    
    private val bucketName = "deal-images"
    
    /**
     * Upload an image to the deal-images bucket
     */
    suspend fun uploadImage(
        fileName: String,
        imageData: ByteArray,
        contentType: String = "image/jpeg"
    ): String {
        val path = "deals/$fileName"
        
        Supa.storage[bucketName].upload(
            path = path,
            data = imageData,
            upsert = true
        )
        
        return getPublicUrl(path)
    }
    
    /**
     * Get public URL for an uploaded image
     */
    fun getPublicUrl(path: String): String {
        return "${Supa.client.supabaseUrl}/storage/v1/object/public/$bucketName/$path"
    }
    
    /**
     * Download an image from storage
     */
    suspend fun downloadImage(path: String): ByteArray {
        return Supa.storage[bucketName].download(path)
    }
    
    /**
     * Delete an image from storage
     */
    suspend fun deleteImage(path: String) {
        Supa.storage[bucketName].delete(path)
    }
    
    /**
     * List all images in a folder
     */
    suspend fun listImages(folder: String = "deals"): List<String> {
        return Supa.storage[bucketName].list(folder).map { it.name }
    }
    
    /**
     * Generate a unique filename for a deal image
     */
    fun generateFileName(dealId: String, extension: String = "jpg"): String {
        val timestamp = System.currentTimeMillis()
        return "${dealId}_${timestamp}.$extension"
    }
}