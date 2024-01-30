package org.hexastacks.heroesdesk.kotlin.squad

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

data class SquadMembers(val squadKey: SquadKey, val heroes: HeroIds = HeroIds.empty) {
    fun containsAll(heroIds: HeroIds): Boolean = heroes.containsAll(heroIds)

}