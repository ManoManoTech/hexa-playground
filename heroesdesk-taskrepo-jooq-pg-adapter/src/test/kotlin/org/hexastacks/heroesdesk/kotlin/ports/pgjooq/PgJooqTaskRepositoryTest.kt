package org.hexastacks.heroesdesk.kotlin.ports.pgjooq

import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.impl.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InstrumentedInMemoryUserRepository
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest
import org.hexastacks.heroesdesk.kotlin.test.InstrumentedUserRepository

class PgJooqTaskRepositoryTest : AbstractHeroesDeskTest() {
    override fun instrumentedUserRepository(): InstrumentedUserRepository = InstrumentedInMemoryUserRepository()

    override fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk = HeroesDeskImpl(userRepo, PgJooqTaskRepository())

    override fun nonExistingRawTaskId(): String = Int.MAX_VALUE.toString()
}