package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
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

    private val database = ConcurrentHashMap<ScopeKey, Pair<Scope, Map<TaskId, Task<*>>>>()

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
            Pair(scope, scopeAndTaskIdsToTask.second.plus(taskId to task))
        }
            // the task could have been deleted in between the computeIfPresent and this line: playing it safe with the AtomicReference and caller will have delete scope error on next interaction, so quite the same in the end
            ?.let { _ -> Right(createdTask.get()) }
            ?: Left(nonEmptyListOf(ScopeNotExistCreateTaskError(scopeKey)))
    }

    override fun getTask(taskId: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>> =
        database[taskId.scope.key]
            ?.let { scopeAndTaskIdsToTask: Pair<Scope, Map<TaskId, Task<*>>> ->
                val task = scopeAndTaskIdsToTask.second[taskId]
                replaceTaskScopeByLatestScopeValue(task, scopeAndTaskIdsToTask.first)
            }
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistError(taskId)))

    private fun replaceTaskScopeByLatestScopeValue(
        task: Task<*>?,
        scope: Scope
    ): Task<*>? =
        task
            ?.let {
                when (it) {
                    is PendingTask -> PendingTask(
                        scope,
                        task.taskId as PendingTaskId,
                        task.title,
                        task.description,
                        task.assignees
                    )

                    is DeletedTask -> DeletedTask(
                        scope,
                        task.taskId as DeletedTaskId,
                        task.title,
                        task.description,
                        task.assignees
                    )

                    is DoneTask -> DoneTask(
                        scope,
                        task.taskId as DoneTaskId,
                        task.title,
                        task.description,
                        task.assignees
                    )

                    is InProgressTask -> InProgressTask(
                        scope,
                        task.taskId as InProgressTaskId,
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
                val task: Task<*>? = scopeAndTaskIdsToTask.second[taskId]
                val updatedTask = task?.updateTitle(title)
                updatedTask?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(taskId to updatedTask)) }
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
                val task: Task<*>? = scopeAndTaskIdsToTask.second[taskId]
                val updatedTask = task?.updateDescription(description)
                updatedTask?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(taskId to updatedTask)) }
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
                val task: Task<*>? = scopeAndTaskIdsToTask.second[taskId]
                val updatedTask = task?.assign(assignees)
                change.set(updatedTask)
                updatedTask?.let { Pair(scope, scopeAndTaskIdsToTask.second.plus(taskId to updatedTask)) }
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
                val task = scopeAndTaskIdsToTask.second[pendingTaskId]
                if (task is PendingTask) {
                    InProgressTaskId(task.scope, task.taskId.value)
                        .map { inProgressTaskId ->
                            val inProgressTask =
                                InProgressTask(
                                    task.scope,
                                    inProgressTaskId,
                                    task.title,
                                    task.description,
                                    task.assignees
                                )
                            change.set(inProgressTask)
                            Pair(
                                scope,
                                scopeAndTaskIdsToTask.second.plus(inProgressTaskId to inProgressTask)
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
                    Pair(createdScope.get(), ConcurrentHashMap<TaskId, Task<*>>())
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
