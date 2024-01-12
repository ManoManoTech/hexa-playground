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
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createScopeKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.getTaskOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTask
import org.hexastacks.heroesdesk.kotlin.impl.task.PendingTaskId
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes.Companion.EMPTY_HEROES
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

abstract class AbstractHeroesDeskTest {

    private lateinit var heroesDesk: HeroesDesk
    private lateinit var userRepo: InstrumentedUserRepository

    @Test
    fun `createScope returns a scope`() {
        val id = createScopeKeyOrThrow("id")
        val name = createNameOrThrow("name")
        val creator = userRepo.ensureAdminExistingOrThrow("adminId")

        val scope = heroesDesk.createScope(
            id,
            name,
            creator.id
        ).getOrElse { throw AssertionError() }

        assertEquals(name, scope.name)
    }

    @Test
    fun `createScope fails on non existing admin`() {
        val creationFailure =
            heroesDesk.createScope(
                createScopeKeyOrThrow("id2"),
                createNameOrThrow("name"),
                createAdminIdOrThrow("adminId2")
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is AdminDoesNotExistCreateScopeError)
        }
    }

    @Test
    fun `createScope fails on pre existing scope with same name`() {
        val name = createNameOrThrow("name")
        heroesDesk.createScope(
            createScopeKeyOrThrow("id1"),
            name,
            userRepo.ensureAdminExistingOrThrow("adminId1").id
        )

        val creationFailure =
            heroesDesk.createScope(
                createScopeKeyOrThrow("id2"),
                name,
                userRepo.ensureAdminExistingOrThrow("adminId2").id
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is ScopeNameAlreadyExistsError)
        }
    }

    @Test
    fun `createScope fails on pre existing scope with same id`() {
        val nameCommonStart = "startEnding"
        val id = createScopeKeyOrThrow("id")
        heroesDesk.createScope(
            id,
            createNameOrThrow("${nameCommonStart}1"),
            userRepo.ensureAdminExistingOrThrow("adminId1").id
        )

        val creationFailure =
            heroesDesk.createScope(
                id,
                createNameOrThrow("${nameCommonStart}2"),
                userRepo.ensureAdminExistingOrThrow("adminId2").id
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is ScopeIdAlreadyExistsError)
        }
    }

    @Test
    fun `createScope works on many parallel creations`() {
        if (Runtime.getRuntime().availableProcessors() < 4)
            return // not running on github actions

        val results = ConcurrentHashMap<Int, EitherNel<CreateScopeError, Scope>>()
        val executor = Executors.newFixedThreadPool(10)
        val dispatcher = executor.asCoroutineDispatcher()
        val runNb = 1000
        val createdTaskTarget = 250
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        runBlocking {
            val jobs = List(runNb) {
                launch(dispatcher) {
                    val suffix = it % createdTaskTarget
                    results[it] = heroesDesk
                        .createScope(
                            createScopeKeyOrThrow("id$suffix"),
                            createNameOrThrow("${suffix}name"),
                            admin.id
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
    fun `assignScope works`() {
        val scopeId = createScopeKeyOrThrow("scopeId")
        val heroIds = HeroIds(ensureHeroExisting("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val scope =
            heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError() }

        val assignedScope =
            heroesDesk.assignScope(scopeId, heroIds, admin.id).getOrElse { throw AssertionError(it.toString()) }

        assertEquals(scope, assignedScope)
        assertEquals(heroIds, assignedScope.assignees)
    }

    @Test
    fun `assignScope fails on inexisting scope`() {
        val scopeId = createScopeKeyOrThrow("scopeId")
        val heroIds = HeroIds(ensureHeroExisting("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        val assignedScope = heroesDesk.assignScope(scopeId, heroIds, admin.id)

        assertTrue(assignedScope.isLeft())
        assignedScope.onLeft {
            assertTrue(it.head is ScopeDoesNotExistAssignHeroesOnScopeError)
        }
    }

    @Test
    fun `assignScope fails on inexisting heroIds`() {
        val scopeId = createScopeKeyOrThrow("scopeId")
        val heroIds = HeroIds(createHeroIdOrThrow("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError() }

        val assignedScope = heroesDesk.assignScope(scopeId, heroIds, admin.id)

        assertTrue(assignedScope.isLeft())
        assignedScope.onLeft {
            assertTrue(it.head is AssignedHeroIdsNotExistAssignHeroesOnScopeError)
        }
    }

    @Test
    fun `assignScope fails on inexisting admin`() {
        val scopeId = createScopeKeyOrThrow("scopeId")
        val heroIds = HeroIds(ensureHeroExisting("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError() }

        val assignedScope = heroesDesk.assignScope(scopeId, heroIds, createAdminIdOrThrow("anotherAdminId"))

        assertTrue(assignedScope.isLeft())
        assignedScope.onLeft {
            assertTrue(it.head is AdminIdNotExistingAssignHeroesOnScopeError)
        }
    }

    @Test
    fun `updateScopeName fails on inexisting admin`() {
        val scopeId = createScopeKeyOrThrow("scopeKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError() }

        val assignedScope =
            heroesDesk.updateScopeName(scopeId, createNameOrThrow("new name"), createAdminIdOrThrow("anotherAdminId"))

        assertTrue(assignedScope.isLeft())
        assignedScope.onLeft {
            assertTrue(it.head is AdminIdNotExistingUpdateScopeNameError)
        }
    }

    @Test
    fun `updateScopeName fails on inexisting scope`() {
        val scopeId = createScopeKeyOrThrow("scopeKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        val assignedScope = heroesDesk.updateScopeName(scopeId, createNameOrThrow("new name"), admin.id)

        assertTrue(assignedScope.isLeft())
        assignedScope.onLeft {
            assertTrue(it.head is ScopeNotExistingUpdateScopeNameError)
        }
    }

    @Test
    fun `updateScopeName works`() {
        val scopeId = createScopeKeyOrThrow("scopeKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError() }
        val newName = createNameOrThrow("new name")

        val assignedScope = heroesDesk.updateScopeName(scopeId, newName, admin.id).getOrElse { throw AssertionError() }

        assertEquals(newName, assignedScope.name)
    }

    @Test
    fun `getScope fails on inexisting scope`() {
        val scopeId = createScopeKeyOrThrow("scopeKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        val scope = heroesDesk.getScope(scopeId)

        assertTrue(scope.isLeft())
        scope.onLeft {
            assertTrue(it.head is ScopeNotExistingGetScopeError)
        }
    }

    @Test
    fun `getScope works`() {
        val scopeId = createScopeKeyOrThrow("scopeKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val name = createNameOrThrow("name")
        heroesDesk.createScope(scopeId, name, admin.id).getOrElse { throw AssertionError() }

        val scope = heroesDesk.getScope(scopeId).getOrElse { throw AssertionError() }

        assertEquals(name, scope.name)
    }

    @Test
    fun `createTask returns a task`() {
        val currentHero = ensureHeroExisting("heroId")
        val title = createTitleOrThrow("title")

        val task = heroesDesk.createTask(title, currentHero.id).getOrElse { throw AssertionError() }

        assertEquals(task.title, title)
    }

    @Test
    fun `2 tasks creation with same title and creator returns 2 distinct tasks`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"

        val task1 = heroesDesk.createTask(createTitleOrThrow(rawTitle), currentHero.id)
        val task2 = heroesDesk.createTask(createTitleOrThrow(rawTitle), currentHero.id)

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
        val currentHero = createHeroOrThrow("heroId")
        val rawTitle = "title"
        val task = heroesDesk.createTask(createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is HeroDoesNotExistCreateTaskError)
        }
    }

    @Test
    fun `get task works on existing TaskId`() {
        val createdTask = createTaskOrThrow("title", "heroId")

        val retrievedTask = heroesDesk.getTask(createdTask.taskId).getOrElse { throw AssertionError() }

        assertEquals(retrievedTask, createdTask)
    }

    @Test
    fun `get task fails on non existing TaskId`() {
        val task = heroesDesk.getTask(nonExistingPendingTaskId())

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is TaskDoesNotExistError)
        }
    }

    @Test
    fun `update title works on existing TaskId`() {
        val hero = ensureHeroExisting("heroId")
        val createdTask = createTaskOrThrow("title", hero)
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId =
            heroesDesk.updateTitle(createdTask.taskId, newTitle, hero.id).getOrElse { throw AssertionError() }

        assertEquals(updatedTaskId, createdTask.taskId)
        assert(heroesDesk.getTaskOrThrow(updatedTaskId).title == newTitle)
    }

    private fun createTaskOrThrow(title: String, hero: String): PendingTask =
        createTaskOrThrow(title, ensureHeroExisting(hero))

    private fun createTaskOrThrow(title: String, hero: Hero): PendingTask =
        heroesDesk.createTask(createTitleOrThrow(title), hero.id).getOrElse { throw AssertionError() }

    @Test
    fun `update title fails with non existing TaskId`() {
        val heroId = ensureHeroExisting("heroId")
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
        val createdTask = createTaskOrThrow("title", "heroId")
        val newTitle = createTitleOrThrow("new title")

        val updatedTaskId =
            heroesDesk.updateTitle(createdTask.taskId, newTitle, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroDoesNotExistUpdateTitleError)
        }
    }

    @Test
    fun `update description works on existing TaskId`() {
        val hero = ensureHeroExisting("heroId")
        val createdTask = createTaskOrThrow("title", hero)
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTaskId =
            heroesDesk.updateDescription(createdTask.taskId, newDescription, hero.id)
                .getOrElse { throw AssertionError() }

        assertEquals(updatedTaskId, createdTask.taskId)
        assert(heroesDesk.getTaskOrThrow(updatedTaskId).description == newDescription)
    }

    @Test
    fun `update description fails with non existing TaskId`() {
        val newDescription = createDescriptionOrThrow("new description")
        val hero = ensureHeroExisting("heroId")

        val updatedTaskId =
            heroesDesk.updateDescription(nonExistingPendingTaskId(), newDescription, hero.id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is TaskDoesNotExistUpdateDescriptionError)
        }
    }

    @Test
    fun `update description fails with non existing hero`() {
        val createdTask = createTaskOrThrow("title", "heroId")
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTaskId =
            heroesDesk.updateDescription(createdTask.taskId, newDescription, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroDoesNotExistUpdateDescriptionError)
        }
    }

    @Test
    fun `assignable heroes returns avail heroes when some`() {
        val createdTask = createTaskOrThrow("title", "heroId")
        userRepo.defineAssignableHeroes(
            createdTask.taskId,
            Heroes(createHeroOrThrow("heroId1"), createHeroOrThrow("heroId2"))
        )

        val assignableHeroes = heroesDesk.assignableHeroes(createdTask.taskId).getOrElse { throw AssertionError() }

        assertTrue(assignableHeroes.value.isNotEmpty())
    }

    @Test
    fun `assignable heroes returns no heroes when none`() {
        val createdTask = createTaskOrThrow("title", "heroId")
        userRepo.defineAssignableHeroes(createdTask.taskId, EMPTY_HEROES)

        val assignableHeroes = heroesDesk.assignableHeroes(createdTask.taskId).getOrElse { throw AssertionError() }

        assertTrue(assignableHeroes.value.isEmpty())
    }

    @Test
    fun `assignable heroes fails on non existing task id`() {
        val assignableHeroes = heroesDesk.assignableHeroes(nonExistingPendingTaskId())

        assertTrue(assignableHeroes.isLeft())
        assignableHeroes.onLeft {
            assertTrue(it.head is TaskDoesNotExistAssignableHeroesError)
        }
    }

    @Test
    fun `assign task works`() {
        val createdTask = createTaskOrThrow("title", "heroId")
        val hero = createHeroOrThrow("heroId1")
        val heroes =
            userRepo
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
        val changeAuthor = ensureHeroExisting("changeAuthor")
        val createdTask = createTaskOrThrow("title", changeAuthor)
        val heroes = Heroes(createHeroOrThrow("heroId1"))
        userRepo
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
        val createdTask = createTaskOrThrow("title", "randomHeroId")
        val taskId = createdTask.taskId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        userRepo.defineAssignableHeroes(
            taskId,
            heroes
        )
        heroesDesk.assignTask(taskId, HeroIds(hero.id), hero.id).getOrElse { throw AssertionError() }
        userRepo.defineHeroesAbleToChangeStatus(
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
        val createdTask = createTaskOrThrow("title", "randomHeroId")
        val taskId = createdTask.taskId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        userRepo.defineAssignableHeroes(
            taskId,
            heroes
        )
        userRepo.defineHeroesAbleToChangeStatus(
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
        val createdTask = createTaskOrThrow("title", "heroId")
        userRepo.defineAssignableHeroes(
            createdTask.taskId,
            EMPTY_HEROES
        )
        userRepo.defineHeroesAbleToChangeStatus(
            createdTask.taskId,
            EMPTY_HEROES
        )

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroDoesNotExistStartWorkError)
        }
    }

    @Test
    fun `start work fails with hero lacking the right to`() {
        val createdTask = createTaskOrThrow("title", "heroId")
        val taskId = createdTask.taskId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        userRepo.defineAssignableHeroes(
            taskId,
            heroes
        )
        userRepo.defineHeroesAbleToChangeStatus(
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

    private fun ensureHeroExisting(rawHeroId: String) = userRepo.ensureHeroExistingOrThrow(rawHeroId)

    private fun nonExistingPendingTaskId(): PendingTaskId = createPendingTaskIdOrThrow(nonExistingRawTaskId())


    abstract fun instrumentedHeroRepository(): InstrumentedUserRepository

    abstract fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk

    abstract fun nonExistingRawTaskId(): String

    @BeforeEach
    fun beforeEach() {
        userRepo = instrumentedHeroRepository()
        heroesDesk = createHeroesDesk(userRepo)
    }

}

