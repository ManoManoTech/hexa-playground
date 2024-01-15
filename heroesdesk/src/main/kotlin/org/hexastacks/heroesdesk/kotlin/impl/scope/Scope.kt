package org.hexastacks.heroesdesk.kotlin.impl.scope

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

data class Scope(val name: Name, val key: ScopeKey, val assignees: Heroes) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Scope) return false

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
