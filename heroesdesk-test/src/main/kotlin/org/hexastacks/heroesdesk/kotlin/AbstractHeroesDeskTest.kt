package org.hexastacks.heroesdesk.kotlin

import arrow.core.flatMap
import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestExtensions.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestExtensions.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestExtensions.createPendingTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestExtensions.createTaskOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestExtensions.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestExtensions.currentHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestExtensions.getTaskOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.Heroes.Companion.EMPTY_HEROES
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedHeroRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

abstract class AbstractHeroesDeskTest {

    @Test
    fun `current hero returns a hero`() {
        val currentHero = heroesDesk().currentHero()

        assertTrue(currentHero.isRight())
        currentHero.onRight {
            assertTrue(it.value.isNotEmpty())
        }
    }

    @Test
    fun `createTask returns a task`() {
        val currentHero = heroesDesk().currentHeroOrThrow()
        val rawTitle = "title"

        val task = heroesDesk().createTask(createTitleOrThrow(rawTitle), currentHero)

        assertTrue(task.isRight())
        task.onRight {
            assertEquals(it.title.value, rawTitle)
            assertEquals(it.creator.id, currentHero)
        }
    }

    @Test
    fun `2 tasks creation with same title and creator returns 2 distinct tasks`() {
        val currentHero = heroesDesk().currentHeroOrThrow()
        val rawTitle = "title"

        val task1 = heroesDesk().createTask(createTitleOrThrow(rawTitle), currentHero)
        val task2 = heroesDesk().createTask(createTitleOrThrow(rawTitle), currentHero)

        assertTrue(task1.isRight())
        assertTrue(task2.isRight())
        task1.flatMap { right1 ->
            task2.map { right2 ->
                right1 != right2
            }
        }
    }

    @Test
    fun `createTask with a non existing user fails`() {
        val currentHero = nonExistingHeroId()
        val rawTitle = "title"
        val task = heroesDesk().createTask(createTitleOrThrow(rawTitle), currentHero)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is HeroDoesNotExistCreateTaskError)
        }
    }

    @Test
    fun `get task works on existing TaskId`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")

        val retrievedTask = heroesDesk.getTask(createdTask.taskId)

        assertTrue(retrievedTask.isRight())
        retrievedTask.onRight {
            assertEquals(it, createdTask)
        }
    }

    @Test
    fun `get task fails on non existing TaskId`() {
        val task = heroesDesk().getTask(nonExistingPendingTaskId())

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is TaskDoesNotExistError)
        }
    }

    @Test
    fun `update title works on existing TaskId`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId = heroesDesk.updateTitle(createdTask.taskId, newTitle, heroesDesk.currentHeroOrThrow())

        assertTrue(updatedTaskId.isRight())
        updatedTaskId.onRight {
            assertEquals(it, createdTask.taskId)
            assert(heroesDesk.getTaskOrThrow(it).title == newTitle)
        }
    }

    @Test
    fun `update title fails with non existing TaskId`() {
        val heroesDesk = heroesDesk()
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId =
            heroesDesk.updateTitle(nonExistingPendingTaskId(), newTitle, heroesDesk.currentHeroOrThrow())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is TaskDoesNotExistUpdateTitleError)
        }
    }

    @Test
    fun `update title fails with non existing hero`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId = heroesDesk.updateTitle(createdTask.taskId, newTitle, nonExistingHeroId())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroDoesNotExistUpdateTitleError)
        }
    }

    @Test
    fun `update description works on existing TaskId`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTaskId =
            heroesDesk.updateDescription(createdTask.taskId, newDescription, heroesDesk.currentHeroOrThrow())

        assertTrue(updatedTaskId.isRight())
        updatedTaskId.onRight {
            assertEquals(it, createdTask.taskId)
            assert(heroesDesk.getTaskOrThrow(it).description == newDescription)
        }
    }

    @Test
    fun `update description fails with non existing TaskId`() {
        val heroesDesk = heroesDesk()
        val newDescription = createDescriptionOrThrow("new description")


        val updatedTaskId =
            heroesDesk.updateDescription(nonExistingPendingTaskId(), newDescription, heroesDesk.currentHeroOrThrow())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is TaskDoesNotExistUpdateDescriptionError)
        }
    }

    @Test
    fun `update description fails with non existing hero`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTaskId = heroesDesk.updateDescription(createdTask.taskId, newDescription, nonExistingHeroId())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroDoesNotExistUpdateDescriptionError)
        }
    }

    @Test
    fun `assignable heroes returns avail heroes when some`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        instrumentedUserRepository.defineAssignableHeroes(
            createdTask.taskId,
            Heroes(createHeroOrThrow("heroId1"), createHeroOrThrow("heroId2"))
        )

        val assignableHeroes = heroesDesk.assignableHeroes(createdTask.taskId)

        assertTrue(assignableHeroes.isRight())
        assignableHeroes.onRight {
            assertTrue(it.value.isNotEmpty())
        }
    }

    @Test
    fun `assignable heroes returns no heroes when none`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        instrumentedUserRepository.defineAssignableHeroes(createdTask.taskId, EMPTY_HEROES)

        val assignableHeroes = heroesDesk.assignableHeroes(createdTask.taskId)

        assertTrue(assignableHeroes.isRight())
        assignableHeroes.onRight {
            assertTrue(it.value.isEmpty())
        }
    }

    @Test
    fun `assignable heroes fails on non existing task id`() {
        val heroesDesk = heroesDesk()

        val assignableHeroes = heroesDesk.assignableHeroes(nonExistingPendingTaskId())

        assertTrue(assignableHeroes.isLeft())
        assignableHeroes.onLeft {
            assertTrue(it.head is TaskDoesNotExistAssignableHeroesError)
        }
    }

    @Test
    fun `assign task works`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val heroes =
            instrumentedUserRepository
                .defineAssignableHeroes(
                    createdTask.taskId,
                    Heroes(createHeroOrThrow("heroId1"))
                )

        val assignedTask =
            heroesDesk.assignTask(
                createdTask.taskId,
                HeroIds(heroes.value.map { it.id }),
                heroesDesk.currentHeroOrThrow()
            )

        assertTrue(assignedTask.isRight())
        assignedTask.onRight {
            assertEquals(it.taskId, createdTask.taskId)
            assertEquals(it.title, createdTask.title)
            assertEquals(it.description, createdTask.description)
            assertEquals(heroes, it.assignees)
        }
    }

    @Test
    fun `assign task fails on non existing task`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val heroes = Heroes(createHeroOrThrow("heroId1"))

        val assignedTask =
            heroesDesk.assignTask(
                nonExistingPendingTaskId(),
                HeroIds(heroes.value.map { it.id }),
                heroesDesk.currentHeroOrThrow()
            )

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is TaskDoesNotExistAssignTaskError)
        }
    }

    @Test
    fun `assign task fails on non assignable users`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val heroes = Heroes(createHeroOrThrow("heroId1"))
        instrumentedUserRepository
            .defineAssignableHeroes(
                createdTask.taskId,
                Heroes(createHeroOrThrow("heroId2"))
            )

        val assignedTask =
            heroesDesk.assignTask(
                createdTask.taskId,
                HeroIds(heroes.value.map { it.id }),
                heroesDesk.currentHeroOrThrow()
            )

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is NonAssignableHeroesAssignTaskError)
        }
    }

    @Test
    fun `start work works on existing & pending task`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val taskId = createdTask.taskId
        val hero = instrumentedUserRepository.ensureExistingOrThrow("heroId1")
        val heroes = Heroes(hero)
        instrumentedUserRepository.defineAssignableHeroes(
            taskId,
            heroes
        )
        heroesDesk.assignTask(taskId, HeroIds(hero.id), hero.id).getOrElse { throw AssertionError() }
        instrumentedUserRepository.defineWorkableHeroes(
            taskId,
            heroes
        )

        val updatedTaskId =
            heroesDesk.startWork(taskId, hero.id)

        assertTrue(updatedTaskId.isRight())
        updatedTaskId
            .onRight {
                assertEquals(it.taskId.value, taskId.value)//TODO: handle at TaskId level
            }
    }

    @Test
    fun `start work assigns hero starting work to task`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val taskId = createdTask.taskId
        val hero = instrumentedUserRepository.ensureExistingOrThrow("heroId1")
        val heroes = Heroes(hero)
        instrumentedUserRepository.defineAssignableHeroes(
            taskId,
            heroes
        )
        instrumentedUserRepository.defineWorkableHeroes(
            taskId,
            heroes
        )
        assertTrue(createdTask.assignees.isEmpty())

        val updatedTaskId =
            heroesDesk.startWork(taskId, hero.id)

        assertTrue(updatedTaskId.isRight())
        updatedTaskId
            .onRight {
                assertEquals(it.taskId.value, taskId.value)
                assertTrue(it.assignees.contains(hero.id))
            }
    }

    @Test
    fun `start work fails with non existing TaskId`() {
        val heroesDesk = heroesDesk()

        val updatedTaskId =
            heroesDesk.startWork(nonExistingPendingTaskId(), heroesDesk.currentHeroOrThrow())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId
            .onLeft {
                assertTrue(it.head is TaskDoesNotExistStartWorkError)
            }
    }

    @Test
    fun `start work fails with non existing hero`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        instrumentedUserRepository.defineAssignableHeroes(
            createdTask.taskId,
            EMPTY_HEROES
        )
        instrumentedUserRepository.defineWorkableHeroes(
            createdTask.taskId,
            EMPTY_HEROES
        )

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, nonExistingHeroId())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroDoesNotExistStartWorkError)
        }
    }

    @Test
    fun `start work fails with hero lacking the right to`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val taskId = createdTask.taskId
        val hero = instrumentedUserRepository.ensureExistingOrThrow("heroId1")
        val heroes = Heroes(hero)
        instrumentedUserRepository.defineAssignableHeroes(
            taskId,
            heroes
        )
        instrumentedUserRepository.defineWorkableHeroes(
            taskId,
            EMPTY_HEROES
        )

        val updatedTaskId =
            heroesDesk.startWork(taskId, hero.id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is NonAllowedToStartWorkError)
        }
    }

    private fun nonExistingPendingTaskId(): PendingTaskId = createPendingTaskIdOrThrow(nonExistingRawTaskId())

    private fun heroesDesk(): HeroesDesk = heroesDesk(instrumentedHeroRepository())

    abstract fun instrumentedHeroRepository(): InstrumentedHeroRepository

    abstract fun heroesDesk(instrumentedUserRepository: InstrumentedHeroRepository): HeroesDesk

    abstract fun nonExistingHeroId(): HeroId

    abstract fun nonExistingRawTaskId(): String

}

