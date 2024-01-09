package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryTaskRepository : TaskRepository {

    private val tasks = ConcurrentHashMap<TaskId, Task<*>>()

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
            title = title,
            creator = hero
        )
        return tasks.putIfAbsent(taskId, task)
            ?.let { Either.Left(nonEmptyListOf(TaskRepositoryHeroDoesNotExistError("New task id $taskId already existing"))) }
            ?: Either.Right(task)
    }


    override fun getTask(taskId: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>> {
        return tasks[taskId]
            ?.let { Either.Right(it) }
            ?: Either.Left(nonEmptyListOf(TaskDoesNotExistError(taskId)))
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
                Either.Right(updatedTask)
            }
            ?: Either.Left(nonEmptyListOf(TaskDoesNotExistAssignTaskError(taskId)))

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
                                        taskToUpdate.creator,
                                        taskToUpdate.assignees
                                    )
                                tasks.replace(pendingTaskId, inProgressTask)
                                inProgressTask
                            }
                    }

                    else -> Either.Left(nonEmptyListOf(TaskNotPendingStartWorkError(taskToUpdate, pendingTaskId)))
                }
            }
            ?: Either.Left(nonEmptyListOf(TaskDoesNotExistStartWorkError(pendingTaskId)))

    companion object {
        const val NON_EXISTING_TASK_ID: String = "nonExistingTask"
    }

}
