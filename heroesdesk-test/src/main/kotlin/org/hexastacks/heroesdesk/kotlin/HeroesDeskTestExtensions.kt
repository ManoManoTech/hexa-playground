package org.hexastacks.heroesdesk.kotlin

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTask
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.Task
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

object HeroesDeskTestExtensions {

    fun createTitleOrThrow(title: String) = Title(title).getOrElse { throw AssertionError() }

    fun createDescriptionOrThrow(description: String) = Description(description).getOrElse { throw AssertionError() }

    fun createHeroNameOrThrow(name: String) = HeroName(name).getOrElse { throw AssertionError() }

    fun createHeroIdOrThrow(id: String) = HeroId(id).getOrElse { throw AssertionError() }

    fun createHeroOrThrow(id: String) = Hero(createHeroNameOrThrow(id), createHeroIdOrThrow(id))

    fun createPendingTaskIdOrThrow(id: String) = PendingTaskId(id).getOrElse { throw AssertionError() }
    fun HeroesDesk.currentHeroOrThrow() = this.currentHero().getOrElse { throw AssertionError() }

    fun HeroesDesk.createTaskOrThrow(title: String): PendingTask =
        this.createTask(createTitleOrThrow(title), currentHeroOrThrow()).getOrElse { throw AssertionError() }

    fun HeroesDesk.getTaskOrThrow(id: TaskId): Task<*> = this.getTask(id).getOrElse { throw AssertionError() }
}