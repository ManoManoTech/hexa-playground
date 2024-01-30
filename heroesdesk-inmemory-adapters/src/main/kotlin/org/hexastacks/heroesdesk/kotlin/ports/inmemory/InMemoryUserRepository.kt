package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import java.util.concurrent.ConcurrentHashMap
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.ports.UserRepository
import org.hexastacks.heroesdesk.kotlin.user.*

open class InMemoryUserRepository : UserRepository {

    internal val users = ConcurrentHashMap.newKeySet<User<*>>()

    override fun getHeroes(heroIds: HeroIds): EitherNel<GetHeroError, Heroes> {
        val userRawIds = users.map { it.id.value }

        val (assignableHeroes, nonAssignableHeroes) =
            heroIds
                .value
                .partition { heroId ->
                    userRawIds.contains(heroId.value)
                }
        return if (nonAssignableHeroes.isEmpty()) {
            Right(
                Heroes(
                    assignableHeroes
                        .mapNotNull { id ->
                            users
                                .firstOrNull { hero -> hero.id == id }
                                ?.asHero()
                        }
                )
            )
        } else {
            Left(
                nonEmptyListOf(
                    HeroesNotExistingError(heroIds)
                )
            )
        }
    }

    override fun getAdmin(adminId: AdminId): EitherNel<GetAdminError, Admin> =
        users
            .firstOrNull { it.id == adminId }
            ?.asAdmin()
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(AdminNotExistingError(adminId)))

}
