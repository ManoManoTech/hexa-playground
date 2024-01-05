package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.Description
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.Title
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTask
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.Task
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
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

    companion object {
        const val NON_EXISTING_TASK_ID: String = "nonExistingTask"
    }

}
