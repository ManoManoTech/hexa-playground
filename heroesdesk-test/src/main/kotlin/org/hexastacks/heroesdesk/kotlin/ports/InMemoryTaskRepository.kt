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


    override fun getTask(id: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>> {
        return tasks[id]
            ?.let { Either.Right(it) }
            ?: Either.Left(nonEmptyListOf(TaskDoesNotExistError(id)))
    }

    override fun updateTitle(
        id: TaskId,
        title: Title,
        hero: Hero
    ): Either<NonEmptyList<UpdateTitleError>, TaskId> =
        getTask(id)
            .mapLeft {
                it.map { error ->
                    when (error) {
                        is TaskDoesNotExistError -> TaskDoesNotExistUpdateTitleError(error.taskId)
                    }
                }
            }
            .map { task ->
                val updatedTask = task.updateTitle(title)
                tasks.replace(id, updatedTask)
                id
            }

    override fun updateDescription(
        id: TaskId,
        description: Description,
        hero: Hero
    ): Either<NonEmptyList<UpdateDescriptionError>, TaskId> =
        getTask(id)
            .mapLeft {
                it.map { error ->
                    when (error) {
                        is TaskDoesNotExistError -> TaskDoesNotExistUpdateDescriptionError(error.taskId)
                    }
                }
            }
            .map { task ->
                val updatedTask = task.updateDescription(description)
                tasks.replace(id, updatedTask)
                id
            }

    override fun assign(id: TaskId, assignees: Heroes, author: HeroId): EitherNel<AssignTaskError, Task<*>> =
        tasks[id]
            ?.let { taskToUpdate ->
                val updatedTask: Task<*> = taskToUpdate.assign(assignees)
                tasks.replace(id, updatedTask)
                Either.Right(updatedTask)
            }
            ?: Either.Left(nonEmptyListOf(TaskDoesNotExistAssignTaskError(id)))

    override fun startWork(id: PendingTaskId, hero: Hero): EitherNel<StartWorkError, InProgressTask> =
        tasks[id]
            ?.let { taskToUpdate ->
                when (taskToUpdate) {
                    is PendingTask -> {
                        InProgressTaskId(taskToUpdate.taskId.value)
                            .mapLeft { errors: NonEmptyList<TaskId.TaskIdError> ->
                                errors.map {
                                    when (it) {
                                        is TaskId.BelowMinLengthError -> InvalidTaskIdStartWorkError(id, it)
                                        is TaskId.AboveMaxLengthError -> InvalidTaskIdStartWorkError(id, it)
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
                                tasks.replace(id, inProgressTask)
                                inProgressTask
                            }
                    }
                    else -> Either.Left(nonEmptyListOf(TaskNotPendingStartWorkError(taskToUpdate, id)))
                }
            }
            ?: Either.Left(nonEmptyListOf(TaskDoesNotExistStartWorkError(id)))

    companion object {
        const val NON_EXISTING_TASK_ID: String = "nonExistingTask"
    }

}
