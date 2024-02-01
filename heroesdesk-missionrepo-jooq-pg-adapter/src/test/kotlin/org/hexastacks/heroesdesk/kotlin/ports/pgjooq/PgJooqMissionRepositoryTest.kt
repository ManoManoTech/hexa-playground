package org.hexastacks.heroesdesk.kotlin.ports.pgjooq

import DbAccess
import DbAccess.createDslContext
import dbDropAndInit
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.misc.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InstrumentedInMemoryUserRepository
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
class PgJooqMissionRepositoryTest : AbstractHeroesDeskTest() {

    private var userRepository: InstrumentedInMemoryUserRepository? = null

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

    override fun createHeroesDesk(): HeroesDesk {
        println("Db init running")
        dbDropAndInit(dslContext)
        println("Db init done")
        userRepository = InstrumentedInMemoryUserRepository()
        return HeroesDeskImpl(userRepository!!, PgJooqMissionRepository(dslContext))
    }

    override fun ensureAdminExistingOrThrow(id: String): AdminId = userRepository!!.ensureAdminExistingOrThrow(id)

    override fun ensureHeroExistingOrThrow(id: String): HeroId = userRepository!!.ensureHeroExistingOrThrow(id)

}