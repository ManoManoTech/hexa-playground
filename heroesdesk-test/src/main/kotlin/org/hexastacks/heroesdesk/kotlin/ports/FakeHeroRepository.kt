package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
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

    override fun assignableHeroes(id: TaskId): EitherNel<AssignableHeroesError, Heroes> {
        return assignableHeroes[id]
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistAssignableHeroesError(id)))
    }

    override fun areAllHeroesAssignable(
        id: TaskId,
        assignees: HeroIds
    ): EitherNel<AssignTaskError, Heroes> =
        assignableHeroes[id]
            ?.let { heroes ->
                val candidates = heroes.value
                val (assignableHeroes, nonAssignableHeroes) =
                    assignees.value.partition { heroId ->
                        candidates.map { it.id }.contains(heroId)
                    }
                if (nonAssignableHeroes.isEmpty()) {
                    Right(
                        Heroes(
                            assignableHeroes
                                .map { id -> existingHeroes.first { hero -> hero.id == id } } //FIXME
                                .toSet())
                    )
                } else {
                    Left(
                        nonEmptyListOf(
                            NonAssignableHeroesAssignTaskError(
                                id,
                                assignees,
                                HeroIds(nonAssignableHeroes)
                            )
                        )
                    )
                }
            }
            ?: Left(nonEmptyListOf(TaskDoesNotExistAssignTaskError(id)))

    override fun canHeroStartWork(id: PendingTaskId, author: HeroId): EitherNel<StartWorkError, Hero> =
        workableHeroes[id]
            ?.let { heroes ->
                if (heroes.contains(author)) {
                    heroes[author]
                        ?.let { Right(it) }
                        ?: Left(nonEmptyListOf(HeroDoesNotExistStartWorkError(author)))
                } else {
                    Left(nonEmptyListOf(NonAssignableHeroStartWorkError(id, HeroIds(listOf(author)))))
                }
            }
            ?: Left(nonEmptyListOf(TaskDoesNotExistStartWorkError(id)))

    override fun getHero(author: HeroId): EitherNel<GetHeroError, Hero> =
        existingHeroes
            .firstOrNull { it.id == author }
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(HeroDoesNotExistError(author)))

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

    override fun canHeroCreateTask(creator: HeroId): EitherNel<CreateTaskError, Hero> =
        if (creator == NON_EXISTING_USER_ID) {
            Left(nonEmptyListOf(HeroDoesNotExistCreateTaskError(creator)))
        } else {
            Right(
                Hero(
                    HeroName(creator.value).getOrElse { throw RuntimeException("failing to create hero name  from $creator") },
                    creator
                )
            )
        }

    override fun canHeroUpdateTaskTitle(author: HeroId): EitherNel<UpdateTitleError, Hero> =
        if (author == NON_EXISTING_USER_ID) {
            Left(nonEmptyListOf(HeroDoesNotExistUpdateTitleError(author)))
        } else {
            Right(
                Hero(
                    HeroName(author.value).getOrElse { throw RuntimeException("failing to create hero name  from $author") },
                    author
                )
            )
        }

    override fun canHeroUpdateDescriptionTitle(author: HeroId): EitherNel<UpdateDescriptionError, Hero> =
        if (author == NON_EXISTING_USER_ID) {
            Left(nonEmptyListOf(HeroDoesNotExistUpdateDescriptionError(author)))
        } else {
            Right(
                Hero(
                    HeroName(author.value).getOrElse { throw RuntimeException("failing to create hero name  from $author") },
                    author
                )
            )
        }


}
