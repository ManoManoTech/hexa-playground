package org.hexastacks.heroesdesk.kotlin.user

import arrow.core.NonEmptyList

data class HeroIds(val value: Set<HeroId>) {

    constructor(vararg heroIds: HeroId) : this(heroIds.toSet())
    constructor(heroIds: List<HeroId>) : this(heroIds.toSet())
    constructor(heroIds: NonEmptyList<HeroId>) : this(heroIds.toSet())
    constructor(heroes: Heroes) : this(heroes.map { it.id })
    constructor(vararg hero: Hero) : this(hero.map { it.id })

    val size: Int = value.size

    fun contains(id: UserId): Boolean = value.any { it.value == id.value }

    fun containsNot(id: UserId): Boolean = value.none { it.value == id.value }

    fun containsAll(heroIds: HeroIds): Boolean = value.containsAll(heroIds.value)

    fun <R> map(transform: (HeroId) -> R): List<R> = value.map { transform(it) }

    operator fun plus(heroId: HeroId): HeroIds = HeroIds(value + heroId)

    fun isNotEmpty(): Boolean = value.isNotEmpty()

    fun add(author: HeroId): HeroIds = HeroIds(value + author)

    fun isEmpty(): Boolean = value.isEmpty()
    fun forEach(action: (HeroId) -> Unit) = value.forEach(action)

    companion object {
        val empty: HeroIds = HeroIds(emptySet())
    }
}