package org.hexastacks.heroesdesk.kotlin.impl.user

sealed interface User<T : UserId> {
    val id: T
    val name: UserName
}
