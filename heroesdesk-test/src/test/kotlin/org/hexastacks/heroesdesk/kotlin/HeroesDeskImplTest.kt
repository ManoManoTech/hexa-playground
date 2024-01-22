package org.hexastacks.heroesdesk.kotlin

import org.hexastacks.heroesdesk.kotlin.adapters.InstrumentedUserRepository
import org.hexastacks.heroesdesk.kotlin.impl.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InMemoryTaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InMemoryUserRepository
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest

class HeroesDeskImplTest : AbstractHeroesDeskTest() {

    override fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk =
        HeroesDeskImpl(userRepo, InMemoryTaskRepository())

    override fun instrumentedHeroRepository(): InstrumentedUserRepository = InMemoryUserRepository()

    override fun nonExistingRawTaskId(): String = InMemoryTaskRepository.NON_EXISTING_TASK_ID

}