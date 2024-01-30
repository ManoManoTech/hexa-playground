package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

interface TaskRepository {
    fun createScope(scopeKey: ScopeKey, name: Name): EitherNel<CreateScopeError, Scope>

    fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope>
    fun getScopeMembers(scopeKey: ScopeKey): EitherNel<GetScopeMembersError, ScopeMembers>

    fun updateScopeName(
        scopeKey: ScopeKey,
        name: Name
    ): EitherNel<UpdateScopeNameError, Scope>

    fun assignScope(
        scopeKey: ScopeKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnScopeError, ScopeMembers>

    fun areHeroesInScope(heroIds: HeroIds, scopeKey: ScopeKey): EitherNel<AreHeroesInScopeError, ScopeMembers>
    fun areHeroesInScope(heroIds: HeroIds, taskId: TaskId): EitherNel<AreHeroesInScopeError, Task<*>> =
        either {
            val task = getTask(taskId).bind()
            areHeroesInScope(heroIds, task.scopeKey()).bind()
            task
        }

    fun createTask(scopeKey: ScopeKey, title: Title): EitherNel<CreateTaskError, PendingTask>

    fun getTask(taskId: TaskId): EitherNel<GetTaskError, Task<*>>

    fun updateTitle(
        taskId: TaskId,
        title: Title
    ): EitherNel<UpdateTitleError, Task<*>>

    fun updateDescription(
        taskId: TaskId,
        description: Description
    ): EitherNel<UpdateDescriptionError, Task<*>>

    fun assignTask(
        taskId: TaskId,
        assignees: HeroIds
    ): EitherNel<AssignTaskError, Task<*>>

    fun startWork(pendingTaskId: PendingTaskId): EitherNel<StartWorkError, InProgressTask>

    fun pauseWork(inProgressTaskId: InProgressTaskId): EitherNel<PauseWorkError, PendingTask>

    fun endWork(inProgressTaskId: InProgressTaskId): EitherNel<EndWorkError, DoneTask>


}
