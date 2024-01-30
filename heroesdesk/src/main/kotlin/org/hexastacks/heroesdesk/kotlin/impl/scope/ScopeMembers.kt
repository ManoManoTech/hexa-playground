package org.hexastacks.heroesdesk.kotlin.impl.scope

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

data class ScopeMembers(val scopeKey:ScopeKey, val heroes: HeroIds = HeroIds.empty) {
    fun containsAll(heroIds: HeroIds): Boolean = heroes.containsAll(heroIds)

}