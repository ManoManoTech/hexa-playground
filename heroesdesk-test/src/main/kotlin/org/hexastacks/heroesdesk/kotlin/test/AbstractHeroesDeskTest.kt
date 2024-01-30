package org.hexastacks.heroesdesk.kotlin.test

import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes.Companion.empty
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createAdminIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createInProgressTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createNameOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createPendingTaskIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createSquadKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.getTaskOrThrow
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
    fun `createSquad returns a squad`() {
        val id = createSquadKeyOrThrow("id")
        val name = createNameOrThrow("name")
        val creator = userRepo.ensureAdminExistingOrThrow("adminId")

        val squad = heroesDesk.createSquad(
            id,
            name,
            creator.id
        ).getOrElse { throw AssertionError("$it") }

        assertEquals(name, squad.name)
    }

    @Test
    fun `createSquad fails on non existing admin`() {
        val creationFailure =
            heroesDesk.createSquad(
                createSquadKeyOrThrow("id2"),
                createNameOrThrow("name"),
                createAdminIdOrThrow("adminId2")
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is AdminNotExistingError)
        }
    }

    @Test
    fun `createSquad fails on pre existing squad with same name`() {
        val name = createNameOrThrow("name")
        heroesDesk.createSquad(
            createSquadKeyOrThrow("id1"),
            name,
            userRepo.ensureAdminExistingOrThrow("adminId1").id
        )

        val creationFailure =
            heroesDesk.createSquad(
                createSquadKeyOrThrow("id2"),
                name,
                userRepo.ensureAdminExistingOrThrow("adminId2").id
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is SquadNameAlreadyExistingError, "$it")
        }
    }

    @Test
    fun `createSquad fails on pre existing squad with same id`() {
        val nameCommonStart = "startEnding"
        val id = createSquadKeyOrThrow("id")
        heroesDesk.createSquad(
            id,
            createNameOrThrow("${nameCommonStart}1"),
            userRepo.ensureAdminExistingOrThrow("adminId1").id
        )

        val creationFailure =
            heroesDesk.createSquad(
                id,
                createNameOrThrow("${nameCommonStart}2"),
                userRepo.ensureAdminExistingOrThrow("adminId2").id
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is SquadKeyAlreadyExistingError, "$it")
        }
    }

    @Test
    fun `createSquad works on many parallel creations`() {
        if (Runtime.getRuntime().availableProcessors() < 4)
            return // not running on github actions

        val results = ConcurrentHashMap<Int, EitherNel<CreateSquadError, Squad>>()
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
                        .createSquad(
                            createSquadKeyOrThrow("id$suffix"),
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
    fun `assignSquad works`() {
        val squadId = createSquadKeyOrThrow("squadId")
        val heroes = Heroes(ensureHeroExisting("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val squad =
            heroesDesk.createSquad(squadId, createNameOrThrow("name"), admin.id)
                .getOrElse { throw AssertionError("$it") }

        val squadMembers =
            heroesDesk.assignSquad(squadId, HeroIds(heroes), admin.id).getOrElse { throw AssertionError(it.toString()) }

        assertEquals(squad.key, squadMembers.squadKey)
        val storedSquad = heroesDesk.getSquad(squadId).getOrElse { throw AssertionError("$it") }
        assertEquals(squad, storedSquad)
    }

    @Test
    fun `assignSquad fails on inexisting squad`() {
        val squadId = createSquadKeyOrThrow("squadId")
        val heroIds = HeroIds(ensureHeroExisting("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        val assignedSquad = heroesDesk.assignSquad(squadId, heroIds, admin.id)

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is SquadNotExistingError)
        }
    }

    @Test
    fun `assignSquad fails on inexisting heroIds`() {
        val squadId = createSquadKeyOrThrow("squadId")
        val heroIds = HeroIds(createHeroIdOrThrow("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createSquad(squadId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }

        val assignedSquad = heroesDesk.assignSquad(squadId, heroIds, admin.id)

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is HeroesNotExistingError, "$it")
        }
    }

    @Test
    fun `assignSquad fails on inexisting admin`() {
        val squadId = createSquadKeyOrThrow("squadId")
        val heroIds = HeroIds(ensureHeroExisting("heroId"))
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createSquad(squadId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }

        val assignedSquad = heroesDesk.assignSquad(squadId, heroIds, createAdminIdOrThrow("anotherAdminId"))

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is AdminNotExistingError)
        }
    }

    @Test
    fun `updateSquadName fails on inexisting admin`() {
        val squadId = createSquadKeyOrThrow("squadKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createSquad(squadId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }

        val assignedSquad =
            heroesDesk.updateSquadName(squadId, createNameOrThrow("new name"), createAdminIdOrThrow("anotherAdminId"))

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is AdminNotExistingError)
        }
    }

    @Test
    fun `updateSquadName fails on inexisting squad`() {
        val squadId = createSquadKeyOrThrow("squadKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        val assignedSquad = heroesDesk.updateSquadName(squadId, createNameOrThrow("new name"), admin.id)

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is SquadNotExistingError)
        }
    }

    @Test
    fun `updateSquadName works`() {
        val squadId = createSquadKeyOrThrow("squadKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        heroesDesk.createSquad(squadId, createNameOrThrow("name"), admin.id).getOrElse { throw AssertionError("$it") }
        val newName = createNameOrThrow("new name")

        val assignedSquad =
            heroesDesk.updateSquadName(squadId, newName, admin.id).getOrElse { throw AssertionError("$it") }

        assertEquals(newName, assignedSquad.name)
    }

    @Test
    fun `getSquad fails on inexisting squad`() {
        val squadId = createSquadKeyOrThrow("squadKey")

        val squad = heroesDesk.getSquad(squadId)

        assertTrue(squad.isLeft())
        squad.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `getSquadMembers works on squad without assignee`() {
        val squadId = createSquadKeyOrThrow("squadKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val name = createNameOrThrow("name")
        val squad = heroesDesk.createSquad(squadId, name, admin.id).getOrElse { throw AssertionError("$it") }

        val squadMembers =
            heroesDesk.getSquadMembers(squadId)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(squadId, squadMembers.squadKey)
        assertEquals(HeroIds.empty, squadMembers.heroes)

    }

    @Test
    fun `getSquadMembers works on squad with assignee`() {
        val squadId = createSquadKeyOrThrow("squadKey")
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val name = createNameOrThrow("name")
        heroesDesk.createSquad(squadId, name, admin.id).getOrElse { throw AssertionError("$it") }
        val assignees = HeroIds(ensureHeroExisting("heroId"))
        heroesDesk.assignSquad(squadId, assignees, admin.id)
            .getOrElse { throw AssertionError("$it") }

        val squadMembers = heroesDesk.getSquadMembers(squadId).getOrElse { throw AssertionError("$it") }

        assertEquals(squadId, squadMembers.squadKey)
        assertEquals(assignees, squadMembers.heroes)
    }

    @Test
    fun `createTask returns a task`() {
        val currentHero = ensureHeroExisting("heroId")
        val squad = ensureSquadExisting("squadKey")
        val title = createTitleOrThrow("title")
        assignSquad(squad.key, Heroes(currentHero))

        val task = heroesDesk.createTask(squad.key, title, currentHero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(title, task.title)
        assertEquals(squad.key, task.squadKey())
    }


    @Test
    fun `2 tasks creation with same title and creator returns 2 distinct tasks`() {
        val currentHero = ensureHeroExisting("heroId")
        val squad = ensureSquadExisting("squadKey")
        val rawTitle = "title"
        assignSquad(squad.key, Heroes(currentHero))


        val task1 = heroesDesk.createTask(squad.key, createTitleOrThrow(rawTitle), currentHero.id)
        val task2 = heroesDesk.createTask(squad.key, createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task1.isRight(), "$task1")
        assertTrue(task2.isRight(), "$task2")
        task1.flatMap { right1 ->
            task2.map { right2 ->
                assertTrue(right1 != right2, "$right1 and $right2 are the same")
            }
        }
    }

    @Test
    fun `createTask with a non existing user fails`() {
        val currentHero = createHeroOrThrow("heroId")
        val rawTitle = "title"

        val task =
            heroesDesk.createTask(ensureSquadExisting("squadKey").key, createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `createTask with a non existing squad fails`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"

        val task =
            heroesDesk.createTask(createSquadKeyOrThrow("squadKey"), createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `createTask with a creator not assigned to squad fails`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"

        val task =
            heroesDesk.createTask(ensureSquadExisting("squadKey").key, createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(task.isLeft())
        task.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
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
            assertTrue(it.head is HeroesNotInSquadError, "$it")
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

        val updatedTaskId = heroesDesk.updateDescription(nonExistingPendingTaskId(), newDescription, hero.id)

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
        assertEquals(HeroIds(hero), assignedTask.assignees)
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
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `assign task on pending task id fails when assigned hero not assigned to squad`() {
        assertAssignTaskFailsWhenAssignedHeroNotAssignedToSquad(createAssignedPendingTask().first)
    }

    @Test
    fun `assign task on in progress task id fails when assigned hero not assigned to squad`() {
        assertAssignTaskFailsWhenAssignedHeroNotAssignedToSquad(createAssignedInProgressTask().first)
    }

    private fun assertAssignTaskFailsWhenAssignedHeroNotAssignedToSquad(
        taskId: TaskId
    ) {
        val nonSquadAssignedHeroes = ensureHeroExisting("nonSquadAssignedHero")
        val assignmentAuthor = ensureHeroExisting("assignmentAuthor").id

        val assignedTask = when (taskId) {
            is PendingTaskId ->
                heroesDesk.assignTask(
                    taskId,
                    HeroIds(nonSquadAssignedHeroes),
                    assignmentAuthor
                )

            is InProgressTaskId ->
                heroesDesk.assignTask(
                    taskId,
                    HeroIds(nonSquadAssignedHeroes),
                    assignmentAuthor
                )

            else -> throw AssertionError("Non assignable task id type $taskId")
        }

        assertTrue(assignedTask.isLeft())
        assignedTask.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
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
        assignSquad(createdTask, heroes)

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
        assignSquad(createdTask, empty)

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `start work fails when hero not in the squad`() {
        val createdTask = ensureTaskExisting("title", "heroId")

        val updatedTaskId =
            heroesDesk.startWork(createdTask.taskId, ensureHeroExisting("heroId1").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
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
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `pause work fails when hero not in the squad`() {
        val (pendingTaskId, hero) = createAssignedPendingTask()
        val updatedTask =
            heroesDesk.startWork(pendingTaskId, hero.id).getOrElse { throw AssertionError("$it") }

        val pauseWorkResult =
            heroesDesk.pauseWork(updatedTask.taskId, ensureHeroExisting("${hero.id.value}Different").id)

        assertTrue(pauseWorkResult.isLeft())
        pauseWorkResult.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
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
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `end work fails when hero not in the squad`() {
        val (inProgressTaskId, hero) = createAssignedInProgressTask()

        val updatedTaskId =
            heroesDesk.endWork(inProgressTaskId, ensureHeroExisting("${hero.id.value}AndMore").id)

        assertTrue(updatedTaskId.isLeft())
        updatedTaskId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
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
        assignSquad(createdTask, heroes)
        heroesDesk.assignTask(taskId, HeroIds(hero), hero.id).getOrElse { throw AssertionError("$it") }
        return Pair(taskId, hero)
    }

    private fun assignSquad(task: Task<*>, heroes: Heroes): SquadMembers {
        return assignSquad(task.squadKey(), heroes)
    }

    private fun assignSquad(
        squadKey: SquadKey,
        heroes: Heroes
    ): SquadMembers {
        return heroesDesk.assignSquad(
            squadKey,
            HeroIds(heroes),
            userRepo.ensureAdminExistingOrThrow("squadAdminId").id
        )
            .getOrElse { throw AssertionError("$it") }
    }

    private fun ensureTaskExisting(title: String, hero: String): PendingTask =
        ensureTaskExisting(title, ensureHeroExisting(hero))

    private fun ensureTaskExisting(title: String, hero: Hero): PendingTask {
        val squad = ensureSquadExisting("squadKey")
        assignSquad(squad.key, Heroes(hero))
        return heroesDesk
            .createTask(
                squad.key, createTitleOrThrow(title), hero.id
            )
            .getOrElse { throw AssertionError("$it") }
    }

    private fun ensureHeroExisting(rawHeroId: String) = userRepo.ensureHeroExistingOrThrow(rawHeroId)

    private fun ensureSquadExisting(squadKey: String): Squad {
        val squadId = createSquadKeyOrThrow(squadKey)
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")
        val name = createNameOrThrow("name")
        return heroesDesk.createSquad(squadId, name, admin.id).getOrElse { throw AssertionError("$it") }

    }

    private fun nonExistingPendingTaskId(): PendingTaskId =
        createPendingTaskIdOrThrow("nonExistingPendingTaskId", nonExistingRawTaskId())

    private fun nonExistingWorkInProgressTaskId(): InProgressTaskId =
        createInProgressTaskIdOrThrow("nonExistingInProgressTaskId", nonExistingRawTaskId())

    abstract fun instrumentedUserRepository(): InstrumentedUserRepository

    abstract fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk

    abstract fun nonExistingRawTaskId(): String

    @BeforeEach
    fun beforeEach() {
        userRepo = instrumentedUserRepository()
        heroesDesk = createHeroesDesk(userRepo)
    }

}

