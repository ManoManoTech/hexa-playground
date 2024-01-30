package org.hexastacks.heroesdesk.kotlin.mission

import org.hexastacks.heroesdesk.kotlin.user.HeroIds

data class PendingMission(
    override val missionId: PendingMissionId,
    override val title: Title,
    override val description: Description = Description.EMPTY_DESCRIPTION,
    override val assignees: HeroIds = HeroIds.empty
) : Mission<PendingMissionId>