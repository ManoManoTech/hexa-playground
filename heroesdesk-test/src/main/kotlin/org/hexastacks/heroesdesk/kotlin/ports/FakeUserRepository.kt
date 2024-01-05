package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.HeroName
import org.hexastacks.heroesdesk.kotlin.impl.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import java.util.concurrent.ConcurrentHashMap

class FakeUserRepository : InstrumentedUserRepository {
    companion object {
        val NON_EXISTING_USER_ID: HeroId =
            HeroId("nonExistingUser").getOrElse { throw RuntimeException("nonExistingUser should be valid") }
    }

    private val existingHeroes = ConcurrentHashMap.newKeySet<Hero>()

    private val assignableHeroes = ConcurrentHashMap<TaskId, Heroes>()
    override fun defineAssignableHeroes(taskId: TaskId, heroesToCreateIfNeeded: Heroes) {
        ensureExisting(heroesToCreateIfNeeded)
        assignableHeroes[taskId] = heroesToCreateIfNeeded
    }

    override fun assignableHeroes(id: TaskId): EitherNel<AssignableHeroesError, Heroes> {
        return assignableHeroes[id]
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(TaskDoesNotExistAssignableHeroesError(id)))
    }

    private fun ensureExisting(heroes: Heroes) {
        heroes.value.forEach { hero ->
            existingHeroes.add(hero)
        }
    }

    override fun currentHero(): EitherNel<CurrentHeroError, HeroId> =
        Right(HeroId("1")
            .getOrElse {
                throw RuntimeException("HeroId(1) should be valid")
            }
        )

    override fun canUserCreateTask(creator: HeroId): EitherNel<CreateTaskError, Hero> =
        if (creator == NON_EXISTING_USER_ID) {
            Left(nonEmptyListOf(HeroDoesNotExistError(creator)))
        } else {
            Right(
                Hero(
                    HeroName(creator.value).getOrElse { throw RuntimeException("failing to create hero name  from $creator") },
                    creator
                )
            )
        }

    override fun canUserUpdateTaskTitle(author: HeroId): EitherNel<UpdateTitleError, Hero> =
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

    override fun canUserUpdateDescriptionTitle(author: HeroId): EitherNel<UpdateDescriptionError, Hero> =
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
