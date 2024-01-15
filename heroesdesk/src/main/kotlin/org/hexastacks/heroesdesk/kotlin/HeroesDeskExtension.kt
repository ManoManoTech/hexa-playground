package org.hexastacks.heroesdesk.kotlin

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

object HeroesDeskExtension {

    fun HeroesDesk.assignableHeroes(id: TaskId): EitherNel<HeroesDesk.GetTaskError, Heroes> =
        this.getTask(id).map { it.scope.assignees }
}