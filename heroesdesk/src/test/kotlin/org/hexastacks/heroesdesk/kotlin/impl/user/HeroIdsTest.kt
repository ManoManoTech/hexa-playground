package org.hexastacks.heroesdesk.kotlin.impl.user

import org.hexastacks.heroesdesk.kotlin.impl.TestUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class HeroIdsTest {
    @Test
    fun `HeroIds can be created empty`() {
        val heroIds = HeroIds(emptySet())

        assertNotNull(heroIds)
    }

    @Test
    fun `HeroIds can be created with one element`() {
        val heroIds = HeroIds(TestUtils.createHeroIdOrThrow("id"))

        assertNotNull(heroIds)
    }

}
