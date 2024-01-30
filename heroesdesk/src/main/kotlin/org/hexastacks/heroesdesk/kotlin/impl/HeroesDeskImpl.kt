package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.UserRepository


class HeroesDeskImpl(private val userRepository: UserRepository, private val taskRepository: TaskRepository) :
    HeroesDesk {
    override fun createScope(scopeKey: ScopeKey, name: Name, creator: AdminId): EitherNel<CreateScopeError, Scope> =
        either {
            userRepository.getAdmin(creator).bind()
            taskRepository.createScope(scopeKey, name).bind()
        }

    override fun assignScope(
        scopeKey: ScopeKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnScopeError, ScopeMembers> =
        either {
            val heroes = userRepository.getHeroes(assignees).bind()
            userRepository.getAdmin(changeAuthor).bind()
            taskRepository.assignScope(scopeKey, heroes).bind()
        }

    override fun updateScopeName(
        scopeKey: ScopeKey,
        name: Name,
        changeAuthor: AdminId
    ): EitherNel<UpdateScopeNameError, Scope> =
        either {
            userRepository.getAdmin(changeAuthor).bind()
            taskRepository.updateScopeName(scopeKey, name).bind()
        }

    override fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope> =
        taskRepository.getScope(scopeKey)

    override fun getScopeMembers(scopeKey: ScopeKey): EitherNel<GetScopeMembersError, ScopeMembers> =
        taskRepository.getScopeMembers(scopeKey)

    override fun createTask(
        scopeKey: ScopeKey,
        title: Title,
        creator: HeroId
    ): EitherNel<CreateTaskError, PendingTask> =
        either {
            taskRepository.areHeroesInScope(HeroIds(creator), scopeKey).bind()
            taskRepository.createTask(scopeKey, title).bind()
        }

    override fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>> = taskRepository.getTask(id)

    override fun updateTitle(
        id: TaskId,
        title: Title,
        author: HeroId
    ): EitherNel<UpdateTitleError, Task<*>> =
        either {
            taskRepository.areHeroesInScope(HeroIds(author), id).bind()
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
            taskRepository.areHeroesInScope(assignees + author, id.scope).bind()
            taskRepository.assignTask(id, assignees).bind()
        }

    override fun startWork(
        id: PendingTaskId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTask> =
        either {
            val task = taskRepository.areHeroesInScope(HeroIds(author), id).bind()
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
            taskRepository.areHeroesInScope(HeroIds(author), verifiedTask.scopeKey()).bind()
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
            taskRepository.areHeroesInScope(HeroIds(author), verifiedTask.scopeKey()).bind()
            taskRepository.endWork(id).bind()
        }
}