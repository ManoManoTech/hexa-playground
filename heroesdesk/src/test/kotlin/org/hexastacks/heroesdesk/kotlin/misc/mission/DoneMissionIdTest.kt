package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.mission.MissionId.MissionIdError
import org.hexastacks.heroesdesk.kotlin.mission.DoneMissionId

class DoneMissionIdTest : AbstractMissionIdTest<DoneMissionId>() {
    override fun createMissionId(squadKey: SquadKey, value: String): EitherNel<MissionIdError, DoneMissionId> =
        DoneMissionId(squadKey, value)

}