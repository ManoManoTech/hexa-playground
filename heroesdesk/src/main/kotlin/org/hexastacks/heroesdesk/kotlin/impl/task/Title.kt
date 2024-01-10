package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValue
import org.hexastacks.heroesdesk.kotlin.impl.StringValueError
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId

class Title private constructor(value: String) : AbstractStringValue(value) {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 255

        operator fun invoke(stringBetween1And255Chars: String): Either<NonEmptyList<TitleError>, Title> =
            either {
                zipOrAccumulate(
                    // TODO: see how to  consolidate
                    {
                        ensure(stringBetween1And255Chars.length >= MIN_LENGTH) {
                            BelowMinLengthError(
                                stringBetween1And255Chars
                            )
                        }
                    },
                    {
                        ensure(stringBetween1And255Chars.length <= MAX_LENGTH) {
                            AboveMaxLengthError(
                                stringBetween1And255Chars
                            )
                        }
                    },
                ) { _, _ ->
                    Title(stringBetween1And255Chars)
                }
            }
    }

    sealed interface TitleError : StringValueError
    data class BelowMinLengthError(val string: String) : TitleError {
        override val message: String =
            "Title must be above ${MIN_LENGTH} characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : TitleError {
        override val message: String =
            "Title must be below ${MAX_LENGTH} characters, got ${string.length} in $string"
    }
}