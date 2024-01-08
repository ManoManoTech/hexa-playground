package org.hexastacks.heroesdesk.kotlin

import org.hexastacks.heroesdesk.kotlin.impl.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.ports.FakeHeroRepository
import org.hexastacks.heroesdesk.kotlin.ports.InMemoryTaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedHeroRepository

class HeroesDeskImplTest : AbstractHeroesDeskTest() {

    override fun heroesDesk(instrumentedUserRepository: InstrumentedHeroRepository): HeroesDesk =
        HeroesDeskImpl(instrumentedUserRepository, InMemoryTaskRepository())

    override fun instrumentedHeroRepository(): InstrumentedHeroRepository = FakeHeroRepository()

    override fun nonExistingHeroId(): HeroId = FakeHeroRepository.NON_EXISTING_USER_ID
    override fun nonExistingRawTaskId(): String = InMemoryTaskRepository.NON_EXISTING_TASK_ID


}