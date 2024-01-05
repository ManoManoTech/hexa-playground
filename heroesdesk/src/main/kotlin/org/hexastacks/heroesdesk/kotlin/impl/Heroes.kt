package org.hexastacks.heroesdesk.kotlin.impl

data class Heroes(val value: List<Hero>) {
    companion object {
        val EMPTY_HEROES: Heroes = Heroes(emptyList())
    }
}