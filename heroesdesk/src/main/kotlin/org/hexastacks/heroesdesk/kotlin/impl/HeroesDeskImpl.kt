package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.EitherNel
import arrow.core.flatMap
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.ports.TaskRepository
import org.hexastacks.heroesdesk.kotlin.ports.HeroRepository

class HeroesDeskImpl(private val heroRepository: HeroRepository, private val taskRepository: TaskRepository) :
    HeroesDesk {
    override fun currentHero(): EitherNel<CurrentHeroError, HeroId> = heroRepository.currentHero()

    override fun createTask(
        title: Title,
        creator: HeroId
    ): EitherNel<CreateTaskError, PendingTask> =
        heroRepository
            .canUserCreateTask(creator)
            .flatMap { hero -> taskRepository.createTask(title, hero) }

    override fun getTask(id: TaskId): EitherNel<GetTaskError, Task<*>> = taskRepository.getTask(id)

    override fun updateTitle(
        id: TaskId,
        title: Title,
        author: HeroId
    ): EitherNel<UpdateTitleError, TaskId> =
        heroRepository
            .canUserUpdateTaskTitle(author)
            .flatMap { hero -> taskRepository.updateTitle(id, title, hero) }

    override fun updateDescription(
        id: TaskId,
        description: Description,
        author: HeroId
    ): EitherNel<UpdateDescriptionError, TaskId> =
        heroRepository
            .canUserUpdateDescriptionTitle(author)
            .flatMap { taskRepository.updateDescription(id, description, it) }

    override fun assignableHeroes(id: TaskId): EitherNel<AssignableHeroesError, Heroes> =
        heroRepository.assignableHeroes(id)

    override fun assignTask(
        id: TaskId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignTaskError, Task<*>> =
        heroRepository
            .areAllHeroesAssignable(id, assignees)
            .flatMap { taskRepository.assign(id, it, author) }


    override fun startWork(
        id: PendingTaskId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressTaskId> = TODO()

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

    override fun delete(id: TaskId, author: HeroId): EitherNel<DeleteTaskError, DeletedTaskId> {
        TODO("Not yet implemented")
    }

    override fun restore(id: DeletedTaskId, author: HeroId): EitherNel<RestoreTaskError, TaskId> {
        TODO("Not yet implemented")
    }
}