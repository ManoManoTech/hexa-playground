package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeMembers
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

data class InMemoryScope(val name: Name, val key: ScopeKey, val members: HeroIds = HeroIds.empty) {
    fun toScope(): Scope = Scope(name, key)
    fun toScopeMembers(): ScopeMembers = ScopeMembers(key, members)
}
