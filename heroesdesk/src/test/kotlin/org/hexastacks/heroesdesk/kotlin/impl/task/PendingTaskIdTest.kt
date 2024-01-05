package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel

class PendingTaskIdTest : AbstractTaskIdTest<PendingTaskId>() {
    override fun createStringValue(value: String): EitherNel<TaskId.TaskIdError, PendingTaskId> = PendingTaskId(value)

}