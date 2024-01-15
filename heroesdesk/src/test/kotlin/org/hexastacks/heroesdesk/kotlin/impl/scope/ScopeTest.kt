package org.hexastacks.heroesdesk.kotlin.impl.scope

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class ScopeTest {
    @Test
    fun `scopes with same keys and different content are equals`() {
        val scopeKey = scopeKey("COD")
        val scope1 = Scope(name("Write code"), scopeKey, HeroIds.EMPTY_HERO_IDS)
        val scope2 = Scope(name("Read coode"), scopeKey, HeroIds(heroId()))

        assertEquals(scope1, scope2)
    }

    @Test
    fun `scopes with different ids and same content aren't equals`() {
        val name = name("Write code")
        val assignees = HeroIds.EMPTY_HERO_IDS
        val scope1 = Scope(name,  scopeKey("COD"), assignees)
        val scope2 = Scope(name,  scopeKey("WRI"), assignees)

        assertNotEquals(scope1, scope2)
    }

    @Test
    fun `scopes with same ids and different content have same hashcode`() {
        val scopeKey = scopeKey("COD")
        val scope1 = Scope(name("Write code"), scopeKey, HeroIds.EMPTY_HERO_IDS).hashCode()
        val scope2 = Scope(name("Read coode"), scopeKey, HeroIds(heroId())).hashCode()

        assertEquals(scope1, scope2)
    }

    private fun heroId() =
        HeroId("id").getOrElse { throw RuntimeException("HeroId should be valid") }


    private fun scopeKey(scopeKey: String): ScopeKey =
        ScopeKey(scopeKey).getOrElse { throw RuntimeException("ScopeKey should be valid") }

    private fun name(scopeName: String): Name =
        Name(scopeName).getOrElse { throw RuntimeException("ScopeName should be valid") }


}