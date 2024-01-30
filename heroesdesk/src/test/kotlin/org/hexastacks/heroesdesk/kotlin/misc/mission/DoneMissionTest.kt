package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.mission.Description
import org.hexastacks.heroesdesk.kotlin.mission.DoneMission
import org.hexastacks.heroesdesk.kotlin.mission.DoneMissionId
import org.hexastacks.heroesdesk.kotlin.mission.Title

class DoneMissionTest : AbstractMissionTest<DoneMissionId, DoneMission>() {
    override fun createMissionOrThrow(
        id: DoneMissionId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ): DoneMission =
        DoneMission(id, title, description)


    override fun createMissionIdOrThrow(squadKey: SquadKey, missionId: String): DoneMissionId =
        DoneMissionId(squadKey, missionId).getOrElse { throw RuntimeException("$missionId should be valid") }

}