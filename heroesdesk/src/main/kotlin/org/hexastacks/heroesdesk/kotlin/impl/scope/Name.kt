package org.hexastacks.heroesdesk.kotlin.impl.scope

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.StringValueError
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId

class Name private constructor(value: String) : AbstractStringValue(value) {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 255

        operator fun invoke(stringBetween1And255Chars: String): Either<NonEmptyList<NameError>, Name> =
            either {
                zipOrAccumulate(
                    {
                        ensure(stringBetween1And255Chars.length >= MIN_LENGTH) {
                            BelowMinLengthNameError(
                                stringBetween1And255Chars
                            )
                        }
                    },
                    {
                        ensure(stringBetween1And255Chars.length <= MAX_LENGTH) {
                            AboveMaxLengthNameError(
                                stringBetween1And255Chars
                            )
                        }
                    },
                ) { _, _ ->
                    Name(stringBetween1And255Chars)
                }
            }
    }

    sealed interface NameError : StringValueError
    data class BelowMinLengthNameError(val string: String) : NameError {
        override val message: String =
            "Name must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthNameError(val string: String) : NameError {
        override val message: String =
            "Name must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}
