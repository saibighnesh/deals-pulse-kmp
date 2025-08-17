package com.dealspulse.app.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String,
    
    @SerialName("post_id")
    val postId: String,
    
    @SerialName("reporter_id")
    val reporterId: String,
    
    val reason: String,
    
    @SerialName("created_at")
    val createdAt: Instant
)

@Serializable
enum class ReportReason(val displayName: String) {
    @SerialName("spam")
    SPAM("Spam or misleading"),
    
    @SerialName("inappropriate")
    INAPPROPRIATE("Inappropriate content"),
    
    @SerialName("fake")
    FAKE("Fake or fraudulent deal"),
    
    @SerialName("expired")
    EXPIRED("Deal already expired"),
    
    @SerialName("other")
    OTHER("Other")
}