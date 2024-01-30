package org.hexastacks.heroesdesk.kotlin.errors

import org.hexastacks.heroesdesk.kotlin.impl.ErrorMessage
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.task.InProgressTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.Task
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

sealed interface HeroesDeskError : ErrorMessage

sealed interface CreateSquadError : HeroesDeskError
sealed interface AreHeroesInSquadError : HeroesDeskError, CreateTaskError, AssignTaskError, PauseWorkError, EndWorkError,UpdateTitleError

data class SquadNameAlreadyExistingError(val name: Name) : CreateSquadError {
    override val message = "Squad $name already exists"
}

data class SquadKeyAlreadyExistingError(val id: SquadKey) : CreateSquadError {
    override val message = "Squad $id already exists"
}

sealed interface AssignHeroesOnSquadError : HeroesDeskError

data class SquadNotExistingError(val squadKey: SquadKey) : GetSquadError, GetSquadMembersError {
    override val message = "Squad $squadKey does not exist"
}

sealed interface UpdateSquadNameError : HeroesDeskError

sealed interface GetSquadError : HeroesDeskError, AssignHeroesOnSquadError, UpdateSquadNameError, CreateTaskError,
    AreHeroesInSquadError

sealed interface GetSquadMembersError : HeroesDeskError

sealed interface CreateTaskError : HeroesDeskError

data class HeroesNotInSquadError(val heroIds: HeroIds, val squadKey: SquadKey) : CreateTaskError, EndWorkError,
    PauseWorkError, StartWorkError, AssignTaskError, AreHeroesInSquadError {
    constructor(heroId: HeroId, squadKey: SquadKey) : this(HeroIds(heroId), squadKey)
    constructor(heroes: Heroes, squadKey: SquadKey) : this(HeroIds(heroes), squadKey)

    override val message = "${heroIds} not in $squadKey squad"
}

sealed interface GetTaskError : HeroesDeskError, AssignTaskError, PauseWorkError, EndWorkError, UpdateTitleError,
    UpdateDescriptionError, AreHeroesInSquadError, StartWorkError

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
sealed interface GetHeroError : UserRepositoryError, AssignHeroesOnSquadError, CreateTaskError, UpdateTitleError,
    UpdateDescriptionError, AssignTaskError, PauseWorkError, EndWorkError

data class HeroesNotExistingError(val heroIds: HeroIds) : GetHeroError {
    override val message = "Heroes $heroIds do not exist"
}

sealed interface GetAdminError : UserRepositoryError, UpdateSquadNameError, CreateSquadError, AssignHeroesOnSquadError

data class AdminNotExistingError(val adminId: AdminId) : GetAdminError {
    override val message = "Admin $adminId does not exist"
}

data class TaskRepositoryError(
    override val message: String,
    val exception: Exception? = null,
    val error: ErrorMessage? = null
) : HeroesDeskError,
    CreateSquadError, GetSquadError, UpdateSquadNameError, GetSquadMembersError, CreateTaskError, GetTaskError {
    constructor(exception: Exception) : this(exception.message ?: "Unknown error", exception)
    constructor(error: ErrorMessage) : this(error.message, error = error)
}