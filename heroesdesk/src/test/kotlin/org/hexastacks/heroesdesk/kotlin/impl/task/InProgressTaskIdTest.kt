package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel

class InProgressTaskIdTest : AbstractTaskIdTest<InProgressTaskId>() {
    override fun createStringValue(value: String): EitherNel<TaskId.TaskIdError, InProgressTaskId> =
        InProgressTaskId(value)

}