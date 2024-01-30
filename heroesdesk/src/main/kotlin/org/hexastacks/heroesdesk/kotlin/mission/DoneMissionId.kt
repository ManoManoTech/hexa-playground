package org.hexastacks.heroesdesk.kotlin.mission

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.mission.MissionId.*
import org.hexastacks.heroesdesk.kotlin.mission.MissionId.Companion.MAX_LENGTH
import org.hexastacks.heroesdesk.kotlin.mission.MissionId.Companion.MIN_LENGTH

class DoneMissionId private constructor(override val squadKey: SquadKey, override val value: String) : AbstractMissionId() {

    companion object {
        operator fun invoke(
            squadKey: SquadKey,
            stringBetween1And36Chars: String
        ): EitherNel<MissionIdError, DoneMissionId> =
            either {
                zipOrAccumulate(
                    {
                        ensure(stringBetween1And36Chars.length >= MIN_LENGTH) {
                            BelowMinLengthError(
                                stringBetween1And36Chars
                            )
                        }
                    },
                    {
                        ensure(stringBetween1And36Chars.length <= MAX_LENGTH) {
                            AboveMaxLengthError(
                                stringBetween1And36Chars
                            )
                        }
                    },
                ) { _, _ ->
                    DoneMissionId(squadKey, stringBetween1And36Chars)
                }
            }
    }
}