package org.hexastacks.heroesdesk.kotlin.impl.user

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.StringValueError
import org.hexastacks.heroesdesk.kotlin.impl.user.UserId.Companion.MAX_LENGTH
import org.hexastacks.heroesdesk.kotlin.impl.user.UserId.Companion.MIN_LENGTH

class HeroId private constructor(value: String) : UserId, AbstractStringValue(value) {

    companion object {

        operator fun invoke(stringBetween1And36Chars: String): EitherNel<HeroIdError, HeroId> =
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
                    HeroId(stringBetween1And36Chars)
                }
            }
    }

    sealed interface HeroIdError : StringValueError
    data class BelowMinLengthError(val string: String) : HeroIdError {
        override val message: String = "HeroId must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : HeroIdError {
        override val message: String = "HeroId must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}