package org.hexastacks.heroesdesk.kotlin.impl.user

data class Hero(override val name: UserName, override val id: HeroId): AbstractUser<HeroId>() {
}
