package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTask
import org.hexastacks.heroesdesk.kotlin.impl.task.Task
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

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

}
