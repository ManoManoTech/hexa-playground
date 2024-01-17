package org.hexastacks.heroesdesk.kotlin.ports

import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createAdminOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.Admin
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

interface InstrumentedUserRepository : UserRepository {

    fun ensureExisting(heroes: Heroes): Heroes

    fun ensureExisting(hero: Hero): Hero
    fun ensureAdminExistingOrThrow(adminId: String): Admin = ensureExisting(createAdminOrThrow(adminId))

    fun ensureExisting(admin: Admin): Admin

    fun ensureHeroExistingOrThrow(rawHeroId: String): Hero = ensureExisting(createHeroOrThrow(rawHeroId))
}
