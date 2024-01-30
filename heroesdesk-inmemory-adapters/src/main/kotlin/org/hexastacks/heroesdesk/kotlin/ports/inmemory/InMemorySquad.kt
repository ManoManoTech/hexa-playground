package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.user.HeroIds

data class InMemorySquad(val name: Name, val key: SquadKey, val members: HeroIds = HeroIds.empty) {
    fun toSquad(): Squad = Squad(name, key)
    fun toSquadMembers(): SquadMembers = SquadMembers(key, members)
}
