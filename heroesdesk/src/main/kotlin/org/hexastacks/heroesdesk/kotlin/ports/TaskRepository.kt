package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.Either
import arrow.core.NonEmptyList
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.Description
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.Title
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTask
import org.hexastacks.heroesdesk.kotlin.impl.task.Task
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

interface TaskRepository {
    fun createTask(title: Title, hero: Hero): Either<NonEmptyList<CreateTaskError>, PendingTask>
    fun getTask(id: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>>
    fun updateTitle(
        id: TaskId,
        title: Title,
        hero: Hero
    ): Either<NonEmptyList<UpdateTitleError>, TaskId>

    fun updateDescription(
        id: TaskId,
        description: Description,
        hero: Hero
    ): Either<NonEmptyList<UpdateDescriptionError>, TaskId>

}
