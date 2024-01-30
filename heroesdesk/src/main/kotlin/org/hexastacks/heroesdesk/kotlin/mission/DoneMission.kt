package org.hexastacks.heroesdesk.kotlin.mission

import org.hexastacks.heroesdesk.kotlin.user.HeroIds

data class DoneMission(
    override val missionId: DoneMissionId,
    override val title: Title,
    override val description: Description,
) : Mission<DoneMissionId> {
    override val assignees: HeroIds = HeroIds.empty
}
