package org.hexastacks.heroesdesk.kotlin

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

interface HeroesDesk {

    fun createScope(scopeKey: ScopeKey, name: Name, creator: AdminId): EitherNel<CreateScopeError, Scope>
    fun assignScope(scopeKey: ScopeKey, assignees: HeroIds, changeAuthor: AdminId): EitherNel<AssignHeroesOnScopeError, Scope>

    // fun updateScopeName(scopeId: ScopeId, name:Name, changeAuthor: AdminId): EitherNel<UpdateScopeNameError, Scope>
//    fun getScope(id: ScopeId): EitherNel<GetScopeError, Scope>

    fun createTask(title: Title, creator: HeroId): EitherNel<CreateTaskError, PendingTask>
    fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>>

    fun updateTitle(id: TaskId, title: Title, author: HeroId): EitherNel<UpdateTitleError, TaskId>  // TODO: handle some history and use author here, apply to other methods here too
    fun updateDescription(
        id: TaskId, description: Description, author: HeroId
    ): EitherNel<UpdateDescriptionError, TaskId>

    fun assignTask(id: TaskId, assignees: HeroIds, author: HeroId): EitherNel<AssignTaskError, Task<*>>
    fun assignableHeroes(id: TaskId): EitherNel<AssignableHeroesError, Heroes>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(
        id: PendingTaskId, // TODO: add heroStartingWork
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTask>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(id: DoneTaskId, author: HeroId): EitherNel<StartWorkError, InProgressTaskId>
    fun pauseWork(id: InProgressTaskId, author: HeroId): EitherNel<StopWorkError, PendingTaskId>
    fun pauseWork(id: DoneTaskId, author: HeroId): EitherNel<StopWorkError, PendingTaskId>
    fun endWork(id: PendingTaskId, author: HeroId): EitherNel<EndWorkError, DoneTaskId>
    fun endWork(id: InProgressTaskId, author: HeroId): EitherNel<EndWorkError, DoneTaskId>

    sealed interface HeroesDeskError {
        val message: String
    }

    sealed interface CreateScopeError: HeroesDeskError

    data class ScopeNameAlreadyExistsError(val name: Name) : CreateScopeError {
        override val message = "Scope $name already exists"
    }

    data class ScopeIdAlreadyExistsError(val id: ScopeKey) : CreateScopeError {
        override val message = "Scope $id already exists"
    }

    data class  AdminDoesNotExistCreateScopeError(val adminId: AdminId) : CreateScopeError {
        override val message = "Admin $adminId does not exist"
    }

    sealed interface AssignHeroesOnScopeError: HeroesDeskError

    data class ScopeDoesNotExistAssignHeroesOnScopeError(val scopeKey: ScopeKey) : AssignHeroesOnScopeError {
        override val message = "Scope $scopeKey does not exist"
    }

    sealed interface CreateTaskError : HeroesDeskError

    data class HeroDoesNotExistCreateTaskError(val heroId: HeroId) : CreateTaskError {
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

    sealed interface EndWorkError : HeroesDeskError
    sealed interface StopWorkError : HeroesDeskError
    sealed interface StartWorkError : HeroesDeskError

    data class TaskDoesNotExistStartWorkError(val taskId: TaskId) : StartWorkError {
        override val message = "Task $taskId does not exist"
    }

    data class TaskNotPendingStartWorkError(val task: Task<*>, val taskId: PendingTaskId) : StartWorkError {
        override val message = "Task $task not a pending one, despite being $taskId"
    }

    data class InvalidTaskIdStartWorkError(val taskId: PendingTaskId, val error: TaskId.TaskIdError) : StartWorkError {
        override val message = "Task id $taskId invalid: ${error.message}"
    }

    data class HeroDoesNotExistStartWorkError(val heroId: HeroId) : StartWorkError {
        override val message = "Hero $heroId does not exist"
    }

    data class NonAllowedToStartWorkError(
        val task: TaskId,
        val nonAssignables: HeroIds
    ) : StartWorkError {
        override val message: String = "Task $task cannot be assigned to ${nonAssignables.value}"
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