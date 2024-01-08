package org.hexastacks.heroesdesk.kotlin

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.task.*

interface HeroesDesk {
    fun currentHero(): EitherNel<CurrentHeroError, HeroId> // TODO: switch to Hero

    fun createTask(title: Title, creator: HeroId): EitherNel<CreateTaskError, PendingTask>

    fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>>

    fun updateTitle(id: TaskId, title: Title, author: HeroId): EitherNel<UpdateTitleError, TaskId>
    fun updateDescription(
        id: TaskId, description: Description, author: HeroId
    ): EitherNel<UpdateDescriptionError, TaskId>

    fun assignableHeroes(id: TaskId): EitherNel<AssignableHeroesError, Heroes>
    fun assignTask(id: TaskId, assignees: HeroIds, author: HeroId): EitherNel<AssignTaskError, Task<*>>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(id: PendingTaskId, author: HeroId): EitherNel<StartWorkError, InProgressTaskId>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(id: DoneTaskId, author: HeroId): EitherNel<StartWorkError, InProgressTaskId>
    fun pauseWork(id: InProgressTaskId, author: HeroId): EitherNel<StopWorkError, PendingTaskId>
    fun pauseWork(id: DoneTaskId, author: HeroId): EitherNel<StopWorkError, PendingTaskId>
    fun endWork(id: PendingTaskId, author: HeroId): EitherNel<EndWorkError, DoneTaskId>
    fun endWork(id: InProgressTaskId, author: HeroId): EitherNel<EndWorkError, DoneTaskId>

    fun delete(id: TaskId, author: HeroId): EitherNel<DeleteTaskError, DeletedTaskId>
    fun restore(id: DeletedTaskId, author: HeroId): EitherNel<RestoreTaskError, TaskId>


    sealed interface HeroesDeskError {
        val message: String
    }

    sealed interface CurrentHeroError :
        HeroesDeskError // TODO: consider (& test !) adding Action*RepoError(msg, exp) to all error parents

    sealed interface CreateTaskError : HeroesDeskError

    data class HeroDoesNotExistError(val heroId: HeroId) : CreateTaskError {
        override val message = "Hero $heroId does not exist"
    }

    data class TaskRepositoryHeroDoesNotExistError(override val message: String, val exception: Exception? = null) :
        CreateTaskError

    sealed interface GetTaskError : HeroesDeskError

    data class TaskDoesNotExistError(val taskId: TaskId) : GetTaskError {
        override val message = "Task $taskId does not exist"
    }

    sealed interface UpdateTitleError : HeroesDeskError

    data class HeroDoesNotExistUpdateTitleError(val heroId: HeroId) : UpdateTitleError {
        override val message = "Hero $heroId does not exist"
    }

    data class TaskDoesNotExistUpdateTitleError(val taskId: TaskId) : UpdateTitleError {
        override val message = "Task $taskId does not exist"
    }

    sealed interface UpdateDescriptionError : HeroesDeskError

    data class HeroDoesNotExistUpdateDescriptionError(val heroId: HeroId) : UpdateDescriptionError {
        override val message = "Hero $heroId does not exist"
    }

    data class TaskDoesNotExistUpdateDescriptionError(val taskId: TaskId) : UpdateDescriptionError {
        override val message = "Task $taskId does not exist"
    }

    sealed interface RestoreTaskError : HeroesDeskError
    sealed interface DeleteTaskError : HeroesDeskError
    sealed interface EndWorkError : HeroesDeskError
    sealed interface StopWorkError : HeroesDeskError
    sealed interface StartWorkError : HeroesDeskError

    data class TaskDoesNotExistStartWorkError(val taskId: TaskId) : StartWorkError {
        override val message = "Task $taskId does not exist"
    }

    sealed interface AssignTaskError : HeroesDeskError
    data class NonAssignableHeroesAssignTaskError(
        val task: TaskId,
        val candidates: HeroIds,
        val nonAssignables: HeroIds
    ) : AssignTaskError {
        override val message: String = "Task $task cannot be assigned to ${nonAssignables.value}"
    }

    data class TaskDoesNotExistAssignTaskError(val taskId: TaskId) : AssignTaskError {
        override val message = "Task $taskId does not exist"
    }
    sealed interface AssignableHeroesError : HeroesDeskError

    data class TaskDoesNotExistAssignableHeroesError(val taskId: TaskId) : AssignableHeroesError {
        override val message = "Task $taskId does not exist"
    }
}