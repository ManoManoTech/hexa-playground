package org.hexastacks.heroesdesk.kotlin.impl.user

data class Heroes(val value: Set<Hero>) {

    val size: Int = value.size

    constructor(vararg heroes: Hero) : this(heroes.toSet())
    constructor(heroes: List<Hero>) : this(heroes.toSet())

    fun contains(author: HeroId): Boolean =
        value
            .map { it.id }
            .contains(author)

    fun add(hero: Hero): Heroes = Heroes(value + hero)

    fun forEach(action: (Hero) -> Unit) {
        value.forEach(action)
    }

    operator fun get(author: HeroId): Hero? =
        value
            .firstOrNull { it.id == author }

    fun isEmpty(): Boolean = value.isEmpty()

    companion object {
        val EMPTY_HEROES: Heroes = Heroes(emptySet())
    }
}