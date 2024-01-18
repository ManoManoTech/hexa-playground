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
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createInProgressTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createNameOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createPendingTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createScopeKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.HeroesDeskTestUtils.getTaskOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes.Companion.empty
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedUserRepository
import org.junit.jupiter.api.Assertions.*
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
        ).getOrElse { throw AssertionError("$it") }

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
            assertTrue(it.head is ScopeKeyAlreadyExistsError)
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
        val heroes = Heroes(ensureHeroExisting("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val scope =
            heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id)
                .getOrElse { throw AssertionError("$it") }

        val assignedScope =
            heroesDesk.assignScope(scopeId, HeroIds(heroes), admin.id).getOrElse { throw AssertionError(it.toString()) }

        assertEquals(scope, assignedScope)
        assertEquals(heroes, assignedScope.assignees)

        val storedScope = heroesDesk.getScope(scopeId).getOrElse { throw AssertionError("$it") }
        assertEquals(scope, storedScope)
        assertEquals(heroes, storedScope.assignees)
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
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }

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
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }

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
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }

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
        heroesDesk.createScope(scopeId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }
        val newName = createNameOrThrow("new name")

        val assignedScope =
            heroesDesk.updateScopeName(scopeId, newName, admin.id).getOrElse { throw AssertionError("$it") }

        assertEquals(newName, assignedScope.name)
    }

    @Test
    fun `getScope fails on inexisting scope`() {
        val scopeId = createScopeKeyOrThrow("scopeKey")

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
        heroesDesk.createScope(scopeId, name, admin.id).getOrElse { throw AssertionError("$it") }

        val scope = heroesDesk.getScope(scopeId).getOrElse { throw AssertionError("$it") }

        assertEquals(name, scope.name)
    }

    @Test
    fun `createTask returns a task`() {
        val currentHero = ensureHeroExisting("heroId")
        val scope = ensureScopeExisting("scopeKey")
        val title = createTitleOrThrow("title")

        val task = heroesDesk.createTask(scope.key, title, currentHero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(task.title, title)
        assertEquals(task.scope, scope)
    }


    @Test
    fun `2 tasks creation with same title and creator returns 2 distinct tasks`() {
        val currentHero = ensureHeroExisting("heroId")
        val scope = ensureScopeExisting("scopeKey")
        val rawTitle = "title"

        val task1 = heroesDesk.createTask(scope.key, createTitleOrThrow(rawTitle), currentHero.id)
        val task2 = heroesDesk.createTask(scope.key, createTitleOrThrow(rawTitle), currentHero.id)

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
        val task =
            heroesDesk.createTask(ensureScopeExisting("scopeKey").key, createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is HeroDoesNotExistCreateTaskError)
        }
    }

    @Test
    fun `createTask with a non existing scope fails`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"
        val task =
            heroesDesk.createTask(createScopeKeyOrThrow("scopeKey"), createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is ScopeNotExistCreateTaskError)
        }
    }


    @Test
    fun `get task works on existing TaskId`() {
        val createdTask = ensureTaskExisting("title", "heroId")

        val retrievedTask = heroesDesk.getTask(createdTask.taskId).getOrElse { throw AssertionError("$it") }

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
        val createdTask = ensureTaskExisting("title", hero)
        val newTitle = createTitleOrThrow("new title")

        val updatedTask =
            heroesDesk.updateTitle(createdTask.taskId, newTitle, hero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(updatedTask.taskId, createdTask.taskId)
        assert(heroesDesk.getTaskOrThrow(updatedTask.taskId).title == newTitle)
    }

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
        val createdTask = ensureTaskExisting("title", "heroId")
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
        val createdTask = ensureTaskExisting("title", hero)
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTask =
            heroesDesk.updateDescription(createdTask.taskId, newDescription, hero.id)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(createdTask.taskId, updatedTask.taskId)
        assert(heroesDesk.getTaskOrThrow(updatedTask.taskId).description == newDescription)
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
        val createdTask = ensureTaskExisting("title", "heroId")
        val newDescription = createDescriptionOrThrow("new description")

        val updatedTaskId =
            heroesDesk.updateDescription(createdTask.taskId, newDescription, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroDoesNotExistUpdateDescriptionError)
        }
    }

    @Test
    fun `assign task works`() {
        val createdTask = ensureTaskExisting("title", "heroId")
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        assignScope(createdTask, heroes)

        val assignedTask =
            heroesDesk.assignTask(
                createdTask.taskId,
                HeroIds(heroes),
                hero.id
            ).getOrElse { throw AssertionError("$it") }

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
                HeroIds(heroes),
                hero.id
            )

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is HeroesDoesNotExistAssignTaskError, "$it")
        }
    }

    @Test
    fun `assign task fails on non scope assigned hero`() {
        val changeAuthor = ensureHeroExisting("changeAuthor")
        val createdTask = ensureTaskExisting("title", changeAuthor)
        val heroes = Heroes(createHeroOrThrow("heroId1"))

        val assignedTask =
            heroesDesk.assignTask(
                createdTask.taskId,
                HeroIds(heroes),
                changeAuthor.id
            )

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is HeroesDoesNotExistAssignTaskError)
        }
    }

    @Test
    fun `start work on pending task works on existing & pending task`() {
        val (taskId, hero) = createAssignedPendingTask()

        val updatedTaskId =
            heroesDesk.startWork(taskId, hero.id)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(updatedTaskId.taskId, taskId)
    }

    @Test
    fun `start work assigns hero starting work to task if no other assignee is present`() {
        val createdTask = ensureTaskExisting("title", "randomHeroId")
        val taskId = createdTask.taskId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        assignScope(createdTask, heroes)

        val updatedTaskId =
            heroesDesk.startWork(taskId, hero.id).getOrElse { throw AssertionError("$it") }

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
        val createdTask = ensureTaskExisting("title", "heroId")
        assignScope(createdTask, empty)

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroesDoesNotExistStartWorkError, "$it")
        }
    }

    @Test
    fun `start work fails when hero not in the scope`() {
        val createdTask = ensureTaskExisting("title", "heroId")

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, ensureHeroExisting("heroId1").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroNotAssignedToScopeStartWorkError, "$it")
        }
    }

    @Test
    fun `pause work on in progress task works`() {
        val (taskId, assignedHero) = createAssignedPendingTask()
        val inProgressTask = heroesDesk.startWork(taskId, assignedHero.id).getOrElse { throw AssertionError("$it") }

        val pausedTask = heroesDesk.pauseWork(inProgressTask.taskId, assignedHero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(taskId, pausedTask.taskId)
    }

    @Test
    fun `pause work fails with non existing TaskId`() {
        val hero = createHeroOrThrow("heroId")

        val updatedTaskId =
            heroesDesk.pauseWork(nonExistingWorkInProgressTaskId(), hero.id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId
            .onLeft {
                assertTrue(it.head is TaskDoesNotExistStopWorkError)
            }
    }

    @Test
    fun `pause work fails with non existing hero`() {
        val (pendingTaskId, hero) = createAssignedPendingTask()
        val updatedTask =
            heroesDesk.startWork(pendingTaskId, hero.id).getOrElse { throw AssertionError("$it") }

        val pauseWorkResult =   heroesDesk.pauseWork(updatedTask.taskId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(pauseWorkResult.isLeft())
        pauseWorkResult.onLeft {
            assertTrue(it.head is HeroesDoesNotExistStopWorkError, "$it")
        }
    }

    @Test
    fun `pause work fails when hero not in the scope`() {
        val (pendingTaskId, hero) = createAssignedPendingTask()
        val updatedTask =
            heroesDesk.startWork(pendingTaskId, hero.id).getOrElse { throw AssertionError("$it") }

        val pauseWorkResult =
            heroesDesk.pauseWork(updatedTask.taskId, ensureHeroExisting("${hero.id.value}Different").id)

        assertTrue(pauseWorkResult.isLeft())
        pauseWorkResult.onLeft {
            assertTrue(it.head is HeroNotAssignedToScopeStopWorkError, "$it")
        }
    }

    @Test
//    fun `stop work on in progress task works`() {
//        val (taskId, hero) = createAssignedPendingTask()
//        val inProgressTask = heroesDesk.startWork(taskId, hero.id)
//                .getOrElse { throw AssertionError("$it") }
//
//        val doneTask = heroesDesk.endWork(inProgressTask.taskId, hero.id).getOrElse { throw AssertionError("$it") }
//
//        assertEquals(inProgressTask.taskId, doneTask)
//    }

//    @Test
//    fun `start work assigns hero starting work to task if no other assignee is present`() {
//        val createdTask = ensureTaskExisting("title", "randomHeroId")
//        val taskId = createdTask.taskId
//        val hero = ensureHeroExisting("heroId1")
//        val heroes = Heroes(hero)
//        assignScope(createdTask, heroes)
//
//        val updatedTaskId =
//            heroesDesk.startWork(taskId, hero.id).getOrElse { throw AssertionError("$it") }
//
//        assertEquals(taskId.value, updatedTaskId.taskId.value)
//        assertTrue(updatedTaskId.assignees.contains(hero.id))
//    }
//
//    @Test
//    fun `start work fails with non existing TaskId`() {
//        val hero = createHeroOrThrow("heroId")
//
//        val updatedTaskId =
//            heroesDesk.startWork(nonExistingPendingTaskId(), hero.id)
//
//        assertTrue(updatedTaskId.isLeft())
//        updatedTaskId
//            .onLeft {
//                assertTrue(it.head is TaskDoesNotExistStartWorkError)
//            }
//    }
//
//    @Test
//    fun `start work fails with non existing hero`() {
//        val createdTask = ensureTaskExisting("title", "heroId")
//        assignScope(createdTask, empty)
//
//        val updatedTaskId =
//            heroesDesk.startWork(createdTask.taskId, createHeroOrThrow("nonExistingHero").id)
//
//        assertTrue(updatedTaskId.isLeft())
//        updatedTaskId.onLeft {
//            assertTrue(it.head is HeroesDoesNotExistStartWorkError, "$it")
//        }
//    }
//
//    @Test
//    fun `start work fails when hero not in the scope`() {
//        val createdTask = ensureTaskExisting("title", "heroId")
//
//        val updatedTaskId =
//            heroesDesk.startWork(createdTask.taskId, ensureHeroExisting("heroId1").id)
//
//        assertTrue(updatedTaskId.isLeft())
//        updatedTaskId.onLeft {
//            assertTrue(it.head is HeroNotAssignedToScopeStartWorkError, "$it")
//        }
//    }

    private fun createAssignedPendingTask(): Pair<PendingTaskId, Hero> {
        val createdTask = ensureTaskExisting("title", "randomHeroId")
        val taskId = createdTask.taskId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        assignScope(createdTask, heroes)
        heroesDesk.assignTask(taskId, HeroIds(hero), hero.id).getOrElse { throw AssertionError("$it") }
        return Pair(taskId, hero)
    }

    private fun assignScope(task: Task<*>, heroes: Heroes): Scope =
        heroesDesk.assignScope(
            task.scope.key,
            HeroIds(heroes),
            userRepo.ensureAdminExistingOrThrow("scopeAdminId").id
        )
            .getOrElse { throw AssertionError("$it") }

    private fun ensureTaskExisting(title: String, hero: String): PendingTask =
        ensureTaskExisting(title, ensureHeroExisting(hero))

    private fun ensureTaskExisting(title: String, hero: Hero): PendingTask =
        heroesDesk.createTask(ensureScopeExisting("scopeKey").key, createTitleOrThrow(title), hero.id)
            .getOrElse { throw AssertionError("$it") }

    private fun ensureHeroExisting(rawHeroId: String) = userRepo.ensureHeroExistingOrThrow(rawHeroId)

    private fun ensureScopeExisting(scopeKey: String): Scope {
        val scopeId = createScopeKeyOrThrow(scopeKey)
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val name = createNameOrThrow("name")
        return heroesDesk.createScope(scopeId, name, admin.id).getOrElse { throw AssertionError("$it") }

    }

    private fun nonExistingPendingTaskId(): PendingTaskId =
        createPendingTaskIdOrThrow("nonExistingPendingTaskId", nonExistingRawTaskId())

    private fun nonExistingWorkInProgressTaskId(): InProgressTaskId =
        createInProgressTaskIdOrThrow("nonExistingInProgressTaskId", nonExistingRawTaskId())

    abstract fun instrumentedHeroRepository(): InstrumentedUserRepository

    abstract fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk

    abstract fun nonExistingRawTaskId(): String

    @BeforeEach
    fun beforeEach() {
        userRepo = instrumentedHeroRepository()
        heroesDesk = createHeroesDesk(userRepo)
    }

}

