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
import org.hexastacks.heroesdesk.kotlin.user.Hero
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.user.Heroes
import org.hexastacks.heroesdesk.kotlin.user.Heroes.Companion.empty
import org.hexastacks.heroesdesk.kotlin.mission.*
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
        val createdMissionTarget = 250
        val admin = userRepo.ensureAdminExistingOrThrow("adminId")

        runBlocking {
            val jobs = List(runNb) {
                launch(dispatcher) {
                    val suffix = it % createdMissionTarget
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
        assertEquals(runNb - createdMissionTarget, failureNb)
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
    fun `createMission returns a mission`() {
        val currentHero = ensureHeroExisting("heroId")
        val squad = ensureSquadExisting("squadKey")
        val title = createTitleOrThrow("title")
        assignSquad(squad.key, Heroes(currentHero))

        val mission = heroesDesk.createMission(squad.key, title, currentHero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(title, mission.title)
        assertEquals(squad.key, mission.squadKey())
    }


    @Test
    fun `2 missions creation with same title and creator returns 2 distinct missions`() {
        val currentHero = ensureHeroExisting("heroId")
        val squad = ensureSquadExisting("squadKey")
        val rawTitle = "title"
        assignSquad(squad.key, Heroes(currentHero))


        val mission1 = heroesDesk.createMission(squad.key, createTitleOrThrow(rawTitle), currentHero.id)
        val mission2 = heroesDesk.createMission(squad.key, createTitleOrThrow(rawTitle), currentHero.id)

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
        val currentHero = createHeroOrThrow("heroId")
        val rawTitle = "title"

        val mission =
            heroesDesk.createMission(ensureSquadExisting("squadKey").key, createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(mission.isLeft())
        mission.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `createMission with a non existing squad fails`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"

        val mission =
            heroesDesk.createMission(createSquadKeyOrThrow("squadKey"), createTitleOrThrow(rawTitle), currentHero.id)

        assertTrue(mission.isLeft())
        mission.onLeft {
            assertTrue(it.head is SquadNotExistingError, "$it")
        }
    }

    @Test
    fun `createMission with a creator not assigned to squad fails`() {
        val currentHero = ensureHeroExisting("heroId")
        val rawTitle = "title"

        val mission =
            heroesDesk.createMission(ensureSquadExisting("squadKey").key, createTitleOrThrow(rawTitle), currentHero.id)

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
            assertTrue(it.head is MissionNotExistingError)
        }
    }

    @Test
    fun `update title works on existing MissionId`() {
        val hero = ensureHeroExisting("heroId")
        val createdMission = ensureMissionExisting("title", hero)
        val newTitle = createTitleOrThrow("new title")

        val updatedMission =
            heroesDesk.updateTitle(createdMission.missionId, newTitle, hero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(updatedMission.missionId, createdMission.missionId)
        assert(heroesDesk.getMissionOrThrow(updatedMission.missionId).title == newTitle)
    }

    @Test
    fun `update title fails with non existing MissionId`() {
        val heroId = ensureHeroExisting("heroId")
        val newTitle = createTitleOrThrow("new title")

        val updatedMissionId =
            heroesDesk.updateTitle(nonExistingPendingMissionId(), newTitle, heroId.id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is MissionNotExistingError)
        }
    }

    @Test
    fun `update title fails with non existing hero`() {
        val createdMission = ensureMissionExisting("title", "heroId")
        val newTitle = createTitleOrThrow("new title")

        val updatedMissionId =
            heroesDesk.updateTitle(createdMission.missionId, newTitle, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `update description works on existing MissionId`() {
        val hero = ensureHeroExisting("heroId")
        val createdMission = ensureMissionExisting("title", hero)
        val newDescription = createDescriptionOrThrow("new description")

        val updatedMission =
            heroesDesk.updateDescription(createdMission.missionId, newDescription, hero.id)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(createdMission.missionId, updatedMission.missionId)
        assert(heroesDesk.getMissionOrThrow(updatedMission.missionId).description == newDescription)
    }

    @Test
    fun `update description fails with non existing MissionId`() {
        val newDescription = createDescriptionOrThrow("new description")
        val hero = ensureHeroExisting("heroId")

        val updatedMissionId = heroesDesk.updateDescription(nonExistingPendingMissionId(), newDescription, hero.id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is MissionNotExistingError)
        }
    }

    @Test
    fun `update description fails with non existing hero`() {
        val createdMission = ensureMissionExisting("title", "heroId")
        val newDescription = createDescriptionOrThrow("new description")

        val updatedMissionId =
            heroesDesk.updateDescription(createdMission.missionId, newDescription, createHeroOrThrow("nonExistingHero").id)

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
        hero: Hero
    ) {
        val assignedMission = when (missionId) {
            is PendingMissionId -> heroesDesk.assignMission(
                missionId,
                HeroIds(hero),
                hero.id
            ).getOrElse { throw AssertionError("$it") }

            is InProgressMissionId -> heroesDesk.assignMission(
                missionId,
                HeroIds(hero),
                hero.id
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
        val hero = createHeroOrThrow("heroId1")
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
        val nonSquadAssignedHeroes = ensureHeroExisting("nonSquadAssignedHero")
        val assignmentAuthor = ensureHeroExisting("assignmentAuthor").id

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
            heroesDesk.startWork(missionId, hero.id)
                .getOrElse { throw AssertionError("$it") }

        assertEquals(updatedMissionId.missionId, missionId)
    }

    @Test
    fun `start work assigns hero starting work to mission if no other assignee is present`() {
        val createdMission = ensureMissionExisting("title", "randomHeroId")
        val missionId = createdMission.missionId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        assignSquad(createdMission, heroes)

        val updatedMissionId =
            heroesDesk.startWork(missionId, hero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(missionId.value, updatedMissionId.missionId.value)
        assertTrue(updatedMissionId.assignees.contains(hero.id))
    }

    @Test
    fun `start work fails with non existing MissionId`() {
        val hero = createHeroOrThrow("heroId")

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
        assignSquad(createdMission, empty)

        val updatedMissionId =
            heroesDesk.startWork(createdMission.missionId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `start work fails when hero not in the squad`() {
        val createdMission = ensureMissionExisting("title", "heroId")

        val updatedMissionId =
            heroesDesk.startWork(createdMission.missionId, ensureHeroExisting("heroId1").id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `pause work on in progress mission works`() {
        val (missionId, assignedHero) = createAssignedPendingMission()
        val inProgressMission = heroesDesk.startWork(missionId, assignedHero.id).getOrElse { throw AssertionError("$it") }

        val pausedMission =
            heroesDesk.pauseWork(inProgressMission.missionId, assignedHero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(missionId, pausedMission.missionId)
    }

    @Test
    fun `pause work fails with non existing MissionId`() {
        val hero = createHeroOrThrow("heroId")

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
            heroesDesk.startWork(pendingMissionId, hero.id).getOrElse { throw AssertionError("$it") }

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
            heroesDesk.startWork(pendingMissionId, hero.id).getOrElse { throw AssertionError("$it") }

        val pauseWorkResult =
            heroesDesk.pauseWork(updatedMission.missionId, ensureHeroExisting("${hero.id.value}Different").id)

        assertTrue(pauseWorkResult.isLeft())
        pauseWorkResult.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `end work on in progress mission works, removing all assignees on the way`() {
        val (missionId, hero) = createAssignedInProgressMission()


        val doneMission = heroesDesk.endWork(missionId, hero.id).getOrElse { throw AssertionError("$it") }

        assertEquals(missionId, doneMission.missionId)
        assertTrue(doneMission.assignees.isEmpty())
    }

    @Test
    fun `end work fails with non existing MissionId`() {
        val hero = createHeroOrThrow("heroId")

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
            heroesDesk.endWork(inProgressMissionId, createHeroOrThrow("nonExistingHero").id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    @Test
    fun `end work fails when hero not in the squad`() {
        val (inProgressMissionId, hero) = createAssignedInProgressMission()

        val updatedMissionId =
            heroesDesk.endWork(inProgressMissionId, ensureHeroExisting("${hero.id.value}AndMore").id)

        assertTrue(updatedMissionId.isLeft())
        updatedMissionId.onLeft {
            assertTrue(it.head is HeroesNotInSquadError, "$it")
        }
    }

    private fun createAssignedInProgressMission(): Pair<InProgressMissionId, Hero> {
        val (missionId, hero) = createAssignedPendingMission()
        return Pair(
            heroesDesk.startWork(missionId, hero.id).getOrElse { throw AssertionError("$it") }.missionId,
            hero
        )
    }

    private fun createAssignedPendingMission(): Pair<PendingMissionId, Hero> {
        val createdMission = ensureMissionExisting("title", "randomHeroId")
        val missionId = createdMission.missionId
        val hero = ensureHeroExisting("heroId1")
        val heroes = Heroes(hero)
        assignSquad(createdMission, heroes)
        heroesDesk.assignMission(missionId, HeroIds(hero), hero.id).getOrElse { throw AssertionError("$it") }
        return Pair(missionId, hero)
    }

    private fun assignSquad(mission: Mission<*>, heroes: Heroes): SquadMembers {
        return assignSquad(mission.squadKey(), heroes)
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

    private fun ensureMissionExisting(title: String, hero: String): PendingMission =
        ensureMissionExisting(title, ensureHeroExisting(hero))

    private fun ensureMissionExisting(title: String, hero: Hero): PendingMission {
        val squad = ensureSquadExisting("squadKey")
        assignSquad(squad.key, Heroes(hero))
        return heroesDesk
            .createMission(
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

    private fun nonExistingPendingMissionId(): PendingMissionId =
        createPendingMissionIdOrThrow("nonExistingPendingMissionId", nonExistingRawMissionId())

    private fun nonExistingWorkInProgressMissionId(): InProgressMissionId =
        createInProgressMissionIdOrThrow("nonExistingInProgressMissionId", nonExistingRawMissionId())

    abstract fun instrumentedUserRepository(): InstrumentedUserRepository

    abstract fun createHeroesDesk(userRepo: InstrumentedUserRepository): HeroesDesk

    abstract fun nonExistingRawMissionId(): String

    @BeforeEach
    fun beforeEach() {
        userRepo = instrumentedUserRepository()
        heroesDesk = createHeroesDesk(userRepo)
    }

}

