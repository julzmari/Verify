package com.mobdeve.s18.verify.model

import kotlinx.serialization.Serializable

@Serializable
data class UserEntry(
    val id: String,
    val username: String,
    val user_id: String,
    val company_id: String,
    val image_url: String,
    val status: String,
    val location_name: String,
    val latitude: Double,
    val longitude: Double,
    val datetime: String,
)

