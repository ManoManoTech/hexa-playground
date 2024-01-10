package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

interface HeroRepository {
    fun getHero(heroId: HeroId): EitherNel<GetHeroError, Hero>
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
