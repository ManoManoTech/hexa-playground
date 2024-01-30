package org.hexastacks.heroesdesk.kotlin

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.mission.*

interface HeroesDesk {

    fun createSquad(squadKey: SquadKey, name: Name, creator: AdminId): EitherNel<CreateSquadError, Squad>
    fun assignSquad(
        squadKey: SquadKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers>

    fun updateSquadName(squadKey: SquadKey, name: Name, changeAuthor: AdminId): EitherNel<UpdateSquadNameError, Squad>
    fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad>
    fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers>

    fun createMission(squadKey: SquadKey, title: Title, creator: HeroId): EitherNel<CreateMissionError, PendingMission>
    fun getMission(id: MissionId): EitherNel<GetMissionError, Mission<*>>

    fun updateTitle(id: MissionId, title: Title, author: HeroId): EitherNel<UpdateTitleError, Mission<*>>
    fun updateDescription(
        id: MissionId, description: Description, author: HeroId
    ): EitherNel<UpdateDescriptionError, Mission<*>>

    fun assignMission(id: PendingMissionId, assignees: HeroIds, author: HeroId): EitherNel<AssignMissionError, Mission<*>>
    fun assignMission(id: InProgressMissionId, assignees: HeroIds, author: HeroId): EitherNel<AssignMissionError, Mission<*>>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(id: PendingMissionId, author: HeroId): EitherNel<StartWorkError, InProgressMission>

    /**
     * Adds the author to the assignees if not in already
     */
    fun startWork(id: DoneMissionId, author: HeroId): EitherNel<StartWorkError, InProgressMission>

    fun pauseWork(id: InProgressMissionId, author: HeroId): EitherNel<PauseWorkError, PendingMission>
    fun pauseWork(id: DoneMissionId, author: HeroId): EitherNel<PauseWorkError, PendingMission>

    /**
     * Clears assignees
     */
    fun endWork(id: PendingMissionId, author: HeroId): EitherNel<EndWorkError, DoneMission>

    /**
     * Clears assignees
     */
    fun endWork(id: InProgressMissionId, author: HeroId): EitherNel<EndWorkError, DoneMission>

}