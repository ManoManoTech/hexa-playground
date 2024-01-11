package org.hexastacks.heroesdesk.kotlin

import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hexastacks.heroesdesk.kotlin.HeroesDesk.*
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createAdminIdOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createNameOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createPendingTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createScopeIdOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createTaskOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.getTaskOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes.Companion.EMPTY_HEROES
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedHeroRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

abstract class AbstractHeroesDeskTest {

    @Test
    @Disabled
    fun `createScope returns a scope`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val id = createScopeIdOrThrow("id")
        val name = createNameOrThrow("name")
        val creator = instrumentedUserRepository.ensureExisting(createAdminIdOrThrow("adminId"))

        val scope = heroesDesk.createScope(
            id,
            name,
            creator.id
        ).getOrElse { throw AssertionError() }

        assertEquals(name, scope.name)
    }

    @Test
    @Disabled
    fun `createScope fails on non existing admin`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val name = createNameOrThrow("name")

        val creationFailure =
            heroesDesk.createScope(createScopeIdOrThrow("id2"), name, createAdminIdOrThrow("adminId2"))

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is ScopeNameAlreadyExistsError)
        }
    }

    @Test
    fun `createScope fails on pre existing scope with same name`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val name = createNameOrThrow("name")
        heroesDesk.createScope(createScopeIdOrThrow("id1"), name, createAdminIdOrThrow("adminId1"))

        val creationFailure =
            heroesDesk.createScope(createScopeIdOrThrow("id2"), name, createAdminIdOrThrow("adminId2"))

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is ScopeNameAlreadyExistsError)
        }
    }

    @Test
    fun `createScope fails on pre existing scope with same id`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val nameCommonStart = "startEnding"
        val id = createScopeIdOrThrow("id")
        heroesDesk.createScope(id, createNameOrThrow("${nameCommonStart}1"), createAdminIdOrThrow("adminId1"))

        val creationFailure =
            heroesDesk.createScope(id, createNameOrThrow("${nameCommonStart}2"), createAdminIdOrThrow("adminId2"))

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is ScopeIdAlreadyExistsError)
        }
    }

    @Test
    fun `createScope works on many parallel creations`() {
        if (Runtime.getRuntime().availableProcessors() < 4)
            return // not running on github actions

        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val results = ConcurrentHashMap<Int, EitherNel<CreateScopeError, Scope>>()
        val executor = Executors.newFixedThreadPool(10)
        val dispatcher = executor.asCoroutineDispatcher()
        val runNb = 1000
        val createdTaskTarget = 250
        val admin = createAdminIdOrThrow("adminId")

        runBlocking {
            val jobs = List(runNb) {
                launch(dispatcher) {
                    val suffix = it % createdTaskTarget
                    results[it] = heroesDesk
                        .createScope(
                            createScopeIdOrThrow("id$suffix"),
                            createNameOrThrow("${suffix}name"),
                            admin
                        )
                }
            }
            jobs.joinAll()
        }
        executor.shutdown()

        assertEquals(runNb, results.size)
        val failureNb =
            results
                .filter { it.value.isLeft() }
                .map {
                    it.value
                }
                .count()
        assertEquals(runNb - createdTaskTarget, failureNb)
    }

    @Test
    @Disabled
    fun `assignScope works`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val scopeId = createScopeIdOrThrow("scopeId")
        val heroIds = HeroIds(createHeroIdOrThrow("heroId"))
        val adminId = createAdminIdOrThrow("adminId")
        val scope =
            heroesDesk.createScope(scopeId, createNameOrThrow("name"), adminId).getOrElse { throw AssertionError() }

        val assignedScope = heroesDesk.assignScope(scopeId, heroIds, adminId).getOrElse { throw AssertionError() }

        assertEquals(scope, assignedScope)
        assertEquals(heroIds, assignedScope.assignees)
    }

    @Test
    fun `createTask returns a task`() {
        val currentHero = createHeroOrThrow("heroId")
        val title = createTitleOrThrow("title")

        val task = heroesDesk().createTask(title, currentHero.id).getOrElse { throw AssertionError() }

        assertEquals(task.title, title)
    }

    @Test
    fun `2 tasks creation with same title and creator returns 2 distinct tasks`() {
        val currentHero = createHeroOrThrow("heroId")
        val rawTitle = "title"

        val task1 = heroesDesk().createTask(createTitleOrThrow(rawTitle), currentHero.id)
        val task2 = heroesDesk().createTask(createTitleOrThrow(rawTitle), currentHero.id)

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
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")

        val retrievedTask = heroesDesk.getTask(createdTask.taskId).getOrElse { throw AssertionError() }

        assertEquals(retrievedTask, createdTask)
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
        val hero = createHeroOrThrow("heroId")
        val createdTask = heroesDesk.createTaskOrThrow("title", hero)
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId =
            heroesDesk.updateTitle(createdTask.taskId, newTitle, hero.id).getOrElse { throw AssertionError() }

        assertEquals(updatedTaskId, createdTask.taskId)
        assert(heroesDesk.getTaskOrThrow(updatedTaskId).title == newTitle)
    }

    @Test
    fun `update title fails with non existing TaskId`() {
        val heroesDesk = heroesDesk()
        val heroId = createHeroOrThrow("heroId")
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId =
            heroesDesk.updateTitle(nonExistingPendingTaskId(), newTitle, heroId.id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is TaskDoesNotExistUpdateTitleError)
        }
    }

    @Test
    fun `update title fails with non existing hero`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")
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
        val hero = createHeroOrThrow("heroId")
        val createdTask = heroesDesk.createTaskOrThrow("title", hero)
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTaskId =
            heroesDesk.updateDescription(createdTask.taskId, newDescription, hero.id)
                .getOrElse { throw AssertionError() }

        assertEquals(updatedTaskId, createdTask.taskId)
        assert(heroesDesk.getTaskOrThrow(updatedTaskId).description == newDescription)
    }

    @Test
    fun `update description fails with non existing TaskId`() {
        val heroesDesk = heroesDesk()
        val newDescription = createDescriptionOrThrow("new description")
        val hero = createHeroOrThrow("heroId")

        val updatedTaskId =
            heroesDesk.updateDescription(nonExistingPendingTaskId(), newDescription, hero.id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is TaskDoesNotExistUpdateDescriptionError)
        }
    }

    @Test
    fun `update description fails with non existing hero`() {
        val heroesDesk = heroesDesk()
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")
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
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")
        instrumentedUserRepository.defineAssignableHeroes(
            createdTask.taskId,
            Heroes(createHeroOrThrow("heroId1"), createHeroOrThrow("heroId2"))
        )

        val assignableHeroes = heroesDesk.assignableHeroes(createdTask.taskId).getOrElse { throw AssertionError() }

        assertTrue(assignableHeroes.value.isNotEmpty())
    }

    @Test
    fun `assignable heroes returns no heroes when none`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")
        instrumentedUserRepository.defineAssignableHeroes(createdTask.taskId, EMPTY_HEROES)

        val assignableHeroes = heroesDesk.assignableHeroes(createdTask.taskId).getOrElse { throw AssertionError() }

        assertTrue(assignableHeroes.value.isEmpty())
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
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")
        val hero = createHeroOrThrow("heroId1")
        val heroes =
            instrumentedUserRepository
                .defineAssignableHeroes(
                    createdTask.taskId,
                    Heroes(hero)
                )

        val assignedTask =
            heroesDesk.assignTask(
                createdTask.taskId,
                HeroIds(heroes.value.map { it.id }),
                hero.id
            ).getOrElse { throw AssertionError() }

        assertEquals(createdTask.taskId, assignedTask.taskId)
        assertEquals(createdTask.title, assignedTask.title)
        assertEquals(createdTask.description, assignedTask.description)
        assertEquals(heroes, assignedTask.assignees)
    }

    @Test
    fun `assign task fails on non existing task`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val hero = createHeroOrThrow("heroId1")
        val heroes = Heroes(hero)

        val assignedTask =
            heroesDesk.assignTask(
                nonExistingPendingTaskId(),
                HeroIds(heroes.value.map { it.id }),
                hero.id
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
        val changeAuthor = createHeroOrThrow("changeAuthor")
        val createdTask = heroesDesk.createTaskOrThrow("title", changeAuthor)
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
                changeAuthor.id
            )

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is NonAssignableHeroesAssignTaskError)
        }
    }

    @Test
    fun `start work on pending task works on existing & pending task`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title", "randomHeroId")
        val taskId = createdTask.taskId
        val hero = instrumentedUserRepository.ensureExistingOrThrow("heroId1")
        val heroes = Heroes(hero)
        instrumentedUserRepository.defineAssignableHeroes(
            taskId,
            heroes
        )
        heroesDesk.assignTask(taskId, HeroIds(hero.id), hero.id).getOrElse { throw AssertionError() }
        instrumentedUserRepository.defineHeroesAbleToChangeStatus(
            taskId,
            heroes
        )

        val updatedTaskId =
            heroesDesk.startWork(taskId, hero.id)
                .getOrElse { throw AssertionError() }

        assertEquals(updatedTaskId.taskId.value, taskId.value)//TODO: handle at TaskId level
    }

    @Test
    fun `start work assigns hero starting work to task`() {
        val instrumentedUserRepository = instrumentedHeroRepository()
        val heroesDesk = heroesDesk(instrumentedUserRepository)
        val createdTask = heroesDesk.createTaskOrThrow("title", "randomHeroId")
        val taskId = createdTask.taskId
        val hero = instrumentedUserRepository.ensureExistingOrThrow("heroId1")
        val heroes = Heroes(hero)
        instrumentedUserRepository.defineAssignableHeroes(
            taskId,
            heroes
        )
        instrumentedUserRepository.defineHeroesAbleToChangeStatus(
            taskId,
            heroes
        )
        assertTrue(createdTask.assignees.isEmpty())

        val updatedTaskId =
            heroesDesk.startWork(taskId, hero.id).getOrElse { throw AssertionError() }

        assertEquals(taskId.value, updatedTaskId.taskId.value)
        assertTrue(updatedTaskId.assignees.contains(hero.id))
    }

    @Test
    fun `start work fails with non existing TaskId`() {
        val heroesDesk = heroesDesk()
        val hero = createHeroOrThrow("heroId")

        val updatedTaskId =
            heroesDesk.startWork(nonExistingPendingTaskId(), hero.id)

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
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")
        instrumentedUserRepository.defineAssignableHeroes(
            createdTask.taskId,
            EMPTY_HEROES
        )
        instrumentedUserRepository.defineHeroesAbleToChangeStatus(
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
        val createdTask = heroesDesk.createTaskOrThrow("title", "heroId")
        val taskId = createdTask.taskId
        val hero = instrumentedUserRepository.ensureExistingOrThrow("heroId1")
        val heroes = Heroes(hero)
        instrumentedUserRepository.defineAssignableHeroes(
            taskId,
            heroes
        )
        instrumentedUserRepository.defineHeroesAbleToChangeStatus(
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

