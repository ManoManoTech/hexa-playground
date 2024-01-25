package org.hexastacks.heroesdesk.kotlin.test

import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.adapters.InstrumentedUserRepository
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createAdminIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createInProgressTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createNameOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createPendingTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createScopeKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.getTaskOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes.Companion.empty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import org.hexastacks.heroesdesk.kotlin.errors.*

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
            assertTrue(it.head is AdminNotExistingError)
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
            assertTrue(it.head is ScopeNameAlreadyExistingError)
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
            assertTrue(it.head is ScopeKeyAlreadyExistingError)
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
            assertTrue(it.head is ScopeNotExistingError)
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
            assertTrue(it.head is HeroesNotExistingError, "$it")
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
            assertTrue(it.head is AdminNotExistingError)
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
            assertTrue(it.head is AdminNotExistingError)
        }
    }

    @Test
    fun `updateScopeName fails on inexisting scope`() {
        val scopeId = createScopeKeyOrThrow("scopeKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        val assignedScope = heroesDesk.updateScopeName(scopeId, createNameOrThrow("new name"), admin.id)

        assertTrue(assignedScope.isLeft())
        assignedScope.onLeft {
            assertTrue(it.head is ScopeNotExistingError)
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
            assertTrue(it.head is ScopeNotExistingError)
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
        assignScope(scope.key, Heroes(currentHero))

        val task = heroesDesk.createTask(scope.key, title, currentHero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(task.title, title)
        assertEquals(task.scope, scope)
    }


    @Test
    fun `2 tasks creation with same title and creator returns 2 distinct tasks`() {
        val currentHero = ensureHeroExisting("heroId")
        val scope = ensureScopeExisting("scopeKey")
        val rawTitle = "title"
        assignScope(scope.key, Heroes(currentHero))


        val task1 = heroesDesk.createTask(scope.key, createTitleOrThrow(rawTitle), currentHero.id)
        val task2 = heroesDesk.createTask(scope.key, createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task1.isRight(), "$task1")
        assertTrue(task2.isRight(), "$task2")
        task1.flatMap { right1 ->
            task2.map { right2 ->
             assertTrue(  right1 != right2 , "$right1 and $right2 are the same")
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
            assertTrue(it.head is HeroesNotExistingError, "$it")
        }
    }

    @Test
    fun `createTask with a non existing scope fails`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"

        val task =
            heroesDesk.createTask(ensureScopeExisting("scopeKey").key, createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is HeroesNotInScopeError, "$it")
        }
    }

    @Test
    fun `createTask with a creator not assigned to scope fails`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"

        val task =
            heroesDesk.createTask(createScopeKeyOrThrow("scopeKey"), createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is ScopeNotExistingError)
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
            assertTrue(it.head is TaskNotExistingError)
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
            assertTrue(it.head is TaskNotExistingError)
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
            assertTrue(it.head is HeroesNotExistingError, "$it")
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
            assertTrue(it.head is TaskNotExistingError)
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
            assertTrue(it.head is HeroesNotExistingError, "$it")
        }
    }

    @Test
    fun `assign task on pending task id works`() {
        val (pendingTaskId, hero) = createAssignedPendingTask()

        assertAssignTaskWorks(pendingTaskId, hero)
    }

    @Test
    fun `assign task on in progress task id works`() {
        val (inProgressTaskId, hero) = createAssignedInProgressTask()

        assertAssignTaskWorks(inProgressTaskId, hero)
    }

    private fun <T : TaskId> assertAssignTaskWorks(
        taskId: T,
        hero: Hero
    ) {
        val assignedTask = when (taskId) {
            is PendingTaskId -> heroesDesk.assignTask(
                taskId,
                HeroIds(hero),
                hero.id
            ).getOrElse { throw AssertionError("$it") }

            is InProgressTaskId -> heroesDesk.assignTask(
                taskId,
                HeroIds(hero),
                hero.id
            ).getOrElse { throw AssertionError("$it") }

            else -> throw AssertionError("Non assignable task id type $taskId")
        }

        assertEquals(taskId, assignedTask.taskId)
        assertEquals(Heroes(hero), assignedTask.assignees)
    }

    @Test
    fun `assign task on pending task id fails on non existing task`() {
        assertAssignTaskFailsOnNonExistingTask(nonExistingPendingTaskId())
    }

    @Test
    fun `assign task on in progress task id fails on non existing task`() {
        assertAssignTaskFailsOnNonExistingTask(nonExistingWorkInProgressTaskId())
    }

    private fun assertAssignTaskFailsOnNonExistingTask(
        id: TaskId
    ) {
        val hero = createHeroOrThrow("heroId1")
        val heroes = Heroes(hero)

        val assignedTask = when (id) {
            is PendingTaskId -> heroesDesk.assignTask(
                id,
                HeroIds(heroes),
                hero.id
            )

            is InProgressTaskId -> heroesDesk.assignTask(
                id,
                HeroIds(heroes),
                hero.id
            )

            else -> throw AssertionError("Non assignable task id type $id")
        }

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is TaskNotExistingError, "$it")
        }
    }

    @Test
    fun `assign task on pending task id fails when assigned hero not assigned to scope`() {
        assertAssignTaskFailsWhenAssignedHeroNotAssignedToScope(createAssignedPendingTask().first)
    }

    @Test
    fun `assign task on in progress task id fails when assigned hero not assigned to scope`() {
        assertAssignTaskFailsWhenAssignedHeroNotAssignedToScope(createAssignedInProgressTask().first)
    }

    private fun assertAssignTaskFailsWhenAssignedHeroNotAssignedToScope(
        taskId: TaskId
    ) {
        val nonScopeAssignedHeroes = ensureHeroExisting("nonScopeAssignedHero")
        val assignmentAuthor = ensureHeroExisting("assignmentAuthor").id

        val assignedTask = when(taskId) {
            is PendingTaskId -> heroesDesk.assignTask(
                taskId,
                HeroIds(nonScopeAssignedHeroes),
                assignmentAuthor
            )

            is InProgressTaskId -> heroesDesk.assignTask(
                taskId,
                HeroIds(nonScopeAssignedHeroes),
                assignmentAuthor
            )

            else -> throw AssertionError("Non assignable task id type $taskId")
        }

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is HeroesNotInScopeError, "$it")
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
                assertTrue(it.head is TaskNotExistingError, "$it")
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
            assertTrue(it.head is HeroesNotExistingError, "$it")
        }
    }

    @Test
    fun `start work fails when hero not in the scope`() {
        val createdTask = ensureTaskExisting("title", "heroId")

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, ensureHeroExisting("heroId1").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroesNotInScopeError, "$it")
        }
    }

    @Test
    fun `pause work on in progress task works`() {
        val (taskId, assignedHero) = createAssignedPendingTask()
        val inProgressTask = heroesDesk.startWork(taskId, assignedHero.id).getOrElse { throw AssertionError("$it") }

        val pausedTask =
            heroesDesk.pauseWork(inProgressTask.taskId, assignedHero.id).getOrElse { throw AssertionError("$it") }

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
                assertTrue(it.head is TaskNotExistingError, "$it")
            }
    }

    @Test
    fun `pause work fails with non existing hero`() {
        val (pendingTaskId, hero) = createAssignedPendingTask()
        val updatedTask =
            heroesDesk.startWork(pendingTaskId, hero.id).getOrElse { throw AssertionError("$it") }

        val pauseWorkResult = heroesDesk.pauseWork(updatedTask.taskId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(pauseWorkResult.isLeft())
        pauseWorkResult.onLeft {
            assertTrue(it.head is HeroesNotExistingError, "$it")
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
            assertTrue(it.head is HeroesNotInScopeError, "$it")
        }
    }

    @Test
    fun `end work on in progress task works, removing all assignees on the way`() {
        val (taskId, hero) = createAssignedInProgressTask()


        val doneTask = heroesDesk.endWork(taskId, hero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(taskId, doneTask.taskId)
        assertTrue(doneTask.assignees.isEmpty())
    }

    @Test
    fun `end work fails with non existing TaskId`() {
        val hero = createHeroOrThrow("heroId")

        val updatedTaskId =
            heroesDesk.endWork(nonExistingWorkInProgressTaskId(), hero.id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId
            .onLeft {
                assertTrue(it.head is TaskNotExistingError, "$it")
            }
    }

    @Test
    fun `end work fails with non existing hero`() {
        val (inProgressTaskId, _) = createAssignedInProgressTask()

        val updatedTaskId =
            heroesDesk.endWork(inProgressTaskId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroesNotExistingError, "$it")
        }
    }

    @Test
    fun `end work fails when hero not in the scope`() {
        val (inProgressTaskId, hero) = createAssignedInProgressTask()

        val updatedTaskId =
            heroesDesk.endWork(inProgressTaskId, ensureHeroExisting("${hero.id.value}AndMore").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroesNotInScopeError, "$it")
        }
    }

    private fun createAssignedInProgressTask(): Pair<InProgressTaskId, Hero> {
        val (taskId, hero) = createAssignedPendingTask()
        return Pair(
            heroesDesk.startWork(taskId, hero.id).getOrElse { throw AssertionError("$it") }.taskId,
            hero
        )
    }

    private fun createAssignedPendingTask(): Pair<PendingTaskId, Hero> {
        val createdTask = ensureTaskExisting("title", "randomHeroId")
        val taskId = createdTask.taskId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        assignScope(createdTask, heroes)
        heroesDesk.assignTask(taskId, HeroIds(hero), hero.id).getOrElse { throw AssertionError("$it") }
        return Pair(taskId, hero)
    }

    private fun assignScope(task: Task<*>, heroes: Heroes): Scope {
        return assignScope(task.scope.key, heroes)
    }

    private fun assignScope(
        scopeKey: ScopeKey,
        heroes: Heroes
    ): Scope {
        return heroesDesk.assignScope(
            scopeKey,
            HeroIds(heroes),
            userRepo.ensureAdminExistingOrThrow("scopeAdminId").id
        )
            .getOrElse { throw AssertionError("$it") }
    }

    private fun ensureTaskExisting(title: String, hero: String): PendingTask =
        ensureTaskExisting(title, ensureHeroExisting(hero))

    private fun ensureTaskExisting(title: String, hero: Hero): PendingTask {
        val scope = ensureScopeExisting("scopeKey")
        assignScope(scope.key, Heroes(hero))
        return heroesDesk
            .createTask(
                scope.key, createTitleOrThrow(title), hero.id
            )
            .getOrElse { throw AssertionError("$it") }
    }

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

