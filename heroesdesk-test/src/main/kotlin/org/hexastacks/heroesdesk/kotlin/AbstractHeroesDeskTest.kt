package org.hexastacks.heroesdesk.kotlin

import arrow.core.flatMap
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
import org.hexastacks.heroesdesk.kotlin.impl.task.InProgressTask
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

abstract class AbstractHeroesDeskTest {

    @Test
    fun `current hero returns a hero`() {
        val currentHero = heroesDesk().currentHero()

        assertTrue(currentHero.isRight())
        currentHero.onRight { assertTrue(it.value.isNotEmpty()) }
    }

    @Test
    fun `createTask returns a task`() {
        val currentHero = heroesDesk().currentHeroOrThrow()
        val rawTitle = "title"

        val task = heroesDesk().createTask(createTitleOrThrow(rawTitle), currentHero)

        assertTrue(task.isRight())
        task.onRight {
            assertEquals(it.title.value, rawTitle)
            assertEquals(it.creator.heroId, currentHero)
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
        task.onLeft { assertTrue(it.head is HeroDoesNotExistError) }
    }

    @Test
    fun `get task works on existing TaskId`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")

        val retrievedTask = heroesDesk.getTask(createdTask.taskId)

        assertTrue(retrievedTask.isRight())
        retrievedTask.onRight { assertEquals(it, createdTask) }
    }

    @Test
    fun `get task fails on non existing TaskId`() {
        val task = heroesDesk().getTask(nonExistingPendingTaskId())

        assertTrue(task.isLeft())
        task.onLeft { assertTrue(it.head is TaskDoesNotExistError) }
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
        updatedTaskId.onLeft { assertTrue(it.head is TaskDoesNotExistUpdateTitleError) }
    }

    @Test
    fun `update title fails with non existing hero`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId = heroesDesk.updateTitle(createdTask.taskId, newTitle, nonExistingHeroId())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft { assertTrue(it.head is HeroDoesNotExistUpdateTitleError) }
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
        updatedTaskId.onLeft { assertTrue(it.head is TaskDoesNotExistUpdateDescriptionError) }
    }

    @Test
    fun `update description fails with non existing hero`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTaskId = heroesDesk.updateDescription(createdTask.taskId, newDescription, nonExistingHeroId())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft { assertTrue(it.head is HeroDoesNotExistUpdateDescriptionError) }
    }

    @Test
    fun `assignable heroes returns avail heroes when some`() {
        val instrumentedUserRepository = instrumentedUserRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        instrumentedUserRepository.defineAssignableHeroes(
            createdTask.taskId,
            Heroes(listOf(createHeroOrThrow("heroId1"), createHeroOrThrow("heroId2")))
        )

        val assignableHeroes = heroesDesk.assignableHeroes(createdTask.taskId)

        assertTrue(assignableHeroes.isRight())
        assignableHeroes.onRight {
            assertTrue(it.value.isNotEmpty())
        }
    }

    @Test
    fun `assignable heroes returns no heroes when none`() {
        val instrumentedUserRepository = instrumentedUserRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        instrumentedUserRepository.defineAssignableHeroes(createdTask.taskId, Heroes(emptyList()))

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
        assignableHeroes.onLeft { assertTrue(it.head is TaskDoesNotExistAssignableHeroesError) }
    }

    @Test
    fun `assign task works`() {
        val instrumentedUserRepository = instrumentedUserRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title")
        val heroes = Heroes(listOf(createHeroOrThrow("heroId1")))
        instrumentedUserRepository.defineAssignableHeroes(
            createdTask.taskId,
            heroes
        )

        val assignedTask = heroesDesk.assignTask(
            createdTask.taskId,
            HeroIds(heroes.value.map { it.heroId }),
            heroesDesk.currentHeroOrThrow()
        )

        assertTrue(assignedTask.isRight())
        assignedTask.onRight {
            assertEquals(it, createdTask.taskId)
            assert(heroesDesk.getTaskOrThrow(it).assignees == heroes)
        }

    }

    @Test
    fun `start work works on existing & pending task`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title")

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, heroesDesk.currentHeroOrThrow())

        assertTrue(updatedTaskId.isRight())
        updatedTaskId.onRight {
            assertEquals(it, createdTask.taskId)
            assert(heroesDesk.getTaskOrThrow(it) is InProgressTask)
        }
    }

    @Test
    fun `start work fails with non existing TaskId`() {
        val heroesDesk = heroesDesk()

        val updatedTaskId =
            heroesDesk.startWork(nonExistingPendingTaskId(), heroesDesk.currentHeroOrThrow())

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft { assertTrue(it.head is TaskDoesNotExistStartWorkError) }
    }

    private fun nonExistingPendingTaskId(): PendingTaskId = createPendingTaskIdOrThrow(nonExistingRawTaskId())

    private fun heroesDesk(): HeroesDesk = heroesDesk(instrumentedUserRepository())

    abstract fun instrumentedUserRepository(): InstrumentedUserRepository
    abstract fun heroesDesk(instrumentedUserRepository: InstrumentedUserRepository): HeroesDesk

    abstract fun nonExistingHeroId(): HeroId

    abstract fun nonExistingRawTaskId(): String

}

