package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.task.*

interface TaskRepository {

    fun createTask(title: Title, hero: Hero): EitherNel<CreateTaskError, PendingTask>

    fun getTask(taskId: TaskId): EitherNel<GetTaskError, Task<*>>

    fun updateTitle(
        taskId: TaskId,
        title: Title,
        hero: Hero
    ): EitherNel<UpdateTitleError, TaskId>

    fun updateDescription(
        taskId: TaskId,
        description: Description,
        hero: Hero
    ): EitherNel<UpdateDescriptionError, TaskId>

    fun assign(
        taskId: TaskId,
        assignees: Heroes,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>>

    fun startWork(pendingTaskId: PendingTaskId, hero: Hero): EitherNel<StartWorkError, InProgressTask>

}
