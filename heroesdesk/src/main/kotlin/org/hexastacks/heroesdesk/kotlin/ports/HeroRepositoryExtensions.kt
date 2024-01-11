package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTask

object HeroRepositoryExtensions {
    fun UserRepository.canHeroStartWork(task: PendingTask, heroId: HeroId): EitherNel<HeroesDesk.StartWorkError, Hero> =
        this.canHeroStartWork(task.taskId, heroId)

//  fun ScopeId(scopeName: Name): Either<NonEmptyList<ScopeIdError>, ScopeId> = invoke(scopeName.value.take(3)) ??
}