package org.hexastacks.heroesdesk.kotlin.impl

data class Heroes(val value: Set<Hero>) {

    constructor(vararg heroes: Hero) : this(heroes.toSet())
    fun contains(author: HeroId): Boolean =
        value
            .map { it.id }
            .contains(author)

    fun add(hero: Hero): Heroes = Heroes(value + hero) // FIXME: test

    fun forEach(action: (Hero) -> Unit) {
        value.forEach(action)
    }

    operator fun get(author: HeroId): Hero? =
        value
            .firstOrNull { it.id == author }

    companion object {
        val EMPTY_HEROES: Heroes = Heroes(emptySet())
    }
}