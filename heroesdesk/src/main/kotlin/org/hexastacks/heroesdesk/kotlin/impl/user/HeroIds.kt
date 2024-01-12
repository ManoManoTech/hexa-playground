package org.hexastacks.heroesdesk.kotlin.impl.user

data class HeroIds(val value: Set<HeroId>) {

    constructor(vararg heroIds: HeroId) : this(heroIds.toSet())
    constructor(heroIds: List<HeroId>) : this(heroIds.toSet())
    constructor(heroes: Heroes) : this(heroes.map { it.id })
    constructor(hero: Hero) : this(hero.id)

    fun size(): Int = value.size
    fun contains(id: UserId): Boolean = value.any { it.value == id.value }

    fun <R> map(transform: (HeroId) -> R): List<R> = value.map { transform(it) }


    companion object {
        val EMPTY_HEROIDS: HeroIds = HeroIds(emptySet())
    }
}