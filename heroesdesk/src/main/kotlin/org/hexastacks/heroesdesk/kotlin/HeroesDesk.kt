package org.hexastacks.heroesdesk.kotlin

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

interface HeroesDesk {

    fun createScope(scopeKey: ScopeKey, name: Name, creator: AdminId): EitherNel<CreateScopeError, Scope>
    fun assignScope(
        scopeKey: ScopeKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnScopeError, ScopeMembers>

    fun updateScopeName(scopeKey: ScopeKey, name: Name, changeAuthor: AdminId): EitherNel<UpdateScopeNameError, Scope>
    fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope>
    fun getScopeMembers(scopeKey: ScopeKey): EitherNel<GetScopeMembersError, ScopeMembers>

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

}