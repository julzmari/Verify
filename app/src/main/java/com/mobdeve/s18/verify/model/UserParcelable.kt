package com.mobdeve.s18.verify.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserParcelable(
    val id: String,
    val companyID: String,
    val role: String,
    val name: String,
    val email: String,
    val isActive: Boolean,
    val dateCreated: String
) : Parcelable
