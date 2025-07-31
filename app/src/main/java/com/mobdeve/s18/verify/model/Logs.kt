package com.mobdeve.s18.verify.model
import kotlinx.serialization.Serializable

@Serializable
data class Logs (
    val id: String,
    val companyID: String?,
    val tag: String,
    val date: String,
    val text: String,
)