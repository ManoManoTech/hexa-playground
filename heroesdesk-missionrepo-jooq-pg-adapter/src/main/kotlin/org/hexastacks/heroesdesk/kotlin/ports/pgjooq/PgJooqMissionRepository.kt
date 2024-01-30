package org.hexastacks.heroesdesk.kotlin.ports.pgjooq

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.user.Heroes
import org.hexastacks.heroesdesk.kotlin.mission.*
import org.hexastacks.heroesdesk.kotlin.ports.MissionRepository
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.Tables.*
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.enums.Missionstatus
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.SquadRecord
import org.jooq.DSLContext
import org.jooq.Record6
import org.jooq.Result
import org.jooq.exception.DataAccessException
import org.jooq.exception.IntegrityConstraintViolationException
import java.util.*

class PgJooqMissionRepository(private val dslContext: DSLContext) : MissionRepository {
    override fun createSquad(squadKey: SquadKey, name: Name): EitherNel<CreateSquadError, Squad> =
        try {
            val execute = dslContext.insertInto(SQUAD)
                .set(SQUAD.KEY, squadKey.value)
                .set(SQUAD.NAME, name.value)
                .execute()
            if (execute != 1)
                Left(nonEmptyListOf(MissionRepositoryError("Insert failed")))
            else
                Right(Squad(name, squadKey))
        } catch (e: DataAccessException) {
            if (e is IntegrityConstraintViolationException && e.message?.contains("""ERROR: duplicate key value violates unique constraint "${Keys.CHK_NAME_UNIQUE.name}"""") ?: false)
                Left(nonEmptyListOf(SquadNameAlreadyExistingError(name)))
            else if (e is IntegrityConstraintViolationException && e.message?.contains("""ERROR: duplicate key value violates unique constraint "${Keys.PK_SQUAD.name}"""") ?: false)
                Left(nonEmptyListOf(SquadKeyAlreadyExistingError(squadKey)))
            else
                Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad> =
        try {
            dslContext.selectFrom(SQUAD)
                .where(SQUAD.KEY.eq(squadKey.value))
                .fetchOneInto(SquadRecord::class.java)
                ?.let {
                    Name(it.name)
                        .mapLeft { errors ->
                            errors.map { error -> MissionRepositoryError(error) }
                        }
                        .map { name -> Squad(name, squadKey) }
                }
                ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers> =
        try {
            (if (isSquadNotExisting(squadKey)
            ) Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
            else dslContext.select(SQUAD_USER.USER_ID)
                .from(SQUAD_USER)
                .where(SQUAD_USER.SQUAD_KEY.eq(squadKey.value))
                .map { HeroId(it.value1()) }
                .toList()
                .let { either { it.bindAll() } }
                .mapLeft { errors ->
                    errors.map { error -> MissionRepositoryError(error) }
                }.map { heroIds -> SquadMembers(squadKey, HeroIds(heroIds)) })
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun updateSquadName(squadKey: SquadKey, name: Name): EitherNel<UpdateSquadNameError, Squad> =
        try {
            dslContext.update(SQUAD)
                .set(SQUAD.NAME, name.value)
                .where(SQUAD.KEY.eq(squadKey.value))
                .returning()
                .fetchOneInto(Squad::class.java)
                ?.let { Right(it) }
                ?: Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun assignSquad(
        squadKey: SquadKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers> =
        try {
            if (isSquadNotExisting(squadKey) // FIXME: if squad not existing then FK should make insert fail and thus no need for extra query
            ) Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
            else {
                dslContext.deleteFrom(SQUAD_USER)
                    .where(SQUAD_USER.SQUAD_KEY.eq(squadKey.value))
                    .execute()
                val nbUpdate = dslContext.insertInto(SQUAD_USER)
                    .columns(SQUAD_USER.SQUAD_KEY, SQUAD_USER.USER_ID)
                    .apply {
                        assignees.forEach { hero ->
                            values(squadKey.value, hero.id.value)
                        }
                    }
                    .execute()
                if (nbUpdate != assignees.size)
                    Left(nonEmptyListOf(MissionRepositoryError("Only $nbUpdate updated on ${assignees.size}")))
                else
                    Right(SquadMembers(squadKey, HeroIds(assignees.map { it.id })))
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    private fun isSquadNotExisting(squadKey: SquadKey) = !dslContext.fetchExists(
        dslContext.selectOne()
            .from(SQUAD)
            .where(SQUAD.KEY.eq(squadKey.value))
    )

    override fun areHeroesInSquad(
        heroIds: HeroIds,
        squadKey: SquadKey
    ): EitherNel<AreHeroesInSquadError, SquadMembers> =
        try {
            if (isSquadNotExisting(squadKey)
            ) Left(nonEmptyListOf(SquadNotExistingError(squadKey)))
            else {
                dslContext.select(SQUAD_USER.USER_ID)
                    .from(SQUAD_USER)
                    .where(
                        SQUAD_USER.SQUAD_KEY.eq(squadKey.value)
                            .and(SQUAD_USER.USER_ID.`in`(heroIds.value.map { it.value }))
                    )
                    .map { HeroId(it.value1()) }
                    .toList()
                    .let { either { it.bindAll() } }
                    .mapLeft { errors ->
                        errors.map { error -> MissionRepositoryError(error) }
                    }
                    .flatMap { fetchedHeroIds: List<HeroId> ->
                        if (fetchedHeroIds.size == heroIds.size)
                            Right(SquadMembers(squadKey, HeroIds(fetchedHeroIds)))
                        else
                            Left(
                                nonEmptyListOf(
                                    HeroesNotInSquadError(
                                        HeroIds(
                                            heroIds.value.filterNot { fetchedHeroIds.contains(it) }),
                                        squadKey
                                    )
                                )
                            )
                    }
            }
        } catch (e: DataAccessException) {
            if (e is IntegrityConstraintViolationException && e.message?.contains("ERROR: insert or update on table \"$MISSION_USER.name\" violates foreign key constraint \"${Keys.MISSION_USER__FK_MISSION}\"") ?: false)
                Left(nonEmptyListOf(HeroesNotInSquadError(heroIds, squadKey)))
            else
                Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun createMission(
        squadKey: SquadKey,
        title: Title
    ): EitherNel<CreateMissionError, PendingMission> =
        try {
            val id = UUID.randomUUID().toString()
            dslContext.insertInto(MISSION)
                .set(MISSION.ID, id)
                .set(MISSION.SQUAD_KEY, squadKey.value)
                .set(MISSION.TITLE, title.value)
                .set(MISSION.STATUS, Missionstatus.Pending)
                .returning()
                .fetchOneInto(MISSION)
                ?.let { mission ->
                    SquadKey(mission.squadKey)
                        .flatMap { dbSquadKey -> PendingMissionId(dbSquadKey, mission.id) }
                        .mapLeft { errors ->
                            errors.map { error -> MissionRepositoryError(error) }
                        }
                        .map { id ->
                            PendingMission(id, title)
                        }
                }
                ?: Left(nonEmptyListOf(MissionRepositoryError("Insert failed")))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun getMission(missionId: MissionId): EitherNel<GetMissionError, Mission<*>> =
        try {
            dslContext.select(MISSION.ID, MISSION.SQUAD_KEY, MISSION.TITLE, MISSION.DESCRIPTION, MISSION.STATUS, MISSION_USER.USER_ID)
                .from(MISSION)
                .leftJoin(MISSION_USER).on(MISSION.ID.eq(MISSION_USER.MISSION_ID))
                .where(MISSION.ID.eq(missionId.value))
                .fetch()
                .takeIf { it.isNotEmpty }
                ?.let { rawMissionAndUsers ->
                    buildMission(rawMissionAndUsers)
                }
                ?: Left(nonEmptyListOf(MissionNotExistingError(missionId)))
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    private fun buildMission(rawMissionAndUsers: Result<Record6<String, String, String, String, Missionstatus, String>>): Either<NonEmptyList<MissionRepositoryError>, Mission<out AbstractMissionId>> {
        val mission = rawMissionAndUsers.first()
        val rawSquadKey = mission[MISSION.SQUAD_KEY]
        val rawMissionId = mission[MISSION.ID]
        val rawTitle = mission[MISSION.TITLE]
        val rawDescription = mission[MISSION.DESCRIPTION]
        val missionStatus = mission.value5()
        val rawAssignees = rawMissionAndUsers
            .filter { it.value6() != null }
            .map { HeroId(it.value6()) }
            .let {
                either {
                    HeroIds(it.bindAll())
                }
            }

        return when (missionStatus) {
            Missionstatus.Pending -> either {
                val key = SquadKey(rawSquadKey).bind()
                val id = PendingMissionId(key, rawMissionId).bind()
                val title = Title(rawTitle).bind()
                val description = Description(rawDescription).bind()
                val assignees = rawAssignees.bind()
                PendingMission(id, title, description, assignees)
            }.mapLeft { errors ->
                errors.map { error -> MissionRepositoryError(error) }
            }

            Missionstatus.InProgress -> either {
                val key = SquadKey(rawSquadKey).bind()
                val id = InProgressMissionId(key, rawMissionId).bind()
                val title = Title(rawTitle).bind()
                val description = Description(rawDescription).bind()
                val assignees = rawAssignees.bind()
                InProgressMission(id, title, description, assignees)
            }.mapLeft { errors ->
                errors.map { error -> MissionRepositoryError(error) }
            }

            Missionstatus.Done -> either {
                val key = SquadKey(rawSquadKey).bind()
                val id = DoneMissionId(key, rawMissionId).bind()
                val title = Title(rawTitle).bind()
                val description = Description(rawDescription).bind()
                DoneMission(id, title, description)
            }.mapLeft { errors ->
                errors.map { error -> MissionRepositoryError(error) }
            }
        }
    }

    override fun updateTitle(
        missionId: MissionId,
        title: Title
    ): EitherNel<UpdateTitleError, Mission<*>> =
        try {
            val nbUpdated = dslContext.update(MISSION)
                .set(MISSION.TITLE, title.value)
                .where(MISSION.ID.eq(missionId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(MissionNotExistingError(missionId)))
            else
                getMission(missionId)
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun updateDescription(
        missionId: MissionId,
        description: Description
    ): EitherNel<UpdateDescriptionError, Mission<*>> =
        try {
            val nbUpdated = dslContext.update(MISSION)
                .set(MISSION.DESCRIPTION, description.value)
                .where(MISSION.ID.eq(missionId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(MissionNotExistingError(missionId)))
            else
                getMission(missionId)
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun assignMission(missionId: MissionId, assignees: HeroIds): EitherNel<AssignMissionError, Mission<*>> =
        try {
            dslContext.deleteFrom(MISSION_USER)
                .where(MISSION_USER.MISSION_ID.eq(missionId.value))
                .execute()
            val nbUpdate = dslContext.insertInto(MISSION_USER)
                .columns(MISSION_USER.MISSION_ID, MISSION_USER.USER_ID)
                .apply {
                    assignees.forEach { hero: HeroId ->
                        values(missionId.value, hero.value)
                    }
                }
                .execute()
            if (nbUpdate != assignees.size)
                Left(nonEmptyListOf(MissionRepositoryError("Only $nbUpdate updated on ${assignees.size}")))
            else
                getMission(missionId)
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun startWork(pendingMissionId: PendingMissionId): EitherNel<StartWorkError, InProgressMission> =
        try {
            val nbUpdated = dslContext.update(MISSION)
                .set(MISSION.STATUS, Missionstatus.InProgress)
                .where(MISSION.ID.eq(pendingMissionId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(MissionNotExistingError(pendingMissionId)))
            else {
                either {
                    val mission = getMission(pendingMissionId).bind()
                    if (mission is InProgressMission)
                        Right(mission).bind()
                    else
                        Left(
                            nonEmptyListOf(
                                MissionNotPendingError(
                                    mission,
                                    pendingMissionId
                                )
                            )
                        ).bind()
                }
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun pauseWork(inProgressMissionId: InProgressMissionId): EitherNel<PauseWorkError, PendingMission> =
        try {
            val nbUpdated = dslContext.update(MISSION)
                .set(MISSION.STATUS, Missionstatus.Pending)
                .where(MISSION.ID.eq(inProgressMissionId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(MissionNotExistingError(inProgressMissionId)))
            else {
                either {
                    val mission = getMission(inProgressMissionId).bind()
                    if (mission is PendingMission)
                        Right(mission).bind()
                    else
                        Left(
                            nonEmptyListOf(
                                MissionNotInProgressError(
                                    mission,
                                    inProgressMissionId
                                )
                            )
                        ).bind()
                }
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }

    override fun endWork(inProgressMissionId: InProgressMissionId): EitherNel<EndWorkError, DoneMission> =
        try {
            val nbUpdated = dslContext.update(MISSION)
                .set(MISSION.STATUS, Missionstatus.Done)
                .where(MISSION.ID.eq(inProgressMissionId.value))
                .execute()
            if (nbUpdated != 1)
                Left(nonEmptyListOf(MissionNotExistingError(inProgressMissionId)))
            else {
                either {
                    val mission = getMission(inProgressMissionId).bind()
                    if (mission is DoneMission)
                        Right(mission).bind()
                    else
                        Left(
                            nonEmptyListOf(
                                MissionNotInProgressError(
                                    mission,
                                    inProgressMissionId
                                )
                            )
                        ).bind()
                }
            }
        } catch (e: DataAccessException) {
            Left(nonEmptyListOf(MissionRepositoryError(e)))
        }
}