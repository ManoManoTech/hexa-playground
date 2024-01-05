package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure

class Description private constructor(value: String) : AbstractStringValue(value) {

    companion object {
        const val MIN_LENGTH = 0
        const val MAX_LENGTH = 1024
        val EMPTY_DESCRIPTION = Description("")


        operator fun invoke(stringBetween0And1024Chars: String): Either<NonEmptyList<DescriptionError>, Description> =
            either {
                ensure(stringBetween0And1024Chars.length <= MAX_LENGTH) {
                    nonEmptyListOf(
                        TooLongTextException(
                            stringBetween0And1024Chars,
                            MAX_LENGTH
                        )
                    ) // TODO: see how to  consolidate
                }
                Description(stringBetween0And1024Chars)
            }
    }

    sealed interface DescriptionError : StringValueError

    data class TooLongTextException(val string: String, val maxLength: Int) : DescriptionError {
        override val message: String =
            "Description must be below $maxLength characters, got ${string.length} in $string"
    }
}