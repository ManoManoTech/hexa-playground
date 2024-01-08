package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

interface HeroRepository {
    fun currentHero(): EitherNel<CurrentHeroError, HeroId>

    fun getHero(author: HeroId): EitherNel<GetHeroError, Hero>

    fun canHeroCreateTask(creator: HeroId): EitherNel<CreateTaskError, Hero>
    fun canHeroUpdateTaskTitle(author: HeroId): EitherNel<UpdateTitleError, Hero>
    fun canHeroUpdateDescriptionTitle(author: HeroId): EitherNel<UpdateDescriptionError, Hero>
    fun assignableHeroes(id: TaskId): EitherNel<AssignableHeroesError, Heroes>
    fun areAllHeroesAssignable(id: TaskId, assignees: HeroIds): EitherNel<AssignTaskError, Heroes>
    fun canHeroStartWork(id: PendingTaskId, author: HeroId): EitherNel<StartWorkError, Hero>
}

sealed interface HeroRepositoryError {
    val message: String
}

sealed interface GetHeroError : HeroRepositoryError

data class HeroDoesNotExistError(val heroId: HeroId) : GetHeroError {
    override val message = "Hero $heroId does not exist"
}
