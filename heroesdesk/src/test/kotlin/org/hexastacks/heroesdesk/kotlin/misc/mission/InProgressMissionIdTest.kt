package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.mission.InProgressMissionId
import org.hexastacks.heroesdesk.kotlin.mission.MissionId
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey

class InProgressMissionIdTest : AbstractMissionIdTest<InProgressMissionId>() {

    override fun createMissionId(squadKey: SquadKey, value: String): EitherNel<MissionId.MissionIdError, InProgressMissionId> = InProgressMissionId(squadKey, value)

}