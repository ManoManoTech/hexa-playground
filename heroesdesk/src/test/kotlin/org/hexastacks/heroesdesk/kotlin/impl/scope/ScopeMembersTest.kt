package org.hexastacks.heroesdesk.kotlin.impl.scope

import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createScopeKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScopeMembersTest{

    @Test
    fun `containsAll(empty) returns true on empty scope members`(){
        val scopeMembers = ScopeMembers(createScopeKeyOrThrow("scopeKey"))

        assertTrue(scopeMembers.containsAll(HeroIds(setOf())))
    }

    @Test
    fun `containsAll(element) returns false on empty scope members`(){
        val scopeMembers = ScopeMembers(createScopeKeyOrThrow("scopeKey"))

        val containsAll = scopeMembers.containsAll(HeroIds(createHeroIdOrThrow("heroId")))

        assertFalse(containsAll)
    }

    @Test
    fun `containsAll(element) returns true on scope members with element`(){
        val heroId = createHeroIdOrThrow("heroId")
        val scopeMembers = ScopeMembers(createScopeKeyOrThrow("scopeKey"), HeroIds(heroId))

        val containsAll = scopeMembers.containsAll(HeroIds(heroId))

        assertTrue(containsAll)
    }

    @Test
    fun `containsAll(elements) returns true on scope members all elements`(){
        val heroId1 = createHeroIdOrThrow("heroId1")
        val heroId2 = createHeroIdOrThrow("heroId2")
        val scopeMembers = ScopeMembers(createScopeKeyOrThrow("scopeKey"), HeroIds(heroId1, heroId2))

        val containsAll = scopeMembers.containsAll(HeroIds(heroId1, heroId2))

        assertTrue(containsAll)
    }
}