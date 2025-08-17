package com.dealspulse.app.data

import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.github.jan.supabase.storage.download
import io.github.jan.supabase.storage.delete
import io.github.jan.supabase.storage.list
import io.github.jan.supabase.storage.getPublicUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageApi {
    
    private val bucketName = "deal-images"
    
    /**
     * Upload an image for a deal
     */
    suspend fun uploadDealImage(
        dealId: String,
        imageBytes: ByteArray,
        contentType: String = "image/jpeg"
    ): String {
        val fileName = "$dealId-${System.currentTimeMillis()}.jpg"
        
        Supa.storage
            .from(bucketName)
            .upload(
                path = fileName,
                data = imageBytes,
                upsert = false
            )
        
        return getDealImageUrl(fileName)
    }
    
    /**
     * Get public URL for an image
     */
    fun getDealImageUrl(fileName: String): String {
        return Supa.storage
            .from(bucketName)
            .getPublicUrl(fileName)
    }
    
    /**
     * Delete an image
     */
    suspend fun deleteDealImage(fileName: String) {
        Supa.storage
            .from(bucketName)
            .delete(fileName)
    }
    
    /**
     * List all images for a deal
     */
    suspend fun listDealImages(dealId: String): List<String> {
        return Supa.storage
            .from(bucketName)
            .list("")
            .filter { it.name.startsWith(dealId) }
            .map { it.name }
    }
    
    /**
     * Download an image
     */
    suspend fun downloadDealImage(fileName: String): ByteArray {
        return Supa.storage
            .from(bucketName)
            .download(fileName)
    }
    
    /**
     * Upload vendor logo
     */
    suspend fun uploadVendorLogo(
        vendorId: String,
        imageBytes: ByteArray,
        contentType: String = "image/jpeg"
    ): String {
        val fileName = "logos/$vendorId-${System.currentTimeMillis()}.jpg"
        
        Supa.storage
            .from(bucketName)
            .upload(
                path = fileName,
                data = imageBytes,
                upsert = true
            )
        
        return getDealImageUrl(fileName)
    }
}