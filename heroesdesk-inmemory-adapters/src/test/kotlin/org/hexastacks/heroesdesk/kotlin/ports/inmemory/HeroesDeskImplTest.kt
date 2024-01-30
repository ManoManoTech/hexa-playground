package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.impl.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest
import org.hexastacks.heroesdesk.kotlin.test.InstrumentedUserRepository

class HeroesDeskImplTest : AbstractHeroesDeskTest() {

    override fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk =
        HeroesDeskImpl(userRepo, InMemoryTaskRepository())

    override fun instrumentedUserRepository(): InstrumentedUserRepository = InstrumentedInMemoryUserRepository()

    override fun nonExistingRawTaskId(): String = InMemoryTaskRepository.NON_EXISTING_TASK_ID

}