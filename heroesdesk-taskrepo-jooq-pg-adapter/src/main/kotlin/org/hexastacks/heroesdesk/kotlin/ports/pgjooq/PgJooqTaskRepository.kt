package org.hexastacks.heroesdesk.kotlin.ports.pgjooq

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository

class PgJooqTaskRepository : TaskRepository {
    override fun createScope(scopeKey: ScopeKey, name: Name): EitherNel<HeroesDesk.CreateScopeError, Scope> {
        TODO("Not yet implemented")
    }

    override fun getScope(scopeKey: ScopeKey): EitherNel<HeroesDesk.GetScopeError, Scope> {
        TODO("Not yet implemented")
    }

    override fun updateScopeName(scopeKey: ScopeKey, name: Name): EitherNel<HeroesDesk.UpdateScopeNameError, Scope> {
        TODO("Not yet implemented")
    }

    override fun assignScope(
        scopeKey: ScopeKey,
        assignees: Heroes
    ): EitherNel<HeroesDesk.AssignHeroesOnScopeError, Scope> {
        TODO("Not yet implemented")
    }

    override fun createTask(
        scope: Scope,
        title: Title,
        hero: Hero
    ): EitherNel<HeroesDesk.CreateTaskError, PendingTask> {
        TODO("Not yet implemented")
    }

    override fun getTask(taskId: TaskId): EitherNel<HeroesDesk.GetTaskError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun updateTitle(
        taskId: TaskId,
        title: Title,
        hero: Hero
    ): EitherNel<HeroesDesk.UpdateTitleError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun updateDescription(
        taskId: TaskId,
        description: Description,
        hero: Hero
    ): EitherNel<HeroesDesk.UpdateDescriptionError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun assignTask(
        taskId: TaskId,
        assignees: Heroes,
        author: HeroId
    ): EitherNel<HeroesDesk.AssignTaskError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun startWork(
        pendingTaskId: PendingTaskId,
        hero: Hero
    ): EitherNel<HeroesDesk.StartWorkError, InProgressTask> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(
        inProgressTaskId: InProgressTaskId,
        hero: Hero
    ): EitherNel<HeroesDesk.PauseWorkError, PendingTask> {
        TODO("Not yet implemented")
    }

    override fun endWork(inProgressTaskId: InProgressTaskId, hero: Hero): EitherNel<HeroesDesk.EndWorkError, DoneTask> {
        TODO("Not yet implemented")
    }
}