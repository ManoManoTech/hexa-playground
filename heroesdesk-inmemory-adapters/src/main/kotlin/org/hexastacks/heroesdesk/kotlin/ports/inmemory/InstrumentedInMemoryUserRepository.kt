package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.user.*

class InstrumentedInMemoryUserRepository : InMemoryUserRepository() {

    fun ensureAdminExistingOrThrow(id: String): AdminId {
        val adminId = AdminId(id).getOrElse { throw IllegalArgumentException("Invalid admin id: $it") }
        val name = UserName(id).getOrElse {
            throw IllegalArgumentException("Invalid user name: $it")
        }
        addUser(Admin(adminId, name))
        return Admin(adminId, name).id
    }

    fun ensureHeroExistingOrThrow(id: String): HeroId {
        val heroId = HeroId(id).getOrElse { throw IllegalArgumentException("Invalid hero id: $it") }
        val name = UserName(id).getOrElse {
            throw IllegalArgumentException("Invalid user name: $it")
        }
        addUser(Hero(heroId, name))
        return Hero(heroId, name).id
    }
}