package org.hexastacks.heroesdesk.kotlin.impl.scope

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.StringValueError

class ScopeKey private constructor(value: String) : AbstractStringValue(value) {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36

        operator fun invoke(stringBetween1And36Chars: String): Either<NonEmptyList<ScopeIdError>, ScopeKey> =
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
                    ScopeKey(stringBetween1And36Chars)
                }
            }
    }

    sealed interface ScopeIdError : StringValueError
    data class BelowMinLengthError(val string: String) : ScopeIdError {
        override val message: String = "ScopeId must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : ScopeIdError {
        override val message: String = "ScopeId must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}