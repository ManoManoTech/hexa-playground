package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.mission.Description
import org.hexastacks.heroesdesk.kotlin.mission.InProgressMission
import org.hexastacks.heroesdesk.kotlin.mission.InProgressMissionId
import org.hexastacks.heroesdesk.kotlin.mission.Title

class InProgressMissionTest : AbstractMissionTest<InProgressMissionId, InProgressMission>() {
    override fun createMissionOrThrow(
        id: InProgressMissionId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ) =
        InProgressMission(id, title, description, assignees)

    override fun createMissionIdOrThrow(squadKey: SquadKey, missionId: String): InProgressMissionId =
        InProgressMissionId(squadKey, missionId).getOrElse { throw RuntimeException("$missionId should be valid") }

}