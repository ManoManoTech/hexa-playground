package org.hexastacks.heroesdesk.kotlin.mission

import org.hexastacks.heroesdesk.kotlin.user.HeroIds

data class InProgressMission(
    override val missionId: InProgressMissionId,
    override val title: Title,
    override val description: Description,
    override val assignees: HeroIds
) : Mission<InProgressMissionId>