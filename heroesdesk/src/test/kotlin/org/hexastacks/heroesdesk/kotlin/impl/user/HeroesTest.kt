package org.hexastacks.heroesdesk.kotlin.impl.user

import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createHeroNameOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createHeroOrThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeroesTest {
    @Test
    fun `Heroes can be created empty`() {
        val heroes = Heroes(emptySet())

        assertNotNull(heroes)
    }

    @Test
    fun `Heroes can be created with one element`() {
        val heroes = Heroes(createHeroOrThrow("name", "id"))

        assertNotNull(heroes)
        assertEquals(heroes.size, 1)
    }

    @Test
    fun `contains works on present element`() {
        val authorId = createHeroIdOrThrow("id")
        val heroes = Heroes(Hero(createHeroNameOrThrow("name"), authorId))

        val contains = heroes.contains(authorId)

        assertTrue(contains)
    }

    @Test
    fun `contains returns false on missing element`() {
        val heroes = Heroes(Hero(createHeroNameOrThrow("name"), createHeroIdOrThrow("id")))

        val contains = heroes.contains(createHeroIdOrThrow("heroIdNotIn"))

        assertFalse(contains)
    }

    @Test
    fun `add works`() {
        val heroes = Heroes(createHeroOrThrow("name1", "id1"))
        val hero2 = createHeroOrThrow("name2", "id2")

        val heroesUpdated = heroes.add(hero2)

        assertEquals(heroesUpdated.size, 2)
        assertTrue(heroesUpdated.contains(hero2.id))
    }

    @Test
    fun `isEmpty returns false on non empty heroes`() {
        val heroes = Heroes(createHeroOrThrow("name1", "id1"))

        assertFalse(heroes.isEmpty())
    }

    @Test
    fun `isNotEmpty returns true non empty heroes`() {
        val heroes = Heroes(createHeroOrThrow("name1", "id1"))

        assertTrue(heroes.isNotEmpty())
    }

    @Test
    fun `isEmpty returns true on empty heroes`() {
        val heroes = Heroes(emptySet())

        assertTrue(heroes.isEmpty())
    }

    @Test
    fun `isNotEmpty returns false on empty heroes`() {
        val heroes = Heroes(emptySet())

        assertFalse(heroes.isNotEmpty())
    }

    @Test
    fun `get return Hero on present id`() {
        val id = createHeroIdOrThrow("id")
        val heroes = Heroes(Hero(createHeroNameOrThrow("name"), id))

        val hero: Hero? = heroes[id]

        assertNotNull(hero)
    }

    @Test
    fun `get returns null on absent id`() {
        val heroes = Heroes(emptySet())

        val hero: Hero? = heroes[createHeroIdOrThrow("id")]

        assertNull(hero)
    }

    @Test
    fun `2 empty heroes instances are equals`() {
        val emptyHeroes1 = Heroes(emptySet())
        val emptyHeroes2 = Heroes.empty

        assertEquals(emptyHeroes1, emptyHeroes2)
    }

    @Test
    fun `for each iterates on each element`() {
        val heroes = Heroes(createHeroOrThrow("name1", "id1"), createHeroOrThrow("name2", "id2"))
        val counter = AtomicInteger(0)

        heroes.forEach { counter.incrementAndGet() }

        assertEquals(heroes.size, counter.get())
    }

    @Test
    fun `Heroes can be constructed from list`() {
        val hero1 = createHeroOrThrow("name1", "id1")
        val heroes = Heroes(listOf(hero1, createHeroOrThrow("name2", "id2"), hero1))

        assertEquals(heroes.size, 2)
    }

    @Test
    fun `firstOrNone returns none on empty Heroes`() {
        val result = Heroes(emptySet()).firstOrNone()

        assertTrue(result.isNone())
    }

    @Test
    fun `firstOrNone returns element on 1 elem Heroes`() {
        val hero = createHeroOrThrow("name1", "id1")

        val result = Heroes(hero).firstOrNone()

        assertTrue(result.isSome())
        result.onSome {
            assertEquals(hero, it)
        }
    }


    @Test
    fun `firstOrNone returns element on N elems Heroes`() {
        val hero1 = createHeroOrThrow("name1", "id1")
        val hero2 = createHeroOrThrow("name2", "id2")
        val heroes = Heroes(hero1, hero2)

        val result = heroes.firstOrNone()

        assertTrue(result.isSome())
        result.onSome {
            assertTrue(
                heroes
                    .contains(it)
            )
        }
    }

    @Test
    fun `contains on empty heroes return false`(){
        val heroes = Heroes(emptySet())
        val hero = createHeroOrThrow("name1", "id1")

        val containsId1 = heroes.contains(hero)

        assertFalse(containsId1)
    }

    @Test
    fun `contains on heroes with hero return true`(){
        val hero = createHeroOrThrow("name1", "id1")
        val heroes = Heroes(hero, createHeroOrThrow("name2","id2"))

        val containsId1 = heroes.contains(hero)

        assertTrue(containsId1)
    }

    @Test
    fun `containsNot on empty heroes return true`(){
        val heroes = Heroes(emptySet())
        val hero = createHeroOrThrow("name1", "id1")

        val containsId1 = heroes.containsNot(hero)

        assertTrue(containsId1)
    }

    @Test
    fun `containsNot on heroes with hero return false`(){
        val hero = createHeroOrThrow("name1", "id1")
        val heroes = Heroes(hero, createHeroOrThrow("name2","id2"))

        val containsId1 = heroes.containsNot(hero)

        assertFalse(containsId1)
    }

    @Test
    fun `map iterates all heroes`(){
        val hero1 = createHeroOrThrow("name1", "id1")
        val hero2 = createHeroOrThrow("name2", "id2")
        val heroes = Heroes(hero1, hero2)
        val counter = AtomicInteger(0)

        heroes.map { counter.incrementAndGet()}

        assertEquals(heroes.size, counter.get())
    }

    @Test
    fun `map works over empty heroes`(){
        val heroes = Heroes.empty
        val counter = AtomicInteger(0)

        heroes.map { counter.incrementAndGet()}

        assertEquals(heroes.size, counter.get())
    }
}
