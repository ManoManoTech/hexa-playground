package org.hexastacks.heroesdesk.kotlin.impl.squad

import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createSquadKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SquadMembersTest{

    @Test
    fun `containsAll(empty) returns true on empty squad members`(){
        val squadMembers = SquadMembers(createSquadKeyOrThrow("squadKey"))

        assertTrue(squadMembers.containsAll(HeroIds(setOf())))
    }

    @Test
    fun `containsAll(element) returns false on empty squad members`(){
        val squadMembers = SquadMembers(createSquadKeyOrThrow("squadKey"))

        val containsAll = squadMembers.containsAll(HeroIds(createHeroIdOrThrow("heroId")))

        assertFalse(containsAll)
    }

    @Test
    fun `containsAll(element) returns true on squad members with element`(){
        val heroId = createHeroIdOrThrow("heroId")
        val squadMembers = SquadMembers(createSquadKeyOrThrow("squadKey"), HeroIds(heroId))

        val containsAll = squadMembers.containsAll(HeroIds(heroId))

        assertTrue(containsAll)
    }

    @Test
    fun `containsAll(elements) returns true on squad members all elements`(){
        val heroId1 = createHeroIdOrThrow("heroId1")
        val heroId2 = createHeroIdOrThrow("heroId2")
        val squadMembers = SquadMembers(createSquadKeyOrThrow("squadKey"), HeroIds(heroId1, heroId2))

        val containsAll = squadMembers.containsAll(HeroIds(heroId1, heroId2))

        assertTrue(containsAll)
    }
}