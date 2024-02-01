package org.hexastacks.heroesdesk.kotlin.user

import arrow.core.Option
import arrow.core.firstOrNone

data class Heroes(val value: Set<Hero>) {

    val size: Int = value.size

    constructor(vararg heroes: Hero) : this(heroes.toSet())
    constructor(heroes: List<Hero>) : this(heroes.toSet())

    fun contains(hero: Hero): Boolean = value.contains(hero)

    fun contains(heroId: HeroId): Boolean =
        value
            .any { it.id == heroId }

    fun containsNot(hero: Hero): Boolean =
        value
            .none { it == hero }

    fun add(hero: Hero): Heroes = Heroes(value + hero)

    fun forEach(action: (Hero) -> Unit) = value.forEach(action)

    operator fun get(author: HeroId): Hero? =
        value
            .firstOrNull { it.id == author }

    fun isEmpty(): Boolean = value.isEmpty()

    fun isNotEmpty(): Boolean = value.isNotEmpty()

    fun firstOrNone(): Option<Hero> = value.firstOrNone()

    fun <R> map(transform: (Hero) -> R): List<R> = value.map { transform(it) }

    fun intersect(authorAndAssignees: Heroes) : Heroes = Heroes(value.intersect(authorAndAssignees.value))

    fun subtract(heroes: Heroes) : Heroes = Heroes(value.subtract(heroes.value))
    fun subtract(heroesId: HeroIds): Heroes = value.filter { !heroesId.contains(it.id) }.let { Heroes(it) }

    fun toHeroIds(): HeroIds = HeroIds(value.map { it.id })

    companion object {
        val empty: Heroes = Heroes(emptySet())
    }
}