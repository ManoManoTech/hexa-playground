package org.hexastacks.heroesdesk.kotlin.ports

import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createAdminOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.Admin
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

interface InstrumentedUserRepository : UserRepository {

    /**
     * @param taskId: the task the heroes can be assigned to, no check is done on the existence of the task
     * @param heroesToCreateIfNeeded: the heroes to assign, creating them on the way if needed
     *
     */
    fun defineAssignableHeroes(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes

    fun defineHeroesAbleToChangeStatus(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes

    fun ensureExisting(heroes: Heroes): Heroes

    fun ensureExisting(hero: Hero): Hero
    fun ensureAdminExistingOrThrow(adminId: String): Admin = ensureExisting(createAdminOrThrow(adminId))

    fun ensureExisting(admin: Admin): Admin

    fun ensureHeroExistingOrThrow(rawHeroId: String): Hero = ensureExisting(createHeroOrThrow(rawHeroId))
}
