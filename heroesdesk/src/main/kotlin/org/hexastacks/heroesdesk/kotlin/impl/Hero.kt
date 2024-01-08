package org.hexastacks.heroesdesk.kotlin.impl

data class Hero(val name: HeroName, val id: HeroId) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hero

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
