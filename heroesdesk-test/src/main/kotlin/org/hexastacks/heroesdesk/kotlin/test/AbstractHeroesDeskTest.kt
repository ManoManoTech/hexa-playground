package org.hexastacks.heroesdesk.kotlin.test

import arrow.atomic.AtomicInt
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.mission.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createAdminIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createHeroIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createHeroOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createInProgressMissionIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createNameOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createPendingMissionIdOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createSquadKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.test.HeroesDeskTestUtils.getMissionOrThrow
import org.hexastacks.heroesdesk.kotlin.user.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

abstract class AbstractHeroesDeskTest {

    private lateinit var heroesDesk: HeroesDesk

    companion object {
        // each test is run in a new instance of the test class: the counter needs to be static
        private val idCounter = AtomicInt(0)
    }

    @Test
    fun `createSquad returns a squad`() {
        val id = createSquadKeyOrThrow()
        val name = createNameOrThrow()
        val creatorId = ensureAdminExistingOrThrow()

        val squad = heroesDesk.createSquad(
            id,
            name,
            creatorId
        ).getOrElse { throw AssertionError("$it") }

        assertEquals(name, squad.name)
    }


    @Test
    fun `createSquad fails on non existing admin`() {
        val creationFailure =
            heroesDesk.createSquad(
                createSquadKeyOrThrow(),
                createNameOrThrow(),
                createAdminIdOrThrow()
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is AdminNotExistingError, "$it")
        }
    }


    @Test
    fun `createSquad fails on pre existing squad with same name`() {
        val name = createNameOrThrow()
        heroesDesk.createSquad(
            createSquadKeyOrThrow(),
            name,
            ensureAdminExistingOrThrow()
        )

        val creationFailure =
            heroesDesk.createSquad(
                createSquadKeyOrThrow(),
                name,
                ensureAdminExistingOrThrow()
            )

        assertTrue(creationFailure.isLeft())
        creationFailure.onLeft {
            assertTrue(it.head is SquadNameAlreadyExistingError, "$it")
        }
    }

    @Test
    fun `createSquad fails on pre existing squad with same id`() {
        val id = createSquadKeyOrThrow()
        val creator = ensureAdminExistingOrThrow()
        heroesDesk.createSquad(
            id,
            createNameOrThrow(),
            creator
        )

        val creationFailure =
            heroesDesk.createSquad(
                id,
                createNameOrThrow(),
                creator
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
        val createdMissionTarget = 250
        val admin = ensureAdminExistingOrThrow()

        runBlocking {
            val jobs = List(runNb) {
                launch(dispatcher) {
                    val suffix = it % createdMissionTarget
                    results[it] = heroesDesk
                        .createSquad(
                            createSquadKeyOrThrow("squadKey$suffix"),
                            createNameOrThrow("squadName${suffix}"),
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
        assertEquals(runNb - createdMissionTarget, failureNb)
    }

    @Test
    fun `assignSquad works`() {
        val squadId = createSquadKeyOrThrow()
        val heroes = HeroIds(ensureHeroExistingOrThrow())
        val admin = ensureAdminExistingOrThrow()
        val squad =
            heroesDesk.createSquad(squadId, createNameOrThrow(), admin)
                .getOrElse { throw AssertionError("$it") }

        val squadMembers =
            heroesDesk.assignSquad(squadId, heroes, admin).getOrElse { throw AssertionError(it.toString()) }

        assertEquals(squad.key, squadMembers.squadKey)
        val storedSquad = heroesDesk.getSquad(squadId).getOrElse { throw AssertionError("$it") }
        assertEquals(squad, storedSquad)
    }


    @Test
    fun `assignSquad fails on inexisting squad`() {
        val squadId = createSquadKeyOrThrow()
        val heroIds = HeroIds(ensureHeroExistingOrThrow())
        val admin = ensureAdminExistingOrThrow()

        val assignedSquad = heroesDesk.assignSquad(squadId, heroIds, admin)

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `assignSquad fails on inexisting heroIds`() {
        val squadId = createSquadKeyOrThrow()
        val heroIds = HeroIds(createHeroIdOrThrow())
        val admin = ensureAdminExistingOrThrow()
        heroesDesk.createSquad(squadId, createNameOrThrow(), admin).getOrElse { throw AssertionError("$it") }

        val assignedSquad = heroesDesk.assignSquad(squadId, heroIds, admin)

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is HeroesNotExistingError, "$it")
        }
    }


    @Test
    fun `assignSquad fails on inexisting admin`() {
        val squadId = createSquadKeyOrThrow()
        val heroIds = HeroIds(ensureHeroExistingOrThrow())
        val admin = ensureAdminExistingOrThrow()
        heroesDesk.createSquad(squadId, createNameOrThrow(), admin).getOrElse { throw AssertionError("$it") }

        val assignedSquad = heroesDesk.assignSquad(squadId, heroIds, createAdminIdOrThrow())

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is AdminNotExistingError, "$it")
        }
    }

    @Test
    fun `updateSquadName fails on inexisting admin`() {
        val squadId = createSquadKeyOrThrow()
        val admin = ensureAdminExistingOrThrow()
        heroesDesk.createSquad(squadId, createNameOrThrow(), admin).getOrElse { throw AssertionError("$it") }

        val assignedSquad =
            heroesDesk.updateSquadName(squadId, createNameOrThrow(), createAdminIdOrThrow())

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is AdminNotExistingError, "$it")
        }
    }

    @Test
    fun `updateSquadName fails on inexisting squad`() {
        val squadId = createSquadKeyOrThrow()
        val admin = ensureAdminExistingOrThrow()

        val assignedSquad = heroesDesk.updateSquadName(squadId, createNameOrThrow(), admin)

        assertTrue(assignedSquad.isLeft())
        assignedSquad.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `updateSquadName works`() {
        val squadId = createSquadKeyOrThrow()
        val admin = ensureAdminExistingOrThrow()
        val initialName = createNameOrThrow("name")
        heroesDesk.createSquad(squadId, initialName, admin).getOrElse { throw AssertionError("$it") }
        val newName = createNameOrThrow(initialName.value + "Updated")

        val assignedSquad =
            heroesDesk.updateSquadName(squadId, newName, admin).getOrElse { throw AssertionError("$it") }

        assertEquals(newName, assignedSquad.name)
    }

    @Test
    fun `getSquad fails on inexisting squad`() {
        val squadId = createSquadKeyOrThrow()

        val squad = heroesDesk.getSquad(squadId)

        assertTrue(squad.isLeft())
        squad.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `getSquadMembers works on squad without assignee`() {
        val squadId = createSquadKeyOrThrow()
        val admin = ensureAdminExistingOrThrow()
        val name = createNameOrThrow()
        heroesDesk.createSquad(squadId, name, admin).getOrElse { throw AssertionError("$it") }

        val squadMembers =
            heroesDesk.getSquadMembers(squadId)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(squadId, squadMembers.squadKey)
        assertEquals(HeroIds.empty, squadMembers.heroes)

    }

    @Test
    fun `getSquadMembers works on squad with assignee`() {
        val squadId = createSquadKeyOrThrow()
        val admin = ensureAdminExistingOrThrow()
        val name = createNameOrThrow()
        heroesDesk.createSquad(squadId, name, admin).getOrElse { throw AssertionError("$it") }
        val assignees = HeroIds(ensureHeroExistingOrThrow())
        heroesDesk.assignSquad(squadId, assignees, admin)
            .getOrElse { throw AssertionError("$it") }

        val squadMembers = heroesDesk.getSquadMembers(squadId).getOrElse { throw AssertionError("$it") }

        assertEquals(squadId, squadMembers.squadKey)
        assertEquals(assignees, squadMembers.heroes)
    }

    @Test
    fun `createMission returns a mission`() {
        val currentHero = ensureHeroExistingOrThrow()
        val squad = ensureSquadExisting()
        val title = createTitleOrThrow()
        assignSquad(squad.key, HeroIds(currentHero))

        val mission = heroesDesk.createMission(squad.key, title, currentHero).getOrElse { throw AssertionError("$it") }

        assertEquals(title, mission.title)
        assertEquals(squad.key, mission.squadKey())
    }


    @Test
    fun `2 missions creation with same title and creator returns 2 distinct missions`() {
        val creator = ensureHeroExistingOrThrow()
        val squad = ensureSquadExisting()
        val rawTitle = "title"
        assignSquad(squad.key, HeroIds(creator))


        val mission1 = heroesDesk.createMission(squad.key, createTitleOrThrow(rawTitle), creator)
        val mission2 = heroesDesk.createMission(squad.key, createTitleOrThrow(rawTitle), creator)

        assertTrue(mission1.isRight(), "$mission1")
        assertTrue(mission2.isRight(), "$mission2")
        mission1.flatMap { right1 ->
            mission2.map { right2 ->
                assertTrue(right1 != right2, "$right1 and $right2 are the same")
            }
        }
    }

    @Test
    fun `createMission with a non existing user fails`() {
        val currentHero = createHeroOrThrow()

        val mission =
            heroesDesk.createMission(ensureSquadExisting().key, createTitleOrThrow(), currentHero.id)

        assertTrue(mission.isLeft())
        mission.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `createMission with a non existing squad fails`() {
        val currentHero = ensureHeroExistingOrThrow()

        val mission =
            heroesDesk.createMission(createSquadKeyOrThrow(), createTitleOrThrow(), currentHero)

        assertTrue(mission.isLeft())
        mission.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `createMission with a creator not assigned to squad fails`() {
        val currentHero = ensureHeroExistingOrThrow()

        val mission =
            heroesDesk.createMission(ensureSquadExisting("squadKey").key, createTitleOrThrow(), currentHero)

        assertTrue(mission.isLeft())
        mission.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }


    @Test
    fun `get mission works on existing MissionId`() {
        val createdMission = ensureMissionExisting("title", "heroId")

        val retrievedMission = heroesDesk.getMission(createdMission.missionId).getOrElse { throw AssertionError("$it") }

        assertEquals(retrievedMission, createdMission)
    }

    @Test
    fun `get mission fails on non existing MissionId`() {
        val mission = heroesDesk.getMission(nonExistingPendingMissionId())

        assertTrue(mission.isLeft())
        mission.onLeft {
            assertTrue(it.head is MissionNotExistingError, "$it")
        }
    }

    @Test
    fun `update title works on existing MissionId`() {
        val hero = ensureHeroExistingOrThrow()
        val createdMission = ensureMissionExisting("title", hero)
        val newTitle = createTitleOrThrow()

        val updatedMission =
            heroesDesk.updateTitle(createdMission.missionId, newTitle, hero).getOrElse { throw AssertionError("$it") }

        assertEquals(updatedMission.missionId, createdMission.missionId)
        assert(heroesDesk.getMissionOrThrow(updatedMission.missionId).title == newTitle)
    }

    @Test
    fun `update title fails with non existing MissionId`() {
        val heroId = ensureHeroExistingOrThrow()
        val newTitle = createTitleOrThrow()

        val updatedMissionId =
            heroesDesk.updateTitle(nonExistingPendingMissionId(), newTitle, heroId)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is MissionNotExistingError, "$it")
        }
    }

    @Test
    fun `update title fails with non existing hero`() {
        val createdMission = ensureMissionExisting("title", "heroId")
        val newTitle = createTitleOrThrow()

        val updatedMissionId =
            heroesDesk.updateTitle(createdMission.missionId, newTitle, createHeroOrThrow().id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `update description works on existing MissionId`() {
        val hero = ensureHeroExistingOrThrow()
        val createdMission = ensureMissionExisting("title", hero)
        val newDescription = createDescriptionOrThrow("new description")

        val updatedMission =
            heroesDesk.updateDescription(createdMission.missionId, newDescription, hero)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(createdMission.missionId, updatedMission.missionId)
        assert(heroesDesk.getMissionOrThrow(updatedMission.missionId).description == newDescription)
    }

    @Test
    fun `update description fails with non existing MissionId`() {
        val newDescription = createDescriptionOrThrow("new description")
        val hero = ensureHeroExistingOrThrow()

        val updatedMissionId = heroesDesk.updateDescription(nonExistingPendingMissionId(), newDescription, hero)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is MissionNotExistingError, "$it")
        }
    }

    @Test
    fun `update description fails with non existing hero`() {
        val createdMission = ensureMissionExisting("title", "heroId")
        val newDescription = createDescriptionOrThrow("new description")

        val updatedMissionId =
            heroesDesk.updateDescription(
                createdMission.missionId,
                newDescription,
                createHeroOrThrow().id
            )

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotExistingError, "$it")
        }
    }

    @Test
    fun `assign mission on pending mission id works`() {
        val (pendingMissionId, hero) = createAssignedPendingMission()

        assertAssignMissionWorks(pendingMissionId, hero)
    }

    @Test
    fun `assign mission on in progress mission id works`() {
        val (inProgressMissionId, hero) = createAssignedInProgressMission()

        assertAssignMissionWorks(inProgressMissionId, hero)
    }

    private fun <T : MissionId> assertAssignMissionWorks(
        missionId: T,
        hero: HeroId
    ) {
        val assignedMission = when (missionId) {
            is PendingMissionId -> heroesDesk.assignMission(
                missionId,
                HeroIds(hero),
                hero
            ).getOrElse { throw AssertionError("$it") }

            is InProgressMissionId -> heroesDesk.assignMission(
                missionId,
                HeroIds(hero),
                hero
            ).getOrElse { throw AssertionError("$it") }

            else -> throw AssertionError("Non assignable mission id type $missionId")
        }

        assertEquals(missionId, assignedMission.missionId)
        assertEquals(HeroIds(hero), assignedMission.assignees)
    }

    @Test
    fun `assign mission on pending mission id fails on non existing mission`() {
        assertAssignMissionFailsOnNonExistingMission(nonExistingPendingMissionId())
    }

    @Test
    fun `assign mission on in progress mission id fails on non existing mission`() {
        assertAssignMissionFailsOnNonExistingMission(nonExistingWorkInProgressMissionId())
    }

    private fun assertAssignMissionFailsOnNonExistingMission(
        id: MissionId
    ) {
        val hero = createHeroOrThrow()
        val heroes = Heroes(hero)

        val assignedMission = when (id) {
            is PendingMissionId -> heroesDesk.assignMission(
                id,
                HeroIds(heroes),
                hero.id
            )

            is InProgressMissionId -> heroesDesk.assignMission(
                id,
                HeroIds(heroes),
                hero.id
            )

            else -> throw AssertionError("Non assignable mission id type $id")
        }

        assertTrue(assignedMission.isLeft())
        assignedMission.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `assign mission on pending mission id fails when assigned hero not assigned to squad`() {
        assertAssignMissionFailsWhenAssignedHeroNotAssignedToSquad(createAssignedPendingMission().first)
    }

    @Test
    fun `assign mission on in progress mission id fails when assigned hero not assigned to squad`() {
        assertAssignMissionFailsWhenAssignedHeroNotAssignedToSquad(createAssignedInProgressMission().first)
    }

    private fun assertAssignMissionFailsWhenAssignedHeroNotAssignedToSquad(
        missionId: MissionId
    ) {
        val nonSquadAssignedHeroes = ensureHeroExistingOrThrow(nextId("nonSquadAssignedHero"))
        val assignmentAuthor = ensureHeroExistingOrThrow(nextId("assignmentAuthor"))

        val assignedMission = when (missionId) {
            is PendingMissionId ->
                heroesDesk.assignMission(
                    missionId,
                    HeroIds(nonSquadAssignedHeroes),
                    assignmentAuthor
                )

            is InProgressMissionId ->
                heroesDesk.assignMission(
                    missionId,
                    HeroIds(nonSquadAssignedHeroes),
                    assignmentAuthor
                )

            else -> throw AssertionError("Non assignable mission id type $missionId")
        }

        assertTrue(assignedMission.isLeft())
        assignedMission.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `start work on pending mission works on existing & pending mission`() {
        val (missionId, hero) = createAssignedPendingMission()

        val updatedMissionId =
            heroesDesk.startWork(missionId, hero)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(updatedMissionId.missionId, missionId)
    }

    @Test
    fun `start work assigns hero starting work to mission if no other assignee is present`() {
        val createdMission = ensureMissionExisting("title", "randomHeroId")
        val missionId = createdMission.missionId
        val hero = ensureHeroExistingOrThrow()
        val heroes = HeroIds(hero)
        assignSquad(createdMission, heroes)

        val updatedMissionId =
            heroesDesk.startWork(missionId, hero).getOrElse { throw AssertionError("$it") }

        assertEquals(missionId.value, updatedMissionId.missionId.value)
        assertTrue(updatedMissionId.assignees.contains(hero))
    }

    @Test
    fun `start work fails with non existing MissionId`() {
        val hero = createHeroOrThrow()

        val updatedMissionId =
            heroesDesk.startWork(nonExistingPendingMissionId(), hero.id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId
            .onLeft {
                assertTrue(it.head is MissionNotExistingError, "$it")
            }
    }

    @Test
    fun `start work fails with non existing hero`() {
        val createdMission = ensureMissionExisting("title", "heroId")
        assignSquad(createdMission, HeroIds.empty)

        val updatedMissionId =
            heroesDesk.startWork(createdMission.missionId, createHeroOrThrow().id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `start work fails when hero not in the squad`() {
        val createdMission = ensureMissionExisting("title", "heroId")

        val updatedMissionId =
            heroesDesk.startWork(createdMission.missionId, ensureHeroExistingOrThrow())

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `pause work on in progress mission works`() {
        val (missionId, assignedHero) = createAssignedPendingMission()
        val inProgressMission = heroesDesk.startWork(missionId, assignedHero).getOrElse { throw AssertionError("$it") }

        val pausedMission =
            heroesDesk.pauseWork(inProgressMission.missionId, assignedHero).getOrElse { throw AssertionError("$it") }

        assertEquals(missionId, pausedMission.missionId)
    }

    @Test
    fun `pause work fails with non existing MissionId`() {
        val hero = createHeroOrThrow()

        val updatedMissionId =
            heroesDesk.pauseWork(nonExistingWorkInProgressMissionId(), hero.id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId
            .onLeft {
                assertTrue(it.head is MissionNotExistingError, "$it")
            }
    }

    @Test
    fun `pause work fails with non existing hero`() {
        val (pendingMissionId, hero) = createAssignedPendingMission()
        val updatedMission =
            heroesDesk.startWork(pendingMissionId, hero).getOrElse { throw AssertionError("$it") }

        val pauseWorkResult = heroesDesk.pauseWork(updatedMission.missionId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(pauseWorkResult.isLeft())
        pauseWorkResult.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `pause work fails when hero not in the squad`() {
        val (pendingMissionId, hero) = createAssignedPendingMission()
        val updatedMission =
            heroesDesk.startWork(pendingMissionId, hero).getOrElse { throw AssertionError("$it") }

        val pauseWorkResult =
            heroesDesk.pauseWork(updatedMission.missionId, ensureHeroExistingOrThrow("${hero.value}Different"))

        assertTrue(pauseWorkResult.isLeft())
        pauseWorkResult.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `end work on in progress mission works, removing all assignees on the way`() {
        val (missionId, hero) = createAssignedInProgressMission()


        val doneMission = heroesDesk.endWork(missionId, hero).getOrElse { throw AssertionError("$it") }

        assertEquals(missionId, doneMission.missionId)
        assertTrue(doneMission.assignees.isEmpty())
    }

    @Test
    fun `end work fails with non existing MissionId`() {
        val hero = createHeroOrThrow()

        val updatedMissionId =
            heroesDesk.endWork(nonExistingWorkInProgressMissionId(), hero.id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId
            .onLeft {
                assertTrue(it.head is MissionNotExistingError, "$it")
            }
    }

    @Test
    fun `end work fails with non existing hero`() {
        val (inProgressMissionId, _) = createAssignedInProgressMission()

        val updatedMissionId =
            heroesDesk.endWork(inProgressMissionId, createHeroOrThrow(nextId("nonExistingHero")).id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `end work fails when hero not in the squad`() {
        val (inProgressMissionId, hero) = createAssignedInProgressMission()

        val updatedMissionId =
            heroesDesk.endWork(inProgressMissionId, ensureHeroExistingOrThrow(nextId("${hero.value}AndMore")))

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    private fun createAssignedInProgressMission(): Pair<InProgressMissionId, HeroId> {
        val (missionId, hero) = createAssignedPendingMission()
        return Pair(
            heroesDesk.startWork(missionId, hero).getOrElse { throw AssertionError("$it") }.missionId,
            hero
        )
    }

    private fun createAssignedPendingMission(): Pair<PendingMissionId, HeroId> {
        val createdMission = ensureMissionExisting("title", "randomHeroId")
        val missionId = createdMission.missionId
        val hero = ensureHeroExistingOrThrow()
        val heroes = HeroIds(hero)
        assignSquad(createdMission, heroes)
        heroesDesk.assignMission(missionId, HeroIds(hero), hero).getOrElse { throw AssertionError("$it") }
        return Pair(missionId, hero)
    }

    private fun assignSquad(mission: Mission<*>, heroes: HeroIds): SquadMembers {
        return assignSquad(mission.squadKey(), heroes)
    }

    private fun assignSquad(
        squadKey: SquadKey,
        heroes: HeroIds
    ): SquadMembers {
        return heroesDesk.assignSquad(
            squadKey,
            heroes,
            ensureAdminExistingOrThrow()
        )
            .getOrElse { throw AssertionError("$it") }
    }

    private fun ensureMissionExisting(title: String, hero: String): PendingMission =
        ensureMissionExisting(title, ensureHeroExistingOrThrow(nextId(hero)))

    private fun ensureMissionExisting(title: String, hero: HeroId): PendingMission {
        val squad = ensureSquadExisting()
        assignSquad(squad.key, HeroIds(hero))
        return heroesDesk
            .createMission(
                squad.key, createTitleOrThrow(title), hero
            )
            .getOrElse { throw AssertionError("$it") }
    }

    private fun ensureSquadExisting(squadKey: String): Squad {
        val squadId = createSquadKeyOrThrow(squadKey)
        val admin = ensureAdminExistingOrThrow()
        val name = createNameOrThrow()
        return heroesDesk.createSquad(squadId, name, admin).getOrElse { throw AssertionError("$it") }

    }

    private fun nonExistingPendingMissionId(): PendingMissionId =
        createPendingMissionIdOrThrow("nonExistingPendingMissionId", "nonExistingRawMissionId")

    private fun nonExistingWorkInProgressMissionId(): InProgressMissionId =
        createInProgressMissionIdOrThrow("nonExistingInProgressMissionId", "nonExistingRawMissionId")

    abstract fun createHeroesDesk(): HeroesDesk

    abstract fun ensureAdminExistingOrThrow(id: String): AdminId

    abstract fun ensureHeroExistingOrThrow(id: String): HeroId


    @BeforeEach
    fun beforeEach() {
        heroesDesk = createHeroesDesk()
    }

    private fun nextId(id: String) = "${id}_${idCounter.incrementAndGet()}"

    private fun ensureAdminExistingOrThrow(): AdminId = ensureAdminExistingOrThrow(nextId("admin"))

    private fun ensureHeroExistingOrThrow(): HeroId = ensureHeroExistingOrThrow(nextId("hero"))

    private fun ensureSquadExisting(): Squad = ensureSquadExisting(nextId("squadId"))

    private fun createNameOrThrow(): Name = createNameOrThrow(nextId("name"))

    private fun createSquadKeyOrThrow(): SquadKey = createSquadKeyOrThrow(nextId("squadId"))

    private fun createAdminIdOrThrow(): AdminId = createAdminIdOrThrow(nextId("admin"))

    private fun createHeroIdOrThrow(): HeroId = createHeroIdOrThrow(nextId("hero"))

    private fun createTitleOrThrow(): Title = createTitleOrThrow(nextId("title"))

    private fun createHeroOrThrow(): Hero = createHeroOrThrow(nextId("hero"))
}

