package org.hexastacks.heroesdesk.kotlin.ports

import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.user.Admin
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

interface InstrumentedHeroRepository : HeroRepository {

    /**
     * @param taskId: the task the heroes can be assigned to, no check is done on the existence of the task
     * @param heroesToCreateIfNeeded: the heroes to assign, creating them on the way if needed
     *
     */
    fun defineAssignableHeroes(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes

    fun defineHeroesAbleToChangeStatus(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes

    fun ensureExisting(heroes: Heroes): Heroes

    fun ensureExisting(hero: Hero): Hero
    fun ensureExisting(admin: AdminId): Admin

    fun ensureExistingOrThrow(rawHeroId: String): Hero = ensureExisting(createHeroOrThrow(rawHeroId))
}
