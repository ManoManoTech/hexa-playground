package org.hexastacks.heroesdesk.kotlin.test

import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createAdminOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.user.Admin
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.ports.UserRepository

interface InstrumentedUserRepository : UserRepository {

    fun ensureExisting(hero: Hero): Hero
    fun ensureAdminExistingOrThrow(adminId: String): Admin = ensureExisting(createAdminOrThrow(adminId))

    fun ensureExisting(admin: Admin): Admin

    fun ensureHeroExistingOrThrow(rawHeroId: String): Hero = ensureExisting(createHeroOrThrow(rawHeroId))
}
