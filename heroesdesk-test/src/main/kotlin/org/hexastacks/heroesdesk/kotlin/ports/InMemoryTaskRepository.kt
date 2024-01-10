package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds.Companion.EMPTY_HEROIDS
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryTaskRepository : TaskRepository {

    private val tasks = ConcurrentHashMap<TaskId, Task<*>>()
    private val scopes = ConcurrentHashMap.newKeySet<Scope>()

    override fun createTask(
        title: Title,
        hero: Hero
    ): Either<NonEmptyList<CreateTaskError>, PendingTask> {
        val uuid: String = UUID.randomUUID().toString()
        val taskId = PendingTaskId(uuid).getOrElse {
            throw RuntimeException("taskId $uuid should be valid")
        }
        val task = PendingTask(
            taskId = taskId,
            title = title
        )
        return tasks.putIfAbsent(taskId, task)
            ?.let { Left(nonEmptyListOf(TaskRepositoryHeroDoesNotExistError("New task id $taskId already existing"))) }
            ?: Right(task)
    }

    override fun getTask(taskId: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>> {
        return tasks[taskId]
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistError(taskId)))
    }

    override fun updateTitle(
        taskId: TaskId,
        title: Title,
        hero: Hero
    ): Either<NonEmptyList<UpdateTitleError>, TaskId> =
        getTask(taskId)
            .mapLeft {
                it.map { error ->
                    when (error) {
                        is TaskDoesNotExistError -> TaskDoesNotExistUpdateTitleError(error.taskId)
                    }
                }
            }
            .map { task ->
                val updatedTask = task.updateTitle(title)
                tasks.replace(taskId, updatedTask)
                taskId
            }

    override fun updateDescription(
        taskId: TaskId,
        description: Description,
        hero: Hero
    ): Either<NonEmptyList<UpdateDescriptionError>, TaskId> =
        getTask(taskId)
            .mapLeft {
                it.map { error ->
                    when (error) {
                        is TaskDoesNotExistError -> TaskDoesNotExistUpdateDescriptionError(error.taskId)
                    }
                }
            }
            .map { task ->
                val updatedTask = task.updateDescription(description)
                tasks.replace(taskId, updatedTask)
                taskId
            }

    override fun assign(taskId: TaskId, assignees: Heroes, author: HeroId): EitherNel<AssignTaskError, Task<*>> =
        tasks[taskId]
            ?.let { taskToUpdate ->
                val updatedTask: Task<*> = taskToUpdate.assign(assignees)
                tasks.replace(taskId, updatedTask)
                Right(updatedTask)
            }
            ?: Left(nonEmptyListOf(TaskDoesNotExistAssignTaskError(taskId)))

    override fun startWork(pendingTaskId: PendingTaskId, hero: Hero): EitherNel<StartWorkError, InProgressTask> =
        tasks[pendingTaskId]
            ?.let { taskToUpdate ->
                when (taskToUpdate) {
                    is PendingTask -> {
                        InProgressTaskId(taskToUpdate.taskId.value)
                            .mapLeft { errors: NonEmptyList<TaskId.TaskIdError> ->
                                errors.map {
                                    when (it) {
                                        is TaskId.BelowMinLengthError -> InvalidTaskIdStartWorkError(pendingTaskId, it)
                                        is TaskId.AboveMaxLengthError -> InvalidTaskIdStartWorkError(pendingTaskId, it)
                                    }
                                }
                            }
                            .map { inProgressTaskId ->
                                val inProgressTask =
                                    InProgressTask(
                                        inProgressTaskId,
                                        taskToUpdate.title,
                                        taskToUpdate.description,
                                        taskToUpdate.assignees
                                    )
                                tasks.replace(pendingTaskId, inProgressTask)
                                inProgressTask
                            }
                    }

                    else -> Left(nonEmptyListOf(TaskNotPendingStartWorkError(taskToUpdate, pendingTaskId)))
                }
            }
            ?: Left(nonEmptyListOf(TaskDoesNotExistStartWorkError(pendingTaskId)))

    override fun createScope(scopeKey: ScopeKey, name: Name, creator: AdminId): EitherNel<CreateScopeError, Scope> {
        return if (scopes.any { it.name == name })
            Left(
                nonEmptyListOf(ScopeNameAlreadyExistsError(name))
            ) else if (scopes.any { it.key == scopeKey })
            Left(
                nonEmptyListOf(ScopeIdAlreadyExistsError(scopeKey))
            ) else {
            val newScope = Scope(name, scopeKey, EMPTY_HEROIDS)
            if (scopes.add(newScope))
                Right(newScope)
            else
                Left(nonEmptyListOf(ScopeIdAlreadyExistsError(scopeKey)))
        }
    }

    override fun assignScope(
        scopeKey: ScopeKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnScopeError, Scope> {
        TODO()
    }

    companion object {
        const val NON_EXISTING_TASK_ID: String = "nonExistingTask"
    }

}
