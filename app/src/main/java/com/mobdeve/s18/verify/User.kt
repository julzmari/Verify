package com.mobdeve.s18.verify

import java.io.Serializable

data class User(
    val username: String,
    val email: String,
    var isActive: Boolean
) : Serializable
