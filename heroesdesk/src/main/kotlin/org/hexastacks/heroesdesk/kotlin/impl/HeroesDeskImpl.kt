package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.UserRepository


class HeroesDeskImpl(private val userRepository: UserRepository, private val taskRepository: TaskRepository) :
    HeroesDesk {
    override fun createSquad(squadKey: SquadKey, name: Name, creator: AdminId): EitherNel<CreateSquadError, Squad> =
        either {
            userRepository.getAdmin(creator).bind()
            taskRepository.createSquad(squadKey, name).bind()
        }

    override fun assignSquad(
        squadKey: SquadKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers> =
        either {
            val heroes = userRepository.getHeroes(assignees).bind()
            userRepository.getAdmin(changeAuthor).bind()
            taskRepository.assignSquad(squadKey, heroes).bind()
        }

    override fun updateSquadName(
        squadKey: SquadKey,
        name: Name,
        changeAuthor: AdminId
    ): EitherNel<UpdateSquadNameError, Squad> =
        either {
            userRepository.getAdmin(changeAuthor).bind()
            taskRepository.updateSquadName(squadKey, name).bind()
        }

    override fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad> =
        taskRepository.getSquad(squadKey)

    override fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers> =
        taskRepository.getSquadMembers(squadKey)

    override fun createTask(
        squadKey: SquadKey,
        title: Title,
        creator: HeroId
    ): EitherNel<CreateTaskError, PendingTask> =
        either {
            taskRepository.areHeroesInSquad(HeroIds(creator), squadKey).bind()
            taskRepository.createTask(squadKey, title).bind()
        }

    override fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>> = taskRepository.getTask(id)

    override fun updateTitle(
        id: TaskId,
        title: Title,
        author: HeroId
    ): EitherNel<UpdateTitleError, Task<*>> =
        either {
            taskRepository.areHeroesInSquad(HeroIds(author), id).bind()
            taskRepository.updateTitle(id, title).bind()
        }

    override fun updateDescription(
        id: TaskId,
        description: Description,
        author: HeroId
    ): EitherNel<UpdateDescriptionError, Task<*>> =
        either {
            userRepository.getHero(author).bind()
            taskRepository.updateDescription(id, description).bind()
        }

    override fun assignTask(
        id: PendingTaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> =
        doAssignTask(id, assignees, author)

    override fun assignTask(
        id: InProgressTaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> =
        doAssignTask(id, assignees, author)

    private fun doAssignTask(
        id: TaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> =
        either {
            taskRepository.areHeroesInSquad(assignees + author, id.squadKey).bind()
            taskRepository.assignTask(id, assignees).bind()
        }

    override fun startWork(
        id: PendingTaskId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTask> =
        either {
            val task = taskRepository.areHeroesInSquad(HeroIds(author), id).bind()
            val verifiedTask = when (task) {
                is PendingTask -> Right(task)
                else -> Left(nonEmptyListOf(TaskNotPendingError(task, id)))
            }.bind()
            if (verifiedTask.assignees.isNotEmpty()) {
                Right(verifiedTask)
            } else {
                Right(assignAuthorToTask(verifiedTask, author, id))
            }.bind()
            taskRepository.startWork(id).bind()
        }


    private fun assignAuthorToTask(
        verifiedTask: PendingTask,
        author: HeroId,
        id: PendingTaskId
    ): EitherNel<StartWorkError, Task<*>> =
        taskRepository
            .assignTask(id, verifiedTask.assignees.add(author))

    override fun startWork(
        id: DoneTaskId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTask> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(
        id: InProgressTaskId,
        author: HeroId
    ): EitherNel<PauseWorkError, PendingTask> =
        either {
            val task = taskRepository.getTask(id).bind()
            val verifiedTask = when (task) {
                is InProgressTask -> Right(task)
                else -> Left(nonEmptyListOf(TaskNotInProgressError(task, id)))
            }.bind()
            taskRepository.areHeroesInSquad(HeroIds(author), verifiedTask.squadKey()).bind()
            taskRepository.pauseWork(id).bind()
        }

    override fun pauseWork(
        id: DoneTaskId,
        author: HeroId
    ): EitherNel<PauseWorkError, PendingTask> {
        TODO("Not yet implemented")
    }

    override fun endWork(id: PendingTaskId, author: HeroId): EitherNel<EndWorkError, DoneTask> {
        TODO("Not yet implemented")
    }

    override fun endWork(
        id: InProgressTaskId,
        author: HeroId
    ): EitherNel<EndWorkError, DoneTask> =
        either {
            val task = taskRepository.getTask(id).bind()
            val verifiedTask = when (task) {
                is InProgressTask -> Right(task)
                else -> Left(nonEmptyListOf(TaskNotInProgressError(task, id)))
            }.bind()
            taskRepository.areHeroesInSquad(HeroIds(author), verifiedTask.squadKey()).bind()
            taskRepository.endWork(id).bind()
        }
}