package org.hexastacks.heroesdesk.kotlin.user

data class Hero(override val id: HeroId, override val name: UserName): AbstractUser<HeroId>() {
}
