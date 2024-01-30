package org.hexastacks.heroesdesk.kotlin.ports.inmemory

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.misc.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.user.Heroes
import org.hexastacks.heroesdesk.kotlin.mission.*
import org.hexastacks.heroesdesk.kotlin.ports.MissionRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class InMemoryMissionRepository : MissionRepository {

    private val database = ConcurrentHashMap<SquadKey, Pair<InMemorySquad, Map<RawMissionId, RawMission>>>()

    override fun createMission(
        squadKey: SquadKey,
        title: Title
    ): Either<NonEmptyList<CreateMissionError>, PendingMission> {
        val createdMission = AtomicReference<PendingMission>()
        return database.computeIfPresent(squadKey) { _, squadAndMissionIdsToMission ->
            val inMemorySquad = squadAndMissionIdsToMission.first
            val uuid = UUID.randomUUID().toString()
            val retrievedSquad = inMemorySquad.toSquad()
            val missionId = PendingMissionId(squadKey, uuid).getOrElse {
                throw RuntimeException("missionId $uuid should be valid")
            }
            val mission = PendingMission(missionId, title)
            createdMission.set(mission)
            Pair(inMemorySquad, squadAndMissionIdsToMission.second.plus(RawMissionId(missionId) to RawMission(mission)))
        }
            ?.let { Right(createdMission.get()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
    }

    override fun getMission(missionId: MissionId): Either<NonEmptyList<GetMissionError>, Mission<*>> =
        database[missionId.squadKey]
            ?.let { squadAndMissionIdsToMission ->
                val rawMissionId = RawMissionId(missionId)
                val mission = squadAndMissionIdsToMission.second[rawMissionId]
                mission?.let { buildMission(rawMissionId, it, squadAndMissionIdsToMission.first.key) }
            }
            ?.let { Right(it) }
            ?: Left(nonEmptyListOf(MissionNotExistingError(missionId)))

    private fun buildMission(
        rawMissionId: RawMissionId,
        mission: RawMission,
        squad: SquadKey
    ): Mission<*> =
        when (mission.type) {
            MissionType.PENDING -> PendingMission(
                PendingMissionId(
                    squad,
                    rawMissionId.value
                ).getOrElse { throw RuntimeException("missionId ${rawMissionId.value} should be valid") },
                mission.title,
                mission.description,
                mission.assignees
            )

            MissionType.DONE -> DoneMission(
                DoneMissionId(
                    squad,
                    rawMissionId.value
                ).getOrElse { throw RuntimeException("missionId ${rawMissionId.value} should be valid") },
                mission.title,
                mission.description
            )

            MissionType.IN_PROGRESS -> InProgressMission(
                InProgressMissionId(
                    squad,
                    rawMissionId.value
                ).getOrElse { throw RuntimeException("missionId ${rawMissionId.value} should be valid") },
                mission.title,
                mission.description,
                mission.assignees
            )
        }

    override fun updateTitle(
        missionId: MissionId,
        title: Title
    ): EitherNel<UpdateTitleError, Mission<*>> =
        database
            .computeIfPresent(missionId.squadKey) { _, squadAndMissionIdsToMission ->
                val squad = squadAndMissionIdsToMission.first
                val rawMissionId = RawMissionId(missionId)
                squadAndMissionIdsToMission
                    .second[rawMissionId]
                    ?.copy(title = title)
                    ?.let { Pair(squad, squadAndMissionIdsToMission.second.plus(rawMissionId to it)) }
            }
            ?.let {
                Right(buildMission(it, missionId))
            }
            ?: Left(nonEmptyListOf(MissionNotExistingError(missionId)))

    private fun buildMission(
        it: Pair<InMemorySquad, Map<RawMissionId, RawMission>>,
        missionId: MissionId
    ): Mission<*> {
        val rawMission: RawMission = it.second[RawMissionId(missionId)]!!
        val mission = buildMission(RawMissionId(missionId), rawMission, missionId.squadKey)
        return mission
    }

    override fun updateDescription(
        missionId: MissionId,
        description: Description
    ): Either<NonEmptyList<UpdateDescriptionError>, Mission<*>> =
        database
            .computeIfPresent(missionId.squadKey) { _, squadAndMissionIdsToMission ->
                val squad = squadAndMissionIdsToMission.first
                val rawMissionId = RawMissionId(missionId)
                squadAndMissionIdsToMission
                    .second[rawMissionId]
                    ?.copy(description = description)
                    ?.let { Pair(squad, squadAndMissionIdsToMission.second.plus(rawMissionId to it)) }
            }
            ?.let { Right(buildMission(it, missionId)) }
            ?: Left(nonEmptyListOf(MissionNotExistingError(missionId)))

    override fun assignMission(
        missionId: MissionId,
        assignees: HeroIds
    ): EitherNel<AssignMissionError, Mission<*>> =
        database
            .computeIfPresent(missionId.squadKey) { _, squadAndMissionIdsToMission ->
                val squad = squadAndMissionIdsToMission.first
                val rawMissionId = RawMissionId(missionId)
                val mission: RawMission? = squadAndMissionIdsToMission.second[rawMissionId]
                val updatedMission = mission?.copy(assignees = assignees)
                updatedMission?.let { Pair(squad, squadAndMissionIdsToMission.second.plus(rawMissionId to updatedMission)) }
            }
            ?.let { Right(buildMission(it, missionId)) }
            ?: Left(nonEmptyListOf(MissionNotExistingError(missionId)))

    override fun startWork(
        pendingMissionId: PendingMissionId
    ): EitherNel<StartWorkError, InProgressMission> =
        database
            .computeIfPresent(pendingMissionId.squadKey) { _, squadAndMissionIdsToMission ->
                val inMemorySquad = squadAndMissionIdsToMission.first
                val rawMissionId = RawMissionId(pendingMissionId)
                val mission = squadAndMissionIdsToMission.second[rawMissionId]
                if (mission?.type == MissionType.PENDING) {
                    val retrievedSquad = inMemorySquad.toSquad()
                    InProgressMissionId(retrievedSquad.key, rawMissionId.value)
                        .map { inProgressMissionId ->
                            val inProgressMission =
                                InProgressMission(
                                    inProgressMissionId,
                                    mission.title,
                                    mission.description,
                                    mission.assignees
                                )
                            Pair(
                                inMemorySquad,
                                squadAndMissionIdsToMission.second.plus(rawMissionId to RawMission(inProgressMission))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(buildMission(it, pendingMissionId) as InProgressMission) }
            ?: Left(nonEmptyListOf(MissionNotExistingError(pendingMissionId)))

    override fun createSquad(squadKey: SquadKey, name: Name): EitherNel<CreateSquadError, Squad> {
        return if (database.any { it.value.first.name == name }) {
            Left(nonEmptyListOf(SquadNameAlreadyExistingError(name)))
        } else if (database.containsKey(squadKey)) {
            Left(nonEmptyListOf(SquadKeyAlreadyExistingError(squadKey)))
        } else
            Right(
                database
                    .computeIfAbsent(squadKey) { _ ->
                        Pair(InMemorySquad(name, squadKey), ConcurrentHashMap<RawMissionId, RawMission>())
                    }.first.toSquad()
            )
    }

    override fun assignSquad(
        squadKey: SquadKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers> =
        database.computeIfPresent(squadKey) { _, squadAndMissionIdsToMission ->
            val retrievedInMemorySquad = squadAndMissionIdsToMission.first
            val updatedInMemorySquad = retrievedInMemorySquad.copy(members = assignees.toHeroIds())
            Pair(updatedInMemorySquad, squadAndMissionIdsToMission.second)
        }
            ?.let { Right(it.first.toSquadMembers()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun areHeroesInSquad(
        heroIds: HeroIds,
        squadKey: SquadKey
    ): EitherNel<AreHeroesInSquadError, SquadMembers> =
        database[squadKey]
            ?.let {
                val squadMembers = it.first.toSquadMembers()
                return if (squadMembers.containsAll(heroIds))
                    Right(squadMembers)
                else
                    Left(nonEmptyListOf(HeroesNotInSquadError(heroIds, squadKey)))
            }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun updateSquadName(
        squadKey: SquadKey,
        name: Name
    ): EitherNel<UpdateSquadNameError, Squad> =
        database.computeIfPresent(squadKey) { _, squadAndMissionIdsToMission ->
            val inMemorySquad = squadAndMissionIdsToMission.first
            val updatedInMemorySquad = inMemorySquad.copy(name = name)
            Pair(updatedInMemorySquad, squadAndMissionIdsToMission.second)
        }
            ?.let { Right(it.first.toSquad()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))


    override fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad> =
        database[squadKey]
            ?.let { Right(it.first.toSquad()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers> =
        database[squadKey]
            ?.let { Right(it.first.toSquadMembers()) }
            ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))

    override fun pauseWork(inProgressMissionId: InProgressMissionId): EitherNel<PauseWorkError, PendingMission> =
        database
            .computeIfPresent(inProgressMissionId.squadKey) { _, squadAndMissionIdsToMission ->
                val inMemorySquad = squadAndMissionIdsToMission.first
                val rawMissionId = RawMissionId(inProgressMissionId)
                val mission = squadAndMissionIdsToMission.second[rawMissionId]
                if (mission?.type == MissionType.IN_PROGRESS) {
                    PendingMissionId(inProgressMissionId.squadKey, rawMissionId.value)
                        .map { inProgressMissionId ->
                            val pendingMission =
                                PendingMission(
                                    inProgressMissionId,
                                    mission.title,
                                    mission.description,
                                    mission.assignees
                                )
                            Pair(
                                inMemorySquad,
                                squadAndMissionIdsToMission.second.plus(rawMissionId to RawMission(pendingMission))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(buildMission(it, inProgressMissionId) as PendingMission) }
            ?: Left(nonEmptyListOf(MissionNotExistingError(inProgressMissionId)))

    override fun endWork(inProgressMissionId: InProgressMissionId): EitherNel<EndWorkError, DoneMission> =
        database
            .computeIfPresent(inProgressMissionId.squadKey) { _, squadAndMissionIdsToMission ->
                val inMemorySquad = squadAndMissionIdsToMission.first
                val rawMissionId = RawMissionId(inProgressMissionId)
                val mission = squadAndMissionIdsToMission.second[rawMissionId]
                if (mission?.type == MissionType.IN_PROGRESS) {
                    val squad = inMemorySquad.toSquad()
                    DoneMissionId(inProgressMissionId.squadKey, rawMissionId.value)
                        .map { doneMissionId ->
                            val doneMission =
                                DoneMission(
                                    doneMissionId,
                                    mission.title,
                                    mission.description
                                )
                            Pair(
                                inMemorySquad,
                                squadAndMissionIdsToMission.second.plus(rawMissionId to RawMission(doneMission))
                            )
                        }
                        .getOrNull()
                } else
                    null
            }
            ?.let { Right(buildMission(it, inProgressMissionId) as DoneMission) }
            ?: Left(nonEmptyListOf(MissionNotExistingError(inProgressMissionId)))

    companion object {
        const val NON_EXISTING_MISSION_ID: String = "nonExistingMission"
    }

}

data class RawMission(
    val type: MissionType,
    val title: Title,
    val description: Description,
    val assignees: HeroIds,
) {
    constructor(pendingMission: PendingMission) : this(
        MissionType.PENDING,
        pendingMission.title,
        pendingMission.description,
        pendingMission.assignees
    )

    constructor(inProgressMission: InProgressMission) : this(
        MissionType.IN_PROGRESS,
        inProgressMission.title,
        inProgressMission.description,
        inProgressMission.assignees
    )

    constructor(doneMission: DoneMission) : this(
        MissionType.DONE,
        doneMission.title,
        doneMission.description,
        doneMission.assignees
    )
}

enum class MissionType {
    PENDING,
    IN_PROGRESS,
    DONE
}

class RawMissionId(value: String) : AbstractStringValue(value) {
    constructor(missionId: MissionId) : this(missionId.value)
}
