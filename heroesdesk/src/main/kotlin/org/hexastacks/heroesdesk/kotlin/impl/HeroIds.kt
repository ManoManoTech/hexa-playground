package org.hexastacks.heroesdesk.kotlin.impl

data class HeroIds(val value: Set<HeroId>) {

    constructor(vararg heroIds: HeroId) : this(heroIds.toSet())
    constructor(heroIds: List<HeroId>) : this(heroIds.toSet())

}