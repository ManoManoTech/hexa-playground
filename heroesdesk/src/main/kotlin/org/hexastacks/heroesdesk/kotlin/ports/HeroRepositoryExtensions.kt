package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTask

object HeroRepositoryExtensions {
    fun HeroRepository.canHeroStartWork(task: PendingTask, heroId: HeroId): EitherNel<HeroesDesk.StartWorkError, Hero> =
        this.canHeroStartWork(task.taskId, heroId)

}