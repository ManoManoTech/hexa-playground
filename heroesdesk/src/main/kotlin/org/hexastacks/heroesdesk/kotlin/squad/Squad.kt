package org.hexastacks.heroesdesk.kotlin.squad

data class Squad(val name: Name, val key: SquadKey) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Squad) return false

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
