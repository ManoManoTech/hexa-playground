package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class InMemoryTaskRepository : TaskRepository {

    private val database = ConcurrentHashMap<SquadKey, Pair<InMemorySquad, Map<RawTaskId, RawTask>>>()

    override fun createTask(
        squadKey: SquadKey,
        title: Title
    ): Either<NonEmptyList<CreateTaskError>, PendingTask> {
        val createdTask = AtomicReference<PendingTask>()
        return database.computeIfPresent(squadKey) { _, squadAndTaskIdsToTask ->
            val inMemorySquad = squadAndTaskIdsToTask.first
            val uuid = UUID.randomUUID().toString()
            val retrievedSquad = inMemorySquad.toSquad()
            val taskId = PendingTaskId(squadKey, uuid).getOrElse {
                throw RuntimeException("taskId $uuid should be valid")
            }
            val task = PendingTask(taskId, title)
            createdTask.set(task)
            Pair(inMemorySquad, squadAndTaskIdsToTask.second.plus(RawTaskId(taskId) to RawTask(task)))
        }
            ?.let { Right(createdTask.get()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
    }

    override fun getTask(taskId: TaskId): Either<NonEmptyList<GetTaskError>, Task<*>> =
        database[taskId.squadKey]
            ?.let { squadAndTaskIdsToTask ->
                val rawTaskId = RawTaskId(taskId)
                val task = squadAndTaskIdsToTask.second[rawTaskId]
                task?.let { buildTask(rawTaskId, it, squadAndTaskIdsToTask.first.key) }
            }
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    private fun buildTask(
        rawTaskId: RawTaskId,
        task: RawTask,
        squad: SquadKey
    ): Task<*> =
        when (task.type) {
            TaskType.PENDING -> PendingTask(
                PendingTaskId(
                    squad,
                    rawTaskId.value
                ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                task.title,
                task.description,
                task.assignees
            )

            TaskType.DONE -> DoneTask(
                DoneTaskId(
                    squad,
                    rawTaskId.value
                ).getOrElse { throw RuntimeException("taskId ${rawTaskId.value} should be valid") },
                task.title,
                task.description
            )

            TaskType.IN_PROGRESS -> InProgressTask(
                InProgressTaskId(
                    squad,
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
            .computeIfPresent(taskId.squadKey) { _, squadAndTaskIdsToTask ->
                val squad = squadAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                squadAndTaskIdsToTask
                    .second[rawTaskId]
                    ?.copy(title = title)
                    ?.let { Pair(squad, squadAndTaskIdsToTask.second.plus(rawTaskId to it)) }
            }
            ?.let {
                Right(buildTask(it, taskId))
            }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    private fun buildTask(
        it: Pair<InMemorySquad, Map<RawTaskId, RawTask>>,
        taskId: TaskId
    ): Task<*> {
        val rawTask: RawTask = it.second[RawTaskId(taskId)]!!
        val task = buildTask(RawTaskId(taskId), rawTask, taskId.squadKey)
        return task
    }

    override fun updateDescription(
        taskId: TaskId,
        description: Description
    ): Either<NonEmptyList<UpdateDescriptionError>, Task<*>> =
        database
            .computeIfPresent(taskId.squadKey) { _, squadAndTaskIdsToTask ->
                val squad = squadAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                squadAndTaskIdsToTask
                    .second[rawTaskId]
                    ?.copy(description = description)
                    ?.let { Pair(squad, squadAndTaskIdsToTask.second.plus(rawTaskId to it)) }
            }
            ?.let { Right(buildTask(it, taskId)) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    override fun assignTask(
        taskId: TaskId,
        assignees: HeroIds
    ): EitherNel<AssignTaskError, Task<*>> =
        database
            .computeIfPresent(taskId.squadKey) { _, squadAndTaskIdsToTask ->
                val squad = squadAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(taskId)
                val task: RawTask? = squadAndTaskIdsToTask.second[rawTaskId]
                val updatedTask = task?.copy(assignees = assignees)
                updatedTask?.let { Pair(squad, squadAndTaskIdsToTask.second.plus(rawTaskId to updatedTask)) }
            }
            ?.let { Right(buildTask(it, taskId)) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(taskId)))

    override fun startWork(
        pendingTaskId: PendingTaskId
    ): EitherNel<StartWorkError, InProgressTask> =
        database
            .computeIfPresent(pendingTaskId.squadKey) { _, squadAndTaskIdsToTask ->
                val inMemorySquad = squadAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(pendingTaskId)
                val task = squadAndTaskIdsToTask.second[rawTaskId]
                if (task?.type == TaskType.PENDING) {
                    val retrievedSquad = inMemorySquad.toSquad()
                    InProgressTaskId(retrievedSquad.key, rawTaskId.value)
                        .map { inProgressTaskId ->
                            val inProgressTask =
                                InProgressTask(
                                    inProgressTaskId,
                                    task.title,
                                    task.description,
                                    task.assignees
                                )
                            Pair(
                                inMemorySquad,
                                squadAndTaskIdsToTask.second.plus(rawTaskId to RawTask(inProgressTask))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(buildTask(it, pendingTaskId) as InProgressTask) }
            ?: Left(nonEmptyListOf(TaskNotExistingError(pendingTaskId)))

    override fun createSquad(squadKey: SquadKey, name: Name): EitherNel<CreateSquadError, Squad> {
        return if (database.any { it.value.first.name == name }) {
            Left(nonEmptyListOf(SquadNameAlreadyExistingError(name)))
        } else if (database.containsKey(squadKey)) {
            Left(nonEmptyListOf(SquadKeyAlreadyExistingError(squadKey)))
        } else
            Right(
                database
                    .computeIfAbsent(squadKey) { _ ->
                        Pair(InMemorySquad(name, squadKey), ConcurrentHashMap<RawTaskId, RawTask>())
                    }.first.toSquad()
            )
    }

    override fun assignSquad(
        squadKey: SquadKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers> =
        database.computeIfPresent(squadKey) { _, squadAndTaskIdsToTask ->
            val retrievedInMemorySquad = squadAndTaskIdsToTask.first
            val updatedInMemorySquad = retrievedInMemorySquad.copy(members = assignees.toHeroIds())
            Pair(updatedInMemorySquad, squadAndTaskIdsToTask.second)
        }
            ?.let { Right(it.first.toSquadMembers()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun areHeroesInSquad(
        heroIds: HeroIds,
        squadKey: SquadKey
    ): EitherNel<AreHeroesInSquadError, SquadMembers> =
        database[squadKey]
            ?.let {
                val squadMembers = it.first.toSquadMembers()
                return if (squadMembers.containsAll(heroIds))
                    Right(squadMembers)
                else
                    Left(nonEmptyListOf(HeroesNotInSquadError(heroIds, squadKey)))
            }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun updateSquadName(
        squadKey: SquadKey,
        name: Name
    ): EitherNel<UpdateSquadNameError, Squad> =
        database.computeIfPresent(squadKey) { _, squadAndTaskIdsToTask ->
            val inMemorySquad = squadAndTaskIdsToTask.first
            val updatedInMemorySquad = inMemorySquad.copy(name = name)
            Pair(updatedInMemorySquad, squadAndTaskIdsToTask.second)
        }
            ?.let { Right(it.first.toSquad()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))


    override fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad> =
        database[squadKey]
            ?.let { Right(it.first.toSquad()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers> =
        database[squadKey]
            ?.let { Right(it.first.toSquadMembers()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun pauseWork(inProgressTaskId: InProgressTaskId): EitherNel<PauseWorkError, PendingTask> =
        database
            .computeIfPresent(inProgressTaskId.squadKey) { _, squadAndTaskIdsToTask ->
                val inMemorySquad = squadAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(inProgressTaskId)
                val task = squadAndTaskIdsToTask.second[rawTaskId]
                if (task?.type == TaskType.IN_PROGRESS) {
                    PendingTaskId(inProgressTaskId.squadKey, rawTaskId.value)
                        .map { inProgressTaskId ->
                            val pendingTask =
                                PendingTask(
                                    inProgressTaskId,
                                    task.title,
                                    task.description,
                                    task.assignees
                                )
                            Pair(
                                inMemorySquad,
                                squadAndTaskIdsToTask.second.plus(rawTaskId to RawTask(pendingTask))
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
            .computeIfPresent(inProgressTaskId.squadKey) { _, squadAndTaskIdsToTask ->
                val inMemorySquad = squadAndTaskIdsToTask.first
                val rawTaskId = RawTaskId(inProgressTaskId)
                val task = squadAndTaskIdsToTask.second[rawTaskId]
                if (task?.type == TaskType.IN_PROGRESS) {
                    val squad = inMemorySquad.toSquad()
                    DoneTaskId(inProgressTaskId.squadKey, rawTaskId.value)
                        .map { doneTaskId ->
                            val doneTask =
                                DoneTask(
                                    doneTaskId,
                                    task.title,
                                    task.description
                                )
                            Pair(
                                inMemorySquad,
                                squadAndTaskIdsToTask.second.plus(rawTaskId to RawTask(doneTask))
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
