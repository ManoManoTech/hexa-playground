package org.hexastacks.heroesdesk.kotlin.mission

import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.user.HeroIds

sealed interface Mission<T : MissionId> {
    fun updateTitle(title: Title): Mission<out MissionId> = when (this) {
        is PendingMission -> copy(title = title)
        is InProgressMission -> copy(title = title)
        is DoneMission -> copy(title = title)
    }

    fun updateDescription(description: Description): Mission<out MissionId> = when (this) {
        is PendingMission -> copy(description = description)
        is InProgressMission -> copy(description = description)
        is DoneMission -> copy(description = description)
    }

    fun assign(assignees: HeroIds): Mission<out MissionId> = when (this) {
        is PendingMission -> copy(assignees = assignees)
        is InProgressMission -> copy(assignees = assignees)
        is DoneMission -> this
    }

    val missionId: T
    val title: Title
    val description: Description
    val assignees: HeroIds

    fun squadKey(): SquadKey = missionId.squadKey
}