package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.nonEmptyListOf
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.UserRepository


class HeroesDeskImpl(private val userRepository: UserRepository, private val taskRepository: TaskRepository) :
    HeroesDesk {
    override fun createScope(scopeKey: ScopeKey, name: Name, creator: AdminId): EitherNel<CreateScopeError, Scope> =
        userRepository
            .getAdmin(creator)
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
            .flatMap { heroes ->
                userRepository
                    .getAdmin(changeAuthor)
                    .map { _ -> heroes }
            }
            .flatMap {
                taskRepository.assignScope(scopeKey, it)
            }

    override fun updateScopeName(
        scopeKey: ScopeKey,
        name: Name,
        changeAuthor: AdminId
    ): EitherNel<UpdateScopeNameError, Scope> =
        userRepository
            .getAdmin(changeAuthor)
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
            .flatMap { hero ->
                taskRepository
                    .getScope(scopeKey)
                    .flatMap { scope ->
                        if (scope.assignees.containsNot(hero))
                            Left(nonEmptyListOf(HeroesNotInScopeError(creator, scopeKey)))
                        else
                            Right(Pair(scope, hero))
                    }
            }
            .flatMap { scopeAndHero -> taskRepository.createTask(scopeAndHero.first, title, scopeAndHero.second) }

    override fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>> = taskRepository.getTask(id)

    override fun updateTitle(
        id: TaskId,
        title: Title,
        author: HeroId
    ): EitherNel<UpdateTitleError, Task<*>> =
        userRepository
            .getHero(author)
            .flatMap { hero -> taskRepository.updateTitle(id, title, hero) }

    override fun updateDescription(
        id: TaskId,
        description: Description,
        author: HeroId
    ): EitherNel<UpdateDescriptionError, Task<*>> =
        userRepository
            .getHero(author)
            .flatMap { taskRepository.updateDescription(id, description, it) }

    override fun assignTask(
        id: PendingTaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> = doAssignTask(id, assignees, author)

    override fun assignTask(
        id: InProgressTaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> = doAssignTask(id, assignees, author)

    private fun doAssignTask(
        id: TaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> =
        taskRepository.getTask(id)
            .flatMap { task ->
                userRepository
                    .getHeroes(assignees + author)
                    .flatMap { authorAndAssignees ->
                        val nonScopeAssignedHeroes = authorAndAssignees.subtract(task.scope.assignees)
                        if (nonScopeAssignedHeroes.isNotEmpty()) {
                            Left(
                                nonEmptyListOf(
                                    HeroesNotInScopeError(
                                        nonScopeAssignedHeroes,
                                        task.scope.key
                                    )
                                )
                            )

                        } else Right(authorAndAssignees)

                    }
                    .flatMap { taskRepository.assignTask(id, it, author) }
            }


    override fun startWork(
        id: PendingTaskId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTask> =
        taskRepository
            .getTask(id)
            .flatMap { task ->
                when (task) {
                    is PendingTask -> Right(task)
                    else -> Left(nonEmptyListOf(TaskNotPendingError(task, id)))
                }
            }
            .flatMap { verifiedTask ->
                userRepository
                    .getHero(author)
                    .flatMap { existingAuthor ->
                        if (verifiedTask.scope.assignees.containsNot(existingAuthor)) {
                            Left(
                                nonEmptyListOf(
                                    HeroesNotInScopeError(
                                        author,
                                        verifiedTask.scope.key
                                    )
                                )
                            )
                        } else
                            if (verifiedTask.assignees.isNotEmpty()) {
                                Right(existingAuthor)
                            } else {
                                assignAuthorToTask(verifiedTask, existingAuthor, id)
                            }
                    }
            }
            .flatMap { hero ->
                taskRepository.startWork(id, hero)
            }

    private fun assignAuthorToTask(
        verifiedTask: PendingTask,
        author: Hero,
        id: PendingTaskId
    ): EitherNel<StartWorkError, Hero> =
        taskRepository
            .assignTask(id, verifiedTask.assignees.add(author), author.id)
            .flatMap {
                when (it) {
                    is PendingTask -> Right(author)
                    else -> Left(nonEmptyListOf(TaskNotPendingError(it, id)))
                }
            }

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
        taskRepository
            .getTask(id)
            .flatMap { task ->
                when (task) {
                    is InProgressTask -> Right(task)
                    else -> Left(nonEmptyListOf(TaskNotInProgressError(task, id)))
                }
            }
            .flatMap { verifiedTask ->
                userRepository
                    .getHero(author)
                    .flatMap { existingAuthor ->
                        if (verifiedTask.scope.assignees.containsNot(existingAuthor)) {
                            Left(
                                nonEmptyListOf(
                                    HeroesNotInScopeError(
                                        author,
                                        verifiedTask.scope.key
                                    )
                                )
                            )
                        } else
                            Right(existingAuthor)
                    }
            }
            .flatMap { hero ->
                taskRepository.pauseWork(id, hero)
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
        taskRepository
            .getTask(id)
            .flatMap { task ->
                when (task) {
                    is InProgressTask -> Right(task)
                    else -> Left(nonEmptyListOf(TaskNotInProgressError(task, id)))
                }
            }
            .flatMap { verifiedTask ->
                userRepository
                    .getHero(author)
                    .flatMap { existingAuthor ->
                        if (verifiedTask.scope.assignees.containsNot(existingAuthor)) {
                            Left(
                                nonEmptyListOf(
                                    HeroesNotInScopeError(
                                        author,
                                        verifiedTask.scope.key
                                    )
                                )
                            )
                        } else
                            Right(existingAuthor)
                    }
            }
            .flatMap { hero ->
                taskRepository.endWork(id, hero)
            }

}