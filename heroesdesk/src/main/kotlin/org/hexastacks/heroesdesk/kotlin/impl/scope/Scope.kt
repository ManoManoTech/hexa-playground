package org.hexastacks.heroesdesk.kotlin.impl.scope

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

data class Scope(val name: Name, val key: ScopeKey, val assignees: HeroIds) {
}
