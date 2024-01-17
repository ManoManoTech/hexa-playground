package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes.Companion.empty
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class InMemoryTaskRepository : TaskRepository {

    private val database = ConcurrentHashMap<ScopeKey, Pair<Scope, Map<RawTaskId, RawTask>>>()

    override fun createTask(
        scopeKey: ScopeKey,
        title: Title,
        hero: Hero
    ): Either<NonEmptyList<CreateTaskError>, PendingTask> {
        val createdTask = AtomicReference<PendingTask>()
        return database.computeIfPresent(scopeKey) { _, scopeAndTaskIdsToTask ->
            val scope = scopeAndTaskIdsToTask.first
            val uuid = UUID.randomUUID().toString()
            val taskId = PendingTaskId(scope, uuid).getOrElse {
                throw RuntimeException("taskId $uuid should be valid")
            }
            val task = PendingTask(scope, taskId, title)
            createdTask.set(task)
            Pair(scope, scopeAndTaskIdsToTask.second.plus(RawTaskId(taskId) to RawTask(task)))
        }
            ?.let { _ -> Right(createdTask.get()) }
            ?: Left(nonEmptyListOf(ScopeNotExistCreateTaskError(scopeKey)))
    }

    override fun getTask(taskId: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>> =
        database[taskId.scope.key]
            ?.let { scopeAndTaskIdsToTask ->
                val rawTaskId = RawTaskId(taskId)
                val task = scopeAndTaskIdsToTask.second[rawTaskId]
                buildTask(rawTaskId, task, scopeAndTaskIdsToTask.first)
            }
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistError(taskId)))

    private fun buildTask(
        rawTaskId: RawTaskId,
        task: RawTask?,
        scope: Scope
    ): Task<*>? =
        task
            ?.let {
                when (it.type) {
                    TaskType.PENDING -> PendingTask(
                        scope,
                        PendingTaskId(
                            scope,
                            rawTaskId.value
                        ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                        task.title,
                        task.description,
                        task.assignees
                    )

                    TaskType.DONE -> DoneTask(
                        scope,
                        DoneTaskId(
                            scope,
                            rawTaskId.value
                        ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                        task.title,
                        task.description,
                        task.assignees
                    )

                    TaskType.IN_PROGRESS -> InProgressTask(
                        scope,
                        InProgressTaskId(
                            scope,
                            rawTaskId.value
                        ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                        task.title,
                        task.description,
                        task.assignees
                    )
                }
            }

    override fun updateTitle(
        taskId: TaskId,
        title: Title,
        hero: Hero
    ): EitherNel<UpdateTitleError, TaskId> =
        database
            .computeIfPresent(taskId.scope.key) { _, scopeAndTaskIdsToTask ->
                val scope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                scopeAndTaskIdsToTask
                    .second[rawTaskId]
                    ?.copy(title = title)
                    ?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(rawTaskId to it)) }
            }
            ?.let { Right(taskId) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistUpdateTitleError(taskId)))

    override fun updateDescription(
        taskId: TaskId,
        description: Description,
        hero: Hero
    ): Either<NonEmptyList<UpdateDescriptionError>, TaskId> =
        database
            .computeIfPresent(taskId.scope.key) { _, scopeAndTaskIdsToTask ->
                val scope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                scopeAndTaskIdsToTask
                    .second[rawTaskId]
                    ?.copy(description = description)
                    ?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(rawTaskId to it)) }
            }
            ?.let { Right(taskId) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistUpdateDescriptionError(taskId)))

    override fun assign(
        taskId: TaskId,
        assignees: Heroes,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> {
        val change = AtomicReference<Task<*>>()
        return database
            .computeIfPresent(taskId.scope.key) { _, scopeAndTaskIdsToTask ->
                val scope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                val task: RawTask? = scopeAndTaskIdsToTask.second[rawTaskId]
                val updatedTask = task?.copy(assignees = assignees)
                change.set(buildTask(rawTaskId, updatedTask, scope))
                updatedTask?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(rawTaskId to updatedTask)) }
            }
            ?.let { Right(change.get()) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistAssignTaskError(taskId)))
    }

    override fun startWork(
        pendingTaskId: PendingTaskId,
        hero: Hero
    ): EitherNel<StartWorkError, InProgressTask> {
        val change = AtomicReference<InProgressTask>()
        return database
            .computeIfPresent(pendingTaskId.scope.key) { _, scopeAndTaskIdsToTask ->
                val scope = scopeAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(pendingTaskId)
                val task = scopeAndTaskIdsToTask.second[rawTaskId]
                if (task?.type == TaskType.PENDING) {
                    InProgressTaskId(scope, rawTaskId.value)
                        .map { inProgressTaskId ->
                            val inProgressTask =
                                InProgressTask(
                                    scope,
                                    inProgressTaskId,
                                    task.title,
                                    task.description,
                                    task.assignees
                                )
                            change.set(inProgressTask)
                            Pair(
                                scope,
                                scopeAndTaskIdsToTask.second.plus(rawTaskId to RawTask(inProgressTask))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(change.get()) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistStartWorkError(pendingTaskId)))
    }

    override fun createScope(scopeKey: ScopeKey, name: Name): EitherNel<CreateScopeError, Scope> {
        val createdScope = AtomicReference<Scope>()

        return if (database.any { it.value.first.name == name }) {
            Left(nonEmptyListOf(ScopeNameAlreadyExistsError(name)))
        } else {
            database
                .computeIfAbsent(scopeKey) { _ ->
                    createdScope.set(Scope(name, scopeKey, empty))
                    Pair(createdScope.get(), ConcurrentHashMap<RawTaskId, RawTask>())
                }
            if (createdScope.get() != null) {
                Right(createdScope.get())
            } else {
                Left(nonEmptyListOf(ScopeKeyAlreadyExistsError(scopeKey)))
            }
        }
    }

    override fun assignScope(
        scopeKey: ScopeKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnScopeError, Scope> =
        database.computeIfPresent(scopeKey) { _, scopeAndTaskIdsToTask ->
            val scope = scopeAndTaskIdsToTask.first
            val newScope = scope.copy(assignees = assignees)
            Pair(newScope, scopeAndTaskIdsToTask.second)
        }
            ?.let { Right(it.first) }
            ?: Left(nonEmptyListOf(ScopeDoesNotExistAssignHeroesOnScopeError(scopeKey)))

    override fun updateScopeName(
        scopeKey: ScopeKey,
        name: Name
    ): EitherNel<UpdateScopeNameError, Scope> =
        database.computeIfPresent(scopeKey) { _, scopeAndTaskIdsToTask ->
            val scope = scopeAndTaskIdsToTask.first
            val newScope = scope.copy(name = name)
            Pair(newScope, scopeAndTaskIdsToTask.second)
        }
            ?.let { Right(it.first) }
            ?: Left(nonEmptyListOf(ScopeNotExistingUpdateScopeNameError(scopeKey)))


    override fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope> =
        database[scopeKey]
            ?.let { Right(it.first) }
            ?: Left(nonEmptyListOf(ScopeNotExistingGetScopeError(scopeKey)))

    companion object {
        const val NON_EXISTING_TASK_ID: String = "nonExistingTask"
    }

}

data class RawTask(
    val type: TaskType,
    val title: Title,
    val description: Description,
    val assignees: Heroes,
) {
    constructor(pendingTask: PendingTask) : this(
        TaskType.PENDING,
        pendingTask.title,
        pendingTask.description,
        pendingTask.assignees
    )

    constructor(pendingTask: InProgressTask) : this(
        TaskType.IN_PROGRESS,
        pendingTask.title,
        pendingTask.description,
        pendingTask.assignees
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
