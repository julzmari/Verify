package com.mobdeve.s18.verify.model
import kotlinx.datetime.Instant

fun User.toParcelable(): UserParcelable {
    return UserParcelable(
        id = this.id,
        companyID = this.companyID,
        role = this.role,
        name = this.name,
        email = this.email,
        isActive = this.isActive,
        dateCreated = this.createdAt.toString()
    )
}

fun UserParcelable.toUser(): User {
    return User(
        id = this.id,
        companyID = this.companyID,
        password = "",
        role = this.role,
        name = this.name,
        email = this.email,
        isActive = this.isActive,
        createdAt = Instant.parse(this.dateCreated),
        profileURL = ""
    )
}
