package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.mission.MissionId
import org.hexastacks.heroesdesk.kotlin.mission.PendingMissionId
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey

class PendingMissionIdTest : AbstractMissionIdTest<PendingMissionId>() {
    override fun createMissionId(squadKey: SquadKey, value: String): EitherNel<MissionId.MissionIdError, PendingMissionId> =
        PendingMissionId(squadKey, value)

}