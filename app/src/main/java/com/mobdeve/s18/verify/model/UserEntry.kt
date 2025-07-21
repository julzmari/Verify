package com.mobdeve.s18.verify.model

import java.io.Serializable

data class UserEntry(
    val username: String,
    val locationName: String,
    val datetime: String,
    val latitude: Double,
    val longitude: Double,
    val status: String
) : Serializable
