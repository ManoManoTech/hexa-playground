package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel

class DoneTaskIdTest : AbstractTaskIdTest<DoneTaskId>() {
    override fun createStringValue(value: String): EitherNel<TaskId.TaskIdError, DoneTaskId> = DoneTaskId(value)

}