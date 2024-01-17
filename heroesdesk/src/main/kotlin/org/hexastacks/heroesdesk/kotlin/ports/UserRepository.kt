package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.user.*

interface UserRepository {
    fun getHero(heroId: HeroId): EitherNel<GetHeroError, Hero> =
        getHeroes(HeroIds(heroId))
            .flatMap {
                it
                    .firstOrNone()
                    .map { hero -> Right(hero) }
                    .getOrElse {
                        Either.Left(nonEmptyListOf(HeroesDoNotExistError(HeroIds(heroId))))
                    }
            }

    fun getHeroes(heroIds: HeroIds): EitherNel<GetHeroError, Heroes>

    fun getAdmin(adminId: AdminId): EitherNel<GetAdminError, Admin>

}

sealed interface HeroRepositoryError {
    val message: String
}

sealed interface GetHeroError : HeroRepositoryError

data class HeroesDoNotExistError(val heroIds: HeroIds) : GetHeroError {
    override val message = "Heroes $heroIds do not exist"
}

sealed interface GetAdminError : HeroRepositoryError

data class AdminDoesNotExistError(val adminId: AdminId) : GetAdminError {
    override val message = "Admin $adminId does not exist"
}
