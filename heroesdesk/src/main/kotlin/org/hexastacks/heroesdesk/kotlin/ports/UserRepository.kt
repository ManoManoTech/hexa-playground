package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.errors.GetAdminError
import org.hexastacks.heroesdesk.kotlin.errors.GetHeroError
import org.hexastacks.heroesdesk.kotlin.errors.HeroesNotExistingError
import org.hexastacks.heroesdesk.kotlin.user.*

interface UserRepository {
    fun getHero(heroId: HeroId): EitherNel<GetHeroError, Hero> =
        getHeroes(HeroIds(heroId))
            .flatMap {
                it
                    .firstOrNone()
                    .map { hero -> Right(hero) }
                    .getOrElse {
                        Either.Left(nonEmptyListOf(HeroesNotExistingError(HeroIds(heroId))))
                    }
            }

    fun getHeroes(heroIds: HeroIds): EitherNel<GetHeroError, Heroes>

    fun getAdmin(adminId: AdminId): EitherNel<GetAdminError, Admin>

}

