package org.hexastacks.heroesdesk.kotlin.ports.pgjooq

import DbAccess
import DbAccess.createDslContext
import dbDropAndInit
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.misc.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InstrumentedInMemoryUserRepository
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest
import org.hexastacks.heroesdesk.kotlin.test.InstrumentedUserRepository
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
class PgJooqMissionRepositoryTest : AbstractHeroesDeskTest() {

    companion object {
        @Container
        private val postgreSQLContainer = DbAccess.postgreSQLContainer()
        lateinit var dslContext: DSLContext

        @JvmStatic
        @BeforeAll
        fun initContainer(): Unit {
            println("Starting container...")
            postgreSQLContainer.start()
            println("Creating dslContext on ${postgreSQLContainer.jdbcUrl}...")
            dslContext = createDslContext(postgreSQLContainer)
        }
    }

    override fun instrumentedUserRepository(): InstrumentedUserRepository = InstrumentedInMemoryUserRepository()

    override fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk {
        println("Db init running")
        dbDropAndInit(dslContext)
        println("Db init done")
        return HeroesDeskImpl(userRepo, PgJooqMissionRepository(dslContext))
    }

    override fun nonExistingRawMissionId(): String = Int.MAX_VALUE.toString()
}