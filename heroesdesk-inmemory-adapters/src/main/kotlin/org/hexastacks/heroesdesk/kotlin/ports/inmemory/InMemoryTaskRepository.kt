package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class InMemoryTaskRepository : TaskRepository {

    private val database = ConcurrentHashMap<ScopeKey, Pair<InMemoryScope, Map<RawTaskId, RawTask>>>()

    override fun createTask(
        scopeKey: ScopeKey,
        title: Title
    ): Either<NonEmptyList<CreateTaskError>, PendingTask> {
        val createdTask = AtomicReference<PendingTask>()
        return database.computeIfPresent(scopeKey) { _, scopeAndTaskIdsToTask ->
            val inMemoryScope = scopeAndTaskIdsToTask.first
            val uuid = UUID.randomUUID().toString()
            val retrievedScope = inMemoryScope.toScope()
            val taskId = PendingTaskId(scopeKey, uuid).getOrElse {
                throw RuntimeException("taskId $uuid should be valid")
            }
            val task = PendingTask(taskId, title)
            createdTask.set(task)
            Pair(inMemoryScope, scopeAndTaskIdsToTask.second.plus(RawTaskId(taskId) to RawTask(task)))
        }
            ?.let { Right(createdTask.get()) }
            ?: Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))
    }

    override fun getTask(taskId: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>> =
        database[taskId.scope]
            ?.let { scopeAndTaskIdsToTask ->
                val rawTaskId = RawTaskId(taskId)
                val task = scopeAndTaskIdsToTask.second[rawTaskId]
                task?.let { buildTask(rawTaskId, it, scopeAndTaskIdsToTask.first.key) }
            }
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    private fun buildTask(
        rawTaskId: RawTaskId,
        task: RawTask,
        scope: ScopeKey
    ): Task<*> =
        when (task.type) {
            TaskType.PENDING -> PendingTask(
                PendingTaskId(
                    scope,
                    rawTaskId.value
                ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                task.title,
                task.description,
                task.assignees
            )

            TaskType.DONE -> DoneTask(
                DoneTaskId(
                    scope,
                    rawTaskId.value
                ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                task.title,
                task.description
            )

            TaskType.IN_PROGRESS -> InProgressTask(
                InProgressTaskId(
                    scope,
                    rawTaskId.value
                ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                task.title,
                task.description,
                task.assignees
            )
        }

    override fun updateTitle(
        taskId: TaskId,
        title: Title
    ): EitherNel<UpdateTitleError, Task<*>> =
        database
            .computeIfPresent(taskId.scope) { _, scopeAndTaskIdsToTask ->
                val scope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                scopeAndTaskIdsToTask
                    .second[rawTaskId]
                    ?.copy(title = title)
                    ?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(rawTaskId to it)) }
            }
            ?.let {
                Right(buildTask(it, taskId))
            }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    private fun buildTask(
        it: Pair<InMemoryScope, Map<RawTaskId, RawTask>>,
        taskId: TaskId
    ): Task<*> {
        val rawTask: RawTask = it.second[RawTaskId(taskId)]!!
        val task = buildTask(RawTaskId(taskId), rawTask, taskId.scope)
        return task
    }

    override fun updateDescription(
        taskId: TaskId,
        description: Description
    ): Either<NonEmptyList<UpdateDescriptionError>, Task<*>> =
        database
            .computeIfPresent(taskId.scope) { _, scopeAndTaskIdsToTask ->
                val scope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                scopeAndTaskIdsToTask
                    .second[rawTaskId]
                    ?.copy(description = description)
                    ?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(rawTaskId to it)) }
            }
            ?.let { Right(buildTask(it, taskId)) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    override fun assignTask(
        taskId: TaskId,
        assignees: HeroIds
    ): EitherNel<AssignTaskError, Task<*>> =
        database
            .computeIfPresent(taskId.scope) { _, scopeAndTaskIdsToTask ->
                val scope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                val task: RawTask? = scopeAndTaskIdsToTask.second[rawTaskId]
                val updatedTask = task?.copy(assignees = assignees)
                updatedTask?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(rawTaskId to updatedTask)) }
            }
            ?.let { Right(buildTask(it, taskId)) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    override fun startWork(
        pendingTaskId: PendingTaskId
    ): EitherNel<StartWorkError, InProgressTask> =
        database
            .computeIfPresent(pendingTaskId.scope) { _, scopeAndTaskIdsToTask ->
                val inMemoryScope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(pendingTaskId)
                val task = scopeAndTaskIdsToTask.second[rawTaskId]
                if (task?.type == TaskType.PENDING) {
                    val retrievedScope = inMemoryScope.toScope()
                    InProgressTaskId(retrievedScope.key, rawTaskId.value)
                        .map { inProgressTaskId ->
                            val inProgressTask =
                                InProgressTask(
                                    inProgressTaskId,
                                    task.title,
                                    task.description,
                                    task.assignees
                                )
                            Pair(
                                inMemoryScope,
                                scopeAndTaskIdsToTask.second.plus(rawTaskId to RawTask(inProgressTask))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(buildTask(it, pendingTaskId) as InProgressTask) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(pendingTaskId)))

    override fun createScope(scopeKey: ScopeKey, name: Name): EitherNel<CreateScopeError, Scope> {
        return if (database.any { it.value.first.name == name }) {
            Left(nonEmptyListOf(ScopeNameAlreadyExistingError(name)))
        } else if (database.containsKey(scopeKey)) {
            Left(nonEmptyListOf(ScopeKeyAlreadyExistingError(scopeKey)))
        } else
            Right(
                database
                    .computeIfAbsent(scopeKey) { _ ->
                        Pair(InMemoryScope(name, scopeKey), ConcurrentHashMap<RawTaskId, RawTask>())
                    }.first.toScope()
            )
    }

    override fun assignScope(
        scopeKey: ScopeKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnScopeError, ScopeMembers> =
        database.computeIfPresent(scopeKey) { _, scopeAndTaskIdsToTask ->
            val retrievedInMemoryScope = scopeAndTaskIdsToTask.first
            val updatedInMemoryScope = retrievedInMemoryScope.copy(members = assignees.toHeroIds())
            Pair(updatedInMemoryScope, scopeAndTaskIdsToTask.second)
        }
            ?.let { Right(it.first.toScopeMembers()) }
            ?: Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))

    override fun areHeroesInScope(
        heroIds: HeroIds,
        scopeKey: ScopeKey
    ): EitherNel<AreHeroesInScopeError, ScopeMembers> =
        database[scopeKey]
            ?.let {
                val scopeMembers = it.first.toScopeMembers()
                return if (scopeMembers.containsAll(heroIds))
                    Right(scopeMembers)
                else
                    Left(nonEmptyListOf(HeroesNotInScopeError(heroIds, scopeKey)))
            }
            ?: Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))

    override fun updateScopeName(
        scopeKey: ScopeKey,
        name: Name
    ): EitherNel<UpdateScopeNameError, Scope> =
        database.computeIfPresent(scopeKey) { _, scopeAndTaskIdsToTask ->
            val inMemoryScope = scopeAndTaskIdsToTask.first
            val updatedInMemoryScope = inMemoryScope.copy(name = name)
            Pair(updatedInMemoryScope, scopeAndTaskIdsToTask.second)
        }
            ?.let { Right(it.first.toScope()) }
            ?: Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))


    override fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope> =
        database[scopeKey]
            ?.let { Right(it.first.toScope()) }
            ?: Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))

    override fun getScopeMembers(scopeKey: ScopeKey): EitherNel<GetScopeMembersError, ScopeMembers> =
        database[scopeKey]
            ?.let { Right(it.first.toScopeMembers()) }
            ?: Left(nonEmptyListOf(ScopeNotExistingError(scopeKey)))

    override fun pauseWork(inProgressTaskId: InProgressTaskId): EitherNel<PauseWorkError, PendingTask> =
        database
            .computeIfPresent(inProgressTaskId.scope) { _, scopeAndTaskIdsToTask ->
                val inMemoryScope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(inProgressTaskId)
                val task = scopeAndTaskIdsToTask.second[rawTaskId]
                if (task?.type == TaskType.IN_PROGRESS) {
                    PendingTaskId(inProgressTaskId.scope, rawTaskId.value)
                        .map { inProgressTaskId ->
                            val pendingTask =
                                PendingTask(
                                    inProgressTaskId,
                                    task.title,
                                    task.description,
                                    task.assignees
                                )
                            Pair(
                                inMemoryScope,
                                scopeAndTaskIdsToTask.second.plus(rawTaskId to RawTask(pendingTask))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(buildTask(it, inProgressTaskId) as PendingTask) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(inProgressTaskId)))

    override fun endWork(inProgressTaskId: InProgressTaskId): EitherNel<EndWorkError, DoneTask> =
        database
            .computeIfPresent(inProgressTaskId.scope) { _, scopeAndTaskIdsToTask ->
                val inMemoryScope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(inProgressTaskId)
                val task = scopeAndTaskIdsToTask.second[rawTaskId]
                if (task?.type == TaskType.IN_PROGRESS) {
                    val scope = inMemoryScope.toScope()
                    DoneTaskId(inProgressTaskId.scope, rawTaskId.value)
                        .map { doneTaskId ->
                            val doneTask =
                                DoneTask(
                                    doneTaskId,
                                    task.title,
                                    task.description
                                )
                            Pair(
                                inMemoryScope,
                                scopeAndTaskIdsToTask.second.plus(rawTaskId to RawTask(doneTask))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(buildTask(it, inProgressTaskId) as DoneTask) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(inProgressTaskId)))

    companion object {
        const val NON_EXISTING_TASK_ID: String = "nonExistingTask"
    }

}

data class RawTask(
    val type: TaskType,
    val title: Title,
    val description: Description,
    val assignees: HeroIds,
) {
    constructor(pendingTask: PendingTask) : this(
        TaskType.PENDING,
        pendingTask.title,
        pendingTask.description,
        pendingTask.assignees
    )

    constructor(inProgressTask: InProgressTask) : this(
        TaskType.IN_PROGRESS,
        inProgressTask.title,
        inProgressTask.description,
        inProgressTask.assignees
    )

    constructor(doneTask: DoneTask) : this(
        TaskType.DONE,
        doneTask.title,
        doneTask.description,
        doneTask.assignees
    )
}

enum class TaskType {
    PENDING,
    IN_PROGRESS,
    DONE
}

class RawTaskId(value: String) : AbstractStringValue(value) {
    constructor(taskId: TaskId) : this(taskId.value)
}
