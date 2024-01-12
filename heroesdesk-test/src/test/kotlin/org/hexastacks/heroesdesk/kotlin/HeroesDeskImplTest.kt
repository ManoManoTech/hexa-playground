package org.hexastacks.heroesdesk.kotlin

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.ports.FakeUserRepository
import org.hexastacks.heroesdesk.kotlin.ports.InMemoryTaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedUserRepository

class HeroesDeskImplTest : AbstractHeroesDeskTest() {

    override fun createHeroesDesk(instrumentedUserRepository: InstrumentedUserRepository): HeroesDesk =
        HeroesDeskImpl(instrumentedUserRepository, InMemoryTaskRepository())

    override fun instrumentedHeroRepository(): InstrumentedUserRepository = FakeUserRepository()

    override fun nonExistingRawTaskId(): String = InMemoryTaskRepository.NON_EXISTING_TASK_ID

}