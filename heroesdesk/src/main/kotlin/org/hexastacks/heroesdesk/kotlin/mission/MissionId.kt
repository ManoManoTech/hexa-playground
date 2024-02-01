package org.hexastacks.heroesdesk.kotlin.mission

import org.hexastacks.heroesdesk.kotlin.misc.ValidationError
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey

sealed interface MissionId {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36
    }

    val squadKey: SquadKey
    val value: String

    sealed interface MissionIdError : ValidationError

    data class BelowMinLengthError(val string: String) : MissionIdError {
        override val message: String =
            "Mission id must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : MissionIdError {
        override val message: String =
            "Mission id must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}