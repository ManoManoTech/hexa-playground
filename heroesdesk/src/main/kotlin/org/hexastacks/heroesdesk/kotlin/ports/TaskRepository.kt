package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

interface TaskRepository {
    fun createScope(scopeKey: ScopeKey, name: Name): EitherNel<CreateScopeError, Scope>
    fun assignScope(
        scopeKey: ScopeKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnScopeError, Scope>

    fun createTask(scopeKey: ScopeKey, title: Title, hero: Hero): EitherNel<CreateTaskError, PendingTask>

    fun getTask(taskId: TaskId): EitherNel<GetTaskError, Task<*>>

    fun updateTitle(
        taskId: TaskId,
        title: Title,
        hero: Hero
    ): EitherNel<UpdateTitleError, Task<*>>

    fun updateDescription(
        taskId: TaskId,
        description: Description,
        hero: Hero
    ): EitherNel<UpdateDescriptionError, Task<*>>

    fun assign(
        taskId: TaskId,
        assignees: Heroes,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>>

    fun startWork(pendingTaskId: PendingTaskId, hero: Hero): EitherNel<StartWorkError, InProgressTask>

    fun updateScopeName(
        scopeKey: ScopeKey,
        name: Name
    ): EitherNel<UpdateScopeNameError, Scope>

    fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope>

}
