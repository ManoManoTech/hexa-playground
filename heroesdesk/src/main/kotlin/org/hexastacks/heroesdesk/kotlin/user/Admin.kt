package org.hexastacks.heroesdesk.kotlin.user

data class Admin(override val id: AdminId, override val  name: UserName): AbstractUser<AdminId>() {
}