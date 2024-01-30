package org.hexastacks.heroesdesk.kotlin.ports.pgjooq

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.Tables.*
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.enums.Taskstatus
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.SquadRecord
import org.jooq.DSLContext
import org.jooq.Record6
import org.jooq.Result
import org.jooq.exception.DataAccessException
import org.jooq.exception.IntegrityConstraintViolationException
import java.util.*

class PgJooqTaskRepository(private val dslContext: DSLContext) : TaskRepository {
    override fun createSquad(squadKey: SquadKey, name: Name): EitherNel<CreateSquadError, Squad> =
        try {
            val execute = dslContext.insertInto(SQUAD)
                .set(SQUAD.KEY, squadKey.value)
                .set(SQUAD.NAME, name.value)
                .execute()
            if (execute != 1)
                Left(nonEmptyListOf(TaskRepositoryError("Insert failed")))
            else
                Right(Squad(name, squadKey))
        } catch (e: DataAccessException) {
            if (e is IntegrityConstraintViolationException && e.message?.contains("""ERROR: duplicate key value violates unique constraint "${Keys.CHK_NAME_UNIQUE.name}"""") ?: false)
                Left(nonEmptyListOf(SquadNameAlreadyExistingError(name)))
            else if (e is IntegrityConstraintViolationException && e.message?.contains("""ERROR: duplicate key value violates unique constraint "${Keys.PK_SQUAD.name}"""") ?: false)
                Left(nonEmptyListOf(SquadKeyAlreadyExistingError(squadKey)))
            else
                Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad> =
        try {
            dslContext.selectFrom(SQUAD)
                .where(SQUAD.KEY.eq(squadKey.value))
                .fetchOneInto(SquadRecord::class.java)
                ?.let {
                    Name(it.name)
                        .mapLeft { errors ->
                            errors.map { error -> TaskRepositoryError(error) }
                        }
                        .map { name -> Squad(name, squadKey) }
                }
                ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers> =
        try {
            (if (isSquadNotExisting(squadKey)
            ) Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
            else dslContext.select(SQUAD_USER.USER_ID)
                .from(SQUAD_USER)
                .where(SQUAD_USER.SQUAD_KEY.eq(squadKey.value))
                .map { HeroId(it.value1()) }
                .toList()
                .let { either { it.bindAll() } }
                .mapLeft { errors ->
                    errors.map { error -> TaskRepositoryError(error) }
                }.map { heroIds -> SquadMembers(squadKey, HeroIds(heroIds)) })
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun updateSquadName(squadKey: SquadKey, name: Name): EitherNel<UpdateSquadNameError, Squad> =
        try {
            dslContext.update(SQUAD)
                .set(SQUAD.NAME, name.value)
                .where(SQUAD.KEY.eq(squadKey.value))
                .returning()
                .fetchOneInto(Squad::class.java)
                ?.let { Right(it) }
                ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun assignSquad(
        squadKey: SquadKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers> =
        try {
            if (isSquadNotExisting(squadKey) // FIXME: if squad not existing then FK should make insert fail and thus no need for extra query
            ) Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
            else {
                dslContext.deleteFrom(SQUAD_USER)
                    .where(SQUAD_USER.SQUAD_KEY.eq(squadKey.value))
                    .execute()
                val nbUpdate = dslContext.insertInto(SQUAD_USER)
                    .columns(SQUAD_USER.SQUAD_KEY, SQUAD_USER.USER_ID)
                    .apply {
                        assignees.forEach { hero ->
                            values(squadKey.value, hero.id.value)
                        }
                    }
                    .execute()
                if (nbUpdate != assignees.size)
                    Left(nonEmptyListOf(TaskRepositoryError("Only $nbUpdate updated on ${assignees.size}")))
                else
                    Right(SquadMembers(squadKey, HeroIds(assignees.map { it.id })))
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    private fun isSquadNotExisting(squadKey: SquadKey) = !dslContext.fetchExists(
        dslContext.selectOne()
            .from(SQUAD)
            .where(SQUAD.KEY.eq(squadKey.value))
    )

    override fun areHeroesInSquad(
        heroIds: HeroIds,
        squadKey: SquadKey
    ): EitherNel<AreHeroesInSquadError, SquadMembers> =
        try {
            if (isSquadNotExisting(squadKey)
            ) Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
            else {
                dslContext.select(SQUAD_USER.USER_ID)
                    .from(SQUAD_USER)
                    .where(
                        SQUAD_USER.SQUAD_KEY.eq(squadKey.value)
                            .and(SQUAD_USER.USER_ID.`in`(heroIds.value.map { it.value }))
                    )
                    .map { HeroId(it.value1()) }
                    .toList()
                    .let { either { it.bindAll() } }
                    .mapLeft { errors ->
                        errors.map { error -> TaskRepositoryError(error) }
                    }
                    .flatMap { fetchedHeroIds: List<HeroId> ->
                        if (fetchedHeroIds.size == heroIds.size)
                            Right(SquadMembers(squadKey, HeroIds(fetchedHeroIds)))
                        else
                            Left(
                                nonEmptyListOf(
                                    HeroesNotInSquadError(
                                        HeroIds(
                                            heroIds.value.filterNot { fetchedHeroIds.contains(it) }),
                                        squadKey
                                    )
                                )
                            )
                    }
            }
        } catch (e: DataAccessException) {
            if (e is IntegrityConstraintViolationException && e.message?.contains("ERROR: insert or update on table \"$TASK_USER.name\" violates foreign key constraint \"${Keys.TASK_USER__FK_TASK}\"") ?: false)
                Left(nonEmptyListOf(HeroesNotInSquadError(heroIds, squadKey)))
            else
                Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun createTask(
        squadKey: SquadKey,
        title: Title
    ): EitherNel<CreateTaskError, PendingTask> =
        try {
            val id = UUID.randomUUID().toString()
            dslContext.insertInto(TASK)
                .set(TASK.ID, id)
                .set(TASK.SQUAD_KEY, squadKey.value)
                .set(TASK.TITLE, title.value)
                .set(TASK.STATUS, Taskstatus.Pending)
                .returning()
                .fetchOneInto(TASK)
                ?.let { task ->
                    SquadKey(task.squadKey)
                        .flatMap { dbSquadKey -> PendingTaskId(dbSquadKey, task.id) }
                        .mapLeft { errors ->
                            errors.map { error -> TaskRepositoryError(error) }
                        }
                        .map { id ->
                            PendingTask(id, title)
                        }
                }
                ?: Left(nonEmptyListOf(TaskRepositoryError("Insert failed")))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun getTask(taskId: TaskId): EitherNel<GetTaskError, Task<*>> =
        try {
            dslContext.select(TASK.ID, TASK.SQUAD_KEY, TASK.TITLE, TASK.DESCRIPTION, TASK.STATUS, TASK_USER.USER_ID)
                .from(TASK)
                .leftJoin(TASK_USER).on(TASK.ID.eq(TASK_USER.TASK_ID))
                .where(TASK.ID.eq(taskId.value))
                .fetch()
                .takeIf { it.isNotEmpty }
                ?.let { rawTaskAndUsers ->
                    buildTask(rawTaskAndUsers)
                }
                ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    private fun buildTask(rawTaskAndUsers: Result<Record6<String, String, String, String, Taskstatus, String>>): Either<NonEmptyList<TaskRepositoryError>, Task<out AbstractTaskId>> {
        val task = rawTaskAndUsers.first()
        val rawSquadKey = task[TASK.SQUAD_KEY]
        val rawTaskId = task[TASK.ID]
        val rawTitle = task[TASK.TITLE]
        val rawDescription = task[TASK.DESCRIPTION]
        val taskStatus = task.value5()
        val rawAssignees = rawTaskAndUsers
            .filter { it.value6() != null }
            .map { HeroId(it.value6()) }
            .let {
                either {
                    HeroIds(it.bindAll())
                }
            }

        return when (taskStatus) {
            Taskstatus.Pending -> either {
                val key = SquadKey(rawSquadKey).bind()
                val id = PendingTaskId(key, rawTaskId).bind()
                val title = Title(rawTitle).bind()
                val description = Description(rawDescription).bind()
                val assignees = rawAssignees.bind()
                PendingTask(id, title, description, assignees)
            }.mapLeft { errors ->
                errors.map { error -> TaskRepositoryError(error) }
            }

            Taskstatus.InProgress -> either {
                val key = SquadKey(rawSquadKey).bind()
                val id = InProgressTaskId(key, rawTaskId).bind()
                val title = Title(rawTitle).bind()
                val description = Description(rawDescription).bind()
                val assignees = rawAssignees.bind()
                InProgressTask(id, title, description, assignees)
            }.mapLeft { errors ->
                errors.map { error -> TaskRepositoryError(error) }
            }

            Taskstatus.Done -> either {
                val key = SquadKey(rawSquadKey).bind()
                val id = DoneTaskId(key, rawTaskId).bind()
                val title = Title(rawTitle).bind()
                val description = Description(rawDescription).bind()
                DoneTask(id, title, description)
            }.mapLeft { errors ->
                errors.map { error -> TaskRepositoryError(error) }
            }
        }
    }

    override fun updateTitle(
        taskId: TaskId,
        title: Title
    ): EitherNel<UpdateTitleError, Task<*>> =
        try {
            val nbUpdated = dslContext.update(TASK)
                .set(TASK.TITLE, title.value)
                .where(TASK.ID.eq(taskId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(TaskNotExistingError(taskId)))
            else
                getTask(taskId)
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun updateDescription(
        taskId: TaskId,
        description: Description
    ): EitherNel<UpdateDescriptionError, Task<*>> =
        try {
            val nbUpdated = dslContext.update(TASK)
                .set(TASK.DESCRIPTION, description.value)
                .where(TASK.ID.eq(taskId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(TaskNotExistingError(taskId)))
            else
                getTask(taskId)
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun assignTask(taskId: TaskId, assignees: HeroIds): EitherNel<AssignTaskError, Task<*>> =
        try {
            dslContext.deleteFrom(TASK_USER)
                .where(TASK_USER.TASK_ID.eq(taskId.value))
                .execute()
            val nbUpdate = dslContext.insertInto(TASK_USER)
                .columns(TASK_USER.TASK_ID, TASK_USER.USER_ID)
                .apply {
                    assignees.forEach { hero: HeroId ->
                        values(taskId.value, hero.value)
                    }
                }
                .execute()
            if (nbUpdate != assignees.size)
                Left(nonEmptyListOf(TaskRepositoryError("Only $nbUpdate updated on ${assignees.size}")))
            else
                getTask(taskId)
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun startWork(pendingTaskId: PendingTaskId): EitherNel<StartWorkError, InProgressTask> =
        try {
            val nbUpdated = dslContext.update(TASK)
                .set(TASK.STATUS, Taskstatus.InProgress)
                .where(TASK.ID.eq(pendingTaskId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(TaskNotExistingError(pendingTaskId)))
            else {
                either {
                    val task = getTask(pendingTaskId).bind()
                    if (task is InProgressTask)
                        Right(task).bind()
                    else
                        Left(
                            nonEmptyListOf(
                                TaskNotPendingError(
                                    task,
                                    pendingTaskId
                                )
                            )
                        ).bind()
                }
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun pauseWork(inProgressTaskId: InProgressTaskId): EitherNel<PauseWorkError, PendingTask> =
        try {
            val nbUpdated = dslContext.update(TASK)
                .set(TASK.STATUS, Taskstatus.Pending)
                .where(TASK.ID.eq(inProgressTaskId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(TaskNotExistingError(inProgressTaskId)))
            else {
                either {
                    val task = getTask(inProgressTaskId).bind()
                    if (task is PendingTask)
                        Right(task).bind()
                    else
                        Left(
                            nonEmptyListOf(
                                TaskNotInProgressError(
                                    task,
                                    inProgressTaskId
                                )
                            )
                        ).bind()
                }
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }

    override fun endWork(inProgressTaskId: InProgressTaskId): EitherNel<EndWorkError, DoneTask> =
        try {
            val nbUpdated = dslContext.update(TASK)
                .set(TASK.STATUS, Taskstatus.Done)
                .where(TASK.ID.eq(inProgressTaskId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(TaskNotExistingError(inProgressTaskId)))
            else {
                either {
                    val task = getTask(inProgressTaskId).bind()
                    if (task is DoneTask)
                        Right(task).bind()
                    else
                        Left(
                            nonEmptyListOf(
                                TaskNotInProgressError(
                                    task,
                                    inProgressTaskId
                                )
                            )
                        ).bind()
                }
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(TaskRepositoryError(e)))
        }
}