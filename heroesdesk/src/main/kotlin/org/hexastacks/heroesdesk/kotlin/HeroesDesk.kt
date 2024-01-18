package org.hexastacks.heroesdesk.kotlin

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.ErrorMessage
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.ports.HeroesDoNotExistError

interface HeroesDesk {

    fun createScope(scopeKey: ScopeKey, name: Name, creator: AdminId): EitherNel<CreateScopeError, Scope>
    fun assignScope(
        scopeKey: ScopeKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnScopeError, Scope>

    fun updateScopeName(scopeKey: ScopeKey, name: Name, changeAuthor: AdminId): EitherNel<UpdateScopeNameError, Scope>
    fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope>

    fun createTask(scopeKey: ScopeKey, title: Title, creator: HeroId): EitherNel<CreateTaskError, PendingTask>
    fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>>

    fun updateTitle(id: TaskId, title: Title, author: HeroId): EitherNel<UpdateTitleError, Task<*>>
    fun updateDescription(
        id: TaskId, description: Description, author: HeroId
    ): EitherNel<UpdateDescriptionError, Task<*>>

    fun assignTask(id: PendingTaskId, assignees: HeroIds, author: HeroId): EitherNel<AssignTaskError, Task<*>>
    fun assignTask(id: InProgressTaskId, assignees: HeroIds, author: HeroId): EitherNel<AssignTaskError, Task<*>>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(id: PendingTaskId, author: HeroId): EitherNel<StartWorkError, InProgressTask>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(id: DoneTaskId, author: HeroId): EitherNel<StartWorkError, InProgressTask>

    fun pauseWork(id: InProgressTaskId, author: HeroId): EitherNel<PauseWorkError, PendingTask>
    fun pauseWork(id: DoneTaskId, author: HeroId): EitherNel<PauseWorkError, PendingTask>

    /**
     * Clears assignees
     */
    fun endWork(id: PendingTaskId, author: HeroId): EitherNel<EndWorkError, DoneTask>

    /**
     * Clears assignees
     */
    fun endWork(id: InProgressTaskId, author: HeroId): EitherNel<EndWorkError, DoneTask>

    sealed interface HeroesDeskError : ErrorMessage

    sealed interface CreateScopeError : HeroesDeskError

    data class ScopeNameAlreadyExistsError(val name: Name) : CreateScopeError {
        override val message = "Scope $name already exists"
    }

    data class ScopeKeyAlreadyExistsError(val id: ScopeKey) : CreateScopeError {
        override val message = "Scope $id already exists"
    }

    data class AdminDoesNotExistCreateScopeError(val adminId: AdminId) : CreateScopeError {
        override val message = "Admin $adminId does not exist"
    }

    sealed interface AssignHeroesOnScopeError : HeroesDeskError

    data class ScopeDoesNotExistAssignHeroesOnScopeError(val scopeKey: ScopeKey) : AssignHeroesOnScopeError {
        override val message = "Scope $scopeKey does not exist"
    }

    data class AssignedHeroIdsNotExistAssignHeroesOnScopeError(val missingAssignees: HeroIds, val assignees: HeroIds) :
        AssignHeroesOnScopeError {
        override val message = "Hero ids $missingAssignees not existing (out of ${assignees.size()} assignees"
    }

    data class AdminIdNotExistingAssignHeroesOnScopeError(val adminId: AdminId) : AssignHeroesOnScopeError {
        override val message = "Admin id $adminId not existing"
    }

    sealed interface UpdateScopeNameError : HeroesDeskError

    data class AdminIdNotExistingUpdateScopeNameError(val adminId: AdminId) : UpdateScopeNameError {
        override val message = "Admin id $adminId not existing"
    }

    data class ScopeNotExistingUpdateScopeNameError(val scopeKey: ScopeKey) : UpdateScopeNameError {
        override val message = "Scope $scopeKey not existing"
    }

    sealed interface GetScopeError : HeroesDeskError

    data class ScopeNotExistingGetScopeError(val scopeKey: ScopeKey) : GetScopeError {
        override val message = "Scope $scopeKey not existing"
    }

    sealed interface CreateTaskError : HeroesDeskError

    data class HeroDoesNotExistCreateTaskError(val heroId: HeroId) : CreateTaskError {
        override val message = "Hero $heroId does not exist"
    }

    data class ScopeNotExistCreateTaskError(val scopeKey: ScopeKey) : CreateTaskError {
        override val message = "Scope $scopeKey does not exist"
    }

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

    data class TaskDoesNotExistEndWorkError(val taskId: TaskId) : EndWorkError {
        override val message = "Task $taskId does not exist"
    }

    data class TaskNotInProgressEndWorkError(val task: Task<*>, val taskId: InProgressTaskId) : EndWorkError {
        override val message = "Task $task not a pending one, despite being $taskId"
    }

    data class HeroesDoesNotExistEndWorkError(val heroIds: HeroIds) : EndWorkError {
        override val message: String = "Heroes $heroIds do not exist"
    }

    data class HeroNotAssignedToScopeEndWorkError(val heroId: HeroId, val scopeKey: ScopeKey) : EndWorkError {
        override val message = "Hero $heroId not assigned to scope $scopeKey"
    }

    sealed interface PauseWorkError : HeroesDeskError

    data class TaskDoesNotExistPauseWorkError(val taskId: TaskId) : PauseWorkError {
        override val message = "Task $taskId does not exist"
    }

    data class TaskNotInProgressPauseWorkError(val task: Task<*>, val taskId: InProgressTaskId) : PauseWorkError {
        override val message = "Task $task not a pending one, despite being $taskId"
    }

    data class HeroesDoesNotExistPauseWorkError(val heroIds: HeroIds) : PauseWorkError {
        override val message: String = "Heroes $heroIds do not exist"
    }

    data class HeroNotAssignedToScopePauseWorkError(val heroId: HeroId, val scopeKey: ScopeKey) : PauseWorkError {
        override val message = "Hero $heroId not assigned to scope $scopeKey"
    }

    sealed interface StartWorkError : HeroesDeskError

    data class TaskDoesNotExistStartWorkError(val taskId: TaskId) : StartWorkError {
        override val message = "Task $taskId does not exist"
    }

    data class TaskNotPendingStartWorkError(val task: Task<*>, val taskId: PendingTaskId) : StartWorkError {
        override val message = "Task $task not a pending one, despite being $taskId"
    }

    data class HeroNotAssignedToScopeStartWorkError(val heroId: HeroId, val scopeKey: ScopeKey) : StartWorkError {
        override val message = "Hero $heroId not assigned to scope $scopeKey"
    }

    data class HeroesDoesNotExistStartWorkError(val heroIds: HeroIds) : StartWorkError {
        override val message: String = "Heroes $heroIds do not exist"
    }

    sealed interface AssignTaskError : HeroesDeskError
    data class HeroesDoesNotExistAssignTaskError(
        val heroesDoNotExistError: HeroesDoNotExistError
    ) : AssignTaskError {
        override val message: String = heroesDoNotExistError.message
    }

    data class TaskDoesNotExistAssignTaskError(val taskId: TaskId) : AssignTaskError {
        override val message = "Task $taskId does not exist"
    }
}