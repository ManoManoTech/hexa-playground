package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.task.*

interface TaskRepository {
    fun createTask(title: Title, hero: Hero): EitherNel<CreateTaskError, PendingTask>
    fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>>
    fun updateTitle(
        id: TaskId,
        title: Title,
        hero: Hero
    ): EitherNel<UpdateTitleError, TaskId>

    fun updateDescription(
        id: TaskId,
        description: Description,
        hero: Hero
    ): EitherNel<UpdateDescriptionError, TaskId>

    fun assign(
        id: TaskId,
        assignees: Heroes,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>>

    fun startWork(id: PendingTaskId, hero: Hero): EitherNel<StartWorkError, InProgressTask>

}
