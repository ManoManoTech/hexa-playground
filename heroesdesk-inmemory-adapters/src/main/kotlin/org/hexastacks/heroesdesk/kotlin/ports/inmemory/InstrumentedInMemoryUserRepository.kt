package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import org.hexastacks.heroesdesk.kotlin.impl.user.Admin
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.test.InstrumentedUserRepository

class InstrumentedInMemoryUserRepository : InMemoryUserRepository(), InstrumentedUserRepository {

    override fun ensureExisting(hero: Hero): Hero {
        users.add(hero)
        return hero
    }

    override fun ensureExisting(admin: Admin): Admin {
        users.add(admin)
        return admin
    }
}