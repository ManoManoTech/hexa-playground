package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.mission.Description
import org.hexastacks.heroesdesk.kotlin.mission.PendingMission
import org.hexastacks.heroesdesk.kotlin.mission.PendingMissionId
import org.hexastacks.heroesdesk.kotlin.mission.Title

class PendingMissionTest : AbstractMissionTest<PendingMissionId, PendingMission>() {
    override fun createMissionOrThrow(
        id: PendingMissionId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ) =
        PendingMission(id, title, description, assignees)

    override fun createMissionIdOrThrow(squadKey: SquadKey, missionId: String): PendingMissionId =
        PendingMissionId(squadKey, missionId).getOrElse { throw RuntimeException("$missionId should be valid") }

}