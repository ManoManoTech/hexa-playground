package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.*

interface UserRepository {
    fun getHero(heroId: HeroId): EitherNel<GetHeroError, Hero>
    fun getAdmin(adminId: AdminId): EitherNel<GetAdminError, Admin>

    fun canHeroCreateTask(heroId: HeroId): EitherNel<CreateTaskError, Hero>
    fun canHeroUpdateTaskTitle(heroId: HeroId): EitherNel<UpdateTitleError, Hero>
    fun canHeroUpdateDescriptionTitle(heroId: HeroId): EitherNel<UpdateDescriptionError, Hero>
    fun assignableHeroes(taskId: TaskId): EitherNel<AssignableHeroesError, Heroes>
    fun areAllHeroesAssignable(taskId: TaskId, heroIds: HeroIds): EitherNel<AssignTaskError, Heroes>
    fun canHeroStartWork(pendingTaskId: PendingTaskId, heroId: HeroId): EitherNel<StartWorkError, Hero>
}

sealed interface HeroRepositoryError {
    val message: String
}

sealed interface GetHeroError : HeroRepositoryError

data class HeroDoesNotExistError(val heroId: HeroId) : GetHeroError {
    override val message = "Hero $heroId does not exist"
}
sealed interface GetAdminError : HeroRepositoryError

data class AdminDoesNotExistError(val adminId: AdminId) : GetAdminError {
    override val message = "Admin $adminId does not exist"
}
