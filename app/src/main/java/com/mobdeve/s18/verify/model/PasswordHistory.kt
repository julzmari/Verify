package com.mobdeve.s18.verify.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordHistory(
    val id: String,
    val user_id: String,
    val user_type: String,
    val password_hash: String,
    val changed_at: String
)

