package org.hexastacks.heroesdesk.kotlin.adapters.inmemory

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import org.hexastacks.heroesdesk.kotlin.adapters.InstrumentedUserRepository
import org.hexastacks.heroesdesk.kotlin.impl.user.*
import java.util.concurrent.ConcurrentHashMap
import org.hexastacks.heroesdesk.kotlin.errors.*

class InMemoryUserRepository : InstrumentedUserRepository {

    private val users = ConcurrentHashMap.newKeySet<User<*>>()

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

    override fun ensureExisting(heroes: Heroes): Heroes {
        heroes.forEach { ensureExisting(it) }
        return heroes
    }

    override fun ensureExisting(hero: Hero): Hero {
        users.add(hero)
        return hero
    }

    override fun ensureExisting(admin: Admin): Admin {
        users.add(admin)
        return admin
    }

}