package com.mobdeve.s18.verify.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class User(
    val id: String,
    val companyID: String,
    val role: String,
    val name: String,
    val email: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val password: String,
    val profileURL: String? = null
)
