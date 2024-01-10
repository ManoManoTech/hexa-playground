package org.hexastacks.heroesdesk.kotlin.impl.user

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.StringValueError

class AdminId private constructor(value: String) : UserId, AbstractStringValue(value) {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36

        operator fun invoke(stringBetween1And36Chars: String): Either<NonEmptyList<AdminIdError>, AdminId> =
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
                    AdminId(stringBetween1And36Chars)
                }
            }
    }

    sealed interface AdminIdError : StringValueError
    data class BelowMinLengthError(val string: String) : AdminIdError {
        override val message: String = "AdminId must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : AdminIdError {
        override val message: String = "AdminId must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}