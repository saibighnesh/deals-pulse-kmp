package com.dealspulse.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Report(
    val id: String? = null,
    @SerialName("post_id") val postId: String,
    @SerialName("reporter_id") val reporterId: String,
    val reason: String,
    @SerialName("created_at") val createdAt: Instant? = null
)