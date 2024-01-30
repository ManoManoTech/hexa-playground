package org.hexastacks.heroesdesk.kotlin.errors

import org.hexastacks.heroesdesk.kotlin.impl.ErrorMessage
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.InProgressTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.Task
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

sealed interface HeroesDeskError : ErrorMessage

sealed interface CreateScopeError : HeroesDeskError
sealed interface AreHeroesInScopeError : HeroesDeskError, CreateTaskError, AssignTaskError, PauseWorkError, EndWorkError,UpdateTitleError

data class ScopeNameAlreadyExistingError(val name: Name) : CreateScopeError {
    override val message = "Scope $name already exists"
}

data class ScopeKeyAlreadyExistingError(val id: ScopeKey) : CreateScopeError {
    override val message = "Scope $id already exists"
}

sealed interface AssignHeroesOnScopeError : HeroesDeskError

data class ScopeNotExistingError(val scopeKey: ScopeKey) : GetScopeError, GetScopeMembersError {
    override val message = "Scope $scopeKey does not exist"
}

sealed interface UpdateScopeNameError : HeroesDeskError

sealed interface GetScopeError : HeroesDeskError, AssignHeroesOnScopeError, UpdateScopeNameError, CreateTaskError,
    AreHeroesInScopeError

sealed interface GetScopeMembersError : HeroesDeskError

sealed interface CreateTaskError : HeroesDeskError

data class HeroesNotInScopeError(val heroIds: HeroIds, val scopeKey: ScopeKey) : CreateTaskError, EndWorkError,
    PauseWorkError, StartWorkError, AssignTaskError, AreHeroesInScopeError {
    constructor(heroId: HeroId, scopeKey: ScopeKey) : this(HeroIds(heroId), scopeKey)
    constructor(heroes: Heroes, scopeKey: ScopeKey) : this(HeroIds(heroes), scopeKey)

    override val message = "${heroIds} not in $scopeKey scope"
}

sealed interface GetTaskError : HeroesDeskError, AssignTaskError, PauseWorkError, EndWorkError, UpdateTitleError,
    UpdateDescriptionError, AreHeroesInScopeError, StartWorkError

data class TaskNotExistingError(val taskId: TaskId) : GetTaskError {
    override val message = "Task $taskId does not exist"
}

sealed interface UpdateTitleError : HeroesDeskError

sealed interface UpdateDescriptionError : HeroesDeskError

sealed interface EndWorkError : HeroesDeskError

data class TaskNotInProgressError(val task: Task<*>, val taskId: InProgressTaskId) : EndWorkError, PauseWorkError {
    override val message = "Task $task not a pending one, despite being $taskId"
}

sealed interface PauseWorkError : HeroesDeskError

sealed interface StartWorkError : HeroesDeskError

data class TaskNotPendingError(val task: Task<*>, val taskId: PendingTaskId) : StartWorkError {
    override val message = "Task $task not a pending one, despite being $taskId"
}

sealed interface AssignTaskError : HeroesDeskError, StartWorkError

sealed interface UserRepositoryError : HeroesDeskError
sealed interface GetHeroError : UserRepositoryError, AssignHeroesOnScopeError, CreateTaskError, UpdateTitleError,
    UpdateDescriptionError, AssignTaskError, PauseWorkError, EndWorkError

data class HeroesNotExistingError(val heroIds: HeroIds) : GetHeroError {
    override val message = "Heroes $heroIds do not exist"
}

sealed interface GetAdminError : UserRepositoryError, UpdateScopeNameError, CreateScopeError, AssignHeroesOnScopeError

data class AdminNotExistingError(val adminId: AdminId) : GetAdminError {
    override val message = "Admin $adminId does not exist"
}

data class TaskRepositoryError(
    override val message: String,
    val exception: Exception? = null,
    val error: ErrorMessage? = null
) : HeroesDeskError,
    CreateScopeError, GetScopeError, UpdateScopeNameError, GetScopeMembersError, CreateTaskError, GetTaskError {
    constructor(exception: Exception) : this(exception.message ?: "Unknown error", exception)
    constructor(error: ErrorMessage) : this(error.message, error = error)
}