package org.hexastacks.heroesdesk.kotlin.impl.user

import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createHeroIdOrThrow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HeroIdsTest {
    @Test
    fun `HeroIds can be created empty`() {
        val heroIds = HeroIds(emptySet())

        assertNotNull(heroIds)
    }

    @Test
    fun `HeroIds can be created with one element`() {
        val heroIds = HeroIds(createHeroIdOrThrow("id"))

        assertNotNull(heroIds)
    }

    @Test
    fun `HeroIds size is 0 when empty`() {
        val heroIds = HeroIds(emptySet())

        assertEquals(0, heroIds.size())
    }

    @Test
    fun `HeroIds size returns the number of elements`() {
        val idsCount = 10
        val heroIds = HeroIds((1..idsCount).map { createHeroIdOrThrow("id$it") })

        assertEquals(idsCount, heroIds.size())
    }

    @Test
    fun `contains on empty heroIds return false`() {
        val heroes = HeroIds(emptySet())
        val hero = createHeroIdOrThrow("id1")

        val containsId1 = heroes.contains(hero)

        Assertions.assertFalse(containsId1)
    }

    @Test
    fun `contains on heroIds with heroId return true`() {
        val hero = createHeroIdOrThrow("id1")
        val heroes = HeroIds(hero, createHeroIdOrThrow("id2"))

        val containsId1 = heroes.contains(hero)

        Assertions.assertTrue(containsId1)
    }
}
