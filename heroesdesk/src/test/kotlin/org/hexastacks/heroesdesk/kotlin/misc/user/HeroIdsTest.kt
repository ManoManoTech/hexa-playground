package org.hexastacks.heroesdesk.kotlin.misc.user

import org.hexastacks.heroesdesk.kotlin.misc.TestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

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

        assertEquals(0, heroIds.size)
    }

    @Test
    fun `HeroIds size returns the number of elements`() {
        val idsCount = 10
        val heroIds = HeroIds((1..idsCount).map { createHeroIdOrThrow("id$it") })

        assertEquals(idsCount, heroIds.size)
    }

    @Test
    fun `contains on empty heroIds return false`() {
        val heroes = HeroIds(emptySet())
        val hero = createHeroIdOrThrow("id1")

        val containsId1 = heroes.contains(hero)

        assertFalse(containsId1)
    }

    @Test
    fun `contains on heroIds with heroId return true`() {
        val hero = createHeroIdOrThrow("id1")
        val heroes = HeroIds(hero, createHeroIdOrThrow("id2"))

        val containsId1 = heroes.contains(hero)

        assertTrue(containsId1)
    }

    @Test
    fun `containsNot on empty heroIds return true`() {
        val heroes = HeroIds(emptySet())
        val hero = createHeroIdOrThrow("id1")

        val containsNotId1 = heroes.containsNot(hero)

        assertTrue(containsNotId1)
    }

    @Test
    fun `containsNot on heroIds with heroId return false`() {
        val hero = createHeroIdOrThrow("id1")
        val heroes = HeroIds(hero, createHeroIdOrThrow("id2"))

        val containsNotId1 = heroes.containsNot(hero)

        assertFalse(containsNotId1)
    }

    @Test
    fun `containsNot on heroIds without heroId return true`() {
        val hero = createHeroIdOrThrow("id1")
        val heroes = HeroIds(createHeroIdOrThrow("id2"), createHeroIdOrThrow("id3"))

        val containsNotId1 = heroes.containsNot(hero)

        assertTrue(containsNotId1)
    }

    @Test
    fun `plus works on empty heroIds`() {
        val hero = createHeroIdOrThrow("id1")

        val heroes = HeroIds.empty + hero

        assertEquals(1, heroes.size)
        assertTrue(heroes.contains(hero))
    }

    @Test
    fun `plus works on non empty heroIds`() {
        val hero = createHeroIdOrThrow("id1")

        val heroes = HeroIds(createHeroIdOrThrow("id2")) + hero

        assertEquals(2, heroes.size)
        assertTrue(heroes.contains(hero))
    }

    @Test
    fun `isNotEmpty returns true non empty heroIds`() {
        val heroes = HeroIds(createHeroIdOrThrow("id1"))

        assertTrue(heroes.isNotEmpty())
    }

    @Test
    fun `isNotEmpty returns false on empty heroIds`() {
        val heroes = HeroIds(emptySet())

        assertFalse(heroes.isNotEmpty())
    }

    @Test
    fun `isEmpty returns false non empty heroIds`() {
        val heroes = HeroIds(createHeroIdOrThrow("id1"))

        assertFalse(heroes.isEmpty())
    }

    @Test
    fun `isEmpty returns true on empty heroIds`() {
        val heroes = HeroIds(emptySet())

        assertTrue(heroes.isEmpty())
    }

    @Test
    fun `add works`() {
        val heroes = HeroIds(createHeroIdOrThrow( "id1"))
        val hero2 = createHeroIdOrThrow( "id2")

        val heroesUpdated = heroes.add(hero2)

        assertEquals(heroesUpdated.size, 2)
        assertTrue(heroesUpdated.contains(hero2))
    }


    @Test
    fun `containsAll(empty) returns true on empty squad members`(){
        val ids = HeroIds.empty

        assertTrue(ids.containsAll(HeroIds.empty))
    }

    @Test
    fun `containsAll(element) returns false on empty squad members`(){
        val ids = HeroIds.empty

        val containsAll = ids.containsAll(HeroIds(createHeroIdOrThrow("heroId")))

        assertFalse(containsAll)
    }

    @Test
    fun `containsAll(element) returns true on squad members with element`(){
        val heroId = createHeroIdOrThrow("heroId")
        val ids = HeroIds(heroId)

        val containsAll = ids.containsAll(HeroIds(heroId))

        assertTrue(containsAll)
    }

    @Test
    fun `containsAll(elements) returns true on squad members all elements`(){
        val heroId1 = createHeroIdOrThrow("heroId1")
        val heroId2 = createHeroIdOrThrow("heroId2")
        val ids = HeroIds(heroId1, heroId2)

        val containsAll = ids.containsAll(HeroIds(heroId1, heroId2))

        assertTrue(containsAll)
    }

    @Test
    fun `forEach iterates on each element`() {
        val heroId1 = createHeroIdOrThrow("heroId1")
        val heroId2 = createHeroIdOrThrow("heroId2")
        val ids = HeroIds(heroId1, heroId2)
        val counter = AtomicInteger(0)

        ids.forEach { counter.incrementAndGet() }

        kotlin.test.assertEquals(ids.size, counter.get())
    }
}
