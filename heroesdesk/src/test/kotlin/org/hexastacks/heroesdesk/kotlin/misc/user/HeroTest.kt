package org.hexastacks.heroesdesk.kotlin.misc.user

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.user.Hero
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.UserName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import kotlin.test.Test

class HeroTest {

    @Test
    fun `heroes with same ids and different names are equals`() {
        val id = "id"
        val hero1 = Hero(heroId(id), heroName("John"))
        val hero2 = Hero(heroId(id), heroName(hero1.name.value + "2"))

        assertEquals(hero1, hero2)
    }

    @Test
    fun `heroes with different ids and same names aren't equals`() {
        val id = "id"
        val hero1 = Hero(heroId(id), heroName("John"))
        val hero2 = Hero(heroId(id + "2"), hero1.name)

        assertNotEquals(hero1.hashCode(), hero2.hashCode())
    }

    @Test
    fun `heroes with same ids and different names have same hashcode`() {
        val id = "id"
        val hero1 = Hero(heroId(id), heroName("John"))
        val hero2 = Hero(heroId(id), heroName(hero1.name.value + "2"))

        assertEquals(hero1.hashCode(), hero2.hashCode())
    }

    private fun heroId(id: String): HeroId = HeroId(id).getOrElse { throw RuntimeException("HeroId should be valid") }

    private fun heroName(name: String): UserName =
        UserName(name).getOrElse { throw RuntimeException("HeroName should be valid") }

}