package com.mobdeve.s18.verify.model

import kotlinx.serialization.Serializable

@Serializable
data class CompanyUpdatePayload(
    val password: String,
    val isActive: Boolean
)
