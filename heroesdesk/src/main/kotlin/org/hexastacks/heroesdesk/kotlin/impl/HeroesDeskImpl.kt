package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.nonEmptyListOf
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.ports.*
import org.hexastacks.heroesdesk.kotlin.ports.UserRepositoryExtensions.canHeroStartWork

class HeroesDeskImpl(private val userRepository: UserRepository, private val taskRepository: TaskRepository) :
    HeroesDesk {
    override fun createScope(scopeKey: ScopeKey, name: Name, creator: AdminId): EitherNel<CreateScopeError, Scope> =
        userRepository
            .getAdmin(creator)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is AdminDoesNotExistError -> AdminDoesNotExistCreateScopeError(creator)
                    }
                }
            }
            .flatMap {
                taskRepository.createScope(scopeKey, name)
            }

    override fun assignScope(
        scopeKey: ScopeKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnScopeError, Scope> =
        userRepository
            .getHeroes(assignees)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is HeroesDoNotExistError ->
                            AssignedHeroIdsNotExistAssignHeroesOnScopeError(it.heroIds, assignees)
                    }
                }
            }
            .flatMap { heroes ->
                userRepository
                    .getAdmin(changeAuthor)
                    .mapLeft { errors ->
                        errors.map { it: GetAdminError ->
                            when (it) {
                                is AdminDoesNotExistError -> AdminIdNotExistingAssignHeroesOnScopeError(it.adminId)
                            }
                        }
                    }
                    .map { _ -> heroes }
            }.flatMap {
                taskRepository.assignScope(scopeKey, it)
            }

    override fun updateScopeName(
        scopeKey: ScopeKey,
        name: Name,
        changeAuthor: AdminId
    ): EitherNel<UpdateScopeNameError, Scope> =
        userRepository
            .getAdmin(changeAuthor)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is AdminDoesNotExistError ->
                            AdminIdNotExistingUpdateScopeNameError(changeAuthor)
                    }
                }
            }
            .flatMap {
                taskRepository.updateScopeName(scopeKey, name)
            }

    override fun getScope(scopeKey: ScopeKey): EitherNel<GetScopeError, Scope> =
        taskRepository.getScope(scopeKey)

    override fun createTask(
        scopeKey: ScopeKey,
        title: Title,
        creator: HeroId
    ): EitherNel<CreateTaskError, PendingTask> =
        userRepository
            .getHero(creator)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is HeroesDoNotExistError -> HeroDoesNotExistCreateTaskError(creator)
                    }
                }
            }
            .flatMap { hero -> taskRepository.createTask(scopeKey, title, hero,) }

    override fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>> = taskRepository.getTask(id)

    override fun updateTitle(
        id: TaskId,
        title: Title,
        author: HeroId
    ): EitherNel<UpdateTitleError, TaskId> =
        userRepository
            .getHero(author)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is HeroesDoNotExistError -> HeroDoesNotExistUpdateTitleError(author)
                    }
                }
            }
            .flatMap { hero -> taskRepository.updateTitle(id, title, hero) }

    override fun updateDescription(
        id: TaskId,
        description: Description,
        author: HeroId
    ): EitherNel<UpdateDescriptionError, TaskId> =
        userRepository
            .getHero(author)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is HeroesDoNotExistError -> HeroDoesNotExistUpdateDescriptionError(author)
                    }
                }
            }
            .flatMap { taskRepository.updateDescription(id, description, it) }

    override fun assignTask(
        id: TaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> =
        userRepository
            .areAllHeroesAssignable(id, assignees)
            .flatMap { taskRepository.assign(id, it, author) }

    override fun startWork(
        id: PendingTaskId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTask> =
        taskRepository
            .getTask(id)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is TaskDoesNotExistError -> TaskDoesNotExistStartWorkError(id)
                    }
                }
            }
            .flatMap { task ->
                when (task) {
                    is PendingTask -> Right(task)
                    else -> Left(nonEmptyListOf(TaskNotPendingStartWorkError(task, id)))
                }
            }
            .flatMap { task: PendingTask ->
                if (!task.assignees.contains(author)) {
                    userRepository.canHeroStartWork(task.taskId, author)
                        .flatMap { hero ->
                            taskRepository
                                .assign(id, task.assignees.add(hero), author)
                                .mapLeft { errors ->
                                    errors.map {
                                        when (it) {
                                            is NonAssignableHeroesAssignTaskError -> NonAllowedToStartWorkError(
                                                id,
                                                HeroIds(author)
                                            )

                                            is TaskDoesNotExistAssignTaskError -> TaskDoesNotExistStartWorkError(id)
                                        }
                                    }
                                }
                                .flatMap {
                                    when (it) {
                                        is PendingTask -> Right(it)
                                        else -> Left(nonEmptyListOf(TaskNotPendingStartWorkError(it, id)))
                                    }
                                }
                        }
                } else Right(task)
            }
            .flatMap { task: PendingTask ->
                userRepository
                    .canHeroStartWork(task, author)
                    .flatMap {
                        taskRepository.startWork(id, it)
                    }
            }

    override fun startWork(
        id: DoneTaskId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTaskId> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(
        id: InProgressTaskId,
        author: HeroId
    ): EitherNel<StopWorkError, PendingTaskId> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(
        id: DoneTaskId,
        author: HeroId
    ): EitherNel<StopWorkError, PendingTaskId> {
        TODO("Not yet implemented")
    }

    override fun endWork(id: PendingTaskId, author: HeroId): EitherNel<EndWorkError, DoneTaskId> {
        TODO("Not yet implemented")
    }

    override fun endWork(
        id: InProgressTaskId,
        author: HeroId
    ): EitherNel<EndWorkError, DoneTaskId> {
        TODO("Not yet implemented")
    }
}