package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import java.util.concurrent.ConcurrentHashMap

class FakeHeroRepository : InstrumentedHeroRepository {
    companion object {
        val NON_EXISTING_USER_ID: HeroId =
            HeroId("nonExistingUser")
                .getOrElse { throw RuntimeException("nonExistingUser should be valid") }
    }

    private val existingHeroes = ConcurrentHashMap.newKeySet<Hero>()

    private val assignableHeroes = ConcurrentHashMap<TaskId, Heroes>()

    private val workableHeroes = ConcurrentHashMap<TaskId, Heroes>()

    override fun defineAssignableHeroes(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes {
        val heroes = ensureExisting(heroesToCreateIfNeeded)
        assignableHeroes[taskId] = heroes
        return heroes
    }

    override fun defineWorkableHeroes(taskId: TaskId, heroesToCreateIfNeeded: Heroes): Heroes {
        val heroes = ensureExisting(heroesToCreateIfNeeded)
        workableHeroes[taskId] = heroes
        return heroes
    }

    override fun assignableHeroes(taskId: TaskId): EitherNel<AssignableHeroesError, Heroes> {
        return assignableHeroes[taskId]
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistAssignableHeroesError(taskId)))
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
                                .map { id ->
                                    existingHeroes.first { hero -> hero.id == id }
                                } //FIXME
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
                        is HeroDoesNotExistError -> HeroDoesNotExistStartWorkError(author)
                    }
                }
            }
            .flatMap { _ ->
                Left(nonEmptyListOf(NonAllowedToStartWorkError(id, HeroIds(listOf(author)))))
            }

    override fun getHero(heroId: HeroId): EitherNel<GetHeroError, Hero> =
        existingHeroes
            .firstOrNull { it.id == heroId }
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(HeroDoesNotExistError(heroId)))

    override fun ensureExisting(heroes: Heroes): Heroes {
        heroes.forEach { ensureExisting(it) }
        return heroes
    }

    override fun ensureExisting(hero: Hero): Hero {
        existingHeroes.add(hero)
        return hero
    }

    override fun currentHero(): EitherNel<CurrentHeroError, HeroId> =
        HeroName("currrentHero")
            .flatMap { name ->
                HeroId("${name.value}Id")
                    .map { id ->
                        Right(ensureExisting(Hero(name, id)).id)
                    }
            }
            .getOrElse {
                throw RuntimeException("HeroId(1) should be valid")
            }

    override fun canHeroCreateTask(heroId: HeroId): EitherNel<CreateTaskError, Hero> =
        if (heroId == NON_EXISTING_USER_ID) {
            Left(nonEmptyListOf(HeroDoesNotExistCreateTaskError(heroId)))
        } else {
            Right(
                Hero(
                    HeroName(heroId.value).getOrElse { throw RuntimeException("failing to create hero name  from $heroId") },
                    heroId
                )
            )
        }

    override fun canHeroUpdateTaskTitle(heroId: HeroId): EitherNel<UpdateTitleError, Hero> =
        if (heroId == NON_EXISTING_USER_ID) {
            Left(nonEmptyListOf(HeroDoesNotExistUpdateTitleError(heroId)))
        } else {
            Right(
                Hero(
                    HeroName(heroId.value).getOrElse { throw RuntimeException("failing to create hero name  from $heroId") },
                    heroId
                )
            )
        }

    override fun canHeroUpdateDescriptionTitle(heroId: HeroId): EitherNel<UpdateDescriptionError, Hero> =
        if (heroId == NON_EXISTING_USER_ID) {
            Left(nonEmptyListOf(HeroDoesNotExistUpdateDescriptionError(heroId)))
        } else {
            Right(
                Hero(
                    HeroName(heroId.value).getOrElse { throw RuntimeException("failing to create hero name  from $heroId") },
                    heroId
                )
            )
        }
}
