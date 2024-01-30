package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId.*
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId.Companion.MAX_LENGTH
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId.Companion.MIN_LENGTH

class DoneTaskId private constructor(override val squadKey: SquadKey, override val value: String) : AbstractTaskId() {

    companion object {
        operator fun invoke(
            squadKey: SquadKey,
            stringBetween1And36Chars: String
        ): EitherNel<TaskIdError, DoneTaskId> =
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
                    DoneTaskId(squadKey, stringBetween1And36Chars)
                }
            }
    }
}