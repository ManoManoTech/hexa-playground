package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.user.Heroes
import org.hexastacks.heroesdesk.kotlin.mission.*

interface MissionRepository {
    fun createSquad(squadKey: SquadKey, name: Name): EitherNel<CreateSquadError, Squad>

    fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad>
    fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers>

    fun updateSquadName(
        squadKey: SquadKey,
        name: Name
    ): EitherNel<UpdateSquadNameError, Squad>

    fun assignSquad(
        squadKey: SquadKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers>

    fun areHeroesInSquad(heroIds: HeroIds, squadKey: SquadKey): EitherNel<AreHeroesInSquadError, SquadMembers>
    fun areHeroesInSquad(heroIds: HeroIds, missionId: MissionId): EitherNel<AreHeroesInSquadError, Mission<*>> =
        either {
            val mission = getMission(missionId).bind()
            areHeroesInSquad(heroIds, mission.squadKey()).bind()
            mission
        }

    fun createMission(squadKey: SquadKey, title: Title): EitherNel<CreateMissionError, PendingMission>

    fun getMission(missionId: MissionId): EitherNel<GetMissionError, Mission<*>>

    fun updateTitle(
        missionId: MissionId,
        title: Title
    ): EitherNel<UpdateTitleError, Mission<*>>

    fun updateDescription(
        missionId: MissionId,
        description: Description
    ): EitherNel<UpdateDescriptionError, Mission<*>>

    fun assignMission(
        missionId: MissionId,
        assignees: HeroIds
    ): EitherNel<AssignMissionError, Mission<*>>

    fun startWork(pendingMissionId: PendingMissionId): EitherNel<StartWorkError, InProgressMission>

    fun pauseWork(inProgressMissionId: InProgressMissionId): EitherNel<PauseWorkError, PendingMission>

    fun endWork(inProgressMissionId: InProgressMissionId): EitherNel<EndWorkError, DoneMission>


}
