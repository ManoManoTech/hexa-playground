package org.hexastacks.heroesdesk.kotlin.impl.user

data class HeroIds(val value: Set<HeroId>) {

    constructor(vararg heroIds: HeroId) : this(heroIds.toSet())
    constructor(heroIds: List<HeroId>) : this(heroIds.toSet())

    companion object {
        val EMPTY_HEROIDS: HeroIds = HeroIds(emptySet())
    }
}