package org.hexastacks.heroesdesk.kotlin.ports.pgjooq

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.ScopeRecord
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.hexastacks.heroesdesk.kotlin.errors.*

class PgJooqTaskRepository(private val dslContext: DSLContext) : TaskRepository {
    override fun createScope(scopeKey: ScopeKey, name: Name): EitherNel<CreateScopeError, Scope> =
        try {
            val execute = dslContext.insertInto(Tables.SCOPE)
                .set(Tables.SCOPE.KEY, scopeKey.value)
                .set(Tables.SCOPE.NAME, name.value)
                .execute()
            if (execute != 1)
                Either.Left(nonEmptyListOf(TaskRepositoryError("Insert failed")))
            else
                Either.Right(Scope(name, scopeKey))
        } catch (e: DataAccessException) {
            Either.Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope> =
        try {
            dslContext.selectFrom(Tables.SCOPE)
                .where(Tables.SCOPE.KEY.eq(scopeKey.value))
                .fetchOneInto(ScopeRecord::class.java)
                ?.let {
                    Name(it.name)
                        .mapLeft { errors ->
                            errors.map {error -> TaskRepositoryError(error)}
                        }
                        .map { name -> Scope(name, scopeKey) }
                }
                ?: Either.Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))
        } catch (e: DataAccessException) {
            Either.Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun updateScopeName(scopeKey: ScopeKey, name: Name): EitherNel<UpdateScopeNameError, Scope> =
        try {
            dslContext.update(Tables.SCOPE)
                .set(Tables.SCOPE.NAME, name.value)
                .where(Tables.SCOPE.KEY.eq(scopeKey.value))
                .returning()
                .fetchOneInto(Scope::class.java)
                ?.let { Either.Right(it) }
                ?: Either.Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))
        } catch (e: DataAccessException) {
            Either.Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun assignScope(
        scopeKey: ScopeKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnScopeError, Scope> {
        TODO("Not yet implemented")
    }

    override fun createTask(
        scope: Scope,
        title: Title,
        hero: Hero
    ): EitherNel<CreateTaskError, PendingTask> {
        TODO("Not yet implemented")
    }

    override fun getTask(taskId: TaskId): EitherNel<GetTaskError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun updateTitle(
        taskId: TaskId,
        title: Title,
        hero: Hero
    ): EitherNel<UpdateTitleError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun updateDescription(
        taskId: TaskId,
        description: Description,
        hero: Hero
    ): EitherNel<UpdateDescriptionError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun assignTask(
        taskId: TaskId,
        assignees: Heroes,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> {
        TODO("Not yet implemented")
    }

    override fun startWork(
        pendingTaskId: PendingTaskId,
        hero: Hero
    ): EitherNel<StartWorkError, InProgressTask> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(
        inProgressTaskId: InProgressTaskId,
        hero: Hero
    ): EitherNel<PauseWorkError, PendingTask> {
        TODO("Not yet implemented")
    }

    override fun endWork(inProgressTaskId: InProgressTaskId, hero: Hero): EitherNel<EndWorkError, DoneTask> {
        TODO("Not yet implemented")
    }
}