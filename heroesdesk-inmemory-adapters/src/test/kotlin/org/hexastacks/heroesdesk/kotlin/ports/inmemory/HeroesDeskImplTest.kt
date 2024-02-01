package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.misc.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest
import org.hexastacks.heroesdesk.kotlin.user.*

class HeroesDeskImplTest : AbstractHeroesDeskTest() {

    private var userRepo: InstrumentedInMemoryUserRepository? = null

    override fun createHeroesDesk(): HeroesDesk {
        userRepo = InstrumentedInMemoryUserRepository()
        return HeroesDeskImpl(userRepo!!, InMemoryMissionRepository())
    }

    override fun ensureAdminExistingOrThrow(id: String): AdminId =
        userRepo!!.ensureAdminExistingOrThrow(id)

    override fun ensureHeroExistingOrThrow(id: String): HeroId  =
        userRepo!!.ensureHeroExistingOrThrow(id)
}