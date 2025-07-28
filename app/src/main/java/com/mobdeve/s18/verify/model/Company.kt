package com.mobdeve.s18.verify.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
data class Company(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val createdAt: Instant,
    val profileURL: String? = null,
    val isActive: Boolean,
    val last_login: Instant? = null,
    val last_failed_login: Instant? = null

)
