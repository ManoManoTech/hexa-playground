package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

interface UserRepository {
    fun currentHero(): EitherNel<CurrentHeroError, HeroId>
    fun canUserCreateTask(creator: HeroId): EitherNel<CreateTaskError, Hero>
    fun canUserUpdateTaskTitle(author: HeroId): EitherNel<UpdateTitleError, Hero>
    fun canUserUpdateDescriptionTitle(author: HeroId): EitherNel<UpdateDescriptionError, Hero>
    fun assignableHeroes(id: TaskId): EitherNel<AssignableHeroesError, Heroes>
}