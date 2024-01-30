package org.hexastacks.heroesdesk.kotlin.squad

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.StringValueError

class SquadKey private constructor(value: String) : AbstractStringValue(value) {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36

        operator fun invoke(stringBetween1And36Chars: String): EitherNel<SquadKeyError, SquadKey> =
            either {
                zipOrAccumulate(
                    // TODO: see how to  consolidate
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
                    SquadKey(stringBetween1And36Chars)
                }
            }
    }

    sealed interface SquadKeyError : StringValueError
    data class BelowMinLengthError(val string: String) : SquadKeyError {
        override val message: String = "SquadKey must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : SquadKeyError {
        override val message: String = "SquadKey must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}