package org.hexastacks.heroesdesk.kotlin.misc

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.mission.*
import org.hexastacks.heroesdesk.kotlin.ports.MissionRepository
import org.hexastacks.heroesdesk.kotlin.ports.UserRepository


class HeroesDeskImpl(private val userRepository: UserRepository, private val missionRepository: MissionRepository) :
    HeroesDesk {
    override fun createSquad(squadKey: SquadKey, name: Name, creator: AdminId): EitherNel<CreateSquadError, Squad> =
        either {
            userRepository.getAdmin(creator).bind()
            missionRepository.createSquad(squadKey, name).bind()
        }

    override fun assignSquad(
        squadKey: SquadKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers> =
        either {
            val heroes = userRepository.getHeroes(assignees).bind()
            userRepository.getAdmin(changeAuthor).bind()
            missionRepository.assignSquad(squadKey, heroes).bind()
        }

    override fun updateSquadName(
        squadKey: SquadKey,
        name: Name,
        changeAuthor: AdminId
    ): EitherNel<UpdateSquadNameError, Squad> =
        either {
            userRepository.getAdmin(changeAuthor).bind()
            missionRepository.updateSquadName(squadKey, name).bind()
        }

    override fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad> =
        missionRepository.getSquad(squadKey)

    override fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers> =
        missionRepository.getSquadMembers(squadKey)

    override fun createMission(
        squadKey: SquadKey,
        title: Title,
        creator: HeroId
    ): EitherNel<CreateMissionError, PendingMission> =
        either {
            missionRepository.areHeroesInSquad(HeroIds(creator), squadKey).bind()
            missionRepository.createMission(squadKey, title).bind()
        }

    override fun getMission(id: MissionId): EitherNel<GetMissionError, Mission<*>> = missionRepository.getMission(id)

    override fun updateTitle(
        id: MissionId,
        title: Title,
        author: HeroId
    ): EitherNel<UpdateTitleError, Mission<*>> =
        either {
            missionRepository.areHeroesInSquad(HeroIds(author), id).bind()
            missionRepository.updateTitle(id, title).bind()
        }

    override fun updateDescription(
        id: MissionId,
        description: Description,
        author: HeroId
    ): EitherNel<UpdateDescriptionError, Mission<*>> =
        either {
            userRepository.getHero(author).bind()
            missionRepository.updateDescription(id, description).bind()
        }

    override fun assignMission(
        id: PendingMissionId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignMissionError, Mission<*>> =
        doAssignMission(id, assignees, author)

    override fun assignMission(
        id: InProgressMissionId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignMissionError, Mission<*>> =
        doAssignMission(id, assignees, author)

    private fun doAssignMission(
        id: MissionId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignMissionError, Mission<*>> =
        either {
            missionRepository.areHeroesInSquad(assignees + author, id.squadKey).bind()
            missionRepository.assignMission(id, assignees).bind()
        }

    override fun startWork(
        id: PendingMissionId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressMission> =
        either {
            val mission = missionRepository.areHeroesInSquad(HeroIds(author), id).bind()
            val verifiedMission = when (mission) {
                is PendingMission -> Right(mission)
                else -> Left(nonEmptyListOf(MissionNotPendingError(mission, id)))
            }.bind()
            if (verifiedMission.assignees.isNotEmpty()) {
                Right(verifiedMission)
            } else {
                Right(assignAuthorToMission(verifiedMission, author, id))
            }.bind()
            missionRepository.startWork(id).bind()
        }


    private fun assignAuthorToMission(
        verifiedMission: PendingMission,
        author: HeroId,
        id: PendingMissionId
    ): EitherNel<StartWorkError, Mission<*>> =
        missionRepository
            .assignMission(id, verifiedMission.assignees.add(author))

    override fun startWork(
        id: DoneMissionId,
        author: HeroId
    ): EitherNel<StartWorkError, InProgressMission> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(
        id: InProgressMissionId,
        author: HeroId
    ): EitherNel<PauseWorkError, PendingMission> =
        either {
            val mission = missionRepository.getMission(id).bind()
            val verifiedMission = when (mission) {
                is InProgressMission -> Right(mission)
                else -> Left(nonEmptyListOf(MissionNotInProgressError(mission, id)))
            }.bind()
            missionRepository.areHeroesInSquad(HeroIds(author), verifiedMission.squadKey()).bind()
            missionRepository.pauseWork(id).bind()
        }

    override fun pauseWork(
        id: DoneMissionId,
        author: HeroId
    ): EitherNel<PauseWorkError, PendingMission> {
        TODO("Not yet implemented")
    }

    override fun endWork(id: PendingMissionId, author: HeroId): EitherNel<EndWorkError, DoneMission> {
        TODO("Not yet implemented")
    }

    override fun endWork(
        id: InProgressMissionId,
        author: HeroId
    ): EitherNel<EndWorkError, DoneMission> =
        either {
            val mission = missionRepository.getMission(id).bind()
            val verifiedMission = when (mission) {
                is InProgressMission -> Right(mission)
                else -> Left(nonEmptyListOf(MissionNotInProgressError(mission, id)))
            }.bind()
            missionRepository.areHeroesInSquad(HeroIds(author), verifiedMission.squadKey()).bind()
            missionRepository.endWork(id).bind()
        }
}