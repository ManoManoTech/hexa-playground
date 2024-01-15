package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.*
import java.util.concurrent.ConcurrentHashMap

class FakeUserRepository : InstrumentedUserRepository {

    private val users = ConcurrentHashMap.newKeySet<User<*>>()

    private val assignableHeroes = ConcurrentHashMap<TaskId, Heroes>()

    private val workableHeroes = ConcurrentHashMap<TaskId, Heroes>()

    override fun defineAssignableHeroes(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes {
        val heroes = ensureExisting(heroesToCreateIfNeeded)
        assignableHeroes[taskId] = heroes
        return heroes
    }

    override fun defineHeroesAbleToChangeStatus(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes {
        val heroes = ensureExisting(heroesToCreateIfNeeded)
        workableHeroes[taskId] = heroes
        return heroes
    }

    override fun areAllHeroesAssignable(
        taskId: TaskId,
        heroIds: HeroIds
    ): EitherNel<AssignTaskError, Heroes> =
        assignableHeroes[taskId]
            ?.let { heroes ->
                val candidates: Set<Hero> = heroes.value
                val (assignableHeroes, nonAssignableHeroes) =
                    heroIds
                        .value
                        .partition { heroId ->
                            candidates
                                .map { it.id }
                                .contains(heroId)
                        }
                if (nonAssignableHeroes.isEmpty()) {
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
                            NonAssignableHeroesAssignTaskError(
                                taskId,
                                heroIds,
                                HeroIds(nonAssignableHeroes)
                            )
                        )
                    )
                }
            }
            ?: Left(nonEmptyListOf(TaskDoesNotExistAssignTaskError(taskId)))

    override fun canHeroStartWork(pendingTaskId: PendingTaskId, heroId: HeroId): EitherNel<StartWorkError, Hero> =
        workableHeroes[pendingTaskId]
            ?.let { heroes ->
                if (heroes.contains(heroId)) {
                    heroes[heroId]
                        ?.let { Right(it) }
                        ?: Left(nonEmptyListOf(HeroDoesNotExistStartWorkError(heroId)))
                } else {
                    nonWorkableHeroHandling(heroId, pendingTaskId)
                }
            }
            ?: nonWorkableHeroHandling(heroId, pendingTaskId)

    private fun nonWorkableHeroHandling(
        author: HeroId,
        id: PendingTaskId
    ): Either<NonEmptyList<StartWorkError>, Nothing> =
        getHero(author)
            .mapLeft { errors ->
                errors.map {
                    when (it) {
                        is HeroesDoNotExistError -> HeroDoesNotExistStartWorkError(author)
                    }
                }
            }
            .flatMap { _ ->
                Left(nonEmptyListOf(NonAllowedToStartWorkError(id, HeroIds(listOf(author)))))
            }

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
                    HeroesDoNotExistError(heroIds)
                )
            )
        }
    }

    override fun getAdmin(adminId: AdminId): EitherNel<GetAdminError, Admin> =
        users
            .firstOrNull { it.id == adminId }
            ?.asAdmin()
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(AdminDoesNotExistError(adminId)))

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

    override fun canHeroCreateTask(heroId: HeroId): EitherNel<CreateTaskError, Hero> =
        Right(
            Hero(
                UserName(heroId.value).getOrElse { throw RuntimeException("failing to create hero name  from $heroId") },
                heroId
            )
        )

    override fun canHeroUpdateTaskTitle(heroId: HeroId): EitherNel<UpdateTitleError, Hero> =
        Right(
            Hero(
                UserName(heroId.value).getOrElse { throw RuntimeException("failing to create hero name  from $heroId") },
                heroId
            )
        )

    override fun canHeroUpdateDescriptionTitle(heroId: HeroId): EitherNel<UpdateDescriptionError, Hero> =
        Right(
            Hero(
                UserName(heroId.value).getOrElse { throw RuntimeException("failing to create hero name  from $heroId") },
                heroId
            )
        )
}
