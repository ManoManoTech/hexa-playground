package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.misc.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest
import org.hexastacks.heroesdesk.kotlin.test.InstrumentedUserRepository

class HeroesDeskImplTest : AbstractHeroesDeskTest() {

    override fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk =
        HeroesDeskImpl(userRepo, InMemoryMissionRepository())

    override fun instrumentedUserRepository(): InstrumentedUserRepository = InstrumentedInMemoryUserRepository()

    override fun nonExistingRawMissionId(): String = InMemoryMissionRepository.NON_EXISTING_MISSION_ID

}