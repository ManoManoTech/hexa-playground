package org.hexastacks.heroesdesk.kotlin.impl.squad

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.user.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import kotlin.test.Test

class SquadTest {

    @Test
    fun `squads with same keys and different content are equals`() {
        val squadKey = squadKey("COD")
        val squad1 = Squad(name("Write code"), squadKey)
        val squad2 = Squad(name("Read coode"), squadKey)

        assertEquals(squad1, squad2)
    }

    @Test
    fun `squads with different ids and same content aren't equals`() {
        val name = name("Write code")
        val assignees = Heroes.empty
        val squad1 = Squad(name, squadKey("COD"))
        val squad2 = Squad(name, squadKey("WRI"))

        assertNotEquals(squad1, squad2)
    }

    @Test
    fun `squads with same ids and different content have same hashcode`() {
        val squadKey = squadKey("COD")
        val squad1 = Squad(name("Write code"), squadKey).hashCode()
        val squad2 = Squad(name("Read coode"), squadKey).hashCode()

        assertEquals(squad1, squad2)
    }

    private fun squadKey(squadKey: String): SquadKey =
        SquadKey(squadKey).getOrElse { throw RuntimeException("SquadKey should be valid") }

    private fun name(squadName: String): Name =
        Name(squadName).getOrElse { throw RuntimeException("SquadName should be valid") }

}