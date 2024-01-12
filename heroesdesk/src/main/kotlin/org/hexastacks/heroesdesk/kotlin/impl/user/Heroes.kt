package org.hexastacks.heroesdesk.kotlin.impl.user

import arrow.core.Option
import arrow.core.firstOrNone

data class Heroes(val value: Set<Hero>) {

    val size: Int = value.size

    constructor(vararg heroes: Hero) : this(heroes.toSet())
    constructor(heroes: List<Hero>) : this(heroes.toSet())

    fun contains(hero: Hero): Boolean = value.contains(hero)

    fun contains(heroId: HeroId): Boolean =
        value
            .map { it.id }
            .contains(heroId)

    fun add(hero: Hero): Heroes = Heroes(value + hero)

    fun forEach(action: (Hero) -> Unit) {
        value.forEach(action)
    }

    operator fun get(author: HeroId): Hero? =
        value
            .firstOrNull { it.id == author }

    fun isEmpty(): Boolean = value.isEmpty()

    fun firstOrNone(): Option<Hero> = value.firstOrNone()
    fun <R> map(transform: (Hero) -> R): List<R> = value.map { transform(it) }

    companion object {
        val EMPTY_HEROES: Heroes = Heroes(emptySet())
    }
}