package org.hexastacks.heroesdesk.kotlin.impl.user

sealed interface User<T : UserId> {
    fun asHero(): Hero? =
        if (this is Hero) this else null

    fun asAdmin(): Admin? =
        if (this is Admin) this else null

    val id: T
    val name: UserName
}
