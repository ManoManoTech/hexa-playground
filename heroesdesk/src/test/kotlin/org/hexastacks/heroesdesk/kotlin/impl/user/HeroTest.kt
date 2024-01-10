package org.hexastacks.heroesdesk.kotlin.impl.user

import arrow.core.getOrElse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import kotlin.test.Test

class HeroTest {
    @Test
    fun `heroes with same ids and different names are equals`() {
        val id = "id"
        val hero1 = Hero(heroName("John"), heroId(id))
        val hero2 = Hero(heroName(hero1.name.value + "2"), heroId(id))

        assertEquals(hero1, hero2)
    }

    @Test
    fun `heroes with different ids and same names aren't equals`() {
        val id = "id"
        val hero1 = Hero(heroName("John"), heroId(id))
        val hero2 = Hero(hero1.name, heroId(id + "2"))

        assertNotEquals(hero1.hashCode(), hero2.hashCode())
    }

    @Test
    fun `heroes with same ids and different names have same hashcode`() {
        val id = "id"
        val hero1 = Hero(heroName("John"), heroId(id))
        val hero2 = Hero(heroName(hero1.name.value + "2"), heroId(id))

        assertEquals(hero1, hero2)
    }

    private fun heroId(id: String): HeroId = HeroId(id).getOrElse { throw RuntimeException("HeroId should be valid") }

    private fun heroName(name: String): UserName =
        UserName(name).getOrElse { throw RuntimeException("HeroName should be valid") }

}