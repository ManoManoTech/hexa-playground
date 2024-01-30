package org.hexastacks.heroesdesk.kotlin.errors

import org.hexastacks.heroesdesk.kotlin.misc.ErrorMessage
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.mission.InProgressMissionId
import org.hexastacks.heroesdesk.kotlin.mission.PendingMissionId
import org.hexastacks.heroesdesk.kotlin.mission.Mission
import org.hexastacks.heroesdesk.kotlin.mission.MissionId
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.user.Heroes

sealed interface HeroesDeskError : ErrorMessage

sealed interface CreateSquadError : HeroesDeskError
sealed interface AreHeroesInSquadError : HeroesDeskError, CreateMissionError, AssignMissionError, PauseWorkError, EndWorkError,UpdateTitleError

data class SquadNameAlreadyExistingError(val name: Name) : CreateSquadError {
    override val message = "Squad $name already exists"
}

data class SquadKeyAlreadyExistingError(val id: SquadKey) : CreateSquadError {
    override val message = "Squad $id already exists"
}

sealed interface AssignHeroesOnSquadError : HeroesDeskError

data class SquadNotExistingError(val squadKey: SquadKey) : GetSquadError, GetSquadMembersError {
    override val message = "Squad $squadKey does not exist"
}

sealed interface UpdateSquadNameError : HeroesDeskError

sealed interface GetSquadError : HeroesDeskError, AssignHeroesOnSquadError, UpdateSquadNameError, CreateMissionError,
    AreHeroesInSquadError

sealed interface GetSquadMembersError : HeroesDeskError

sealed interface CreateMissionError : HeroesDeskError

data class HeroesNotInSquadError(val heroIds: HeroIds, val squadKey: SquadKey) : CreateMissionError, EndWorkError,
    PauseWorkError, StartWorkError, AssignMissionError, AreHeroesInSquadError {
    constructor(heroId: HeroId, squadKey: SquadKey) : this(HeroIds(heroId), squadKey)
    constructor(heroes: Heroes, squadKey: SquadKey) : this(HeroIds(heroes), squadKey)

    override val message = "${heroIds} not in $squadKey squad"
}

sealed interface GetMissionError : HeroesDeskError, AssignMissionError, PauseWorkError, EndWorkError, UpdateTitleError,
    UpdateDescriptionError, AreHeroesInSquadError, StartWorkError

data class MissionNotExistingError(val missionId: MissionId) : GetMissionError {
    override val message = "Mission $missionId does not exist"
}

sealed interface UpdateTitleError : HeroesDeskError

sealed interface UpdateDescriptionError : HeroesDeskError

sealed interface EndWorkError : HeroesDeskError

data class MissionNotInProgressError(val mission: Mission<*>, val missionIdId: InProgressMissionId) : EndWorkError, PauseWorkError {
    override val message = "Mission $mission not a pending one, despite being $missionIdId"
}

sealed interface PauseWorkError : HeroesDeskError

sealed interface StartWorkError : HeroesDeskError

data class MissionNotPendingError(val mission: Mission<*>, val missionId: PendingMissionId) : StartWorkError {
    override val message = "Mission $mission not a pending one, despite being $missionId"
}

sealed interface AssignMissionError : HeroesDeskError, StartWorkError

sealed interface UserRepositoryError : HeroesDeskError
sealed interface GetHeroError : UserRepositoryError, AssignHeroesOnSquadError, CreateMissionError, UpdateTitleError,
    UpdateDescriptionError, AssignMissionError, PauseWorkError, EndWorkError

data class HeroesNotExistingError(val heroIds: HeroIds) : GetHeroError {
    override val message = "Heroes $heroIds do not exist"
}

sealed interface GetAdminError : UserRepositoryError, UpdateSquadNameError, CreateSquadError, AssignHeroesOnSquadError

data class AdminNotExistingError(val adminId: AdminId) : GetAdminError {
    override val message = "Admin $adminId does not exist"
}

data class MissionRepositoryError(
    override val message: String,
    val exception: Exception? = null,
    val error: ErrorMessage? = null
) : HeroesDeskError,
    CreateSquadError, GetSquadError, UpdateSquadNameError, GetSquadMembersError, CreateMissionError, GetMissionError {
    constructor(exception: Exception) : this(exception.message ?: "Unknown error", exception)
    constructor(error: ErrorMessage) : this(error.message, error = error)
}